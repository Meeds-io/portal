/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.services.organization.mock;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.DisabledUserException;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.idm.EntityMapperUtils;
import org.exoplatform.services.organization.idm.UserImpl;

public class InMemoryUserHandler implements UserHandler {

  private static final String      ERROR_BROADCASTING_EVENT_MESSAGE = "Error broadcasting event : {}";

  public static final String       USER_PASSWORD                    = EntityMapperUtils.USER_PASSWORD;

  public static final String       USER_PASSWORD_SALT               = "passwordSalt";

  public static final String       USER_FIRST_NAME                  = EntityMapperUtils.USER_FIRST_NAME;

  public static final String       USER_LAST_NAME                   = EntityMapperUtils.USER_LAST_NAME;

  public static final String       USER_DISPLAY_NAME                = EntityMapperUtils.USER_DISPLAY_NAME;

  public static final String       USER_EMAIL                       = EntityMapperUtils.USER_EMAIL;

  public static final String       USER_CREATED_DATE                = EntityMapperUtils.USER_CREATED_DATE;

  public static final String       USER_LAST_LOGIN_TIME             = EntityMapperUtils.USER_LAST_LOGIN_TIME;

  public static final String       USER_ORGANIZATION_ID             = EntityMapperUtils.USER_ORGANIZATION_ID;

  public static final String       USER_ENABLED                     = EntityMapperUtils.USER_ENABLED;

  public static final DateFormat   dateFormat                       = DateFormat.getInstance();

  private OrganizationService      organizationService;

  private List<UserEventListener>  userListeners                    = new ArrayList<>();

  private static Map<String, User> usersById                        = new ConcurrentHashMap<>();

