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
package org.picketlink.idm.impl.store.hibernate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.helper.Tools;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObject;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectAttribute;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectAttributeBinaryValue;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectCredential;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectCredentialBinaryValue;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectCredentialType;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectRelationship;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectRelationshipName;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectRelationshipType;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectType;
import org.picketlink.idm.impl.model.hibernate.HibernateRealm;
import org.picketlink.idm.impl.store.FeaturesMetaDataImpl;
import org.picketlink.idm.impl.types.SimpleIdentityObject;
import org.picketlink.idm.spi.configuration.IdentityStoreConfigurationContext;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectAttributeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectTypeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityStoreConfigurationMetaData;
import org.picketlink.idm.spi.configuration.metadata.RealmConfigurationMetaData;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectAttribute;
import org.picketlink.idm.spi.model.IdentityObjectCredential;
import org.picketlink.idm.spi.model.IdentityObjectCredentialType;
import org.picketlink.idm.spi.model.IdentityObjectRelationship;
import org.picketlink.idm.spi.model.IdentityObjectRelationshipType;
import org.picketlink.idm.spi.model.IdentityObjectType;
import org.picketlink.idm.spi.search.IdentityObjectSearchCriteria;
import org.picketlink.idm.spi.store.FeaturesMetaData;
import org.picketlink.idm.spi.store.IdentityObjectSearchCriteriaType;
import org.picketlink.idm.spi.store.IdentityStore;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;
import org.picketlink.idm.spi.store.IdentityStoreSession;

import org.exoplatform.services.organization.idm.EntityMapperUtils;

import jakarta.persistence.Tuple;

/**
 * @author Boleslaw Dawidowicz
 * @version : 0.1 $
 */
public class PatchedHibernateIdentityStoreImpl implements IdentityStore, Serializable {

  private static final String                                       ATTRIBUTES_NULL_MSG                            =
                                                                                        "attributes are null";

  private static final String                                       RELATIONSHIP_NOT_FOUND_MSG                     =
                                                                                               "Relationship name '%s' not present in the store";

  private static final String                                       NAME_IS_NULL_MSG                               =
                                                                                     "name is null";

  private static final String                                       ASC_ORDER                                      = " ASC";

  private static final String                                       DESC_ORDER                                     = " DESC";

  private static final String                                       TO_IDENTITY_OBJECT_PROP_NAME                   =
                                                                                                 "toIdentityObject";

  private static final String                                       FROM_IDENTITY_OBJECT_PROP_NAME                 =
                                                                                                   "fromIdentityObject";

  private static final String                                       TYPE_ID_PROP_NAME                              = "typeId";

  private static final String                                       REALM_PROP_NAME                                = "realm";

  private static final String                                       NAME_PROP_NAME                                 = "name";

  private static final String                                       TYPE_NAME_PROP_NAME                            = "typeName";

  private static final String                                       REALM_NAME_PROP_NAME                           = "realmName";

  private static final long                                         serialVersionUID                               =
                                                                                     -130355852189832805L;

  private static Logger                                             log                                            =
                                                                        Logger.getLogger(PatchedHibernateIdentityStoreImpl.class.getName());

  public static final String                                        HIBERNATE_SESSION_FACTORY_REGISTRY_NAME        =
                                                                                                            "hibernateSessionFactoryRegistryName";

  public static final String                                        HIBERNATE_CONFIGURATION                        =
                                                                                            "hibernateConfiguration";

  public static final String                                        ADD_HIBERNATE_MAPPINGS                         =
                                                                                           "addHibernateMappings";

  public static final String                                        HIBERNATE_SESSION_FACTORY_JNDI_NAME            =
                                                                                                        "hibernateSessionFactoryJNDIName";

  public static final String                                        POPULATE_MEMBERSHIP_TYPES                      =
                                                                                              "populateRelationshipTypes";

  public static final String                                        POPULATE_IDENTITY_OBJECT_TYPES                 =
                                                                                                   "populateIdentityObjectTypes";

  public static final String                                        IS_REALM_AWARE                                 =
                                                                                   "isRealmAware";

  public static final String                                        MANAGE_TRANSACTION_DURING_BOOTSTRAP            =
                                                                                                        "manageTransactionDuringBootstrap";

  public static final String                                        ALLOW_NOT_DEFINED_ATTRIBUTES                   =
                                                                                                 "allowNotDefinedAttributes";

  public static final String                                        ALLOW_NOT_DEFINED_IDENTITY_OBJECT_TYPES_OPTION =
                                                                                                                   "allowNotDefinedIdentityObjectTypes";

  public static final String                                        ALLOW_NOT_CASE_SENSITIVE_SEARCH                =
                                                                                                    "allowNotCaseSensitiveSearch";

  public static final String                                        LAZY_START_OF_HIBERNATE_TRANSACTION            =
                                                                                                        "lazyStartOfHibernateTransaction";

  public static final String                                        DEFAULT_REALM_NAME                             =
                                                                                       HibernateIdentityStoreImpl.class.getName() +
                                                                                           ".DEFAULT_REALM";

  public static final String                                        CREDENTIAL_TYPE_PASSWORD                       = "PASSWORD";

  public static final String                                        CREDENTIAL_TYPE_BINARY                         = "BINARY";

  public static final String                                        ALL_GROUPS_TYPE                                =
                                                                                    "@@ALL_GROUPS@@";

  private String                                                    id;

  private FeaturesMetaData                                          supportedFeatures;                                                                  // NOSONAR

  private SessionFactory                                            sessionFactory;

  private boolean                                                   isRealmAware                                   = false;

  private boolean                                                   isAllowNotDefinedAttributes                    = false;

  private boolean                                                   isAllowNotDefinedIdentityObjectTypes           = false;

  private boolean                                                   isAllowNotCaseSensitiveSearch                  = false;

  private boolean                                                   lazyStartOfHibernateTransaction                = false;

  private boolean                                                   isManageTransactionDuringBootstrap             = true;

  // TODO: rewrite this into some more handy object
  private IdentityStoreConfigurationMetaData                        configurationMD;                                                                    // NOSONAR

  private static Set<IdentityObjectSearchCriteriaType>              supportedIdentityObjectSearchCriteria          =
                                                                                                          new HashSet<>();

  private static Set<String>                                        supportedCredentialTypes                       =
                                                                                             new HashSet<>();

  // <IdentityObjectType name, Set<Attribute name>>
  private Map<String, Set<String>>                                  attributeMappings                              =
                                                                                      new HashMap<>();

  // <IdentityObjectType name, <Attribute name, MD>
  private Map<String, Map<String, IdentityObjectAttributeMetaData>> attributesMetaData                             =
                                                                                       new HashMap<>();                                                 // NOSONAR

  // <IdentityObjectType name, <Attribute store mapping, Attribute name>
  private Map<String, Map<String, String>>                          reverseAttributeMappings                       =
                                                                                             new HashMap<>();

  static {
    // List all supported criteria classes
    supportedIdentityObjectSearchCriteria.add(IdentityObjectSearchCriteriaType.ATTRIBUTE_FILTER);
    supportedIdentityObjectSearchCriteria.add(IdentityObjectSearchCriteriaType.NAME_FILTER);
    supportedIdentityObjectSearchCriteria.add(IdentityObjectSearchCriteriaType.PAGE);
    supportedIdentityObjectSearchCriteria.add(IdentityObjectSearchCriteriaType.SORT);

    // credential types supported by this impl
    supportedCredentialTypes.add(CREDENTIAL_TYPE_PASSWORD);
    supportedCredentialTypes.add(CREDENTIAL_TYPE_BINARY);
  }

  public PatchedHibernateIdentityStoreImpl(String id) {
    this.id = id;
  }

