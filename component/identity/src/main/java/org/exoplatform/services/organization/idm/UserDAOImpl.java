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
package org.exoplatform.services.organization.idm;

import static org.exoplatform.services.organization.idm.EntityMapperUtils.ORIGINATING_STORE;

import java.text.DateFormat;
import java.util.*;

import org.exoplatform.services.log.LogLevel;
import org.picketlink.idm.api.*;
import org.picketlink.idm.api.query.UserQueryBuilder;
import org.picketlink.idm.impl.api.SimpleAttribute;
import org.picketlink.idm.impl.api.model.SimpleUser;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.*;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.externalstore.IDMExternalStoreService;

public class UserDAOImpl extends AbstractDAOImpl implements UserHandler {

  private static final String     LISTENER_EXECUTION_ERR_MSG = "Error executing %s on listener %s for user %s";

  public static final String      USER_PASSWORD        = EntityMapperUtils.USER_PASSWORD;

  public static final String      USER_PASSWORD_SALT   = "passwordSalt";

  public static final String      USER_FIRST_NAME      = EntityMapperUtils.USER_FIRST_NAME;

  public static final String      USER_LAST_NAME       = EntityMapperUtils.USER_LAST_NAME;

  public static final String      USER_DISPLAY_NAME    = EntityMapperUtils.USER_DISPLAY_NAME;

  public static final String      USER_EMAIL           = EntityMapperUtils.USER_EMAIL;

  public static final String      USER_CREATED_DATE    = EntityMapperUtils.USER_CREATED_DATE;

  public static final String      USER_LAST_LOGIN_TIME = EntityMapperUtils.USER_LAST_LOGIN_TIME;

  public static final String      USER_ORGANIZATION_ID = EntityMapperUtils.USER_ORGANIZATION_ID;

  public static final String      USER_ENABLED          = EntityMapperUtils.USER_ENABLED;

  public static final String      USER_PASSWORD_SALT128 = "passwordSalt128";


  public static final Set<String> USER_NON_PROFILE_KEYS;

  public static final DateFormat  dateFormat           = DateFormat.getInstance();

  static {
    Set<String> keys = new HashSet<String>();
    keys.add(USER_PASSWORD);
    keys.add(USER_PASSWORD_SALT);
    keys.add(USER_FIRST_NAME);
    keys.add(USER_LAST_NAME);
    keys.add(USER_DISPLAY_NAME);
    keys.add(USER_EMAIL);
    keys.add(USER_CREATED_DATE);
    keys.add(USER_LAST_LOGIN_TIME);
    keys.add(USER_ORGANIZATION_ID);
    keys.add(USER_ENABLED);
    keys.add(ORIGINATING_STORE);
    keys.add(USER_PASSWORD_SALT128);

    USER_NON_PROFILE_KEYS = Collections.unmodifiableSet(keys);
  }

  private List<UserEventListener> listeners_ = new ArrayList<UserEventListener>(3);

  public UserDAOImpl(PicketLinkIDMOrganizationServiceImpl orgService, PicketLinkIDMService idmService) {
    super(orgService, idmService);
  }

  public final List<UserEventListener> getUserEventListeners() {
    return Collections.unmodifiableList(listeners_);
  }

  public void addUserEventListener(UserEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    listeners_.add(listener);
  }

  public void removeUserEventListener(UserEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    listeners_.remove(listener);
  }

  public User createUserInstance() {
    return new UserImpl();
  }

  public User createUserInstance(String username) {
    return new UserImpl(username);
  }

