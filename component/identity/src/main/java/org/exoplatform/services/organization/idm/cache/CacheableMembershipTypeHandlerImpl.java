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
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.cache.MembershipCacheKey;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.MembershipTypeDAOImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;

public class CacheableMembershipTypeHandlerImpl extends MembershipTypeDAOImpl {

  private final ExoCache<String, MembershipType>     membershipTypeCache;

  private final ExoCache<MembershipCacheKey, Object> membershipCache;

  private final FutureExoCache<String, MembershipType, Object> futureMembershipTypeCache;

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

  @SuppressWarnings("unchecked")
  public CacheableMembershipTypeHandlerImpl(OrganizationCacheHandler organizationCacheHandler,
                                            PicketLinkIDMOrganizationServiceImpl orgService,
                                            PicketLinkIDMService service) {
    super(orgService, service);
    this.membershipTypeCache = organizationCacheHandler.getMembershipTypeCache();
    futureMembershipTypeCache = new FutureExoCache<>(new Loader<String, MembershipType, Object>() {
      @Override
      public MembershipType retrieve(Object context, String key) throws Exception {
        disableCacheInThread.set(true);
        try {
          return findMembershipType(key);
        } finally {
          disableCacheInThread.set(false);
        }
      }
    }, membershipTypeCache);
    this.membershipCache = organizationCacheHandler.getMembershipCache();
  }

  /**
   * {@inheritDoc}
   */
  public MembershipType findMembershipType(String name) throws Exception {
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      MembershipType membershipType = futureMembershipTypeCache.get(null, name);
      return membershipType == null ? membershipType : ((MembershipTypeImpl) membershipType).clone();
    } else {
      return super.findMembershipType(name);
    }
  }

  /**
   * {@inheritDoc}
   */
  public Collection<MembershipType> findMembershipTypes() throws Exception {
    return super.findMembershipTypes();
  }

  /**
   * {@inheritDoc}
   */
  public MembershipType removeMembershipType(String name, boolean broadcast) throws Exception {
    MembershipType membershipType = null;
    disableCacheInThread.set(true);
    try {
      membershipType = super.removeMembershipType(name, broadcast);
      membershipTypeCache.remove(name);

      if (membershipType != null) {
        membershipCache.select(new ClearMembershipCacheByMembershipTypeSelector(name));
      }
    } finally {
      disableCacheInThread.set(false);
    }
    return membershipType;
  }

  @Override
  public MembershipType createMembershipType(MembershipType mt, boolean broadcast) throws Exception {
    mt = super.createMembershipType(mt, broadcast);
    membershipTypeCache.remove(mt.getName());
    return mt;
  }

  /**
   * {@inheritDoc}
   */
  public MembershipType saveMembershipType(MembershipType mt, boolean broadcast) throws Exception {
    disableCacheInThread.set(true);
    try {
      mt = super.saveMembershipType(mt, broadcast);
      membershipTypeCache.remove(mt.getName());
      return mt;
    } finally {
      disableCacheInThread.set(false);
    }
  }

  public void clearCache() {
    membershipTypeCache.clearCache();
  }

  public void disableCache() {
    disableCacheInThread.set(true);
  }

  public void enableCache() {
    disableCacheInThread.remove();
  }

  public static final class ClearMembershipCacheByMembershipTypeSelector
      implements CachedObjectSelector<MembershipCacheKey, Object> {
    private String membershipType;

    public ClearMembershipCacheByMembershipTypeSelector(String membershipType) {
      this.membershipType = membershipType;
    }

    @Override
    public void onSelect(ExoCache<? extends MembershipCacheKey, ? extends Object> cache,
                         MembershipCacheKey key,
                         ObjectCacheInfo<? extends Object> ocinfo) throws Exception {
      cache.remove(key);
    }

    @Override
    public boolean select(MembershipCacheKey key, ObjectCacheInfo<? extends Object> ocinfo) {
      Object obj = ocinfo.get();
      if (obj instanceof Membership) {
        Membership cachedMembership = (Membership) ocinfo.get();
        if (cachedMembership.getMembershipType().equals(membershipType)) {
          return true;
        }
      } else if (obj instanceof Collection) {
        // Delete all cached user's memberships when deleting a membershipType
        return true;
      }
      return false;
    }
  }
}
