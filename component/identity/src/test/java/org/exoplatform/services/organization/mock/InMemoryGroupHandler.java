/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.exoplatform.services.organization.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.ExtGroup;

public class InMemoryGroupHandler implements GroupHandler {

  private static final String                    ERROR_BROADCASTING_EVENT_MESSAGE = "Error broadcasting event : {}";

  private static final String                    ROOT_PARENT_ID                   = "";

  private static final String                    DOESN_T_EXISTS_MESSAGE           = " doesn't exists";

  private OrganizationService                    organizationService;

  private List<GroupEventListener>               groupListeners                   = new ArrayList<>();

  private static Map<String, Group>              groupsById                       = new HashMap<>();

  private static Map<String, Map<String, Group>> groupChildsById                  = new HashMap<>();

  public InMemoryGroupHandler(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  public void addGroupEventListener(GroupEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    groupListeners.add(listener);
  }

  public void removeGroupEventListener(GroupEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    groupListeners.remove(listener);
  }

  public final Group createGroupInstance() {
    return new ExtGroup();
  }

  public void createGroup(Group group, boolean broadcast) {
    addChild(null, group, broadcast);
  }

  public void addChild(Group parent, Group childGroup, boolean broadcast) {
    String parentId = parent == null ? ROOT_PARENT_ID : parent.getId();
    childGroup.setId(parentId + "/" + childGroup.getGroupName());
    childGroup.setParentId(parent == null ? ROOT_PARENT_ID : parentId);

    saveGroup(childGroup, broadcast);
  }

  @Override
  public void moveGroup(Group parentOriginGroup, Group parentTargetGroup, Group groupToMove) {
    if (!groupsById.containsKey(parentOriginGroup.getId())) {
      throw new IllegalArgumentException(parentOriginGroup.getId() + DOESN_T_EXISTS_MESSAGE);
    }
    if (!groupsById.containsKey(parentTargetGroup.getId())) {
      throw new IllegalArgumentException(parentTargetGroup.getId() + DOESN_T_EXISTS_MESSAGE);
    }
    if (!groupsById.containsKey(groupToMove.getId())) {
      throw new IllegalArgumentException(groupToMove.getId() + DOESN_T_EXISTS_MESSAGE);
    }
    if (!groupChildsById.containsKey(parentOriginGroup.getId())) {
      throw new IllegalArgumentException(parentOriginGroup.getId() + " doesn't have child groups");
    }
    if (!groupChildsById.get(parentOriginGroup.getId()).containsKey(groupToMove.getId())) {
      throw new IllegalArgumentException(groupToMove.getId() + "isn't a child group of " + parentOriginGroup.getId());
    }

    groupChildsById.get(parentOriginGroup.getId()).remove(groupToMove.getId());
    addChild(parentTargetGroup, groupToMove, false);
  }

  public void saveGroup(Group group, boolean broadcast) {
    if (broadcast) {
      preSave(group, broadcast);
    }

    groupsById.put(group.getId(), ObjectUtils.clone(group));
    if (group.getId().lastIndexOf("/") > 0) {
      String[] parts = group.getId().split("/");
      String parentId = StringUtils.join(Arrays.copyOfRange(parts, 0, parts.length - 1), "/");
      group.setParentId(parentId);
    } else {
      group.setParentId(ROOT_PARENT_ID);
    }
    groupChildsById.computeIfAbsent(group.getParentId(), key -> new HashMap<String, Group>())
                   .put(group.getId(), ObjectUtils.clone(group));

    if (broadcast) {
      postSave(group, broadcast);
    }
  }

  public Group removeGroup(Group group, boolean broadcast) {
    String groupId = group.getId();
    if (!groupsById.containsKey(groupId)) {
      throw new IllegalArgumentException(groupId + DOESN_T_EXISTS_MESSAGE);
    }
    if (broadcast) {
      preDelete(group);
    }

    removeGroup(groupId, broadcast);

    if (broadcast) {
      postDelete(group);
    }
    return group;
  }

  public Collection<Group> findGroupByMembership(String userName, String membershipType) {
    Collection<Membership> memberships = getMembershipHandler().findMembershipsByUserAndGroup(userName, membershipType);
    return memberships.stream()
                      .map(membership -> groupsById.get(membership.getGroupId()))
                      .filter(Objects::nonNull)
                      .map(ObjectUtils::clone)
                      .toList();
  }

  @Override
  public Collection<Group> resolveGroupByMembership(String userName, String membershipType) {
    Collection<Membership> memberships = getMembershipHandler()
                                                               .findMembershipsByUser(userName);
    boolean allMembershipTypes = StringUtils.equals("*", membershipType);
    return memberships.stream()
                      .map(membership -> allMembershipTypes || StringUtils.equals(membership.getMembershipType(),
                                                                                  membershipType) ? groupsById.get(membership.getGroupId())
                                                                                                  : null)
                      .filter(Objects::nonNull)
                      .map(ObjectUtils::clone)
                      .toList();
  }

  @Override
  public Group findGroupById(String groupId) {
    return ObjectUtils.clone(groupsById.get(groupId));
  }

  @Override
  public ListAccess<Group> findGroupChildren(Group parent, String keyword) {
    List<Group> childGroups = groupChildsById.computeIfAbsent(parent.getId(), key -> new HashMap<>())
                                             .values()
                                             .stream()
                                             .filter(group -> StringUtils.contains(group.getLabel(), keyword)
                                                 || StringUtils.contains(group.getGroupName(), keyword))
                                             .toList();
    return new InMemoryListAccess<>(childGroups, new Group[0]);
  }

  public Collection<Group> findGroups(Group parent) {
    return groupChildsById.computeIfAbsent(parent.getId(), key -> new HashMap<>())
                          .values()
                          .stream()
                          .map(ObjectUtils::clone)
                          .toList();
  }

  public Collection<Group> findGroupsOfUser(String userName) {
    Collection<Membership> memberships = getMembershipHandler().findMembershipsByUser(userName);
    return memberships.stream()
                      .map(membership -> groupsById.get(membership.getGroupId()))
                      .filter(Objects::nonNull)
                      .map(ObjectUtils::clone)
                      .toList();
  }

  @Override
  public Collection<Group> findGroupsOfUserByKeyword(String userName, String keyword,
                                                     String excludeParentGroup) {
    return findGroupsOfUserByKeyword(userName, keyword, Collections.singletonList(excludeParentGroup));
  }

  public Collection<Group> getAllGroups() {
    return groupsById.values().stream().map(ObjectUtils::clone).toList();
  }

  public ListAccess<Group> findGroupsByKeyword(String keyword) {
    List<Group> childGroups = getAllGroups().stream()
                                            .filter(group -> StringUtils.contains(group.getLabel(), keyword)
                                                || StringUtils.contains(group.getGroupName(), keyword))
                                            .toList();
    return new InMemoryListAccess<>(childGroups, new Group[0]);
  }

  @Override
  public Collection<Group> findAllGroupsByKeyword(String keyword, List<String> excludedGroupsParent) {
    return getAllGroups().stream()
                         .filter(group -> excludedGroupsParent.stream()
                                                              .noneMatch(groupId -> StringUtils.contains(group.getId(),
                                                                                                         groupId)))
                         .filter(group -> StringUtils.contains(group.getLabel(), keyword)
                             || StringUtils.contains(group.getGroupName(), keyword))
                         .toList();
  }

  @Override
  public Collection<Group> findGroupsOfUserByKeyword(String userName,
                                                     String keyword,
                                                     List<String> excludedGroupsParent) {
    return findGroupsOfUser(userName).stream()
                                     .filter(group -> excludedGroupsParent.stream()
                                                                          .noneMatch(groupId -> StringUtils.contains(group.getId(),
                                                                                                                     groupId)))
                                     .filter(group -> StringUtils.contains(group.getLabel(), keyword)
                                         || StringUtils.contains(group.getGroupName(), keyword))
                                     .toList();
  }

  @Override
  public void clearGroupCache(String groupId) {
    // Nothing to do
  }

  private void preSave(Group group, boolean isNew) {
    for (GroupEventListener listener : groupListeners) {
      try {
        listener.preSave(group, isNew);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void postSave(Group group, boolean isNew) {
    for (GroupEventListener listener : groupListeners) {
      try {
        listener.postSave(group, isNew);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void preDelete(Group group) {
    for (GroupEventListener listener : groupListeners) {
      try {
        listener.preDelete(group);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void postDelete(Group group) {
    for (GroupEventListener listener : groupListeners) {
      try {
        listener.postDelete(group);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private InMemoryMembershipHandler getMembershipHandler() {
    return (InMemoryMembershipHandler) organizationService.getMembershipHandler();
  }

  private void removeGroup(String groupId, boolean broadcast) {
    removeSubGroups(groupId, broadcast);

    groupsById.remove(groupId);
    getMembershipHandler().removeMembershipByGroup(groupId, broadcast);
  }

  private void removeSubGroups(String groupId, boolean broadcast) {
    Map<String, Group> childGroupsById = groupChildsById.get(groupId);
    if (childGroupsById != null) {
      Group[] childGroupsArray = childGroupsById.values().toArray(new Group[0]);
      for (Group childGroup : childGroupsArray) {
        removeGroup(childGroup.getId(), broadcast);
      }
      childGroupsById.remove(groupId);
    }
  }

}