  public void createUser(User user, boolean broadcast) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "createUser", new Object[] { "user", user, "broadcast", broadcast });
    }

    IdentitySession session = service_.getIdentitySession();
    if (broadcast) {
      preSave(user, true);
    }

    org.picketlink.idm.api.User plIDMUser = null;
    try {
      plIDMUser = session.getPersistenceManager().createUser(user.getUserName());
    } catch (Exception e) {
      handleException("Identity operation error: ", e);
    } finally {
      orgService.flush();
    }

    try {
      persistUserInfo(user, plIDMUser, session, true);
    } catch (Exception e) {
      // Workaround due to issues in Picketlink
      // 1. it has not support transaction for LDAP yet
      // 2. it use internal cache (infinispan) but this cache is not clear when
      // there is exception occurred
      try {
        session.getPersistenceManager().removeUser(user.getUserName(), true);
        throw e;
      } catch (Exception e2) {
        handleException("Can't remove user", e);
      }
    } finally {
      orgService.flush();
    }

    if (broadcast) {
      postSave(user, true);
    }

  }

  public void saveUser(User user, boolean broadcast) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "saveUser", new Object[] { "user", user, "broadcast", broadcast });
    }

    if (user != null && !user.isEnabled()) {
      throw new DisabledUserException(user.getUserName());
    }

    IdentitySession session = service_.getIdentitySession();
    if (broadcast) {
      preSave(user, false);
    }

    org.picketlink.idm.api.User plIDMUser = session.getPersistenceManager().findUser(user.getUserName());
    persistUserInfo(user, plIDMUser, session, false);

    if (broadcast) {
      postSave(user, false);
    }
  }

  @Override
  public User setEnabled(String userName, boolean enabled, boolean broadcast) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log,
                        LogLevel.TRACE,
                        "setEnabled",
                        new Object[] { "userName", userName, "enabled", enabled, "broadcast", broadcast });
    }

    IdentitySession session = service_.getIdentitySession();
    User foundUser = getPopulatedUser(userName, session, UserStatus.ANY);

    if (!disableUserActived()) {
      log.debug("disableUserActived option is set to FALSE, setEnabled method will be ignored");
      return foundUser;
    }

    if (foundUser == null || foundUser.isEnabled() == enabled) {
      return foundUser;
    }
    ((UserImpl) foundUser).setEnabled(enabled);
    if (broadcast)
      preSetEnabled(foundUser);

    AttributesManager am = session.getAttributesManager();

    if (enabled) {
      try {
        am.removeAttributes(userName, new String[] { USER_ENABLED });
      } catch (Exception e) {
        handleException("Cannot update enabled status for user: " + userName + "; ", e);
      } finally {
        orgService.flush();
      }
    } else {
      Attribute[] attrs = new Attribute[] { new SimpleAttribute(USER_ENABLED, String.valueOf(enabled)) };
      try {
        am.updateAttributes(userName, attrs);
      } catch (Exception e) {
        handleException("Cannot update enabled status for user: " + userName + "; ", e);
      } finally {
        orgService.flush();
      }
    }

    if (broadcast)
      postSetEnabled(foundUser);

    return foundUser;
  }

  public User removeUser(String userName, boolean broadcast) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "removeUser", new Object[] { "userName", userName, "broadcast", broadcast });
    }

    IdentitySession session = service_.getIdentitySession();

    org.picketlink.idm.api.User foundUser = null;

    try {
      foundUser = session.getPersistenceManager().findUser(userName);
    } catch (IllegalArgumentException e) {
      // Don't rethrow the exception to be compatible with other Org Service
      // implementations
      log.debug("Can NOT find any user with username is NULL");
    } catch (Exception e) {
      handleException("Cannot obtain user: " + userName + "; ", e);

    }

    if (foundUser == null) {
      return null;
    }

    User exoUser = getPopulatedUser(userName, session, UserStatus.ANY);

    if (broadcast) {
      preDelete(exoUser);
    }

    try {
      // Remove all memberships and profile first
      orgService.getMembershipHandler().removeMembershipByUser(userName, false);
      orgService.getUserProfileHandler().removeUserProfile(userName, false);
    } catch (Exception e) {
      handleException("Cannot cleanup user relationships: " + userName + "; ", e);
    }

    try {
      session.getPersistenceManager().removeUser(foundUser, true);
    } catch (Exception e) {
      handleException("Cannot remove user: " + userName + "; ", e);
    } finally {
      orgService.flush();
    }

    if (broadcast) {
      postDelete(exoUser);
    }
    return exoUser;
  }

  //
  public User findUserByName(String userName) throws Exception {
    return findUserByName(userName, UserStatus.ENABLED);
  }

  @Override
  public User findUserByName(String userName, UserStatus userStatus) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "findUserByName", new Object[] { "userName", userName, "userStatus", userStatus });
    }

    IdentitySession session = service_.getIdentitySession();
    User user = getPopulatedUser(userName, session, userStatus);

    if (log.isTraceEnabled()) {
      Tools.logMethodOut(log, LogLevel.TRACE, "findUserByName", user);
    }

    return user;
  }

  public LazyPageList<User> getUserPageList(int pageSize) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "getUserPagetList", new Object[] { "pageSize", pageSize });
    }

    UserQueryBuilder qb = service_.getIdentitySession().createUserQueryBuilder();
    boolean enabledOnly = filterDisabledUsersInQueries();
    if (enabledOnly) {
      qb = addEnabledUserFilter(qb);
    }
    return new LazyPageList<User>(new IDMUserListAccess(qb,
                                                        pageSize,
                                                        true,
                                                        countPaginatedUsers(),
                                                        enabledOnly ? UserStatus.ENABLED : UserStatus.DISABLED),
                                  pageSize);
  }

  public ListAccess<User> findAllUsers() throws Exception {
    return findAllUsers(UserStatus.ENABLED);
  }

  @Override
  public ListAccess<User> findAllUsers(UserStatus userStatus) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "findAllUsers", new Object[] { "userStatus", userStatus });
    }

    UserQueryBuilder qb = service_.getIdentitySession().createUserQueryBuilder();
    if (disableUserActived()) {
      switch (userStatus) {
      case DISABLED:
        if (filterDisabledUsersInQueries()) {
          qb = addDisabledUserFilter(qb);
        }
        break;
      case ANY:
        break;
      case ENABLED:
        if (filterDisabledUsersInQueries()) {
          qb = addEnabledUserFilter(qb);
        }
        break;
      }
    }
    return new IDMUserListAccess(qb, 20, !countPaginatedUsers(), countPaginatedUsers(), userStatus);
  }

  //
  public boolean authenticate(String username, String password) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "authenticate", new Object[] { "userName", username, "password", "****" });
    }

    boolean authenticated = false;

    User user = findUserByName(username, UserStatus.ANY);
    if (user != null) {
      if (log.isTraceEnabled()) {
        Tools.logMethodOut(log, LogLevel.TRACE, "authenticate", false);
      }

      if (!user.isEnabled()) {
        throw new DisabledUserException(username);
      }

      if (user.isInternalStore()) {
        authenticated = authenticateDB(user, password);
        if (authenticated) {
          return true;
        }
      }

    }

    return authenticateExternal(username, password);
  }

  public boolean authenticateExternal(String username, String password) throws Exception {
    ExoContainer currentContainer = getPortalContainer();
    IDMExternalStoreService externalStoreService = currentContainer.getComponentInstanceOfType(IDMExternalStoreService.class);
    if (externalStoreService == null || !externalStoreService.isEnabled()) {
      return false;
    } else {
      return externalStoreService.authenticate(username, password);
    }
  }

  //

  public boolean authenticateDB(User user, String password) throws Exception {
    boolean authenticated = false;

    if (orgService.getConfiguration().isPasswordAsAttribute()) {
      authenticated = user.getPassword().equals(password);
    } else {
      try {
        IdentitySession session = service_.getIdentitySession();
        org.picketlink.idm.api.User idmUser = session.getPersistenceManager().findUser(user.getUserName());

        authenticated = service_.getExtendedAttributeManager().validatePassword(idmUser, password);
      } catch (Exception e) {
        handleException("Cannot authenticate user: " + user.getUserName() + "; ", e);
      }
    }

    if (log.isTraceEnabled()) {
      Tools.logMethodOut(log, LogLevel.TRACE, "authenticate", authenticated);
    }

    return authenticated;
  }
  public LazyPageList findUsers(Query q) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "findUsers", new Object[] { "q", q });
    }

    ListAccess list = findUsersByQuery(q);

    return new LazyPageList(list, 20);
  }


  //
  public ListAccess<User> findUsersByQuery(Query q) throws Exception {
    return findUsersByQuery(q, UserStatus.ENABLED);
  }

  @Override
  public ListAccess<User> findUsersByQuery(Query q, UserStatus userStatus) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "findUsersByQuery", new Object[] { q, userStatus });
    }

    // if only condition is email which is unique then delegate to other method
    // as it will be more efficient
    if (q.getUserName() == null && q.getEmail() != null && q.getFirstName() == null && q.getLastName() == null) {
      final User uniqueUser = findUserByUniqueAttribute(USER_EMAIL, q.getEmail(), userStatus);

      if (uniqueUser != null) {
        return new ListAccess<User>() {
          public User[] load(int index, int length) throws Exception {
            return new User[] { uniqueUser };
          }

          public int getSize() throws Exception {
            return 1;
          }
        };
      } else if (!q.getEmail().contains("*")) {
        return new ListAccess<User>() {
          public User[] load(int index, int length) throws Exception {
            return new User[0];
          }

          public int getSize() throws Exception {
            return 0;
          }
        };
      }
    }

    // otherwise use PLIDM queries

    UserQueryBuilder qb = service_.getIdentitySession().createUserQueryBuilder();

    if (q.getUserName() != null) {
      // Process username
      String username = q.getUserName();
      if (!username.startsWith("*")) {
        username = "*" + username;
      }
      if (!username.endsWith("*")) {
        username = username + "*";
      }
      qb.idFilter(username);
    }
    if (q.getEmail() != null) {
      qb.attributeValuesFilter(USER_EMAIL, new String[] { q.getEmail() });
    }
    if (q.getFirstName() != null) {
      qb.attributeValuesFilter(USER_FIRST_NAME, new String[] { q.getFirstName() });
    }

    // TODO: from/to login date

    if (q.getLastName() != null) {
      qb.attributeValuesFilter(USER_LAST_NAME, new String[] { q.getLastName() });
    }

    if (disableUserActived()) {
      switch (userStatus) {
      case DISABLED:
        if (filterDisabledUsersInQueries()) {
          qb = addDisabledUserFilter(qb);
        }
        break;
      case ANY:
        break;
      case ENABLED:
        if (filterDisabledUsersInQueries()) {
          qb = addEnabledUserFilter(qb);
        }
        break;
      }
    }

    IDMUserListAccess list;
    if (q.getUserName() == null && q.getEmail() == null && q.getFirstName() == null && q.getLastName() == null) {
      list = new IDMUserListAccess(qb, 20, !countPaginatedUsers(), countPaginatedUsers(), userStatus);
    } else {
      list = new IDMUserListAccess(qb, 20, false, countPaginatedUsers(), userStatus);
    }

    return list;
  }

  public LazyPageList findUsersByGroup(String groupId) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "findUsersByGroup", new Object[] { "groupId", groupId });
    }

    return new LazyPageList(findUsersByGroupId(groupId), 20);
  }

  public User findUserByEmail(String email) throws Exception {
    return findUserByUniqueAttribute(USER_EMAIL, email, UserStatus.ENABLED);
  }

  public User findUserByUniqueAttribute(String attributeName, String attributeValue, UserStatus userStatus) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log,
                        LogLevel.TRACE,
                        "findUserByUniqueAttribute",
                        new Object[] { "findUserByUniqueAttribute", attributeName, attributeValue, userStatus });
    }

    IdentitySession session = service_.getIdentitySession();

    org.picketlink.idm.api.User plUser = null;

    try {
      plUser = session.getAttributesManager().findUserByUniqueAttribute(attributeName, attributeValue);
    } catch (Exception e) {
      handleException("Cannot find user by unique attribute: attrName=" + attributeName + ", attrValue=" + attributeValue + "; ",
                      e);

    }

    User user = null;

    if (plUser != null) {
      user = new UserImpl(plUser.getId());
      populateUser(user, session);

      if (disableUserActived() && !userStatus.matches(user.isEnabled())) {
        user = null;
      }
    }

    if (log.isTraceEnabled()) {
      Tools.logMethodOut(log, LogLevel.TRACE, "findUserByUniqueAttribute", user);
    }

    return user;
  }

  public ListAccess<User> findUsersByGroupId(String groupId) throws Exception {
    return findUsersByGroupId(groupId, UserStatus.ENABLED);
  }

  public ListAccess<User> findUsersByQuery(Query query, List<String> groupIds, UserStatus userStatus) throws Exception {

    UserQueryBuilder qb = service_.getIdentitySession().createUserQueryBuilder();

    List<org.picketlink.idm.api.Group> groups = new ArrayList<>();
    for (String groupId : groupIds) {
      try {
        org.picketlink.idm.api.Group group = orgService.getJBIDMGroup(groupId);
        if (group != null) {
          groups.add(group);
        }
      } catch (Exception e) {
        handleException("Cannot obtain group: " + groupId + "; ", e);
      }
    }

    qb.addRelatedGroups(groups);

    if (query.getUserName() != null) {
      String username = query.getUserName();
      if (!username.startsWith("*")) {
        username = "*" + username;
      }
      if (!username.endsWith("*")) {
        username = username + "*";
      }
      qb.idFilter(username);
    }
    if (query.getEmail() != null) {
      qb.attributeValuesFilter(USER_EMAIL, new String[] { query.getEmail() });
    }
    if (query.getFirstName() != null) {
      qb.attributeValuesFilter(USER_FIRST_NAME, new String[] { query.getFirstName() });
    }

    if (query.getLastName() != null) {
      qb.attributeValuesFilter(USER_LAST_NAME, new String[] { query.getLastName() });
    }

    if (disableUserActived()) {
      switch (userStatus) {
        case DISABLED:
          if (filterDisabledUsersInQueries()) {
            qb = addDisabledUserFilter(qb);
          }
          break;
        case ANY:
          break;
        case ENABLED:
          if (filterDisabledUsersInQueries()) {
            qb = addEnabledUserFilter(qb);
          }
          break;
      }
    }

    return new IDMUserListAccess(qb, 20, false, countPaginatedUsers(), userStatus);
  }

  @Override
  public ListAccess<User> findUsersByGroupId(String groupId, UserStatus userStatus) throws Exception {
    if (log.isTraceEnabled()) {
      Tools.logMethodIn(log, LogLevel.TRACE, "findUsersByGroupId", new Object[] { groupId, userStatus });
    }

    UserQueryBuilder qb = service_.getIdentitySession().createUserQueryBuilder();

    org.picketlink.idm.api.Group jbidGroup = null;
    try {
      jbidGroup = orgService.getJBIDMGroup(groupId);
    } catch (Exception e) {
      handleException("Cannot obtain group: " + groupId + "; ", e);

    }

    // As test case supposed, we should return empty list instead of Exception
    // if group is not exist.
    if (jbidGroup == null) {
      return new ListAccess<User>() {
        public User[] load(int index, int length) throws Exception {
          if (index > 0 || length > 0) {
            throw new IndexOutOfBoundsException("Try to access an empty list");
          }
          return new User[0];
        }

        public int getSize() throws Exception {
          return 0;
        }
      };
    }

    qb.addRelatedGroup(jbidGroup);

    if (disableUserActived()) {
      switch (userStatus) {
      case DISABLED:
        if (filterDisabledUsersInQueries()) {
          qb = addDisabledUserFilter(qb);
        }
        break;
      case ANY:
        break;
      case ENABLED:
        if (filterDisabledUsersInQueries()) {
          qb = addEnabledUserFilter(qb);
        }
        break;
      }
    }

    return new IDMUserListAccess(qb, 20, false, countPaginatedUsers(), userStatus);
  }


  //
  private void preSave(User user, boolean isNew) {
    for (UserEventListener listener : listeners_) {
      executeWithTransaction(() -> preSave(listener, user, isNew));
    }
  }

  private void postSave(User user, boolean isNew) {
    for (UserEventListener listener : listeners_) {
      executeWithTransaction(() -> postSave(listener, user, isNew));
    }
  }

  private void preDelete(User user) {
    for (UserEventListener listener : listeners_) {
      executeWithTransaction(() -> preDelete(listener, user));
    }
  }

  private void postDelete(User user) {
    for (UserEventListener listener : listeners_) {
      executeWithTransaction(() -> postDelete(listener, user));
    }
  }

  private void preSetEnabled(User user) {
    for (UserEventListener listener : listeners_) {
      executeWithTransaction(() -> preSetEnabled(listener, user));
    }
  }

  private void postSetEnabled(User user) throws Exception {
    for (UserEventListener listener : listeners_) {
      executeWithTransaction(() -> postSetEnabled(listener, user));
    }
  }

  public void persistUserInfo(User user, IdentitySession session, boolean isNew) throws Exception {
    persistUserInfo(user, null, session, isNew);
  }

  @SuppressWarnings("unchecked")
  public void persistUserInfo(User user,
                              org.picketlink.idm.api.User plIDMUser,
                              IdentitySession session,
                              boolean isNew) throws Exception {
    AttributesManager am = session.getAttributesManager();

    ArrayList attributes = new ArrayList();

    if (user.getCreatedDate() != null) {
      attributes.add(new SimpleAttribute(USER_CREATED_DATE, "" + user.getCreatedDate().getTime()));
    }
    if (user.getLastLoginTime() != null) {
      attributes.add(new SimpleAttribute(USER_LAST_LOGIN_TIME, "" + user.getLastLoginTime().getTime()));
    }
    if (user.getEmail() != null) {
      attributes.add(new SimpleAttribute(USER_EMAIL, user.getEmail()));
    }
    if (user.getFirstName() != null) {
      attributes.add(new SimpleAttribute(USER_FIRST_NAME, user.getFirstName()));
    }
    if (user.getLastName() != null) {
      attributes.add(new SimpleAttribute(USER_LAST_NAME, user.getLastName()));
    }
    if(!user.isEnabled()) {
      attributes.add(new SimpleAttribute(USER_ENABLED, Boolean.toString(user.isEnabled())));
    }

    // TODO: GTNPORTAL-2358 Change once displayName will be available as part of
    // Organization API
    if (user instanceof UserImpl) {
      UserImpl userImpl = (UserImpl) user;
      if (userImpl.getDisplayName() != null) {
        attributes.add(new SimpleAttribute(USER_DISPLAY_NAME, ((UserImpl) user).getDisplayName()));
      } else {
        removeDisplayNameIfNeeded(am, user);
      }
    } else {
      log.warn("User is of class " + user.getClass() + " which is not instanceof " + UserImpl.class);
    }

    if (user.getOrganizationId() != null) {
      attributes.add(new SimpleAttribute(USER_ORGANIZATION_ID, user.getOrganizationId()));
    }
    if (user instanceof UserImpl && ((UserImpl) user).getOriginatingStore() != null) {
      attributes.add(new SimpleAttribute(ORIGINATING_STORE, ((UserImpl) user).getOriginatingStore()));
    }
    if (user.getPassword() != null) {
      if (user instanceof UserImpl && !((UserImpl) user).isInternalStore()) {
        throw new IllegalStateException("User originating store is external, thus the password can't be changed");
      }
      if (orgService.getConfiguration().isPasswordAsAttribute()) {
        attributes.add(new SimpleAttribute(USER_PASSWORD, user.getPassword()));
      } else {
        try {
          if (plIDMUser == null) {
            plIDMUser = session.getPersistenceManager().findUser(user.getUserName());
          }
          am.updatePassword(plIDMUser, user.getPassword());
        } catch (Exception e) {
          handleException("Cannot update password: " + user.getUserName() + "; ", e);
        } finally {
          orgService.flush();
        }
      }
    }

    Attribute[] attrs = new Attribute[attributes.size()];
    attrs = (Attribute[]) attributes.toArray(attrs);

    try {
      am.updateAttributes(user.getUserName(), attrs);
    } catch (Exception e) {
      handleException("Cannot update attributes for user: " + user.getUserName() + "; ", e);
    } finally {
      orgService.flush();
    }

  }

  public User getPopulatedUser(String userName, IdentitySession session, UserStatus userStatus) throws Exception {
    org.picketlink.idm.api.User u = null;

    try {
      u = session.getPersistenceManager().findUser(userName);
    } catch (IllegalArgumentException e) {
      // Don't rethrow the exception to be compatible with other Org Service
      // implementations
      log.debug("Can NOT find any user with username is NULL");
    } catch (Exception e) {
      handleException("Cannot obtain user: " + userName + "; ", e);
    }

    if (u == null) {
      return null;
    }

    User user = new UserImpl(u.getId());
    populateUser(user, session);

    if (disableUserActived()) {
      return userStatus.matches(user.isEnabled()) ? user : null;
    } else {
      return user;
    }
  }

  public void populateUser(User user, IdentitySession session) throws Exception {
    AttributesManager am = session.getAttributesManager();

    Map<String, Attribute> attrs = null;

    try {
      attrs = am.getAttributes(new SimpleUser(user.getUserName()));
    } catch (Exception e) {

      handleException("Cannot obtain attributes for user: " + user.getUserName() + "; ", e);

    }
    if (attrs == null) {
      return;
    } else {
      EntityMapperUtils.populateUser(user, attrs);
      if (attrs.containsKey(USER_PASSWORD)) {
        user.setPassword(attrs.get(USER_PASSWORD).getValue().toString());
      }
    }
  }

  public PicketLinkIDMOrganizationServiceImpl getOrgService() {
    return orgService;
  }

  /**
   * Returns namespace to be used with integration cache
   *
   * @return
   */
  private String getCacheNS() {
    // TODO: refactor to remove cast. For now to avoid adding new config option
    // and share existing cache instannce
    // TODO: it should be there.
    return ((PicketLinkIDMServiceImpl) service_).getRealmName();
  }

  // Field displayName is not mandatory. We need to handle situation when user
  // deleted displayName, which had been set
  // previously.
  // We need to ask if current User has displayName set previously and if yes,
  // it needs to be removed.

  private void removeDisplayNameIfNeeded(AttributesManager am, User user) throws Exception {
    try {
      Attribute attr = am.getAttribute(user.getUserName(), USER_DISPLAY_NAME);
      if (attr != null) {
        am.removeAttributes(user.getUserName(), new String[] { USER_DISPLAY_NAME });
      }
    } catch (Exception e) {
      handleException("Cannot remove displayName attribute of user: " + user.getUserName() + "; ", e);
    } finally {
      orgService.flush();
    }
  }
  private UserQueryBuilder addEnabledUserFilter(UserQueryBuilder qb) throws Exception {
    return qb.attributeValuesFilter(USER_ENABLED, new String[] {});
  }

  private UserQueryBuilder addDisabledUserFilter(UserQueryBuilder qb) throws Exception {
    return qb.attributeValuesFilter(USER_ENABLED, new String[] { Boolean.FALSE.toString() });
  }

  private boolean countPaginatedUsers() {
    return orgService.getConfiguration().isCountPaginatedUsers();
  }

  private boolean filterDisabledUsersInQueries() {
    return orgService.getConfiguration().isFilterDisabledUsersInQueries();
  }

  private boolean disableUserActived() {
    return orgService.getConfiguration().isDisableUserActived();
  }

  private void preSave(UserEventListener listener, User user, boolean isNew) {
    try {
      listener.preSave(user, isNew);
    } catch (Exception e) {
      // throw the error if the operation shouldn't continue
      throw new IllegalStateException(String.format(LISTENER_EXECUTION_ERR_MSG,
                                                    "preSave",
                                                    listener.getClass().getName(),
                                                    user.getUserName()),
                                      e);
    }
  }

  private void postSave(UserEventListener listener, User user, boolean isNew) {
    try {
      listener.postSave(user, isNew);
    } catch (Exception e) {
      // Just log the error to not stop event propagation
      log.warn(String.format(LISTENER_EXECUTION_ERR_MSG,
                             "postSave",
                             listener.getClass().getName(),
                             user.getUserName()),
               e);
    }
  }

  private void preDelete(UserEventListener listener, User user) {
    try {
      listener.preDelete(user);
    } catch (Exception e) {
      // throw the error if the operation shouldn't continue
      throw new IllegalStateException(String.format(LISTENER_EXECUTION_ERR_MSG,
                                                    "preDelete",
                                                    listener.getClass().getName(),
                                                    user.getUserName()),
                                      e);
    }
  }

  private void postDelete(UserEventListener listener, User user) {
    try {
      listener.postDelete(user);
    } catch (Exception e) {
      // Just log the error to not stop event propagation
      log.warn(String.format(LISTENER_EXECUTION_ERR_MSG,
                             "postDelete",
                             listener.getClass().getName(),
                             user.getUserName()),
               e);
    }
  }

  private void preSetEnabled(UserEventListener listener, User user) {
    try {
      listener.preSetEnabled(user);
    } catch (Exception e) {
      // throw the error if the operation shouldn't continue
      throw new IllegalStateException(String.format(LISTENER_EXECUTION_ERR_MSG,
                                                    "preSetEnabled",
                                                    listener.getClass().getName(),
                                                    user.getUserName()),
                                      e);
    }
  }

  private void postSetEnabled(UserEventListener listener, User user) {
    try {
      listener.postSetEnabled(user);
    } catch (Exception e) {
      // Just log the error to not stop event propagation
      log.warn(String.format(LISTENER_EXECUTION_ERR_MSG,
                             "postSetEnabled",
                             listener.getClass().getName(),
                             user.getUserName()),
               e);
    }
  }

  public void executeWithTransaction(Runnable runnable) {
    int transactionCount = restartTransaction();
    ExoContainer portalContainer = getPortalContainer();
    if (transactionCount == 0) {
      orgService.startRequest(portalContainer);
    }
    try {
      runnable.run();
    } finally {
      if (transactionCount == 0 && orgService.isStarted(portalContainer)) {
        orgService.endRequest(portalContainer);
      }
    }
  }

  protected int restartTransaction() {
    ExoContainer portalContainer = getPortalContainer();
    int i = 0;
    // Close transactions until no encapsulated transaction
    while (orgService.isStarted(portalContainer)) {
      orgService.endRequest(portalContainer);
      i++;
    }
    // Restart transactions with the same number of encapsulations
    for (int j = 0; j < i; j++) {
      orgService.startRequest(portalContainer);
    }
    return i;
  }

  private ExoContainer getPortalContainer() {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    if (currentContainer == null || (currentContainer instanceof RootContainer)) {
      currentContainer = PortalContainer.getInstance();
    }
    return currentContainer;
  }

}