  public InMemoryUserHandler(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  public final List<UserEventListener> getUserEventListeners() {
    // Clone list to avoid ConcurrentModificationException (event with Vector got it)
    return userListeners.stream().toList();
  }

  @Override
  public void addUserEventListener(UserEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    userListeners.add(listener);
  }

  @Override
  public void removeUserEventListener(UserEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    userListeners.remove(listener);
  }

  @Override
  public User createUserInstance() {
    return new UserImpl();
  }

  @Override
  public User createUserInstance(String username) {
    return new UserImpl(username);
  }

  @Override
  public void createUser(User user, boolean broadcast) {
    if (usersById.containsKey(user.getUserName())) {
      return;
    }
    saveUser(user, broadcast, true);
  }

  @Override
  public void saveUser(User user, boolean broadcast) throws DisabledUserException {
    if (user != null && !user.isEnabled()) {
      throw new DisabledUserException(user.getUserName());
    }
    saveUser(user, broadcast, false);
  }

  @Override
  public User setEnabled(String userName, boolean enabled, boolean broadcast) {
    if (!usersById.containsKey(userName)) {
      return null;
    }
    User user = usersById.get(userName);
    if (user.isEnabled() == enabled) {
      return getClonedUser(userName);
    }
    if (broadcast) {
      preSetEnabled(user);
    }

    ((UserImpl) user).setEnabled(enabled);

    if (broadcast) {
      postSetEnabled(user);
    }
    return getClonedUser(userName);
  }

  @Override
  public User removeUser(String userName, boolean broadcast) {
    User user = usersById.get(userName);
    if (broadcast) {
      preDelete(user);
    }

    // Remove all memberships and profile first
    getMembershipHandler().removeMembershipByUser(userName, false);
    getUserProfileHandler().removeUserProfile(userName, false);

    usersById.remove(userName);

    if (broadcast) {
      postDelete(user);
    }
    return user;
  }

  @Override
  public User findUserByName(String userName) {
    return findUserByName(userName, UserStatus.ENABLED);
  }

  @Override
  public User findUserByName(String userName, UserStatus userStatus) {
    if (!usersById.containsKey(userName)) {
      return null;
    }
    User user = getClonedUser(userName);
    return filterUserStatus(user, userStatus);
  }

  @Override
  public LazyPageList<User> getUserPageList(int pageSize) {
    return new LazyPageList<>(new InMemoryListAccess<>(usersById.values().stream().toList(), new User[0]),
                              pageSize);
  }

  public ListAccess<User> findAllUsers() {
    return findAllUsers(UserStatus.ENABLED);
  }

  @Override
  public ListAccess<User> findAllUsers(UserStatus userStatus) {
    List<User> users = usersById.values()
                                .stream()
                                .map(user -> filterUserStatus(user, userStatus))
                                .filter(Objects::nonNull)
                                .toList();
    return new InMemoryListAccess<>(users, new User[0]);
  }

  @Override
  public boolean authenticate(String username, String password) throws DisabledUserException {
    if (!usersById.containsKey(username)) {
      return false;
    }
    User user = usersById.get(username);
    if (!user.isEnabled()) {
      throw new DisabledUserException(username);
    }
    return StringUtils.equals(user.getPassword(), password);
  }

  @Override
  public LazyPageList<User> findUsers(Query q) {
    return new LazyPageList<>(findUsersByQuery(q), 20);
  }

  @Override
  public InMemoryListAccess<User> findUsersByQuery(Query q) {
    return findUsersByQuery(q, UserStatus.ENABLED);
  }

  @Override
  public InMemoryListAccess<User> findUsersByQuery(Query query, UserStatus userStatus) {
    List<User> users = usersById.values()
                                .stream()
                                .map(user -> filterUserStatus(user, userStatus))
                                .filter(Objects::nonNull)
                                .filter(user -> contains(user.getEmail(), query.getEmail())
                                                && contains(user.getFirstName(), query.getFirstName())
                                                && contains(user.getLastName(), query.getLastName())
                                                && contains(user.getUserName(), query.getUserName()))
                                .filter(Objects::nonNull)
                                .toList();
    return new InMemoryListAccess<>(users, new User[0]);
  }

  public LazyPageList<User> findUsersByGroup(String groupId) {
    InMemoryListAccess<User> users = findUsersByGroupId(groupId);
    return new LazyPageList<>(users, users.getSize());
  }

  public InMemoryListAccess<User> findUsersByGroupId(String groupId) {
    return findUsersByGroupId(groupId, UserStatus.ENABLED);
  }

  @Override
  public InMemoryListAccess<User> findUsersByGroupId(String groupId, UserStatus userStatus) {
    List<User> users = getMembershipHandler().findMembershipsByGroupId(groupId)
                                             .stream()
                                             .map(membership -> usersById.get(membership.getUserName()))
                                             .map(user -> filterUserStatus(user, userStatus))
                                             .filter(Objects::nonNull)
                                             .toList();
    return new InMemoryListAccess<>(users, new User[0]);
  }

  @Override
  public InMemoryListAccess<User> findUsersByQuery(Query query, List<String> groupIds, UserStatus userStatus) {
    List<User> users = usersById.values()
                                .stream()
                                .map(user -> filterUserStatus(user, userStatus))
                                .filter(user -> CollectionUtils.isEmpty(groupIds)
                                                || groupIds.stream()
                                                           .anyMatch(groupId -> !getMembershipHandler().findMembershipsByUserAndGroup(user.getUserName(),
                                                                                                                                      groupId)
                                                                                                       .isEmpty()))
                                .filter(Objects::nonNull)
                                .toList();
    return new InMemoryListAccess<>(users, new User[0]);
  }

  // TODO Not Overridden but used, should be part of API in core module !!!
  public User findUserByUniqueAttribute(String attributeName, String attributeValue, UserStatus userStatus) {
    InMemoryUserProfileHandler userProfileHandler = (InMemoryUserProfileHandler) organizationService.getUserProfileHandler();
    List<UserProfile> profiles = userProfileHandler.findUserProfiles()
                                                   .stream()
                                                   .filter(Objects::nonNull)
                                                   .filter(profile -> profile.getUserInfoMap() != null
                                                                      && profile.getUserInfoMap().containsKey(attributeName)
                                                                      && StringUtils.equals(profile.getUserInfoMap()
                                                                                                   .get(attributeName),
                                                                                            attributeValue))
                                                   .distinct()
                                                   .toList();
    User user = null;
    if (profiles.isEmpty()) {
      if (StringUtils.equals("userName", attributeName)) {
        user = usersById.values()
                        .stream()
                        .filter(existingUser -> StringUtils.equals(existingUser.getUserName(), attributeValue))
                        .findFirst()
                        .orElse(null);
      } else if (StringUtils.equals("email", attributeName)) {
        user = usersById.values()
                        .stream()
                        .filter(existingUser -> StringUtils.equalsIgnoreCase(existingUser.getEmail(), attributeValue))
                        .findFirst()
                        .orElse(null);
      }
    } else if (profiles.size() == 1) {
      user = usersById.get(profiles.get(0).getUserName());
    }
    return filterUserStatus(user, userStatus);
  }

  @Override
  public boolean isUpdateLastLoginTime() {
    return true;
  }

  private User filterUserStatus(User user, UserStatus userStatus) {
    if (user == null || userStatus == null) {
      return user;
    }
    switch (userStatus) {
    case ANY: {
      return user;
    }
    case ENABLED: {
      return user.isEnabled() ? user : null;
    }
    case DISABLED: {
      return user.isEnabled() ? null : user;
    }
    default:
      return null;
    }
  }

  private void saveUser(User user, boolean broadcast, boolean isNew) {
    if (broadcast) {
      preSave(user, isNew);
    }
    String userName = user.getUserName();
    if (StringUtils.isBlank(user.getPassword()) && usersById.containsKey(userName)) {
      // Preserve old password of user if not changed by current save
      user.setPassword(usersById.get(userName).getPassword());
    }
    usersById.put(userName, user);
    if (broadcast) {
      postSave(user, isNew);
    }
  }

  private void preSave(User user, boolean isNew) {
    getUserEventListeners().forEach(listener -> {
                             try {
                               listener.preSave(user, isNew);
                             } catch (RuntimeException e) {
                               throw e;
                             } catch (Exception e) {
                               throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}",
                                                                                                        listener.getClass()
                                                                                                                .getName()),
                                                               e);
                             }
                           });
  }

  private void postSave(User user, boolean isNew) {
    getUserEventListeners().forEach(listener -> {
                             try {
                               listener.postSave(user, isNew);
                             } catch (RuntimeException e) {
                               throw e;
                             } catch (Exception e) {
                               throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}",
                                                                                                        listener.getClass()
                                                                                                                .getName()),
                                                               e);
                             }
                           });
  }

  private void preDelete(User user) {
    getUserEventListeners().forEach(listener -> {
                             try {
                               listener.preDelete(user);
                             } catch (RuntimeException e) {
                               throw e;
                             } catch (Exception e) {
                               throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}",
                                                                                                        listener.getClass()
                                                                                                                .getName()),
                                                               e);
                             }
                           });
  }

  private void postDelete(User user) {
    getUserEventListeners().forEach(listener -> {
                             try {
                               listener.postDelete(user);
                             } catch (RuntimeException e) {
                               throw e;
                             } catch (Exception e) {
                               throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}",
                                                                                                        listener.getClass()
                                                                                                                .getName()),
                                                               e);
                             }
                           });
  }

  private void preSetEnabled(User user) {
    getUserEventListeners().forEach(listener -> {
                             try {
                               listener.preSetEnabled(user);
                             } catch (RuntimeException e) {
                               throw e;
                             } catch (Exception e) {
                               throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}",
                                                                                                        listener.getClass()
                                                                                                                .getName()),
                                                               e);
                             }
                           });
  }

  private void postSetEnabled(User user) {
    getUserEventListeners().forEach(listener -> {
                             try {
                               listener.postSetEnabled(user);
                             } catch (RuntimeException e) {
                               throw e;
                             } catch (Exception e) {
                               throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}",
                                                                                                        listener.getClass()
                                                                                                                .getName()),
                                                               e);
                             }
                           });
  }

  private InMemoryUserProfileHandler getUserProfileHandler() {
    return (InMemoryUserProfileHandler) organizationService.getUserProfileHandler();
  }

  private InMemoryMembershipHandler getMembershipHandler() {
    return (InMemoryMembershipHandler) organizationService.getMembershipHandler();
  }

  private boolean contains(String value, String queryString) {
    return StringUtils.isBlank(queryString) || StringUtils.contains(value, queryString.replace("*", ""));
  }

  private User getClonedUser(String userName) {
    return ObjectUtils.clone(usersById.get(userName));
  }

}
