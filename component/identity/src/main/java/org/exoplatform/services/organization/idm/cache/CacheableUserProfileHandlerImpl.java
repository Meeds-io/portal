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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.UserProfileDAOImpl;
import org.exoplatform.services.organization.impl.UserProfileImpl;

public class CacheableUserProfileHandlerImpl extends UserProfileDAOImpl {
  private UserProfile                         NULL_OBJECT          = createUserProfileInstance();

  /**
   * Used to avoid this problem 
   * 1/ Delete from cache
   * 2/ super.delete
   * 2.1 trigger preDelete listeners: the listener.findEntity => cache is populated again
   * 2.2 delete from Store
   * 2.3 trigger postDelete listeners => Error: when
   *    listener a listener calls findUserById, the entity is returned from cache
   */
  private final ThreadLocal<Boolean>          disableCacheInThread = new ThreadLocal<>();

  private final ExoCache<String, UserProfile> userProfileCache;

  private final FutureExoCache<String, UserProfile, UserProfileCacheOperationType> futureUserProfile;

  @SuppressWarnings("unchecked")
  public CacheableUserProfileHandlerImpl(OrganizationCacheHandler organizationCacheHandler,
                                         PicketLinkIDMOrganizationServiceImpl orgService,
                                         PicketLinkIDMService service) {
    super(orgService, service);
    this.userProfileCache = organizationCacheHandler.getUserProfileCache();
    futureUserProfile = new FutureExoCache<>(new Loader<String, UserProfile, UserProfileCacheOperationType>() {
      @Override
      public UserProfile retrieve(UserProfileCacheOperationType context, String key) throws Exception {
        disableCacheInThread.set(true);
        try {
          UserProfile userProfile = null;
          switch (context) {
          case PROFILE_BY_USERNAME_NO_NULL:
            userProfile = findUserProfileByName(key);
            break;
          case PROFILE_BY_USERNAME:
            userProfile = getProfile(key);
            break;
          default:
            throw new IllegalArgumentException("Unrecognized context value " + context);
          }
          return userProfile == null || userProfile.getUserInfoMap() == null || userProfile.getUserInfoMap().isEmpty() ? NULL_OBJECT : userProfile;
        } finally {
          disableCacheInThread.set(false);
        }
      }
    }, userProfileCache);
  }

  /**
   * {@inheritDoc}
   */
  public UserProfile findUserProfileByName(String userName) throws Exception {
    UserProfile userProfile = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      userProfile = futureUserProfile.get(UserProfileCacheOperationType.PROFILE_BY_USERNAME_NO_NULL, storedName(userName));
    } else {
      return super.findUserProfileByName(userName);
    }

    if (userProfile == null || userProfile == NULL_OBJECT
        || StringUtils.isBlank(userProfile.getUserName())
        || userProfile.getUserInfoMap() == null || userProfile.getUserInfoMap().isEmpty()) {
      userProfile = null;
    }
    return userProfile == null ? null : ((UserProfileImpl)userProfile).clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserProfile getProfile(String userName) {
    UserProfile userProfile = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      userProfile = futureUserProfile.get(UserProfileCacheOperationType.PROFILE_BY_USERNAME, userName);
    } else {
      userProfile = super.getProfile(userName);
    }

    if (userProfile == null || userProfile == NULL_OBJECT
        || StringUtils.isBlank(userProfile.getUserName())
        || userProfile.getUserInfoMap() == null || userProfile.getUserInfoMap().isEmpty()) {
      userProfile = null;
    }
    return userProfile == null ? null : ((UserProfileImpl)userProfile).clone();
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("rawtypes")
  public Collection findUserProfiles() throws Exception {
    return super.findUserProfiles();
  }

  /**
   * {@inheritDoc}
   */
  public UserProfile removeUserProfile(String userName, boolean broadcast) throws Exception {
    disableCacheInThread.set(true);
    UserProfile userProfile = null;
    try {
      userProfile = super.removeUserProfile(userName, broadcast);
      return userProfile;
    } finally {
      disableCacheInThread.set(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void saveUserProfile(UserProfile profile, boolean broadcast) throws Exception {
    disableCacheInThread.set(true);
    try {
      super.saveUserProfile(profile, broadcast);
    } finally {
      disableCacheInThread.set(false);
    }
  }

  @Override
  protected void onUserProfileWritten(String userName) {
    String key = storedNameToEvict(userName);
    orgService.runNowAndAfterTransaction(() -> {
      futureUserProfile.removeFuture(key);
      userProfileCache.remove(key);
    });
  }

  /**
   * The stored name of a written profile's user, or the name as given when the
   * store cannot be read: the write is already flushed, and its eviction must
   * not fail it
   */
  private String storedNameToEvict(String userName) {
    try {
      return storedName(userName);
    } catch (Exception e) {
      log.warn("Cannot read the user '{}' of a written profile, which is evicted under the name as given", userName, e);
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

  public void clearCache() {
    userProfileCache.clearCache();
  }

  /**
   * Evicts one profile, by its login, so that the next read reaches the
   * store
   *
   * @param userName the stored login
   */
  public void clearCache(String userName) {
    if (userName != null && !userName.isBlank()) {
      userProfileCache.remove(userName);
    }
  }

  public void disableCache() {
    disableCacheInThread.set(true);
  }

  public void enableCache() {
    disableCacheInThread.remove();
  }

  public enum UserProfileCacheOperationType {
    PROFILE_BY_USERNAME_NO_NULL, PROFILE_BY_USERNAME
  }
}
