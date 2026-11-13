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
import org.exoplatform.services.organization.UserStatus;
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
      MembershipImpl asked = new MembershipImpl(id);
      Membership membership = (Membership) futureMembershipCache.get(MembershipCacheOperationType.MEMBERSHIP_BY_ID,
                                                                     new MembershipCacheKey(storedName(asked.getUserName()),
                                                                                            asked.getGroupId(),
                                                                                            asked.getMembershipType()));
      return membership == null ? null : (Membership) ((ExtendedCloneable) membership).clone();
    } else {
      return super.findMembership(id);
    }
  }

  @Override
  public Membership findMembershipByUserGroupAndType(String userName, String groupId, String type) throws Exception {
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      Membership membership = (Membership) futureMembershipCache.get(MembershipCacheOperationType.MEMBERSHIP_BY_ID,
                                                                     new MembershipCacheKey(storedName(userName), groupId, type));
      return membership == null ? null : (Membership) ((ExtendedCloneable) membership).clone();
    } else {
      return super.findMembershipByUserGroupAndType(userName, groupId, type);
    }
  }

  @Override
  public Collection<Membership> findMembershipsByUser(String userName) throws Exception {
    if (useCacheList && (disableCacheInThread.get() == null || !disableCacheInThread.get())) {
      MembershipCacheKey cacheKey = new MembershipCacheKey(storedName(userName), null, null);
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
    } finally {
      disableCacheInThread.remove();
    }
    return membership;
  }

  @Override
  public void saveMembership(Membership m, boolean broadcast) throws Exception {
    try {
      super.saveMembership(m, broadcast);
    } catch (Exception e) {
      // A failed write may have kept part of the membership, which the
      // request commits: the hook did not run
      evictAfterFailure(m, e);
      throw e;
    }
  }

  @Override
  public void linkMembership(User user, Group g, MembershipType mt, boolean broadcast) throws Exception {
    disableCacheInThread.set(true);
    try {
      super.linkMembership(user, g, mt, broadcast);
    } catch (Exception e) {
      // A failed write may have kept part of the membership, which the
      // request commits: the hook did not run
      if (user != null && g != null && mt != null) {
        MembershipImpl membership = new MembershipImpl();
        membership.setUserName(user.getUserName());
        membership.setGroupId(g.getId());
        membership.setMembershipType(mt.getName());
        evictAfterFailure(membership, e);
      }
      throw e;
    } finally {
      disableCacheInThread.remove();
    }
  }

  @Override
  public Collection<Membership> removeMembershipByUser(String username, boolean broadcast) throws Exception {
    Collection<Membership> memberships = null;
    disableCacheInThread.set(true);
    try {
      memberships = super.removeMembershipByUser(username, broadcast);
    } finally {
      disableCacheInThread.remove();
    }
    return memberships;
  }

  @Override
  protected void onMembershipWritten(Membership membership, boolean storedUserName) {
    evictMembership(membership, storedUserName);
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

  /**
   * Evicts a written membership and its user's list, now and once the IDM
   * transaction ends, the loads of them in flight included, so that none of
   * them caches what it read before the write
   */
  private void evictAfterFailure(Membership membership, Exception failure) {
    try {
      evictMembership(membership, false);
    } catch (RuntimeException e) {
      failure.addSuppressed(e);
    }
  }

  private void evictMembership(Membership membership, boolean storedUserName) {
    if (membership == null) {
      return;
    }
    String userName = storedUserName ? membership.getUserName() : storedNameToEvict(membership.getUserName());
    MembershipCacheKey key = new MembershipCacheKey(userName, membership.getGroupId(), membership.getMembershipType());
    MembershipCacheKey listKey = new MembershipCacheKey(userName, null, null);
    orgService.runNowAndAfterTransaction(() -> {
      futureMembershipCache.removeFuture(key);
      futureMembershipCache.removeFuture(listKey);
      membershipCache.remove(key);
      if (useCacheList) {
        membershipCache.remove(listKey);
      }
    });
  }

  /**
   * The stored name of a written membership's user, or the name as given when
   * the store cannot be read: the write is already flushed, and its eviction
   * must not fail it
   */
  private String storedNameToEvict(String userName) {
    try {
      return storedName(userName);
    } catch (Exception e) {
      log.warn("Cannot read the user '{}' of a written membership, which is evicted under the name as given", userName, e);
      return userName;
    }
  }

  /**
   * The user's name as stored, which keys the entries of this cache: a name
   * asked under another spelling, which the store finds too, would otherwise
   * key an entry that no write of the user evicts
   */
  private String storedName(String userName) throws Exception {
    if (userName == null) {
      return null;
    }
    User user = orgService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    return user == null ? userName : user.getUserName();
  }

  public enum MembershipCacheOperationType {
    MEMBERSHIP_BY_ID, MEMBERSHIPS_FOR_USER
  }
}
