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
package org.exoplatform.services.organization.idm.externalstore;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.idm.impl.repository.ExoFallbackIdentityStoreRepository;
import org.picketlink.idm.api.*;
import org.picketlink.idm.api.query.QueryBuilder;
import org.picketlink.idm.api.query.UserQueryBuilder;
import org.picketlink.idm.impl.api.IdentitySearchCriteriaImpl;
import org.picketlink.idm.impl.api.session.IdentitySessionImpl;
import org.picketlink.idm.impl.store.ldap.ExoLDAPIdentityStoreImpl;
import org.picketlink.idm.impl.store.ldap.LDAPIdentityStoreImpl;
import org.picketlink.idm.spi.repository.IdentityStoreRepository;
import org.picketlink.idm.spi.store.FeaturesMetaData;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.externalstore.IDMExternalStoreImportService;
import org.exoplatform.services.organization.externalstore.IDMExternalStoreService;
import org.exoplatform.services.organization.externalstore.model.IDMEntityType;
import org.exoplatform.services.organization.idm.*;

/**
 * External Store API implementation for Picket Link IDM based
 * OrganizationService implementation
 */
public class PicketLinkIDMExternalStoreService implements IDMExternalStoreService, Startable {
  public static final Log                                        LOG                           =
                                                                     ExoLogger.getLogger(PicketLinkIDMExternalStoreService.class);

  public static final IDMEntityType<org.picketlink.idm.api.User> IDMUSER                       =
                                                                         new IDMEntityType<>("Picket Link IDM User",
                                                                                             org.picketlink.idm.api.User.class);

  private IDMExternalStoreImportService                          externalStoreImportService;

  private PicketLinkIDMOrganizationServiceImpl                   organizationService;

  private ListenerService                                        listenerService;

  private PicketLinkIDMService                                   picketLinkIDMService;

  private ExoFallbackIdentityStoreRepository                     fallbackStoreRepository       = null;

  private boolean                                                updateInformationOnLogin      = true;
  private boolean                                                authorizeLogin      = true;

  private Set<IDMEntityType<?>>                                  entityTypes                   = Collections.emptySet();

  private Set<String>                                            externalMappedGroups          = null;

  /**
   * A Function to make operation exclusively on external store
   */
  private CheckedFunction<CheckedSupplier<?>, ?>                 executeOnExternalStoreFuntion = null;

  public PicketLinkIDMExternalStoreService(OrganizationService organizationService,
                                           PicketLinkIDMService picketLinkIDMService,
                                           ListenerService listenerService,
                                           InitParams params) {
    this.picketLinkIDMService = picketLinkIDMService;
    this.listenerService = listenerService;
    if (!(organizationService instanceof PicketLinkIDMOrganizationServiceImpl)) {
      LOG.info("OrganizationService implementation doesn't extend PicketLinkIDMOrganizationServiceImpl. External Store management is disabled.");
      return;
    }
    executeOnExternalStoreFuntion = (idmOperation) -> {
      boolean disableCache = !fallbackStoreRepository.isUseExternalStore();
      if (disableCache) {
        this.fallbackStoreRepository.setUseExternalStore(true);
        this.organizationService.setEnableCache(false);
      }
      try {
        return idmOperation == null ? null : idmOperation.get();
      } catch (Exception e) {
        throw new RuntimeException("Operation error on LDAP store", e);
      } finally {
        if (disableCache) {
          this.organizationService.setEnableCache(true);
          fallbackStoreRepository.setUseExternalStore(null);
        }
      }
    };

    this.organizationService = (PicketLinkIDMOrganizationServiceImpl) organizationService;
    if (params != null) {
      if (params.containsKey(UPDATE_USER_ON_LOGIN_PARAM)) {
        updateInformationOnLogin = Boolean.parseBoolean(params.getValueParam(UPDATE_USER_ON_LOGIN_PARAM).getValue());
      }
      if (params.containsKey(AUTHORIZE_LOGIN_PARAM)) {
        authorizeLogin = Boolean.parseBoolean(params.getValueParam(AUTHORIZE_LOGIN_PARAM).getValue());
      }
    }
  }

