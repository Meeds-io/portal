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
package org.exoplatform.services.organization.idm.cache;

import java.util.Collection;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.organization.ExtendedCloneable;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.cache.MembershipCacheKey;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.MembershipDAOImpl;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;

@SuppressWarnings("unchecked")
public class CacheableMembershipHandlerImpl extends MembershipDAOImpl {

  private final ExoCache<MembershipCacheKey, Object> membershipCache;

  private final FutureExoCache<MembershipCacheKey, Object, MembershipCacheOperationType> futureMembershipCache;

  /**
   * Used to avoid this problem 
   * 1/ Delete from cache
   * 2/ super.delete
   * 2.1 trigger preDelete listeners: the listener.findEntity => cache is populated again
   * 2.2 delete from Store
   * 2.3 trigger postDelete listeners => Error: when
   *    listener a listener calls findUserById, the entity is returned from cache
   */
  private final ThreadLocal<Boolean>                 disableCacheInThread = new ThreadLocal<>();

  private boolean                                    useCacheList;

  public CacheableMembershipHandlerImpl(OrganizationCacheHandler organizationCacheHandler,
                                        PicketLinkIDMOrganizationServiceImpl orgService,
                                        PicketLinkIDMService service,
                                        boolean useCacheList) {
    super(orgService, service);
    this.membershipCache = organizationCacheHandler.getMembershipCache();
    futureMembershipCache = new FutureExoCache<>(new Loader<MembershipCacheKey, Object, MembershipCacheOperationType>() {
      @Override
      public Object retrieve(MembershipCacheOperationType context, MembershipCacheKey key) throws Exception {
        disableCacheInThread.set(true);
        try {
          switch (context) {
          case MEMBERSHIP_BY_ID:
            return findMembershipByUserGroupAndType(key.getUserName(), key.getGroupId(), key.getType());
          case MEMBERSHIPS_FOR_USER:
            return findMembershipsByUser(key.getUserName());
          default:
            throw new IllegalArgumentException("context value " + context + " is not recognized");
          }
        } finally {
          disableCacheInThread.remove();
        }
      }
    }, membershipCache);
    this.useCacheList = useCacheList;
  }

  @Override
  public Membership findMembership(String id) throws Exception {
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      Membership membership = (Membership) futureMembershipCache.get(MembershipCacheOperationType.MEMBERSHIP_BY_ID,
                                                                     new MembershipCacheKey(new MembershipImpl(id)));
      return membership == null ? null : (Membership) ((ExtendedCloneable) membership).clone();
    } else {
      return super.findMembership(id);
    }
  }

  @Override
  public Membership findMembershipByUserGroupAndType(String userName, String groupId, String type) throws Exception {
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      Membership membership = (Membership) futureMembershipCache.get(MembershipCacheOperationType.MEMBERSHIP_BY_ID,
                                                                     new MembershipCacheKey(userName, groupId, type));
      return membership == null ? null : (Membership) ((ExtendedCloneable) membership).clone();
    } else {
      return super.findMembershipByUserGroupAndType(userName, groupId, type);
    }
  }

  @Override
  public Collection<Membership> findMembershipsByUser(String userName) throws Exception {
    if (useCacheList && (disableCacheInThread.get() == null || !disableCacheInThread.get())) {
      MembershipCacheKey cacheKey = new MembershipCacheKey(userName, null, null);
      return (Collection<Membership>) futureMembershipCache.get(MembershipCacheOperationType.MEMBERSHIPS_FOR_USER, cacheKey);
    } else {
      try {
        return super.findMembershipsByUser(userName);
      } finally {
        clearMembershipCache(userName);
      }
    }
  }

  @Override
  public Membership removeMembership(String id, boolean broadcast) throws Exception {
    Membership membership = null;
    disableCacheInThread.set(true);
    try {
      membership = super.removeMembership(id, broadcast);
      clearMembershipCache(membership);
    } finally {
      disableCacheInThread.remove();
    }
    return membership;
  }

  @Override
  public void saveMembership(Membership m, boolean broadcast) throws Exception {
    super.saveMembership(m, broadcast);
    clearMembershipCache(m);
  }

  @Override
  public void createMembership(Membership m, boolean broadcast) throws Exception {
    super.createMembership(m, broadcast);
    clearMembershipCache(m);
  }

  @Override
  public void linkMembership(User user, Group g, MembershipType mt, boolean broadcast) throws Exception {
    disableCacheInThread.set(true);
    try {
      super.linkMembership(user, g, mt, broadcast);
    } finally {
      if (user != null && g != null && mt != null) {
        futureMembershipCache.remove(new MembershipCacheKey(user.getUserName(), g.getId(), mt.getName()));
        clearMembershipCache(user.getUserName());
      }
      disableCacheInThread.remove();
    }
  }

  @Override
  public Collection<Membership> removeMembershipByUser(String username, boolean broadcast) throws Exception {
    Collection<Membership> memberships = null;
    disableCacheInThread.set(true);
    try {
      memberships = super.removeMembershipByUser(username, broadcast);
      for (Membership membership : memberships) {
        clearMembershipCache(membership);
      }
    } finally {
      disableCacheInThread.remove();
    }
    return memberships;
  }

  public void clearCache() {
    futureMembershipCache.clear();
  }

  public void disableCache() {
    disableCacheInThread.set(true);
  }

  public void enableCache() {
    disableCacheInThread.remove();
  }

  public void clearMembershipCache(Membership membership) {
    if (membership != null) {
      membershipCache.remove(new MembershipCacheKey(membership));
      clearMembershipCache(membership.getUserName());
    }
  }

  public void clearMembershipCache(String username) {
    if (useCacheList) {
      membershipCache.remove(new MembershipCacheKey(username, null, null));
    }
  }

  public enum MembershipCacheOperationType {
    MEMBERSHIP_BY_ID, MEMBERSHIPS_FOR_USER
  }
}
