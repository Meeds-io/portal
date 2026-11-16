/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.services.organization.listener;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.security.ConversationRegistry;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class GroupLinkGroupListener extends GroupEventListener {

  private static final int     PAGE_SIZE = 100;

  private static final Log     LOG       = ExoLogger.getLogger(GroupLinkGroupListener.class);

  private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
                                           Thread thread = new Thread(runnable, "group-link-invalidation");
                                           thread.setDaemon(true);
                                           return thread;
                                         });

  @Autowired
  private ConversationRegistry conversationRegistry;

  @Autowired
  private OrganizationService  organizationService;

  @PostConstruct
  public void setup() {
    this.organizationService.getGroupHandler().addGroupEventListener(this);
  }

  @Override
  public void linkGroups(NestedMembership nestedMembership) throws Exception {
    refreshNowAndAfterTransaction(nestedMembership);
  }

  @Override
  public void unlinkGroups(NestedMembership nestedMembership) throws Exception {
    refreshNowAndAfterTransaction(nestedMembership);
  }

  /**
   * Once more after the commit: a node that reads the memberships before it
   * would build the conversation again with the link as it was
   */
  private void refreshNowAndAfterTransaction(NestedMembership nestedMembership) {
    if (organizationService instanceof PicketLinkIDMOrganizationServiceImpl idmOrganizationService) {
      idmOrganizationService.runNowAndAfterTransaction(() -> refreshIdentitiesMemberships(nestedMembership));
    } else {
      refreshIdentitiesMemberships(nestedMembership);
    }
  }

  private void refreshIdentitiesMemberships(NestedMembership nestedMembership) {
    ExoContainer container = ExoContainerContext.getCurrentContainerIfPresent();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    CompletableFuture.runAsync(() -> runInContext(container,
                                                  classLoader,
                                                  () -> findMembers(nestedMembership).forEach(conversationRegistry::invalidateUser)),
                               executor)
                     .exceptionally(e -> {
                       LOG.warn("Cannot invalidate the conversations of the members of group {}",
                                nestedMembership.getNestedGroupId(),
                                e);
                       return null;
                     });
  }

  @PreDestroy
  public void shutdown() {
    executor.shutdown();
  }

  /**
   * Runs the action with the container and the class loader of the thread that
   * asked for it, which the threads of the executor do not hold.
   */
  private static void runInContext(ExoContainer container, ClassLoader classLoader, Runnable action) {
    Thread thread = Thread.currentThread();
    ClassLoader previousClassLoader = thread.getContextClassLoader();
    ExoContainer previousContainer = ExoContainerContext.getCurrentContainerIfPresent();
    ExoContainerContext.setCurrentContainer(container);
    thread.setContextClassLoader(classLoader);
    try {
      action.run();
    } finally {
      thread.setContextClassLoader(previousClassLoader);
      ExoContainerContext.setCurrentContainer(previousContainer);
    }
  }

  /**
   * The users of the linked group are read from the store, not from the
   * identities this node holds: a user signed in on another node only is
   * invalidated as well, on every node. The members of the groups linked into
   * it, at any depth, inherit the link too: they are read as well, whatever
   * their membership type. The read runs in a request of its own, since the
   * calling thread holds none.
   */
  private Set<String> findMembers(NestedMembership nestedMembership) {
    boolean begun = organizationService instanceof ComponentRequestLifecycle;
    if (begun) {
      RequestLifeCycle.begin((ComponentRequestLifecycle) organizationService);
    }
    try {
      Group group = organizationService.getGroupHandler().findGroupById(nestedMembership.getNestedGroupId());
      if (group == null) {
        return Collections.emptySet();
      }
      Set<String> userIds = new HashSet<>();
      addMembers(group, nestedMembership, userIds);
      Set<String> visited = new HashSet<>(Set.of(group.getId()));
      Deque<String> toVisit = new ArrayDeque<>(linkedInto(group.getId()));
      while (!toVisit.isEmpty()) {
        String groupId = toVisit.pop();
        Group linked = visited.add(groupId) ? organizationService.getGroupHandler().findGroupById(groupId) : null;
        if (linked != null) {
          addMembers(linked, null, userIds);
          toVisit.addAll(linkedInto(groupId));
        }
      }
      return userIds;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read the members of group " + nestedMembership.getNestedGroupId(), e);
    } finally {
      if (begun) {
        RequestLifeCycle.end();
      }
    }
  }

  /**
   * @param nestedMembership the link whose membership type filters the
   *          group's members, null to read them all
   */
  private void addMembers(Group group, NestedMembership nestedMembership, Set<String> userIds) throws Exception {
    ListAccess<Membership> memberships = organizationService.getMembershipHandler().findAllMembershipsByGroup(group);
    int size = memberships.getSize();
    for (int offset = 0; offset < size; offset += PAGE_SIZE) {
      Arrays.stream(memberships.load(offset, Math.min(PAGE_SIZE, size - offset)))
            .filter(membership -> nestedMembership == null
                                  || nestedMembership.isIncludeAllMembershipTypes()
                                  || membership.getMembershipType().equals(nestedMembership.getNestedMembershipType()))
            .map(Membership::getUserName)
            .forEach(userIds::add);
    }
  }

  /**
   * @return the groups linked into this one
   */
  private List<String> linkedInto(String groupId) {
    try {
      return organizationService.getGroupHandler()
                                .getNestedMemberships(groupId)
                                .stream()
                                .map(NestedMembership::getNestedGroupId)
                                .toList();
    } catch (UnsupportedOperationException e) {
      return Collections.emptyList();
    }
  }

}
