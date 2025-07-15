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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileEventListener;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.organization.impl.UserProfileImpl;

public class InMemoryUserProfileHandler implements UserProfileHandler {

  private static final String             ERROR_BROADCASTING_EVENT_MESSAGE = "Error broadcasting event : {}";

  private List<UserProfileEventListener>  profileListeners                 = new ArrayList<>();

  private static Map<String, UserProfile> profilesById                     = new HashMap<>();

  @Override
  public void addUserProfileEventListener(UserProfileEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    profileListeners.add(listener);
  }

  @Override
  public void removeUserProfileEventListener(UserProfileEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    profileListeners.remove(listener);
  }

  @Override
  public final UserProfile createUserProfileInstance() {
    return new UserProfileImpl();
  }

  @Override
  public UserProfile createUserProfileInstance(String userName) {
    return new UserProfileImpl(userName);
  }

  @Override
  public void saveUserProfile(UserProfile profile, boolean broadcast) {
    boolean isNew = profilesById.containsKey(profile.getUserName());
    if (broadcast) {
      preSave(profile, isNew);
    }
    profilesById.put(profile.getUserName(), ObjectUtils.clone(profile));
    if (broadcast) {
      postSave(profile, isNew);
    }
  }

  @Override
  public UserProfile removeUserProfile(String userName, boolean broadcast) {
    if (!profilesById.containsKey(userName)) {
      return null;
    }
    UserProfile profile = profilesById.get(userName);
    if (broadcast) {
      preDelete(profile);
    }

    profilesById.remove(userName);

    if (broadcast) {
      postDelete(profile);
    }
    return profile;
  }

  @Override
  public UserProfile findUserProfileByName(String userName) {
    return ObjectUtils.clone(profilesById.get(userName));
  }

  @Override
  public List<UserProfile> findUserProfiles() {
    return profilesById.values().stream().map(ObjectUtils::clone).toList();
  }

  private void preSave(UserProfile profile, boolean isNew) {
    for (UserProfileEventListener listener : profileListeners) {
      try {
        listener.preSave(profile, isNew);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void postSave(UserProfile profile, boolean isNew) {
    for (UserProfileEventListener listener : profileListeners) {
      try {
        listener.postSave(profile, isNew);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void preDelete(UserProfile profile) {
    for (UserProfileEventListener listener : profileListeners) {
      try {
        listener.preDelete(profile);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void postDelete(UserProfile profile) {
    for (UserProfileEventListener listener : profileListeners) {
      try {
        listener.postDelete(profile);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

}