  @Override
  public void start() {
    if (this.organizationService == null) {
      return;
    }
    RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
    try {
      final IdentitySession identitySession = picketLinkIDMService.getIdentitySession();
      if (!(identitySession instanceof IdentitySessionImpl)) {
        LOG.warn("Can't configure external store, unrecognized IDM session type = " + identitySession.getClass());
        return;
      }
      IdentityStoreRepository identityStoreRepository = ((IdentitySessionImpl) identitySession).getSessionContext()
                                                                                               .getIdentityStoreRepository();
      if (!(identityStoreRepository instanceof ExoFallbackIdentityStoreRepository)) {
        LOG.info("No IDM external store was configured, store type = " + identityStoreRepository.getClass());
        return;
      }
      fallbackStoreRepository = ((ExoFallbackIdentityStoreRepository) identityStoreRepository);
      if (fallbackStoreRepository == null || !fallbackStoreRepository.hasExternalStore()) {
        fallbackStoreRepository = null;
        LOG.info("No IDM external store was configured with store type = " + identityStoreRepository.getClass());
        return;
      }

      organizationService.getConfiguration().setCountPaginatedUsers(true);
      organizationService.getConfiguration().setSkipPaginationInMembershipQuery(false);

      final Map<String, String> reversedGroupTypeMappings = getReversedGroupTypeMappings();

      // Check managed entity types
      computeManagedIDMEntityTypes(reversedGroupTypeMappings);

      // Create parent LDAP Groups if not existing
      initializeGroupTree(reversedGroupTypeMappings);

    } catch (Exception e) {
      LOG.error("Error while configuring external store", e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
  }

  @Override
  public boolean authenticate(String username, String password) throws Exception {
    checkEnabled();
    if (!authorizeLogin) {
      throw new IllegalStateException("LDAP Store does not authorize login");
    }

    checkManagedType(IDMEntityType.USER);

    final org.picketlink.idm.api.User idmUser = getEntity(IDMUSER, username);
    if (idmUser == null) {
      return false;
    }

    // Get user from internal IDM DB store
    boolean authenticated = false;
    authenticated = validatePassword(idmUser, password);
    if (authenticated) {
      LOG.trace("User '{}' authenticated successfully on external store", username);
      User user = getExternalStoreImportService().importEntityToInternalStore(IDMEntityType.USER,
                                                                              username,
                                                                              isUpdateInformationOnLogin(),
                                                                              false);
      if (user == null) {
        authenticated = false;
      } else {
        // Import user information from LDAP if some mandatory information is
        // missing
        if (!isUpdateInformationOnLogin() && (StringUtils.isBlank(user.getEmail()) || StringUtils.isBlank(user.getFirstName())
            || StringUtils.isBlank(user.getLastName()))) {
          user = getExternalStoreImportService().importEntityToInternalStore(IDMEntityType.USER, username, true, false);
        }
        if (StringUtils.isBlank(user.getFirstName())) {
          LOG.warn("User '{}' has empty firstName field coming from external store. This may cause some issues, please review IDM configuration",
                   username);
        }
        if (StringUtils.isBlank(user.getLastName())) {
          LOG.warn("User '{}' has empty lastName field coming from external store. This may cause some issues, please review IDM configuration",
                   username);
        }
        if (StringUtils.isBlank(user.getEmail())) {
          LOG.warn("User '{}' has empty email field coming from external store. This may cause some issues, please review IDM configuration",
                   username);
        }
        try {
          getListenerService().broadcast(USER_AUTHENTICATED_USING_EXTERNAL_STORE, this, user);
        } catch (Exception e) {
          LOG.warn("Error while triggering event on user authentication using external store", e);
        }

        if (isUpdateInformationOnLogin()) {
          try {
            getExternalStoreImportService().importEntityToInternalStore(IDMEntityType.USER_MEMBERSHIPS, username, true, getExternalStoreImportService().isUpdateDeletedUserMemberships());
          } catch (Exception e) {
            LOG.error("Error while importing user memberships '" + username + "' from external store", e);
          }
        }
      }
    }
    return authenticated;
  }

  @Override
  public ListAccess<String> getAllOfType(IDMEntityType<?> entityType, LocalDateTime sinceLastModified) throws Exception {
    if (entityType == null) {
      throw new IllegalArgumentException("Entity type is mandatory");
    }
    checkEnabled();
    checkManagedType(entityType);

    QueryBuilder queryBuilder = null;
    IdentitySearchCriteriaImpl searchCriteria = null;
    if (IDMEntityType.USER.equals(entityType)) {
      queryBuilder = picketLinkIDMService.getIdentitySession().createUserQueryBuilder();
      if (sinceLastModified != null) {
        String sinceLastModifiedString = LDAP_MODIFICATION_DATE_FORMAT.format(sinceLastModified);
        ((UserQueryBuilder) queryBuilder).attributeValuesFilter(ExoLDAPIdentityStoreImpl.MODIFICATION_DATE_SINCE,
                                                                new String[] { sinceLastModifiedString });
      }
    } else if (IDMEntityType.GROUP.equals(entityType)) {
      // No query builder could be used for roles, thus it will remain null and
      // the list access will retrieve the complete list independently from
      // index & length
      queryBuilder = null;

      searchCriteria = new IdentitySearchCriteriaImpl();
      if (sinceLastModified != null) {
        String sinceLastModifiedString = LDAP_MODIFICATION_DATE_FORMAT.format(sinceLastModified);
        searchCriteria.attributeValuesFilter(ExoLDAPIdentityStoreImpl.MODIFICATION_DATE_SINCE,
                                             new String[] { sinceLastModifiedString });
      }
    } else if (IDMEntityType.ROLE.equals(entityType)) {
      // No query builder could be used for roles, thus it will remain null and
      // the list access will retrieve the complete list independently from
      // index & length
      queryBuilder = null;
    }
    return new IDMExternalStoreListAccess(this,
                                          organizationService,
                                          picketLinkIDMService,
                                          entityType.getClassType(),
                                          queryBuilder,
                                          searchCriteria,
                                          getExternalMappedGroups());
  }

  @Override
  public ListAccess<User> getAllInternalUsers() throws Exception {
    ListAccess<User> allUsers = organizationService.getUserHandler().findAllUsers();
    if (allUsers instanceof IDMUserListAccess) {
      ((IDMUserListAccess) allUsers).setLoadUserAttributes(false);
    }
    return allUsers;
  }

  @Override
  public boolean isEntityPresent(IDMEntityType<?> entityType, Object entityId) throws Exception {
    if (entityType == null) {
      throw new IllegalArgumentException("entityType is mandatory");
    }
    if (entityId == null || StringUtils.isBlank(entityId.toString())) {
      throw new IllegalArgumentException("entityId is mandatory");
    }
    checkEnabled();
    checkManagedType(entityType);

    if (IDMUSER.equals(entityType) || IDMEntityType.USER.equals(entityType) || IDMEntityType.USER_PROFILE.equals(entityType)
        || IDMEntityType.USER_MEMBERSHIPS.equals(entityType)) {
      return getIDMUser(entityId.toString()) != null;
    } else if (IDMEntityType.GROUP_MEMBERSHIPS.equals(entityType) || IDMEntityType.GROUP.equals(entityType)) {
      return (Boolean) executeOnExternalStoreFuntion.apply(() -> {
        org.picketlink.idm.api.Group jbidGroup;
        try {
          jbidGroup = organizationService.getJBIDMGroup(entityId.toString());
          return jbidGroup != null;
        } catch (Exception e) {
          return false;
        }
      });
    } else if (IDMEntityType.ROLE.equals(entityType)) {
      return (Boolean) executeOnExternalStoreFuntion.apply(() -> {
        RoleType rt = picketLinkIDMService.getIdentitySession().getRoleManager().getRoleType(entityId.toString());
        return rt != null;
      });
    } else if (IDMEntityType.MEMBERSHIP.equals(entityType)) {
      return getMembership(entityId.toString()) != null;
    } else {
      LOG.warn("unrecognized entity type {}", entityType);
    }
    return false;
  }

  @Override
  public boolean isEntityModified(IDMEntityType<?> entityType, String username) throws Exception {
    if (!entityType.equals(IDMEntityType.USER)) {
      throw new UnsupportedOperationException("Entity type " + entityType.getClassType().getName()
          + " is not supported by this operation");
    }
    checkEnabled();
    checkManagedType(entityType);

    User user = organizationService.getUserHandler().findUserByName(username, UserStatus.ANY);
    org.picketlink.idm.api.User idmUser = getEntity(IDMUSER, username);
    if (user == null) {
      if (idmUser == null) {
        return false;
      } else {
        return true;
      }
    } else {
      if (idmUser == null || !user.isEnabled()) { // user is present inside eXo DB but not on external store, or user is present in both stores and is disabled
        return true;
      }
    }
    // Retrieve LDAP attributes
    Map<String, Attribute> attributes = getAttributes(idmUser);
    // Detect if LDAP attributes has changed comparing
    // to IDM internal DB attributes
    return EntityMapperUtils.populateUser(user, attributes, true);
  }

  @Override
  public <T> T getEntity(IDMEntityType<T> entityType, Object entityId) throws Exception {
    if (entityType == null) {
      throw new IllegalArgumentException("entityType is mandatory");
    }
    if (entityId == null || StringUtils.isBlank(entityId.toString())) {
      throw new IllegalArgumentException("entityId is mandatory");
    }
    checkEnabled();
    checkManagedType(entityType);

    if (!isEntityPresent(entityType, entityId)) {
      return null;
    }

    if (IDMUSER.equals(entityType)) {
      return entityType.getClassType().cast(getIDMUser(entityId.toString()));
    } else if (IDMEntityType.USER.equals(entityType)) {
      return entityType.getClassType().cast(executeOnExternalStoreFuntion.apply(() -> {
        User user = organizationService.getUserHandler().findUserByName(entityId.toString(), UserStatus.ANY);
        user.setOriginatingStore(OrganizationService.EXTERNAL_STORE);
        return user;
      }));
    } else if (IDMEntityType.USER_PROFILE.equals(entityType)) {
      return entityType.getClassType().cast(executeOnExternalStoreFuntion.apply(() -> {
        return organizationService.getUserProfileHandler().findUserProfileByName(entityId.toString());
      }));
    } else if (IDMEntityType.USER_MEMBERSHIPS.equals(entityType)) {
      return entityType.getClassType().cast(executeOnExternalStoreFuntion.apply(() -> {
        return new HashSet<>(organizationService.getMembershipHandler().findMembershipsByUser(entityId.toString()));
      }));
    } else if (IDMEntityType.GROUP_MEMBERSHIPS.equals(entityType))

    {
      return entityType.getClassType().cast(executeOnExternalStoreFuntion.apply(() -> {
        final Group group = organizationService.getGroupHandler().findGroupById(entityId.toString());
        if (group == null) {
          LOG.warn("Can't find group with id : " + entityId);
          return null;
        }
        ListAccess<Membership> membershipsByGroup = organizationService.getMembershipHandler().findAllMembershipsByGroup(group);
        if (membershipsByGroup == null) {
          return null;
        }
        int size = membershipsByGroup.getSize();
        if (size == 0) {
          return Collections.emptyList();
        }
        return Arrays.asList(membershipsByGroup.load(0, size));
      }));
    } else if (IDMEntityType.GROUP.equals(entityType)) {
      return entityType.getClassType().cast(executeOnExternalStoreFuntion.apply(() -> {
        Group group = organizationService.getGroupHandler().findGroupById(entityId.toString());
        group.setOriginatingStore(OrganizationService.EXTERNAL_STORE);
        return group;
      }));
    } else if (IDMEntityType.ROLE.equals(entityType)) {
      return entityType.getClassType().cast(executeOnExternalStoreFuntion.apply(() -> {
        return organizationService.getMembershipTypeHandler().findMembershipType(entityId.toString());
      }));
    } else if (IDMEntityType.MEMBERSHIP.equals(entityType)) {
      return entityType.getClassType().cast(getMembership(entityId.toString()));
    } else {
      LOG.warn("unrecognized entity type {}", entityType);
    }
    return null;
  }

  @Override
  public Set<IDMEntityType<?>> getManagedEntityTypes() {
    return entityTypes;
  }

  public Object executeOnExternalStore(CheckedSupplier<Object> funtionToExecute) throws Exception {
    return executeOnExternalStoreFuntion.apply(() -> {
      return funtionToExecute.get();
    });
  }

  @Override
  public boolean isEnabled() {
    return fallbackStoreRepository != null && fallbackStoreRepository.hasExternalStore();
  }

  @Override
  public boolean isUpdateInformationOnLogin() {
    return updateInformationOnLogin;
  }

  public void setUpdateInformationOnLogin(boolean updateInformationOnLogin) {
    this.updateInformationOnLogin = updateInformationOnLogin;
  }

  public OrganizationService getOrganizationService() {
    return organizationService;
  }

  public ListenerService getListenerService() {
    return listenerService;
  }

  public IDMExternalStoreImportService getExternalStoreImportService() {
    if (externalStoreImportService == null) {
      // Could not be injected using constructor because of cyclic dependency
      externalStoreImportService = ExoContainerContext.getCurrentContainer()
                                                      .getComponentInstanceOfType(IDMExternalStoreImportService.class);
    }
    return externalStoreImportService;
  }

  public ExoFallbackIdentityStoreRepository getFallbackStoreRepository() {
    return fallbackStoreRepository;
  }

  public Map<String, String> getReversedGroupTypeMappings() throws Exception {
    final Map<String, String> reversedGroupTypeMappings = new HashMap<>();
    Map<String, String> groupTypeMappings = organizationService.getConfiguration().getGroupTypeMappings();

    Set<String> externalMappedGroups = getExternalMappedGroups();
    if (externalMappedGroups != null && !externalMappedGroups.isEmpty()) {
      for (Entry<String, String> groupTypeMappingEntry : groupTypeMappings.entrySet()) {
        reversedGroupTypeMappings.put(groupTypeMappingEntry.getValue(), groupTypeMappingEntry.getKey());
      }
    }
    return reversedGroupTypeMappings;
  }

  public void initializeGroupTree(final Map<String, String> reversedGroupTypeMappings) throws Exception {
    Set<String> externalMappedGroups = getExternalMappedGroups();
    if (externalMappedGroups != null && !externalMappedGroups.isEmpty()) {
      for (String externalMappedGroup : externalMappedGroups) {
        String groupId = reversedGroupTypeMappings.get(externalMappedGroup);
        if (StringUtils.isBlank(groupId) || groupId.equals("/")) {
          // Root group is a fake group, thus, no need to create it
          continue;
        }

        groupId = groupId.replace("/*", "");

        Group group = organizationService.getGroupHandler().findGroupById(groupId);
        if (group == null) {
          group = getEntity(IDMEntityType.GROUP, groupId);
          if (group == null) {
            group = organizationService.getGroupHandler().createGroupInstance();
            String groupName = groupId.substring(groupId.lastIndexOf("/") + 1, groupId.length());
            group.setGroupName(groupName);
            group.setLabel(groupName);
            group.setId(groupId);

            Group parentGroup = null;
            if (groupId.lastIndexOf("/") != 0) {
              String parentId = groupId.substring(0, groupId.lastIndexOf("/"));
              parentGroup = organizationService.getGroupHandler().findGroupById(parentId);
              if (parentGroup == null) {
                throw new IllegalStateException("Can't initialize group '" + groupId + "' becaue parent '" + parentId
                    + "' doesn't exist");
              }
              group.setParentId(parentId);
            }
            LOG.info("Adding parent LDAP mapped groups : {}", groupId);
            organizationService.getGroupHandler().addChild(parentGroup, group, true);
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Attribute> getAttributes(final IdentityType identityType) throws Exception {
    return (Map<String, Attribute>) executeOnExternalStoreFuntion.apply(() -> {
      return picketLinkIDMService.getIdentitySession().getAttributesManager().getAttributes(identityType);
    });
  }

  private void checkManagedType(IDMEntityType<?> entityType) {
    if (getManagedEntityTypes() == null || !getManagedEntityTypes().contains(entityType)) {
      throw new IllegalStateException("Entity type " + entityType.getClassType().getName() + " is not managed");
    }
  }

  @SuppressWarnings("unchecked")
  private void computeManagedIDMEntityTypes(final Map<String, String> reversedGroupTypeMappings) throws Exception {
    entityTypes = (Set<IDMEntityType<?>>) executeOnExternalStoreFuntion.apply(() -> {
      FeaturesMetaData supportedFeatures = fallbackStoreRepository.getExternalIdentityStore().getSupportedFeatures();
      Set<String> supportedIdentityTypes = supportedFeatures.getSupportedIdentityObjectTypes();
      Set<String> supportedRelationshipTypes = supportedFeatures.getSupportedRelationshipTypes();
      if (supportedIdentityTypes == null || supportedIdentityTypes.isEmpty()) {
        return Collections.emptySet();
      }
      Set<IDMEntityType<?>> entityTypes = new HashSet<>();
      for (String supportedRelationshipType : supportedRelationshipTypes) {
        if (LDAPIdentityStoreImpl.MEMBERSHIP_TYPE.equals(supportedRelationshipType)) {
          entityTypes.add(IDMEntityType.MEMBERSHIP);
          entityTypes.add(IDMEntityType.USER_MEMBERSHIPS);
          entityTypes.add(IDMEntityType.GROUP_MEMBERSHIPS);
        } else if (LDAPIdentityStoreImpl.ROLE_TYPE.equals(supportedRelationshipType)) {
          entityTypes.add(IDMEntityType.ROLE);
        } else {
          LOG.warn("unrecognized relationship type {} " + supportedRelationshipType);
        }
      }
      for (String supportedIdentityType : supportedIdentityTypes) {
        if (supportedIdentityType.equals("USER")) {
          entityTypes.add(IDMEntityType.USER);
          entityTypes.add(IDMEntityType.USER_PROFILE);
          entityTypes.add(IDMUSER);
        } else if (supportedIdentityTypes.contains("GROUP")) {
          entityTypes.add(IDMEntityType.GROUP);
        } else if (reversedGroupTypeMappings.containsKey(supportedIdentityType)) {
          // Custom group mappings
          entityTypes.add(IDMEntityType.GROUP);
        } else {
          LOG.warn("Unrecognized identity type {}, please verify that you added the groups mappings in PicketLinkIDMOrganizationServiceImpl.groupTypeMappings parameter",
                   supportedIdentityType);
        }
      }
      return entityTypes;
    });
  }

  private Set<String> getExternalMappedGroups() throws Exception {
    if (externalMappedGroups != null) {
      return externalMappedGroups;
    }
    Set<String> supportedIdentityObjectTypes = new HashSet<>(fallbackStoreRepository.getExternalIdentityStore()
                                                                                    .getSupportedFeatures()
                                                                                    .getSupportedIdentityObjectTypes());
    supportedIdentityObjectTypes.remove("USER");
    externalMappedGroups = new HashSet<String>(supportedIdentityObjectTypes);
    return externalMappedGroups;
  }

  private Membership getMembership(String id) throws Exception {
    return IDMEntityType.MEMBERSHIP.getClassType().cast(executeOnExternalStoreFuntion.apply(() -> {
      return organizationService.getMembershipHandler().findMembership(id);
    }));
  }

  private org.picketlink.idm.api.User getIDMUser(String username) throws Exception {
    return IDMUSER.getClassType().cast(executeOnExternalStoreFuntion.apply(() -> {
      IdentitySession identitySession = picketLinkIDMService.getIdentitySession();
      return identitySession.getPersistenceManager().findUser(username);
    }));
  }

  private boolean validatePassword(final org.picketlink.idm.api.User idmUser, String password) throws Exception {
    return (Boolean) executeOnExternalStoreFuntion.apply(() -> picketLinkIDMService.getIdentitySession().getAttributesManager()
                                                                                   .validatePassword(idmUser, password));
  }

  private void checkEnabled() {
    if (!isEnabled()) {
      throw new IllegalStateException("LDAP Store is disabled");
    }
  }

  @FunctionalInterface
  public static interface CheckedFunction<T, R> {
    R apply(T t) throws Exception;
  }

  @FunctionalInterface
  public static interface CheckedSupplier<R> {
    R get() throws Exception;
  }
}