  public void bootstrap(IdentityStoreConfigurationContext configurationContext) throws IdentityException { // NOSONAR
    this.configurationMD = configurationContext.getStoreConfigurationMetaData();

    id = configurationMD.getId();

    supportedFeatures = new FeaturesMetaDataImpl(configurationMD,
                                                 supportedIdentityObjectSearchCriteria,
                                                 true,
                                                 true,
                                                 new HashSet<>());

    String populateMembershipTypes = configurationMD.getOptionSingleValue(POPULATE_MEMBERSHIP_TYPES);
    String populateIdentityObjectTypes = configurationMD.getOptionSingleValue(POPULATE_IDENTITY_OBJECT_TYPES);

    String manageTransactionDuringBootstrap = configurationMD.getOptionSingleValue(MANAGE_TRANSACTION_DURING_BOOTSTRAP);

    if (manageTransactionDuringBootstrap != null && manageTransactionDuringBootstrap.equalsIgnoreCase("false")) {
      this.isAllowNotDefinedAttributes = false;
    }

    sessionFactory = bootstrapHibernateSessionFactory(configurationContext);

    Session hibernateSession = sessionFactory.openSession();

    // Attribute mappings - helper structures

    for (IdentityObjectTypeMetaData identityObjectTypeMetaData : configurationMD.getSupportedIdentityTypes()) {
      Set<String> names = new HashSet<>();
      Map<String, IdentityObjectAttributeMetaData> metadataMap = new HashMap<>();
      Map<String, String> reverseMap = new HashMap<>();
      for (IdentityObjectAttributeMetaData attributeMetaData : identityObjectTypeMetaData.getAttributes()) {
        names.add(attributeMetaData.getName());
        metadataMap.put(attributeMetaData.getName(), attributeMetaData);
        if (attributeMetaData.getStoreMapping() != null) {
          reverseMap.put(attributeMetaData.getStoreMapping(), attributeMetaData.getName());
        }
      }

      // Use unmodifiableSet as it'll be exposed directly
      attributeMappings.put(identityObjectTypeMetaData.getName(), Collections.unmodifiableSet(names));

      attributesMetaData.put(identityObjectTypeMetaData.getName(), metadataMap);

      reverseAttributeMappings.put(identityObjectTypeMetaData.getName(), reverseMap);
    }

    attributeMappings = Collections.unmodifiableMap(attributeMappings);

    if (isManageTransactionDuringBootstrap()) {
      hibernateSession.getTransaction().begin();
    }

    if (populateMembershipTypes != null && populateMembershipTypes.equalsIgnoreCase("true")) {
      List<String> memberships = new LinkedList<>();

      for (String membership : configurationMD.getSupportedRelationshipTypes()) {
        memberships.add(membership);
      }

      try {
        populateRelationshipTypes(hibernateSession, memberships.toArray(new String[memberships.size()]));
      } catch (Exception e) {
        throw new IdentityException("Failed to populate relationship types", e);
      }
    }

    if (populateIdentityObjectTypes != null && populateIdentityObjectTypes.equalsIgnoreCase("true")) {
      List<String> types = new LinkedList<>();

      for (IdentityObjectTypeMetaData metaData : configurationMD.getSupportedIdentityTypes()) {
        types.add(metaData.getName());
      }

      try {
        populateObjectTypes(hibernateSession, types.toArray(new String[types.size()]));
      } catch (Exception e) {
        throw new IdentityException("Failed to populate identity object types", e);
      }

    }

    if (supportedCredentialTypes != null && !supportedCredentialTypes.isEmpty()) {
      try {
        populateCredentialTypes(hibernateSession, supportedCredentialTypes.toArray(new String[supportedCredentialTypes.size()]));
      } catch (Exception e) {
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, "Exception occurred: ", e);
        }

        throw new IdentityException("Failed to populated credential types", e);
      }
    }

    String realmAware = configurationMD.getOptionSingleValue(IS_REALM_AWARE);

    if (realmAware != null && realmAware.equalsIgnoreCase("true")) {
      this.isRealmAware = true;
    }

    String allowNotDefineAttributes = configurationMD.getOptionSingleValue(ALLOW_NOT_DEFINED_ATTRIBUTES);

    if (allowNotDefineAttributes != null && allowNotDefineAttributes.equalsIgnoreCase("true")) {
      this.isAllowNotDefinedAttributes = true;
    }

    String allowNotDefinedIOT = configurationMD.getOptionSingleValue(ALLOW_NOT_DEFINED_IDENTITY_OBJECT_TYPES_OPTION);

    if (allowNotDefinedIOT != null && allowNotDefinedIOT.equalsIgnoreCase("true")) {
      this.isAllowNotDefinedIdentityObjectTypes = true;
    }

    String allowNotCaseSensitiveSearch = configurationMD.getOptionSingleValue(ALLOW_NOT_CASE_SENSITIVE_SEARCH);

    if (allowNotCaseSensitiveSearch == null || allowNotCaseSensitiveSearch.equalsIgnoreCase("true")) {
      this.isAllowNotCaseSensitiveSearch = true;
    }

    String lazyStartOfHibernateTransactionString = configurationMD.getOptionSingleValue(LAZY_START_OF_HIBERNATE_TRANSACTION);
    if (lazyStartOfHibernateTransactionString != null && lazyStartOfHibernateTransactionString.equalsIgnoreCase("true")) {
      this.lazyStartOfHibernateTransaction = true;
    }

    // Default realm

    HibernateRealm realm = null;

    try {
      realm = getHibernateRealmByName(hibernateSession, DEFAULT_REALM_NAME);
    } catch (HibernateException e) {
      // Realm does not exist
    }

    if (realm == null) {
      addRealm(hibernateSession, DEFAULT_REALM_NAME);
    }

    // If store is realm aware than creat all configured realms

    if (isRealmAware()) {
      Set<String> realmNames = new HashSet<>();

      for (RealmConfigurationMetaData realmMD : configurationContext.getConfigurationMetaData().getRealms()) {
        realmNames.add(realmMD.getId());
      }

      for (String rid : realmNames) {
        realm = getHibernateRealmByName(hibernateSession, rid);
        if (realm == null) {
          addRealm(hibernateSession, rid);
        }
      }
    }

    if (isManageTransactionDuringBootstrap()) {
      hibernateSession.getTransaction().commit();
    }

    if (hibernateSession.getTransaction().isActive()) {
      hibernateSession.flush();
    }

    hibernateSession.close();
  }

  protected SessionFactory bootstrapHibernateSessionFactory(IdentityStoreConfigurationContext configurationContext) throws IdentityException {
    String sfJNDIName = configurationContext.getStoreConfigurationMetaData()
                                            .getOptionSingleValue(HIBERNATE_SESSION_FACTORY_JNDI_NAME);
    String sfRegistryName = configurationContext.getStoreConfigurationMetaData()
                                                .getOptionSingleValue(HIBERNATE_SESSION_FACTORY_REGISTRY_NAME);
    String hibernateConfiguration = configurationContext.getStoreConfigurationMetaData()
                                                        .getOptionSingleValue(HIBERNATE_CONFIGURATION);
    if (sfJNDIName != null) {
      try {
        return (SessionFactory) new InitialContext().lookup(sfJNDIName);
      } catch (NamingException e) {
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, "Exception occurred: ", e);
        }

        throw new IdentityException("Cannot obtain hibernate SessionFactory from provided JNDI name: " + sfJNDIName, e);
      }
    } else if (sfRegistryName != null) {
      Object registryObject = configurationContext.getConfigurationRegistry().getObject(sfRegistryName);

      if (registryObject == null) {
        throw new IdentityException("Cannot obtain hibernate SessionFactory from provided registry name: " + sfRegistryName);
      }

      if (!(registryObject instanceof SessionFactory)) {
        throw new IdentityException("Cannot obtain hibernate SessionFactory from provided registry name: " + sfRegistryName +
            "; Registered object is not an instance of SessionFactory: " + registryObject.getClass().getName());
      }

      return (SessionFactory) registryObject;

    } else if (hibernateConfiguration != null) {

      try {
        Configuration config = new Configuration().configure(hibernateConfiguration);
        return config
                     .addAnnotatedClass(HibernateIdentityObject.class)
                     .addAnnotatedClass(HibernateIdentityObjectCredentialBinaryValue.class)
                     .addAnnotatedClass(HibernateIdentityObjectAttributeBinaryValue.class)
                     .addAnnotatedClass(HibernateIdentityObjectAttribute.class)
                     .addAnnotatedClass(HibernateIdentityObjectCredential.class)
                     .addAnnotatedClass(HibernateIdentityObjectCredentialType.class)
                     .addAnnotatedClass(HibernateIdentityObjectRelationship.class)
                     .addAnnotatedClass(HibernateIdentityObjectRelationshipName.class)
                     .addAnnotatedClass(HibernateIdentityObjectRelationshipType.class)
                     .addAnnotatedClass(HibernateIdentityObjectType.class)
                     .addAnnotatedClass(HibernateRealm.class)
                     .buildSessionFactory();
      } catch (Exception e) {
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, "Exception occurred: ", e);
        }

        throw new IdentityException("Cannot obtain hibernate SessionFactory using provided hibernate configuration: " +
            hibernateConfiguration, e);
      }

    }
    throw new IdentityException("Cannot obtain hibernate SessionFactory. None of supported options specified: " +
        HIBERNATE_SESSION_FACTORY_JNDI_NAME + ", " + HIBERNATE_SESSION_FACTORY_REGISTRY_NAME + ", " + HIBERNATE_CONFIGURATION);

  }

  public IdentityStoreSession createIdentityStoreSession() throws IdentityException {
    try {
      return new ExoHibernateIdentityStoreSessionImpl(sessionFactory, lazyStartOfHibernateTransaction);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Failed to obtain Hibernate SessionFactory", e);
    }
  }

  public IdentityStoreSession createIdentityStoreSession(
                                                         Map<String, Object> sessionOptions) throws IdentityException {
    return createIdentityStoreSession();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public FeaturesMetaData getSupportedFeatures() {
    return supportedFeatures;
  }

  public IdentityObject createIdentityObject(IdentityStoreInvocationContext invocationCtx,
                                             String name,
                                             IdentityObjectType identityObjectType) throws IdentityException {
    return createIdentityObject(invocationCtx, name, identityObjectType, null);
  }

  public IdentityObject createIdentityObject(IdentityStoreInvocationContext ctx,
                                             String name,
                                             IdentityObjectType identityObjectType,
                                             Map<String, String[]> attributes) throws IdentityException {

    if (name == null) {
      throw new IllegalArgumentException("IdentityObject name is null");
    }

    checkIOType(identityObjectType);

    Session session = getHibernateSession(ctx);

    HibernateRealm realm = getRealm(session, ctx);

    Number boxedSize = null;
    if (isAllowNotCaseSensitiveSearch(identityObjectType.getName())) {
      boxedSize = session.createNamedQuery("HibernateIdentityObject.countIdentityObjectByNameAndTypeIgnoreCase", Number.class)
                         .setParameter(NAME_PROP_NAME, name.toLowerCase())
                         .setParameter(REALM_NAME_PROP_NAME, realm.getName())
                         .setParameter(TYPE_NAME_PROP_NAME, identityObjectType.getName())
                         .uniqueResult();
    } else {
      boxedSize = session.createNamedQuery("HibernateIdentityObject.countIdentityObjectByNameAndType", Number.class)
                         .setParameter(NAME_PROP_NAME, name)
                         .setParameter(REALM_NAME_PROP_NAME, realm.getName())
                         .setParameter(TYPE_NAME_PROP_NAME, identityObjectType.getName())
                         .uniqueResult();
    }
    int size = boxedSize.intValue();
    if (size != 0) {
      throw new IdentityException("IdentityObject already present in this IdentityStore:" +
          "name=" + name + "; type=" + identityObjectType.getName() + "; realm=" + realm);
    }
    HibernateIdentityObjectType hibernateType = getHibernateIdentityObjectType(ctx, identityObjectType);
    HibernateIdentityObject io = new HibernateIdentityObject(name, hibernateType, realm);
    if (attributes != null) {
      for (Map.Entry<String, String[]> entry : attributes.entrySet()) {
        io.addTextAttribute(entry.getKey(), entry.getValue());
      }
    }
    try {
      getHibernateSession(ctx).persist(io);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("Cannot persist new IdentityObject" + io, e);
    }
    return io;
  }

  public void removeIdentityObject(IdentityStoreInvocationContext ctx, IdentityObject identity) throws IdentityException {
    HibernateIdentityObject hio = safeGet(ctx, identity);

    Session hibernateSession = getHibernateSession(ctx);

    Hibernate.initialize(hio);
    try {

      // Remove all related relationships
      HibernateIdentityObjectRelationship[] from = new HibernateIdentityObjectRelationship[hio.getFromRelationships()// NOSONAR
                                                                                              .size()];
      for (HibernateIdentityObjectRelationship relationship : hio.getFromRelationships().toArray(from)) {
        relationship.getFromIdentityObject().getFromRelationships().remove(relationship);
        relationship.getToIdentityObject().getToRelationships().remove(relationship);
        hibernateSession.remove(relationship);
        hibernateSession.flush();
      }

      HibernateIdentityObjectRelationship[] to = new HibernateIdentityObjectRelationship[hio.getToRelationships()
                                                                                            .size()];
      for (HibernateIdentityObjectRelationship relationship : hio.getToRelationships().toArray(to)) {
        relationship.getFromIdentityObject().getFromRelationships().remove(relationship);
        relationship.getToIdentityObject().getToRelationships().remove(relationship);

        hibernateSession.remove(relationship);
        hibernateSession.flush();

      }

      hio.getCredentials().forEach(cred -> {
        hibernateSession.refresh(hio);
        hibernateSession.remove(cred);
        hibernateSession.flush();
      });

      hibernateSession.refresh(hio);
      hibernateSession.remove(hio);
      hibernateSession.flush();
    } catch (Exception e) {
      throw new IdentityException(String.format("Cannot remove IdentityObject: %s", identity), e);
    }
  }

  public int getIdentityObjectsCount(IdentityStoreInvocationContext ctx,
                                     IdentityObjectType identityType) throws IdentityException {
    checkIOType(identityType);

    HibernateIdentityObjectType jpaType = getHibernateIdentityObjectType(ctx, identityType);
    Session hibernateSession = getHibernateSession(ctx);
    try {
      return hibernateSession.createNamedQuery("HibernateIdentityObject.countIdentityObjectsByType", Long.class)
                             .setParameter(TYPE_NAME_PROP_NAME, jpaType.getName())
                             .setParameter(REALM_NAME_PROP_NAME, getRealmName(ctx))
                             .setCacheable(true)
                             .uniqueResult()
                             .intValue();
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot count stored IdentityObjects with type: " + identityType.getName(), e);
    }
  }

  public IdentityObject findIdentityObject(IdentityStoreInvocationContext ctx,
                                           String name,
                                           IdentityObjectType type) throws IdentityException {

    if (name == null) {
      throw new IllegalArgumentException("IdentityObject name is null");
    }

    checkIOType(type);

    HibernateIdentityObject hibernateObject = safeGet(ctx, new SimpleIdentityObject(name, type));
    if (isAllowNotCaseSensitiveSearch() || (hibernateObject != null && hibernateObject.getName().equals(name))) {
      return hibernateObject;
    } else {
      return null;
    }
  }

  public IdentityObject findIdentityObject(IdentityStoreInvocationContext ctx, String id) throws IdentityException {
    if (id == null) {
      throw new IllegalArgumentException("id is null");
    }
    try {
      return getHibernateSession(ctx).get(HibernateIdentityObject.class, Long.parseLong(id));
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot find IdentityObject with id: " + id, e);
    }
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext ctx, // NOSONAR
                                                       IdentityObjectType identityType,
                                                       IdentityObjectSearchCriteria criteria) throws IdentityException {
    checkIOType(identityType);

    HibernateIdentityObjectType hibernateType = getHibernateIdentityObjectType(ctx, identityType);
    HibernateRealm realm = getRealm(getHibernateSession(ctx), ctx);

    Session hibernateSession = getHibernateSession(ctx);
    try {
      StringBuilder hqlBuilderSelect = new StringBuilder("select distinct io from HibernateIdentityObject io");
      Map<String, Object> queryParams = new HashMap<>();

      StringBuilder hqlBuilderConditions = new StringBuilder(" where io.realm=:realm");
      queryParams.put(REALM_PROP_NAME, realm);
      /*
       * BEGIN SOC-6210: Search for all groups by keyword. If type name passed
       * is equals to @@ALL_GROUPS@@, then exclude USER type (search for all
       * group types)
       */
      String hibernateTypeName = hibernateType.getName();
      if (ALL_GROUPS_TYPE.equals(hibernateTypeName)) {
        hqlBuilderConditions.append(" and io.identityType.name <> 'USER'");
      } else {
        hqlBuilderConditions.append(" and io.identityType=:identityType");
        queryParams.put("identityType", hibernateType);
      }
      /* END SOC-6210 */
      /* BEGIN CAL-1225: User picker in Participants tab is case sensitive */
      if (criteria != null && criteria.getFilter() != null) {
        String attrValue = criteria.getFilter().replace("\\*", "%").replace("*", "%");
        String operator = "=";
        if (attrValue.contains("%")) {
          operator = "like";
        }
        if (isAllowNotCaseSensitiveSearch()) {
          attrValue = attrValue.toLowerCase();
          hqlBuilderConditions.append(" and lower(io.name) " + operator + " :ioName");
        } else {
          hqlBuilderConditions.append(" and io.name " + operator + " :ioName");
        }
        queryParams.put("ioName", attrValue);
      }
      /* END CAL-1225: User picker in Participants tab is case sensitive */

      if (criteria != null && criteria.isFiltered() && criteria.getValues() != null) {
        int i = 0;
        for (Map.Entry<String, String[]> entry : criteria.getValues().entrySet()) {
          // Resolve attribute name from the store attribute mapping
          String mappedAttributeName = null;
          try { // NOSONAR
            mappedAttributeName = resolveAttributeStoreMapping(hibernateType, entry.getKey());
          } catch (IdentityException e) {
            // Nothing
          }
          /** Begin eXo customization : PLF-7270 **/
          if (entry.getValue() == null || entry.getValue().length == 0) {
            i++;
            String attrTableJoinName = "attrs" + i;
            String attrParamName = "attr" + i;
            hqlBuilderConditions.append(" and not exists(from io.attributes as " + attrTableJoinName + " where " +
                attrTableJoinName + ".name = :" + attrParamName + ")");
            queryParams.put(attrParamName, mappedAttributeName);
            /** End eXo customization **/
          } else {
            Set<String> given = new HashSet<>(Arrays.asList(entry.getValue()));

            for (String attrValue : given) {
              attrValue = attrValue.replace("\\*", "%").replace("*", "%");

              /*
               * BEGIN CAL-1225: User picker in Participants tab is case
               * sensitive
               */
              String operator = "=";
              if (attrValue.contains("%")) {
                operator = "like";
              }
              if (isAllowNotCaseSensitiveSearch()) {
                attrValue = attrValue.toLowerCase();
              }
              /*
               * END CAL-1225: User picker in Participants tab is case sensitive
               */
              i++;
              String attrTableJoinName = "attrs" + i;
              String textValuesTableJoinName = "textValues" + i;
              String attrParamName = "attr" + i;
              String textValueParamName = "textValue" + i;

              hqlBuilderSelect.append(" join io.attributes as " + attrTableJoinName);
              hqlBuilderSelect.append(" join " + attrTableJoinName + ".textValues as " + textValuesTableJoinName);
              /*
               * BEGIN CAL-1225: User picker in Participants tab is case
               * sensitive
               */
              hqlBuilderConditions.append(" and " + attrTableJoinName + ".name = :" + attrParamName);
              if (isAllowNotCaseSensitiveSearch()) {
                hqlBuilderConditions.append(" and lower(" + textValuesTableJoinName + ") " + operator + " :" +
                    textValueParamName);
              } else {
                hqlBuilderConditions.append(" and " + textValuesTableJoinName + " " + operator + " :" + textValueParamName);
              }
              /*
               * END CAL-1225: User picker in Participants tab is case sensitive
               */

              queryParams.put(attrParamName, mappedAttributeName);
              queryParams.put(textValueParamName, attrValue);
            }
          }
        }
      }

      if (criteria != null && criteria.isSorted()) {
        if (criteria.isAscending()) {
          hqlBuilderConditions.append(" order by io.name asc");
        } else {
          hqlBuilderConditions.append(" order by io.name desc");
        }
      }

      Query<HibernateIdentityObject> hibernateQuery = hibernateSession.createQuery(
                                                                                   hqlBuilderSelect.toString() +
                                                                                       hqlBuilderConditions.toString(),
                                                                                   HibernateIdentityObject.class);

      if (criteria != null && criteria.isPaged()) {
        if (criteria.getMaxResults() > 0) {
          hibernateQuery.setMaxResults(criteria.getMaxResults());
        }
        hibernateQuery.setFirstResult(criteria.getFirstResult());
      }

      // Apply parameters to Hibernate query
      applyQueryParameters(hibernateQuery, queryParams);

      hibernateQuery.setCacheable(true);
      List<HibernateIdentityObject> results = hibernateQuery.list();
      Hibernate.initialize(results);
      return results.stream().map(IdentityObject.class::cast).toList();
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot find IdentityObjects with type '" + identityType.getName() + "'", e);
    }
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext ctx,
                                                       IdentityObjectType identityType) throws IdentityException {
    return findIdentityObject(ctx, identityType, null);
  }

  public int getIdentityObjectCount(IdentityStoreInvocationContext invocationCxt,
                                    IdentityObject identity,
                                    IdentityObjectRelationshipType relationshipType,
                                    boolean parent,
                                    IdentityObjectSearchCriteria criteria) throws IdentityException {
    return getIdentityObjectCount(invocationCxt, identity, relationshipType, null, parent, criteria);
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext invocationCxt,
                                                       IdentityObject identity,
                                                       IdentityObjectRelationshipType relationshipType,
                                                       boolean parent,
                                                       IdentityObjectSearchCriteria criteria) throws IdentityException {
    return findIdentityObject(invocationCxt, identity, relationshipType, null, parent, criteria);
  }

  public int getIdentityObjectCount(IdentityStoreInvocationContext ctx,
                                    IdentityObject identity,
                                    IdentityObjectRelationshipType relationshipType,
                                    Collection<IdentityObjectType> excludes,
                                    boolean parent,
                                    IdentityObjectSearchCriteria criteria) throws IdentityException {
    try {
      Query<?> q = prepareIdentityObjectQuery(
                                              ctx,
                                              identity,
                                              relationshipType,
                                              excludes,
                                              parent,
                                              criteria,
                                              false);
      Number result = (Number) q.uniqueResult();
      return Tools.convertToInt(result);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot get IdentityObject count", e);
    }
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext ctx,
                                                       IdentityObject identity,
                                                       IdentityObjectRelationshipType relationshipType,
                                                       Collection<IdentityObjectType> excludes,
                                                       boolean parent,
                                                       IdentityObjectSearchCriteria criteria) throws IdentityException {

    try {
      Query<?> q = prepareIdentityObjectQuery(ctx,
                                              identity,
                                              relationshipType,
                                              excludes,
                                              parent,
                                              criteria,
                                              false);
      List<?> results = q.getResultList();
      Hibernate.initialize(results);

      List<IdentityObject> identities = new ArrayList<>(results.stream().map(IdentityObject.class::cast).toList());
      if (criteria != null && criteria.isFiltered()) {
        filterByAttributesValues(identities, criteria);
        if (criteria.isPaged()) {
          identities = cutPageFromResults(identities, criteria);
        }
      }
      return identities;
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("Cannot find IdentityObjects", e);
    }
  }

  public Query<?> prepareIdentityObjectQuery(IdentityStoreInvocationContext ctx, // NOSONAR
                                             IdentityObject identity,
                                             IdentityObjectRelationshipType relationshipType,
                                             Collection<IdentityObjectType> excludes,
                                             boolean parent,
                                             IdentityObjectSearchCriteria criteria,
                                             boolean count) throws IdentityException {
    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);
    HibernateRealm realm = getRealm(getHibernateSession(ctx), ctx);

    boolean orderByName = false;
    boolean ascending = true;

    if (criteria != null && criteria.isSorted()) {
      orderByName = true;
      ascending = criteria.isAscending();
    }

    try {

      StringBuilder hqlString = new StringBuilder("");

      if (parent) {

        if (count) {
          hqlString.append("select count(distinct toio) from HibernateIdentityObjectRelationship ior join ior.toIdentityObject toio where ");
        } else {
          hqlString.append("select distinct toio from HibernateIdentityObjectRelationship ior join ior.toIdentityObject toio where ");
        }

        hqlString.append("toio.realm = :realm and ior.fromIdentityObject.realm = :realm and ");

        if (relationshipType != null) {

          hqlString.append("toio.name like :nameFilter and ior.type.name = :relType and ior.fromIdentityObject = :identity");
        } else {
          hqlString.append("toio.name like :nameFilter and ior.fromIdentityObject = :identity");
        }

        if (CollectionUtils.isNotEmpty(excludes)) {
          for (int i = 0; i < excludes.size(); i++) {
            hqlString.append(" and toio.identityType.id <> ")
                     .append(":exclude" + i);
          }
        }

        if (orderByName) {
          hqlString.append(" order by toio.name");
          if (ascending) {
            hqlString.append(" asc");
          }
        }
      } else {

        if (count) {
          hqlString.append("select count(distinct fromio) from HibernateIdentityObjectRelationship ior join ior.fromIdentityObject fromio where ");

        } else {
          hqlString.append("select distinct fromio from HibernateIdentityObjectRelationship ior join ior.fromIdentityObject fromio where ");
        }

        hqlString.append("ior.toIdentityObject.realm = :realm and fromio.realm = :realm and ");

        if (relationshipType != null) {
          hqlString.append("fromio.name like :nameFilter and ior.type.name = :relType and ior.toIdentityObject = :identity");
        } else {
          hqlString.append("fromio.name like :nameFilter and ior.toIdentityObject = :identity");
        }

        if (CollectionUtils.isNotEmpty(excludes)) {
          for (int i = 0; i < excludes.size(); i++) {
            hqlString.append(" and fromio.identityType.id <> ")
                     .append(":exclude" + i);
          }
        }

        if (orderByName) {
          hqlString.append(" order by fromio.name");
          if (ascending) {
            hqlString.append(" asc");
          }
        }
      }

      Query<?> q = count ? getHibernateSession(ctx).createQuery(hqlString.toString(), Long.class) :
                         getHibernateSession(ctx).createQuery(hqlString.toString(), HibernateIdentityObject.class);
      q.setParameter("identity", hibernateObject)
       .setParameter(REALM_PROP_NAME, realm)
       .setCacheable(true);

      if (relationshipType != null) {
        q.setParameter("relType", relationshipType.getName());
      }

      if (criteria != null && criteria.getFilter() != null) {
        q.setParameter("nameFilter", criteria.getFilter().replace("\\*", "%").replace("*", "%"));
      } else {
        q.setParameter("nameFilter", "%");
      }

      if (CollectionUtils.isNotEmpty(excludes)) {
        int i = 0;
        for (IdentityObjectType exclude : excludes) {
          HibernateIdentityObjectType exType = getHibernateIdentityObjectType(ctx, exclude);
          q.setParameter("exclude" + i++, exType.getId());
        }
      }
      if (criteria != null && criteria.isPaged() && !criteria.isFiltered()) {
        q.setFirstResult(criteria.getFirstResult());
        if (criteria.getMaxResults() > 0) {
          q.setMaxResults(criteria.getMaxResults());
        }
      }

      q.setCacheable(true);
      return q;
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot prepare hibernate query", e);
    }
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext ctx,
                                                       IdentityObject identity,
                                                       IdentityObjectRelationshipType relationshipType,
                                                       boolean parent) throws IdentityException {

    return findIdentityObject(ctx, identity, relationshipType, parent, null);
  }

  public IdentityObjectRelationship createRelationship(IdentityStoreInvocationContext ctx,
                                                       IdentityObject fromIdentity,
                                                       IdentityObject toIdentity,
                                                       IdentityObjectRelationshipType relationshipType,
                                                       String name,
                                                       boolean createNames) throws IdentityException {

    if (relationshipType == null) {
      throw new IllegalArgumentException("RelationshipType is null");
    }
    HibernateIdentityObject fromIO = safeGet(ctx, fromIdentity);
    HibernateIdentityObject toIO = safeGet(ctx, toIdentity);
    HibernateIdentityObjectRelationshipType type = getHibernateIdentityObjectRelationshipType(ctx, relationshipType);

    Session hibernateSession = getHibernateSession(ctx);

    if (!isAllowNotDefinedIdentityObjectTypes()
        && !getSupportedFeatures().isRelationshipTypeSupported(fromIO.getIdentityType(), // NOSONAR
                                                               toIO.getIdentityType(), // NOSONAR
                                                               relationshipType)) {
      throw new IdentityException("Relationship not supported. RelationshipType[ " + relationshipType.getName() + " ] " +
          "beetween: [ " + fromIO.getIdentityType().getName() + " ] and [ " + toIO.getIdentityType().getName() + " ]");
    }

    HibernateIdentityObjectRelationship relationship = null;
    if (name != null) {
      HibernateIdentityObjectRelationshipName relationshipName = getRelationshipByName(hibernateSession, getRealmName(ctx), name);
      if (relationshipName == null) {
        throw new IdentityException(String.format(RELATIONSHIP_NOT_FOUND_MSG, name));
      }
      relationship = new HibernateIdentityObjectRelationship(type, fromIO, toIO, relationshipName);
    } else {
      relationship = new HibernateIdentityObjectRelationship(type, fromIO, toIO);
    }

    try {
      Session session = hibernateSession;
      session.persist(relationship);
      session.flush();
    } catch (HibernateException e) {
      throw new IdentityException("Cannot create relationship: ", e);
    }
    return relationship;

  }

  public void removeRelationship(IdentityStoreInvocationContext ctx,
                                 IdentityObject fromIdentity,
                                 IdentityObject toIdentity,
                                 IdentityObjectRelationshipType relationshipType,
                                 String name) throws IdentityException {

    if (relationshipType == null) {
      throw new IllegalArgumentException("RelationshipType is null");
    }

    HibernateIdentityObject fromIO = safeGet(ctx, fromIdentity);
    HibernateIdentityObject toIO = safeGet(ctx, toIdentity);
    HibernateIdentityObjectRelationshipType type = getHibernateIdentityObjectRelationshipType(ctx, relationshipType);

    Session hibernateSession = getHibernateSession(ctx);
    Query<HibernateIdentityObjectRelationship> query = null;
    if (name == null) {
      query = hibernateSession.createNamedQuery("HibernateIdentityObjectRelationship.findIdentityObjectRelationshipWithoutName",
                                                HibernateIdentityObjectRelationship.class)
                              .setParameter(TYPE_ID_PROP_NAME, type.getId())
                              .setParameter(FROM_IDENTITY_OBJECT_PROP_NAME, fromIO)
                              .setParameter(TO_IDENTITY_OBJECT_PROP_NAME, toIO);
    } else {
      HibernateIdentityObjectRelationshipName relationshipName = getRelationshipByName(hibernateSession, getRealmName(ctx), name);
      if (relationshipName == null) {
        throw new IdentityException(String.format(RELATIONSHIP_NOT_FOUND_MSG, name));
      }
      query = hibernateSession.createNamedQuery("HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByAttributes",
                                                HibernateIdentityObjectRelationship.class)
                              .setParameter(TYPE_ID_PROP_NAME, type.getId())
                              .setParameter(NAME_PROP_NAME, name)
                              .setParameter(FROM_IDENTITY_OBJECT_PROP_NAME, fromIO)
                              .setParameter(TO_IDENTITY_OBJECT_PROP_NAME, toIO);
    }
    HibernateIdentityObjectRelationship relationship = query.uniqueResult();
    if (relationship == null) {
      throw new IdentityException("Relationship not present in the store");
    }
    try {
      fromIO.getFromRelationships().remove(relationship);// NOSONAR
      toIO.getToRelationships().remove(relationship);// NOSONAR
      hibernateSession.remove(relationship);
      hibernateSession.flush();
    } catch (HibernateException e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("Cannot remove relationship");
    }

  }

  public void removeRelationships(IdentityStoreInvocationContext ctx,
                                  IdentityObject identity1,
                                  IdentityObject identity2,
                                  boolean named) throws IdentityException {
    HibernateIdentityObject hio1 = safeGet(ctx, identity1);
    HibernateIdentityObject hio2 = safeGet(ctx, identity2);

    Session hibernateSession = getHibernateSession(ctx);
    List<HibernateIdentityObjectRelationship> results =
                                                      hibernateSession.createNamedQuery("HibernateIdentityObjectRelationship.findIdentityObjectRelationshipsByIdentities",
                                                                                        HibernateIdentityObjectRelationship.class)
                                                                      .setParameter("hio1", hio1)
                                                                      .setParameter("hio2", hio2)
                                                                      .list();
    Hibernate.initialize(results);
    for (Iterator<HibernateIdentityObjectRelationship> iterator = results.iterator(); iterator.hasNext();) {
      HibernateIdentityObjectRelationship relationship = iterator.next();
      if ((named && relationship.getName() != null) ||
          (!named && relationship.getName() == null)) {
        try {
          relationship.getFromIdentityObject().getFromRelationships().remove(relationship);
          relationship.getToIdentityObject().getToRelationships().remove(relationship);
          hibernateSession.remove(relationship);
          hibernateSession.flush();
        } catch (HibernateException e) {
          if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "Exception occurred: ", e);
          }

          throw new IdentityException("Cannot remove relationship");
        }
      }
    }
  }

  public Set<IdentityObjectRelationship> resolveRelationships(IdentityStoreInvocationContext ctx,
                                                              IdentityObject fromIdentity,
                                                              IdentityObject toIdentity,
                                                              IdentityObjectRelationshipType relationshipType) throws IdentityException {

    HibernateIdentityObject hio1 = safeGet(ctx, fromIdentity);
    HibernateIdentityObject hio2 = safeGet(ctx, toIdentity);

    List<HibernateIdentityObjectRelationship> results = null;
    if (relationshipType == null) {
      results =
              getHibernateSession(ctx).createNamedQuery("HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByIdentityByType",
                                                        HibernateIdentityObjectRelationship.class)
                                      .setParameter(FROM_IDENTITY_OBJECT_PROP_NAME, hio1)
                                      .setParameter(TO_IDENTITY_OBJECT_PROP_NAME, hio2)
                                      .list();
    } else {
      results =
              getHibernateSession(ctx).createNamedQuery("HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByIdentityByType",
                                                        HibernateIdentityObjectRelationship.class)
                                      .setParameter(TYPE_NAME_PROP_NAME, relationshipType.getName())
                                      .setParameter(FROM_IDENTITY_OBJECT_PROP_NAME, hio1)
                                      .setParameter(TO_IDENTITY_OBJECT_PROP_NAME, hio2)
                                      .list();
    }
    Hibernate.initialize(results);
    return new HashSet<>(results);
  }

  public int getRelationshipsCount(IdentityStoreInvocationContext ctx,
                                   IdentityObject identity,
                                   IdentityObjectRelationshipType type,
                                   boolean parent,
                                   boolean named,
                                   String name,
                                   IdentityObjectSearchCriteria searchCriteria) throws IdentityException {

    org.hibernate.query.Query<?> criteria = prepareResolveRelationshipsCriteria(ctx,
                                                                                identity,
                                                                                type,
                                                                                parent,
                                                                                named,
                                                                                name,
                                                                                searchCriteria,
                                                                                true);
    Number result = (Number) criteria.uniqueResult();
    return Tools.convertToInt(result);
  }

  public Set<IdentityObjectRelationship> resolveRelationships(IdentityStoreInvocationContext ctx,
                                                              IdentityObject identity,
                                                              IdentityObjectRelationshipType type,
                                                              boolean parent,
                                                              boolean named,
                                                              String name,
                                                              IdentityObjectSearchCriteria searchCriteria) throws IdentityException {
    org.hibernate.query.Query<?> criteria = prepareResolveRelationshipsCriteria(ctx,
                                                                                identity,
                                                                                type,
                                                                                parent,
                                                                                named,
                                                                                name,
                                                                                searchCriteria,
                                                                                false);

    List<HibernateIdentityObjectRelationship> results = new ArrayList<>();
    List<?> list = criteria.list();
    Hibernate.initialize(list);

    for (Object object : list) {
      if (object instanceof Tuple t) {
        HibernateIdentityObjectRelationship hibernateIdentityObjectRelationship =
                                                                                t.get(0,
                                                                                      HibernateIdentityObjectRelationship.class);
        results.add(hibernateIdentityObjectRelationship);
      } else if (object instanceof HibernateIdentityObjectRelationship hioRel) {
        results.add(hioRel);
      } else if (object.getClass().isArray()) {
        HibernateIdentityObjectRelationship hibernateIdentityObjectRelationship =
                                                                                (HibernateIdentityObjectRelationship) ((Object[]) object)[0];
        results.add(hibernateIdentityObjectRelationship);
      } else {
        log.warning("Unsupported retrieved object type: " + object.getClass());
      }
    }
    return new HashSet<>(results);
  }

  public org.hibernate.query.Query<?> prepareResolveRelationshipsCriteria(IdentityStoreInvocationContext ctx, // NOSONAR
                                                                          IdentityObject identity,
                                                                          IdentityObjectRelationshipType type,
                                                                          boolean parent,
                                                                          boolean named,
                                                                          String name,
                                                                          IdentityObjectSearchCriteria searchCriteria,
                                                                          boolean count) throws IdentityException {
    boolean isSorted = searchCriteria != null && searchCriteria.isSorted();

    StringBuilder queryString = null;
    if (count) {
      queryString = new StringBuilder("SELECT COUNT(r) FROM HibernateIdentityObjectRelationship r ");
    } else if (isSorted) {
      queryString = new StringBuilder("SELECT r, io.name FROM HibernateIdentityObjectRelationship r ");
    } else {
      queryString = new StringBuilder("SELECT r FROM HibernateIdentityObjectRelationship r ");
    }

    if (parent) {
      if (isSorted) {
        queryString.append(" JOIN r.toIdentityObject io");
      }
      queryString.append(" WHERE r.fromIdentityObject = :hio");
    } else {
      if (isSorted) {
        queryString.append(" JOIN r.fromIdentityObject io");
      }
      queryString.append(" WHERE r.toIdentityObject = :hio");
    }
    if (type != null) {
      queryString.append(" AND r.type.id = :typeId");
    }
    if (name != null) {
      queryString.append(" AND r.name.name = :name");
    } else if (named) {
      queryString.append(" AND r.name IS NOT NULL");
    } else {
      queryString.append(" AND r.name IS NULL");
    }
    if (isSorted) {
      queryString.append(" ORDER BY io.name");
      if (searchCriteria.isAscending()) {
        queryString.append(ASC_ORDER);
      } else {
        queryString.append(DESC_ORDER);
      }
    }

    Session hibernateSession = getHibernateSession(ctx);
    org.hibernate.query.Query<?> query;
    if (count) {
      query = hibernateSession.createQuery(queryString.toString(), Long.class);
    } else if (isSorted) {
      query = hibernateSession.createQuery(queryString.toString(), Tuple.class);
    } else {
      query = hibernateSession.createQuery(queryString.toString(), HibernateIdentityObjectRelationship.class);
    }

    HibernateIdentityObject hio = safeGet(ctx, identity);
    query.setParameter("hio", hio);

    if (type != null) {
      HibernateIdentityObjectRelationshipType hibernateType = getHibernateIdentityObjectRelationshipType(ctx, type);
      query.setParameter(TYPE_ID_PROP_NAME, hibernateType.getId());
    }
    if (name != null) {
      query.setParameter(NAME_PROP_NAME, name);
    }
    if (searchCriteria != null && searchCriteria.isPaged() && !searchCriteria.isFiltered()) {
      if (searchCriteria.getMaxResults() > 0) {
        query.setMaxResults(searchCriteria.getMaxResults());
      }
      query.setFirstResult(searchCriteria.getFirstResult());
    }
    return query;
  }

  public String createRelationshipName(IdentityStoreInvocationContext ctx, String name) throws IdentityException {
    if (name == null) {
      throw new IllegalArgumentException(NAME_IS_NULL_MSG);
    }

    Session hibernateSession = getHibernateSession(ctx);
    try {
      HibernateIdentityObjectRelationshipName hiorn = getRelationshipByName(hibernateSession, getRealmName(ctx), name);
      if (hiorn != null) {
        throw new IdentityException(String.format("Relationship name '%s' already exists in the store", name));
      }

      HibernateRealm realm = getRealm(hibernateSession, ctx);
      hiorn = new HibernateIdentityObjectRelationshipName(name, realm);
      getHibernateSession(ctx).persist(hiorn);
      getHibernateSession(ctx).flush();

    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot create new relationship name: " + name, e);
    }

    return name;
  }

  public String removeRelationshipName(IdentityStoreInvocationContext ctx, String name) throws IdentityException {
    if (name == null) {
      throw new IllegalArgumentException(NAME_IS_NULL_MSG);
    }

    Session hibernateSession = getHibernateSession(ctx);
    try {
      HibernateIdentityObjectRelationshipName hioRel = getRelationshipByName(hibernateSession, getRealmName(ctx), name);
      if (hioRel == null) {
        throw new IdentityException(String.format(RELATIONSHIP_NOT_FOUND_MSG, name));
      }
      removeRelationshipsByName(hibernateSession, hioRel);
      hibernateSession.flush();
      hibernateSession.getTransaction().commit();

      hibernateSession = getHibernateSession(ctx);
      hibernateSession.beginTransaction();
      hibernateSession.remove(hioRel);
      hibernateSession.flush();
    } catch (Exception e) {
      throw new IdentityException("Cannot remove new relationship name: " + name, e);
    }
    return name;
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx,
                                          IdentityObjectSearchCriteria criteria) throws IdentityException {
    try {
      StringBuilder queryString = new StringBuilder("SELECT DISTINCT rn.name FROM HibernateIdentityObjectRelationshipName rn ");
      queryString.append(" INNER JOIN rn.realm r ON r.name = :realmName");

      boolean hasFilter = criteria != null && criteria.getFilter() != null;
      if (hasFilter) {
        queryString.append(" WHERE rn.name LIKE :name");
      } else {
        queryString.append(" WHERE rn.name IS NOT NULL");
      }

      boolean isSorted = criteria != null && criteria.isSorted();
      if (isSorted) {
        queryString.append(" ORDER BY rn.name ");
        if (criteria.isAscending()) {
          queryString.append(ASC_ORDER);
        } else {
          queryString.append(DESC_ORDER);
        }
      }

      org.hibernate.query.Query<String> query = getHibernateSession(ctx).createQuery(queryString.toString(), String.class);
      query.setParameter(REALM_NAME_PROP_NAME, getRealmName(ctx));
      if (hasFilter) {
        query.setParameter(NAME_PROP_NAME, criteria.getFilter().replace("\\*", "%").replace("*", "%"));
      }

      if (criteria != null && criteria.isPaged()) {
        query.setFirstResult(criteria.getFirstResult());
        if (criteria.getMaxResults() > 0) {
          query.setMaxResults(criteria.getMaxResults());
        }
      }

      List<String> results = query.list();
      Hibernate.initialize(results);
      return new LinkedHashSet<>(results);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("Cannot get relationship names. ", e);
    }
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx) throws IdentityException {
    return getRelationshipNames(ctx, (IdentityObjectSearchCriteria) null);
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx,
                                          IdentityObject identity,
                                          IdentityObjectSearchCriteria criteria) throws IdentityException {

    try {
      StringBuilder queryString = new StringBuilder("SELECT DISTINCT rn.name FROM HibernateIdentityObjectRelationship r ");
      queryString.append(" INNER JOIN r.name rn");

      boolean hasFilter = criteria != null && criteria.getFilter() != null;
      if (hasFilter) {
        queryString.append(" ON rn.name LIKE :name");
      }

      if (identity != null) {
        queryString.append(" WHERE r.fromIdentityObject = :hio OR r.toIdentityObject = :hio ");
      }

      boolean isSorted = criteria != null && criteria.isSorted();
      if (isSorted) {
        queryString.append(" ORDER BY rn.name ");
        if (criteria.isAscending()) {
          queryString.append(ASC_ORDER);
        } else {
          queryString.append(DESC_ORDER);
        }
      }

      org.hibernate.query.Query<String> query = getHibernateSession(ctx).createQuery(queryString.toString(), String.class);
      if (hasFilter) {
        query.setParameter(NAME_PROP_NAME, criteria.getFilter().replace("\\*", "%").replace("*", "%"));
      }
      if (identity != null) {
        HibernateIdentityObject hibernateObject = safeGet(ctx, identity);
        query.setParameter("hio", hibernateObject);
      }
      if (criteria != null && criteria.isPaged()) {
        query.setFirstResult(criteria.getFirstResult());
        if (criteria.getMaxResults() > 0) {
          query.setMaxResults(criteria.getMaxResults());
        }
      }
      List<String> results = query.list();
      Hibernate.initialize(results);
      return new LinkedHashSet<>(results);
    } catch (Exception e) {
      throw new IdentityException("Cannot get relationship names. ", e);
    }
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx,
                                          IdentityObject identity) throws IdentityException {
    return getRelationshipNames(ctx, identity, null);
  }

  public Map<String, String> getRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                                           String name) throws IdentityException {
    if (name == null) {
      throw new IllegalArgumentException(NAME_IS_NULL_MSG);
    }

    Session hibernateSession = getHibernateSession(ctx);

    try {
      HibernateIdentityObjectRelationshipName hiorn = getRelationshipByName(hibernateSession, getRealmName(ctx), name);
      if (hiorn == null) {
        throw new IdentityException(String.format(RELATIONSHIP_NOT_FOUND_MSG, name));
      }

      Hibernate.initialize(hiorn.getProperties());

      return new HashMap<>(hiorn.getProperties());

    } catch (Exception e) {
      throw new IdentityException("Cannot get relationship name properties: " + name, e);
    }
  }

  public void setRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                            String name,
                                            Map<String, String> properties) throws IdentityException {
    if (name == null) {
      throw new IllegalArgumentException(NAME_IS_NULL_MSG);
    }

    Session hibernateSession = getHibernateSession(ctx);

    try {
      HibernateIdentityObjectRelationshipName hiorn = getRelationshipByName(hibernateSession, getRealmName(ctx), name);
      if (hiorn == null) {
        throw new IdentityException("Relationship name doesn't exist");
      }

      hiorn.getProperties().putAll(properties);

    } catch (Exception e) {
      throw new IdentityException("Cannot set relationship name properties: " + name, e);
    }
  }

  public void removeRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                               String name,
                                               Set<String> properties) throws IdentityException {
    if (name == null) {
      throw new IllegalArgumentException(NAME_IS_NULL_MSG);
    }

    Session hibernateSession = getHibernateSession(ctx);

    try {
      HibernateIdentityObjectRelationshipName hiorn = getRelationshipByName(hibernateSession, getRealmName(ctx), name);
      if (hiorn == null) {
        throw new IdentityException("Relationship name doesn't exist");
      }

      Hibernate.initialize(hiorn.getProperties());

      for (String property : properties) {
        hiorn.getProperties().remove(property);
      }

    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot remove relationship name properties: " + name, e);
    }
  }

  public Map<String, String> getRelationshipProperties(IdentityStoreInvocationContext ctx,
                                                       IdentityObjectRelationship relationship) throws IdentityException {
    try {
      HibernateIdentityObjectRelationship hibernateRelationship = getHibernateIdentityObjectRelationship(ctx, relationship);
      Hibernate.initialize(hibernateRelationship.getProperties());
      return new HashMap<>(hibernateRelationship.getProperties());
    } catch (HibernateException e) {
      throw new IdentityException("Cannot obtain relationship properties: ", e);
    }
  }

  public void setRelationshipProperties(IdentityStoreInvocationContext ctx,
                                        IdentityObjectRelationship relationship,
                                        Map<String, String> properties) throws IdentityException {
    HibernateIdentityObjectRelationship hibernateRelationship = getHibernateIdentityObjectRelationship(ctx, relationship);
    try {
      hibernateRelationship.getProperties().putAll(properties);
    } catch (HibernateException e) {
      throw new IdentityException("Cannot update relationship properties: ", e);
    }
  }

  public void removeRelationshipProperties(IdentityStoreInvocationContext ctx,
                                           IdentityObjectRelationship relationship,
                                           Set<String> properties) throws IdentityException {
    HibernateIdentityObjectRelationship hibernateRelationship = getHibernateIdentityObjectRelationship(ctx, relationship);
    try {
      Hibernate.initialize(hibernateRelationship.getProperties());
      for (String property : properties) {
        hibernateRelationship.getProperties().remove(property);
      }
    } catch (HibernateException e) {
      throw new IdentityException("Cannot update relationship properties: ", e);
    }
  }

  // Attribute store

  public Set<String> getSupportedAttributeNames(IdentityStoreInvocationContext ctx,
                                                IdentityObjectType identityType) throws IdentityException {
    checkIOType(identityType);

    if (attributeMappings.containsKey(identityType.getName())) {
      return attributeMappings.get(identityType.getName());
    }

    return new HashSet<>();

  }

  public IdentityObjectAttribute getAttribute(IdentityStoreInvocationContext ctx,
                                              IdentityObject identity,
                                              String name) throws IdentityException {
    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);

    Set<HibernateIdentityObjectAttribute> storeAttributes = hibernateObject.getAttributes(); // NOSONAR

    Hibernate.initialize(storeAttributes);

    // Remap the names
    for (HibernateIdentityObjectAttribute attribute : storeAttributes) {
      String mappedName = resolveAttributeNameFromStoreMapping(identity.getIdentityType(), name);
      if (mappedName != null) {
        return attribute;
      }
    }

    return null;
  }

  public Map<String, IdentityObjectAttribute> getAttributes(IdentityStoreInvocationContext ctx,
                                                            IdentityObject identity) throws IdentityException {
    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);
    Hibernate.initialize(hibernateObject.getAttributes()); // NOSONAR

    Map<String, IdentityObjectAttribute> result = new HashMap<>();
    // Remap the names
    for (HibernateIdentityObjectAttribute attribute : hibernateObject.getAttributes()) {
      String name = resolveAttributeNameFromStoreMapping(identity.getIdentityType(), attribute.getName());
      if (name != null) {
        result.put(name, attribute);
      }
    }
    return result;
  }

  public Map<String, IdentityObjectAttributeMetaData> getAttributesMetaData(IdentityStoreInvocationContext invocationContext,
                                                                            IdentityObjectType identityType) {
    return attributesMetaData.get(identityType.getName());
  }

  @SuppressWarnings("unchecked")
  public void updateAttributes(IdentityStoreInvocationContext ctx, // NOSONAR
                               IdentityObject identity,
                               IdentityObjectAttribute[] attributes) throws IdentityException {

    if (attributes == null) {
      throw new IllegalArgumentException(ATTRIBUTES_NULL_MSG);
    }

    Map<String, IdentityObjectAttribute> mappedAttributes = new HashMap<>();

    Map<String, IdentityObjectAttributeMetaData> mdMap = attributesMetaData.get(identity.getIdentityType().getName());

    for (IdentityObjectAttribute attribute : attributes) {
      String name = resolveAttributeStoreMapping(identity.getIdentityType(), attribute.getName());
      mappedAttributes.put(name, attribute);

      if (!isAllowNotDefinedAttributes
          && (mdMap == null || !mdMap.containsKey(attribute.getName()))) {
        throw new IdentityException(String.format("Cannot add not defined attribute. Use '%s' option if needed. Attribute name: %s",
                                                  ALLOW_NOT_DEFINED_ATTRIBUTES,
                                                  attribute.getName()));
      }

      if (mdMap != null && mdMap.containsKey(attribute.getName())) {

        IdentityObjectAttributeMetaData amd = mdMap.get(attribute.getName());

        if (!amd.isMultivalued() && attribute.getSize() > 1) {
          throw new IdentityException("Cannot assigned multiply values to single valued attribute: " + attribute.getName());
        }
        if (amd.isReadonly()) {
          // Just silently fail and go on
          mappedAttributes.remove(name);
          continue;
        }

        if (amd.isUnique()) {
          IdentityObject checkIdentity = findIdentityObjectByUniqueAttribute(ctx,
                                                                             identity.getIdentityType(),
                                                                             attribute);

          if (checkIdentity != null && !checkIdentity.getName().equals(identity.getName())) {
            throw new IdentityException("Unique attribute '" + attribute.getName() + " value already set for identityObject: " +
                checkIdentity);
          }
        }

        String type = amd.getType();

        // check if all values have proper type

        for (Object value : attribute.getValues()) {
          if (type.equals(IdentityObjectAttributeMetaData.TEXT_TYPE) && !(value instanceof String)) {
            throw new IdentityException("Cannot update text type attribute with not String type value: " + attribute.getName() +
                " / " + value);
          }
          if (type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE) && !(value instanceof byte[])) {
            throw new IdentityException("Cannot update binary type attribute with not byte[] type value: " + attribute.getName() +
                " / " + value);
          }
        }
        if (type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE) && attribute.getValues().size() > 1) {
          throw new IdentityException("Cannot add binary type attribute with more than one value - this implementation" +
              "support only single value binary attributes: " + attribute.getName());
        }
      }
    }

    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);

    Hibernate.initialize(hibernateObject.getAttributes());

    for (String name : mappedAttributes.keySet()) { // NOSONAR
      IdentityObjectAttribute attribute = mappedAttributes.get(name);

      IdentityObjectAttributeMetaData amd = null;

      if (mdMap != null) {
        amd = mdMap.get(attribute.getName());
      }

      // Default to text
      String type = amd != null ? amd.getType() : IdentityObjectAttributeMetaData.TEXT_TYPE;

      boolean present = false;

      for (HibernateIdentityObjectAttribute storeAttribute : hibernateObject.getAttributes()) {
        if (storeAttribute.getName().equals(name)) {
          present = true;
          if (storeAttribute.getType().equals(HibernateIdentityObjectAttribute.TYPE_TEXT)) {
            if (!type.equals(IdentityObjectAttributeMetaData.TEXT_TYPE)) {
              throw new IdentityException("Wrong attribute mapping. Attribute persisted as text is mapped with: " + type +
                  ". Attribute name: " + name); // NOSONAR
            }

            Set<String> v = new HashSet<>();
            for (Object value : attribute.getValues()) {
              v.add(value.toString());
            }

            storeAttribute.setTextValues(v);
          } else if (storeAttribute.getType().equals(HibernateIdentityObjectAttribute.TYPE_BINARY)) {

            if (!type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE)) {
              throw new IdentityException("Wrong attribute mapping. Attribute persisted as binary is mapped with: " + type +
                  ". Attribute name: " + name);
            }
            HibernateIdentityObjectAttributeBinaryValue bv =
                                                           new HibernateIdentityObjectAttributeBinaryValue((byte[]) attribute.getValue());
            getHibernateSession(ctx).persist(bv);
            storeAttribute.setBinaryValue(bv);
          } else {
            throw new IdentityException("Internal identity store error");
          }
          break;
        }
      }

      if (!present && attribute.getValues() != null && !attribute.getValues().isEmpty()) {
        HibernateIdentityObjectAttribute newAttribute = new HibernateIdentityObjectAttribute(hibernateObject, name, type);
        if (type.equals(HibernateIdentityObjectAttribute.TYPE_TEXT)) {
          newAttribute.setTextValues(attribute.getValues());
        } else if (type.equals(HibernateIdentityObjectAttribute.TYPE_BINARY)) {
          HibernateIdentityObjectAttributeBinaryValue bv =
                                                         new HibernateIdentityObjectAttributeBinaryValue((byte[]) attribute.getValue());
          getHibernateSession(ctx).persist(bv);
          newAttribute.setBinaryValue(bv);
        }
        hibernateObject.addAttribute(newAttribute);
      }

    }

  }

  public void addAttributes(IdentityStoreInvocationContext ctx, // NOSONAR
                            IdentityObject identity,
                            IdentityObjectAttribute[] attributes) throws IdentityException {

    if (attributes == null) {
      throw new IllegalArgumentException(ATTRIBUTES_NULL_MSG);
    }

    Map<String, IdentityObjectAttribute> mappedAttributes = new HashMap<>();

    Map<String, IdentityObjectAttributeMetaData> mdMap = attributesMetaData.get(identity.getIdentityType().getName());

    Session hibernateSession = getHibernateSession(ctx);

    for (IdentityObjectAttribute attribute : attributes) {
      String name = resolveAttributeStoreMapping(identity.getIdentityType(), attribute.getName());
      mappedAttributes.put(name, attribute);

      if ((mdMap == null || !mdMap.containsKey(attribute.getName())) &&
          !isAllowNotDefinedAttributes) {
        throw new IdentityException("Cannot add not defined attribute. Use '" + ALLOW_NOT_DEFINED_ATTRIBUTES +
            "' option if needed. Attribute name: " + attribute.getName());

      }

      IdentityObjectAttributeMetaData amd = null;

      if (mdMap != null) {
        amd = mdMap.get(attribute.getName());
      }

      if (amd != null) {

        if (!amd.isMultivalued() && attribute.getSize() > 1) {
          throw new IdentityException("Cannot add multiply values to single valued attribute: " + attribute.getName());
        }
        if (amd.isReadonly()) {
          // Just silently fail and go on
          mappedAttributes.remove(name);
          continue;
        }

        if (amd.isUnique()) {
          IdentityObject checkIdentity = findIdentityObjectByUniqueAttribute(ctx,
                                                                             identity.getIdentityType(),
                                                                             attribute);

          if (checkIdentity != null && !checkIdentity.getName().equals(identity.getName())) {
            throw new IdentityException("Unique attribute '" + attribute.getName() + " value already set for identityObject: " +
                checkIdentity);
          }
        }

        String type = amd.getType();

        // check if all values have proper type

        for (Object value : attribute.getValues()) {
          if (type.equals(IdentityObjectAttributeMetaData.TEXT_TYPE) && !(value instanceof String)) {
            throw new IdentityException("Cannot add text type attribute with not String type value: " + attribute.getName() +
                " / " + value);
          }
          if (type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE) && !(value instanceof byte[])) {
            throw new IdentityException("Cannot add binary type attribute with not byte[] type value: " + attribute.getName() +
                " / " + value);
          }

        }
        if (type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE) && attribute.getValues().size() > 1) {
          throw new IdentityException("Cannot add binary type attribute with more than one value - this implementation" +
              "support only single value binary attributes: " + attribute.getName());
        }
      }
    }

    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);

    Hibernate.initialize(hibernateObject.getAttributes()); // NOSONAR

    for (String name : mappedAttributes.keySet()) { // NOSONAR
      IdentityObjectAttribute attribute = mappedAttributes.get(name);

      IdentityObjectAttributeMetaData amd = mdMap != null ? mdMap.get(attribute.getName()) : null;

      // Default to text
      String type = amd != null ? amd.getType() : IdentityObjectAttributeMetaData.TEXT_TYPE;

      HibernateIdentityObjectAttribute hibernateAttribute = null;

      for (HibernateIdentityObjectAttribute storeAttribute : hibernateObject.getAttributes()) {
        if (storeAttribute.getName().equals(name)) {
          hibernateAttribute = storeAttribute;
          break;
        }
      }

      if (hibernateAttribute != null) {
        if (hibernateAttribute.getType().equals(HibernateIdentityObjectAttribute.TYPE_TEXT)) {
          if (!type.equals(IdentityObjectAttributeMetaData.TEXT_TYPE)) {
            throw new IdentityException("Wrong attribute mapping. Attribute persisted as text is mapped with: " + type +
                ". Attribute name: " + name);
          }

          @SuppressWarnings("unchecked")
          Set<String> mergedValues = new HashSet<>(hibernateAttribute.getValues());
          for (Object value : attribute.getValues()) {
            mergedValues.add(value.toString());
          }

          hibernateAttribute.setTextValues(mergedValues);
        } else if (hibernateAttribute.getType().equals(HibernateIdentityObjectAttribute.TYPE_BINARY)) {

          if (!type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE)) {
            throw new IdentityException("Wrong attribute mapping. Attribute persisted as binary is mapped with: " + type +
                ". Attribute name: " + name);
          }

          HibernateIdentityObjectAttributeBinaryValue bv =
                                                         new HibernateIdentityObjectAttributeBinaryValue((byte[]) attribute.getValue());
          hibernateSession.persist(bv);
          hibernateAttribute.setBinaryValue(bv);
        } else {
          throw new IdentityException("Internal identity store error");
        }
        break;

      } else {
        if (type.equals(IdentityObjectAttributeMetaData.TEXT_TYPE)) {
          Set<String> values = new HashSet<>();

          for (Object value : attribute.getValues()) {
            values.add(value.toString());
          }
          hibernateAttribute = new HibernateIdentityObjectAttribute(hibernateObject,
                                                                    name,
                                                                    HibernateIdentityObjectAttribute.TYPE_TEXT);
          hibernateAttribute.setTextValues(values);
        } else if (type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE)) {
          Set<byte[]> values = new HashSet<>();

          for (Object value : attribute.getValues()) {
            values.add((byte[]) value);
          }
          hibernateAttribute = new HibernateIdentityObjectAttribute(hibernateObject,
                                                                    name,
                                                                    HibernateIdentityObjectAttribute.TYPE_BINARY);
          HibernateIdentityObjectAttributeBinaryValue bv =
                                                         new HibernateIdentityObjectAttributeBinaryValue((byte[]) attribute.getValue());
          hibernateSession.persist(bv);
          hibernateAttribute.setBinaryValue(bv);
        }

        hibernateObject.addAttribute(hibernateAttribute);

      }
    }
  }

  public void removeAttributes(IdentityStoreInvocationContext ctx,
                               IdentityObject identity,
                               String[] attributes) throws IdentityException {

    if (attributes == null) {
      throw new IllegalArgumentException(ATTRIBUTES_NULL_MSG);
    }

    String[] mappedAttributes = new String[attributes.length];

    for (int i = 0; i < attributes.length; i++) {
      String name = resolveAttributeStoreMapping(identity.getIdentityType(), attributes[i]);
      mappedAttributes[i] = name;

      Map<String, IdentityObjectAttributeMetaData> mdMap = attributesMetaData.get(identity.getIdentityType().getName());

      if (mdMap != null) {
        IdentityObjectAttributeMetaData amd = mdMap.get(attributes[i]);
        if (amd != null && amd.isRequired()) {
          throw new IdentityException("Cannot remove required attribute: " + attributes[i]);
        }
      } else {
        if (!isAllowNotDefinedAttributes) {
          throw new IdentityException("Cannot remove not defined attribute. Use '" + ALLOW_NOT_DEFINED_ATTRIBUTES +
              "' option if needed. Attribute name: " + attributes[i]);
        }
      }

    }

    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);

    Hibernate.initialize(hibernateObject.getAttributes()); // NOSONAR

    for (String attr : mappedAttributes) {
      hibernateObject.removeAttribute(attr);
    }
  }

  public IdentityObject findIdentityObjectByUniqueAttribute(IdentityStoreInvocationContext invocationCtx,
                                                            IdentityObjectType identityObjectType,
                                                            IdentityObjectAttribute attribute) throws IdentityException {
    if (attribute == null) {
      throw new IllegalArgumentException("attribute is null");
    }
    checkIOType(identityObjectType);
    String attrMappedName = resolveAttributeStoreMapping(identityObjectType, attribute.getName());
    HibernateIdentityObjectType hiot = getHibernateIdentityObjectType(invocationCtx, identityObjectType);
    Session session = getHibernateSession(invocationCtx);
    HibernateRealm realm = getRealm(session, invocationCtx);
    if (attribute.getValues() == null || attribute.getValues().isEmpty()) {
      return null;
    }

    boolean attrDuctTypeText = true;

    if (attribute.getValue() instanceof byte[]) {
      attrDuctTypeText = false;
    }

    StringBuilder queryString = new StringBuilder("SELECT a FROM HibernateIdentityObjectAttribute a ");
    queryString.append(" WHERE a.identityObject.identityType = :identityType");
    queryString.append(" AND a.name = :attributeName");
    queryString.append(" AND a.identityObject.realm = :realm");

    if (attrDuctTypeText) {
      for (int i = 0; i < attribute.getValues().size(); i++) {
        String paramName = " :value" + i;
        queryString.append(" and").append(paramName).append(" = any elements(a.textValues)");

      }
    } else {
      queryString.append(" and :value = a.binaryValue");
    }

    Query<HibernateIdentityObjectAttribute> q =
                                              session.createQuery(queryString.toString(), HibernateIdentityObjectAttribute.class)
                                                     .setParameter("identityType", hiot)
                                                     .setParameter("attributeName", attrMappedName)
                                                     .setParameter(REALM_PROP_NAME, realm);

    if (attrDuctTypeText) {
      int i = 0;
      for (Object o : attribute.getValues()) {
        String value = o.toString();
        String paramName = "value" + i;
        q.setParameter(paramName, value);
        i++;
      }
    } else {
      q.setParameter("value", attribute.getValue());
    }

    List<HibernateIdentityObjectAttribute> attrs = q.list();
    if (CollectionUtils.isEmpty(attrs)) {
      return null;
    } else if (attrs.size() > 1) {
      throw new IdentityException("Illegal state - more than one IdentityObject with the same unique attribute value: " +
          attribute);
    }
    return attrs.get(0).getIdentityObject();
  }

  public boolean validateCredential(IdentityStoreInvocationContext ctx,
                                    IdentityObject identityObject,
                                    IdentityObjectCredential credential) throws IdentityException {
    if (credential == null) {
      throw new IllegalArgumentException();
    }

    HibernateIdentityObject hibernateObject = safeGet(ctx, identityObject);

    if (supportedFeatures.isCredentialSupported(hibernateObject.getIdentityType(), credential.getType())) { // NOSONAR
      Session hibernateSession = getHibernateSession(ctx);
      org.hibernate.query.Query<HibernateIdentityObjectCredential> query =
                                                                         hibernateSession.createNamedQuery("HibernateIdentityObjectCredential.findCredentialByTypeAndIdentity",
                                                                                                           HibernateIdentityObjectCredential.class);
      query.setParameter("cTypeName", credential.getType().getName());
      query.setParameter("ioId", hibernateObject.getIdLong());
      HibernateIdentityObjectCredential hibernateCredential = query.uniqueResult();
      if (hibernateCredential == null) {
        return false;
      }

      Object tmpEncodedValue = credential.getEncodedValue();
      Object value = null;
      if (tmpEncodedValue != null) {
        value = tmpEncodedValue;
      } else {
        value = credential.getValue();
      }

      if (value instanceof String && hibernateCredential.getTextValue() != null) {
        return value.toString().equals(hibernateCredential.getTextValue());
      } else if (value instanceof byte[] bytes && hibernateCredential.getBinaryValue() != null) {
        return Arrays.equals(bytes, hibernateCredential.getBinaryValue().getValue());
      } else {
        throw new IdentityException("Not supported credential value: " + value.getClass());
      }
    } else {
      throw new IdentityException("CredentialType not supported for a given IdentityObjectType");
    }
  }

  public void updateCredential(IdentityStoreInvocationContext ctx,
                               IdentityObject identityObject,
                               IdentityObjectCredential credential) throws IdentityException {

    if (credential == null) {
      throw new IllegalArgumentException();
    }

    HibernateIdentityObject hibernateObject = safeGet(ctx, identityObject);

    Session hibernateSession = getHibernateSession(ctx);

    if (supportedFeatures.isCredentialSupported(hibernateObject.getIdentityType(), credential.getType())) { // NOSONAR

      HibernateIdentityObjectCredentialType hibernateCredentialType =
                                                                    getHibernateIdentityObjectCredentialType(ctx,
                                                                                                             credential.getType());

      if (hibernateCredentialType == null) {
        throw new IllegalStateException("Credential type not present in this store: " + credential.getType().getName());
      }

      Object value = null;

      Object tmpEncodedValue = credential.getEncodedValue();
      if (tmpEncodedValue != null) {
        value = tmpEncodedValue;
      } else {
        value = credential.getValue();
      }

      HibernateIdentityObjectCredential hibernateCredential = hibernateObject.getCredential(credential.getType());
      if (hibernateCredential == null) {
        hibernateCredential = new HibernateIdentityObjectCredential();
        hibernateCredential.setType(hibernateCredentialType);
        hibernateObject.addCredential(hibernateCredential);
      }

      if (value instanceof String) {
        hibernateCredential.setTextValue(value.toString());
      } else if (value instanceof byte[] bytes) {
        HibernateIdentityObjectCredentialBinaryValue bv = new HibernateIdentityObjectCredentialBinaryValue(bytes);
        getHibernateSession(ctx).persist(bv);
        hibernateCredential.setBinaryValue(bv);
      } else {
        throw new IdentityException("Not supported credential value: " + value.getClass());
      }

      hibernateSession.persist(hibernateCredential);
      hibernateObject.addCredential(hibernateCredential);
      hibernateSession.flush();
    } else {
      throw new IdentityException("CredentialType not supported for a given IdentityObjectType");
    }
  }

  // Internal

  public void addIdentityObjectType(IdentityStoreInvocationContext ctx, IdentityObjectType type) throws IdentityException {
    HibernateIdentityObjectType hibernateType = new HibernateIdentityObjectType(type);
    getHibernateSession(ctx).persist(hibernateType);
    getHibernateSession(ctx).flush();

  }

  public void addIdentityObjectRelationshipType(IdentityStoreInvocationContext ctx,
                                                IdentityObjectRelationshipType type) throws IdentityException {
    HibernateIdentityObjectRelationshipType hibernateType = new HibernateIdentityObjectRelationshipType(type);
    getHibernateSession(ctx).persist(hibernateType);
    getHibernateSession(ctx).flush();
  }

  protected Session getHibernateSession(IdentityStoreInvocationContext ctx) throws IdentityException {
    try {
      ExoHibernateIdentityStoreSessionImpl hbIdentityStoreSession =
                                                                  (ExoHibernateIdentityStoreSessionImpl) ctx.getIdentityStoreSession();

      if (lazyStartOfHibernateTransaction) {
        hbIdentityStoreSession.startHibernateTransactionIfNotStartedYet();
      }

      return ((Session) hbIdentityStoreSession.getSessionContext());
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot obtain Hibernate Session", e);
    }
  }

  private void checkIOInstance(IdentityObject io) {
    if (io == null) {
      throw new IllegalArgumentException("IdentityObject is null");
    }

  }

  private HibernateIdentityObject safeGet(IdentityStoreInvocationContext ctx, IdentityObject io) throws IdentityException {
    checkIOInstance(io);

    if (io instanceof HibernateIdentityObject hio) {
      return hio;
    } else {
      return getHibernateIdentityObject(ctx, io);
    }
  }

  private void checkIOType(IdentityObjectType iot) throws IdentityException {
    if (iot == null) {
      throw new IllegalArgumentException("IdentityObjectType is null");
    }

    if (!isAllowNotDefinedIdentityObjectTypes()
        && !getSupportedFeatures().isIdentityObjectTypeSupported(iot)) {
      throw new IdentityException("IdentityType not supported by this IdentityStore implementation: " + iot);
    }
  }

  private HibernateIdentityObjectType getHibernateIdentityObjectType(IdentityStoreInvocationContext ctx,
                                                                     IdentityObjectType type) throws IdentityException {
    checkIOType(type);
    String typeName = type.getName();
    try {
      Session hibernateSession = getHibernateSession(ctx);
      HibernateIdentityObjectType hibernateType = getHibernateIdentityObjectType(hibernateSession, typeName);
      if (hibernateType == null && isAllowNotDefinedIdentityObjectTypes()) {
        populateObjectTypes(hibernateSession, new String[] { typeName });
        hibernateType = getHibernateIdentityObjectType(hibernateSession, typeName);
      }
      if (hibernateType == null) {
        throw new IdentityException("IdentityObjectType[" + typeName + "] not present in the store."); // NOSONAR
      }
      return hibernateType;
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("IdentityObjectType[" + typeName + "] not present in the store.", e);
    }

  }

  private HibernateIdentityObjectType getHibernateIdentityObjectType(Session hibernateSession, String typeName) {
    org.hibernate.query.Query<HibernateIdentityObjectType> query =
                                                                 hibernateSession.createNamedQuery("HibernateIdentityObjectType.findIdentityObjectTypeByName",
                                                                                                   HibernateIdentityObjectType.class);
    query.setParameter(NAME_PROP_NAME, typeName);
    return query.uniqueResult();
  }

  private HibernateIdentityObject getHibernateIdentityObject(IdentityStoreInvocationContext ctx,
                                                             IdentityObject io) throws IdentityException {
    Session hibernateSession = getHibernateSession(ctx);
    String typeName = io.getIdentityType().getName();
    try {
      if (isAllowNotCaseSensitiveSearch(typeName)) {
        return hibernateSession.createNamedQuery("HibernateIdentityObject.findIdentityObjectByNameAndTypeIgnoreCase",
                                                 HibernateIdentityObject.class)
                               .setParameter(NAME_PROP_NAME, io.getName().toLowerCase())
                               .setParameter(REALM_NAME_PROP_NAME, getRealmName(ctx))
                               .setParameter(TYPE_NAME_PROP_NAME, typeName)
                               .uniqueResult();
      } else {
        return hibernateSession.createNamedQuery("HibernateIdentityObject.findIdentityObjectByNameAndType",
                                                 HibernateIdentityObject.class)
                               .setParameter(NAME_PROP_NAME, io.getName())
                               .setParameter(REALM_NAME_PROP_NAME, getRealmName(ctx))
                               .setParameter(TYPE_NAME_PROP_NAME, typeName)
                               .uniqueResult();
      }
    } catch (NonUniqueResultException e) {
      log.log(Level.SEVERE, // NOSONAR
              "The identity of type '" + typeName + "' and with name '" + io.getName() +
                  "' is not unique. Thus a null result will be returned.",
              e);
      return null;
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("IdentityObject[ " + io.getName() + " | " + typeName + "] not present in the store.", e);
    }
  }

  private HibernateIdentityObjectRelationshipType getHibernateIdentityObjectRelationshipType(IdentityStoreInvocationContext ctx,
                                                                                             IdentityObjectRelationshipType iot) throws IdentityException {
    Session hibernateSession = getHibernateSession(ctx);
    String typeName = iot.getName();
    try {
      return getHibernateIdentityObjectRelationshipType(hibernateSession, typeName);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("IdentityObjectRelationshipType[ " + typeName + "] not present in the store.");
    }
  }

  private HibernateIdentityObjectRelationshipType getHibernateIdentityObjectRelationshipType(Session hibernateSession,
                                                                                             String typeName) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectRelationshipType.findIdentityRelationshipTypeByName",
                                             HibernateIdentityObjectRelationshipType.class)
                           .setParameter(NAME_PROP_NAME, typeName)
                           .uniqueResult();
  }

  private HibernateIdentityObjectCredentialType getHibernateIdentityObjectCredentialType(IdentityStoreInvocationContext ctx,
                                                                                         IdentityObjectCredentialType credentialType) throws IdentityException {
    Session hibernateSession = getHibernateSession(ctx);
    String typeName = credentialType.getName();
    try {
      return getHibernateIdentityObjectCredentialType(hibernateSession, typeName);
    } catch (HibernateException e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("IdentityObjectCredentialType[ " + typeName + "] not present in the store.");
    }
  }

  private HibernateIdentityObjectCredentialType getHibernateIdentityObjectCredentialType(Session hibernateSession,
                                                                                         String typeName) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectCredentialType.findIdentityCredentialTypeByName",
                                             HibernateIdentityObjectCredentialType.class)
                           .setParameter(NAME_PROP_NAME, typeName)
                           .uniqueResult();
  }

  public void populateObjectTypes(Session hibernateSession, String[] typeNames) {
    for (String typeName : typeNames) {
      // Check if present
      HibernateIdentityObjectType hibernateType = getHibernateIdentityObjectType(hibernateSession, typeName);
      if (hibernateType == null) {
        hibernateType = new HibernateIdentityObjectType(typeName);
        hibernateSession.persist(hibernateType);
      }
    }
  }

  public void populateRelationshipTypes(Session hibernateSession, String[] typeNames) {
    for (String typeName : typeNames) {
      HibernateIdentityObjectRelationshipType hibernateType = getHibernateIdentityObjectRelationshipType(hibernateSession,
                                                                                                         typeName);
      if (hibernateType == null) {
        hibernateType = new HibernateIdentityObjectRelationshipType(typeName);
        hibernateSession.persist(hibernateType);
      }
    }
  }

  public void populateCredentialTypes(Session hibernateSession, String[] typeNames) {
    for (String typeName : typeNames) {
      HibernateIdentityObjectCredentialType hibernateType = getHibernateIdentityObjectCredentialType(hibernateSession, typeName);
      if (hibernateType == null) {
        hibernateType = new HibernateIdentityObjectCredentialType(typeName);
        hibernateSession.persist(hibernateType);
      }
    }
  }

  public void addRealm(Session hibernateSession, String realmName) throws IdentityException {

    try {

      HibernateRealm realm = new HibernateRealm(realmName);
      hibernateSession.persist(realm);

    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Failed to create store realm", e);
    }
  }

  private HibernateRealm getRealm(Session hibernateSession, IdentityStoreInvocationContext ctx) {
    if (getRealmName(ctx) == null) {
      throw new IllegalStateException("Realm Id not present");
    }

    HibernateRealm realm = null;

    // If store is not realm aware return null to create/get objects accessible
    // from other realms
    if (!isRealmAware()) {
      realm = getHibernateRealmByName(hibernateSession, DEFAULT_REALM_NAME);
      if (realm == null) {
        throw new IllegalStateException("Default store realm is not present: " + DEFAULT_REALM_NAME);
      }
    } else {
      realm = getHibernateRealmByName(hibernateSession, getRealmName(ctx));
      if (realm == null) {
        HibernateRealm newRealm = new HibernateRealm(getRealmName(ctx));
        hibernateSession.persist(newRealm);
        return newRealm;
      }
    }

    return realm;
  }

  private String getRealmName(IdentityStoreInvocationContext ctx) {
    if (isRealmAware()) {
      return ctx.getRealmId();
    } else {
      return DEFAULT_REALM_NAME;
    }
  }

  private boolean isRealmAware() {
    return isRealmAware;
  }

  private boolean isAllowNotDefinedAttributes() {
    return isAllowNotDefinedAttributes;
  }

  /**
   * Resolve store mapping for attribute name. If attribute is not mapped and
   * store doesn't allow not defined attributes throw exception
   * 
   * @param type
   * @param name
   * @return
   */
  private String resolveAttributeStoreMapping(IdentityObjectType type, String name) throws IdentityException {
    String mapping = null;

    if (attributesMetaData.containsKey(type.getName())) {
      IdentityObjectAttributeMetaData amd = attributesMetaData.get(type.getName()).get(name);

      if (amd != null) {
        mapping = amd.getStoreMapping() != null ? amd.getStoreMapping() : amd.getName();
        return mapping;
      }
    }

    if (isAllowNotDefinedAttributes()) {
      mapping = name;
      return mapping;
    }

    throw new IdentityException("Attribute name is not configured in this store");
  }

  private String resolveAttributeNameFromStoreMapping(IdentityObjectType type, String mapping) {
    if (reverseAttributeMappings.containsKey(type.getName())) {
      Map<String, String> map = reverseAttributeMappings.get(type.getName());
      if (map != null) {
        return map.containsKey(mapping) ? map.get(mapping) : mapping;
      }
    }
    if (isAllowNotDefinedAttributes()) {
      return mapping;
    }
    return null;
  }

  // TODO: this kills performance and is present here only as "quick" hack to
  // have the feature present and let to add test cases
  // TODO: needs to be redone at the hibernate query level
  @SuppressWarnings("rawtypes")
  private void filterByAttributesValues(Collection<IdentityObject> objects, IdentityObjectSearchCriteria criteria) {
    Set<IdentityObject> toRemove = new HashSet<>();
    Map<String, String[]> attrs = criteria.getValues();
    for (IdentityObject object : objects) {
      Map<String, Collection> presentAttrs = ((HibernateIdentityObject) object).getAttributesAsMap();
      for (Map.Entry<String, String[]> entry : attrs.entrySet()) { // NOSONAR
        // Resolve attribute name from the store attribute mapping
        String mappedAttributeName = null;
        try {
          mappedAttributeName = resolveAttributeStoreMapping(object.getIdentityType(), entry.getKey());
        } catch (IdentityException e) {
          // Nothing
        }

        // If the attribute key is enable, consider its absence as it was equals
        // to true
        if (entry.getKey().equals(EntityMapperUtils.USER_ENABLED) && entry.getValue() != null && entry.getValue().length == 0) {
          if (presentAttrs.containsKey(mappedAttributeName)) {
            toRemove.add(object);
          }
          continue;
        }

        if (mappedAttributeName == null) {
          toRemove.add(object);
          break;
        }

        if (presentAttrs.containsKey(mappedAttributeName)) {
          Set<String> given = new HashSet<>(Arrays.asList(entry.getValue()));

          Collection present = presentAttrs.get(mappedAttributeName);

          for (String s : given) {
            String regex = Tools.wildcardToRegex(s);

            boolean matches = false;

            for (Object o : present) {
              if (o.toString().matches(regex)) {
                matches = true;
              }
            }

            if (!matches) {
              toRemove.add(object);
              break;
            }
          }

        } else {
          toRemove.add(object);
          break;

        }
      }
    }

    for (IdentityObject identityObject : toRemove) {
      objects.remove(identityObject);
    }
  }

  // TODO: need to be implemented at HQL level
  private <T> List<T> cutPageFromResults(List<T> objects, IdentityObjectSearchCriteria criteria) {
    List<T> results = new LinkedList<>();
    if (criteria.getMaxResults() == 0) {
      for (int i = criteria.getFirstResult(); i < objects.size(); i++) {
        if (i < objects.size()) {
          results.add(objects.get(i));
        }
      }
    } else {
      for (int i = criteria.getFirstResult(); i < criteria.getFirstResult() + criteria.getMaxResults(); i++) {
        if (i < objects.size()) {
          results.add(objects.get(i));
        }
      }
    }
    return results;
  }

  private void applyQueryParameters(Query<?> hibernateQuery, Map<String, Object> queryParams) {
    for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
      hibernateQuery.setParameter(entry.getKey(), entry.getValue());
    }
  }

  protected boolean isAllowNotDefinedIdentityObjectTypes() {
    return isAllowNotDefinedIdentityObjectTypes;
  }

  public boolean isManageTransactionDuringBootstrap() {
    return isManageTransactionDuringBootstrap;
  }

  public boolean isAllowNotCaseSensitiveSearch() {
    return isAllowNotCaseSensitiveSearch;
  }

  private boolean isAllowNotCaseSensitiveSearch(String typeName) {
    return isAllowNotCaseSensitiveSearch() && typeName.equalsIgnoreCase("USER");
  }

  private void removeRelationshipsByName(Session hibernateSession,
                                         HibernateIdentityObjectRelationshipName hiorn) throws IdentityException {
    hibernateSession.createNamedQuery("HibernateIdentityObjectRelationship.removeRelationshipsByName")
                    .setParameter("nameId", hiorn.getId())
                    .executeUpdate();
  }

  private HibernateRealm getHibernateRealmByName(Session hibernateSession, String realmName) {
    return hibernateSession.createNamedQuery("HibernateRealm.findRealmByName", HibernateRealm.class)
                           .setParameter(NAME_PROP_NAME, realmName)
                           .uniqueResult();
  }

  private HibernateIdentityObjectRelationshipName getRelationshipByName(Session hibernateSession,
                                                                        String realmName,
                                                                        String name) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectRelationshipName.findIdentityObjectRelationshipNameByName",
                                             HibernateIdentityObjectRelationshipName.class)
                           .setParameter(NAME_PROP_NAME, name)
                           .setParameter(REALM_NAME_PROP_NAME, realmName)
                           .uniqueResult();
  }

  private HibernateIdentityObjectRelationship getHibernateIdentityObjectRelationship(IdentityStoreInvocationContext ctx,
                                                                                     IdentityObjectRelationship relationship) throws IdentityException {
    try {
      StringBuilder queryString = new StringBuilder("SELECT r FROM HibernateIdentityObjectRelationship r");
      queryString.append(" WHERE r.type.id = :typeId ");
      queryString.append(" AND r.fromIdentityObject = :fromIo ");
      queryString.append(" AND r.toIdentityObject = :toIo ");
      if (relationship.getName() != null) {
        queryString.append(" AND r.name.id = :nameId ");
      }

      Session hibernateSession = getHibernateSession(ctx);
      org.hibernate.query.Query<HibernateIdentityObjectRelationship> query =
                                                                           hibernateSession.createQuery(queryString.toString(),
                                                                                                        HibernateIdentityObjectRelationship.class);

      HibernateIdentityObjectRelationshipType type = getHibernateIdentityObjectRelationshipType(ctx, relationship.getType());
      query.setParameter(TYPE_ID_PROP_NAME, type.getId());

      HibernateIdentityObject fromIO = safeGet(ctx, relationship.getFromIdentityObject());
      query.setParameter("fromIo", fromIO);

      HibernateIdentityObject toIO = safeGet(ctx, relationship.getToIdentityObject());
      query.setParameter("toIo", toIO);

      if (relationship.getName() != null) {
        HibernateIdentityObjectRelationshipName relationshipName = getRelationshipByName(hibernateSession,
                                                                                         getRealmName(ctx),
                                                                                         relationship.getName());
        if (relationshipName == null) {
          throw new IdentityException("Relationship name not present in the store");
        }
        query.setParameter("nameId", relationshipName.getId());
      }
      return query.uniqueResult();
    } catch (HibernateException e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("Cannot obtain relationship properties: ", e);
    }
  }

}
