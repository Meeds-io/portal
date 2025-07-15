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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
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

public class HibernateIdentityStoreImpl implements IdentityStore {

  private static final String NOT_PRESENT_IN_THE_STORE = "] not present in the store.";

  private static final String                                       ATTRIBUTE_NAME                                 =
                                                                                   ". Attribute name: ";

  private static final String                                       OPTION_IF_NEEDED_ATTRIBUTE_NAME                =
                                                                                                    "' option if needed. Attribute name: ";

  private static final String                                       CANNOT_OBTAIN_RELATIONSHIP_PROPERTIES          =
                                                                                                          "Cannot obtain relationship properties: ";

  private static final String                                       IDENTITY_PARAM                                 = "identity";

  public static final String                                        HIBERNATE_SESSION_FACTORY_REGISTRY_NAME        =
                                                                                                            "hibernateSessionFactoryRegistryName";

  public static final String                                        HIBERNATE_CONFIGURATION                        =
                                                                                            "hibernateConfiguration";

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

  public static final String                                        NAME_PARAM                                     = "name";

  public static final String                                        REALM_PARAM                                    = "realm";

  private static final String                                       TYPE_NAME_PARAM                                = "typeName";

  public static final String                                        IDENTITY_TYPE_NAME                             =
                                                                                       TYPE_NAME_PARAM;

  public static final String                                        REALM_NAME_PARAM                               = "realmName";

  private String                                                    id;

  private FeaturesMetaData                                          supportedFeatures; // NOSONAR

  private SessionFactory                                            sessionFactory;

  private boolean                                                   isRealmAware                                   = false;

  private boolean                                                   isAllowNotDefinedAttributes                    = false;

  private boolean                                                   isAllowNotDefinedIdentityObjectTypes           = false;

  private boolean                                                   isAllowNotCaseSensitiveSearch                  = false;

  private boolean                                                   lazyStartOfHibernateTransaction                = false;

  private boolean                                                   isManageTransactionDuringBootstrap             = true;

  private IdentityStoreConfigurationMetaData                        configurationMD; // NOSONAR

  private static Set<IdentityObjectSearchCriteriaType>              supportedIdentityObjectSearchCriteria          =
                                                                                                          new HashSet<>();

  private static Set<String>                                        supportedCredentialTypes                       =
                                                                                             new HashSet<>();

  // <IdentityObjectType name, Set<Attribute name>>
  private Map<String, Set<String>>                                  attributeMappings                              =
                                                                                      new HashMap<>();

  // <IdentityObjectType name, <Attribute name, MD>
  private Map<String, Map<String, IdentityObjectAttributeMetaData>> attributesMetaData                             = // NOSONAR
                                                                                       new HashMap<>();

  // <IdentityObjectType name, <Attribute store mapping, Attribute name>
  private Map<String, Map<String, String>>                          reverseAttributeMappings                       =
                                                                                             new HashMap<>();

  private static final long                                         serialVersionUID                               =
                                                                                     -130355852189832805L;

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

  public HibernateIdentityStoreImpl(String id) {
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
        throw new IdentityException("Failed to populated credential types");
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

    if (allowNotCaseSensitiveSearch != null && allowNotCaseSensitiveSearch.equalsIgnoreCase("true")) {
      this.isAllowNotCaseSensitiveSearch = true;
    }

    String lazyStartOfHibernateTransactionValue = configurationMD.getOptionSingleValue(LAZY_START_OF_HIBERNATE_TRANSACTION);
    if (lazyStartOfHibernateTransactionValue != null && lazyStartOfHibernateTransactionValue.equalsIgnoreCase("true")) {
      this.lazyStartOfHibernateTransaction = true;
    }

    // Default realm

    HibernateRealm realm = getRealmByName(hibernateSession, DEFAULT_REALM_NAME);
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
        realm = getRealmByName(hibernateSession, rid);
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
        return config.addAnnotatedClass(HibernateIdentityObject.class)
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
        throw new IdentityException("Cannot obtain hibernate SessionFactory using provided hibernate configuration: " +
            hibernateConfiguration, e);
      }
    } else {
      throw new IdentityException("Cannot obtain hibernate SessionFactory. None of supported options specified: " +
          HIBERNATE_SESSION_FACTORY_JNDI_NAME + ", " + HIBERNATE_SESSION_FACTORY_REGISTRY_NAME + ", " + HIBERNATE_CONFIGURATION);
    }
  }

  public IdentityStoreSession createIdentityStoreSession() throws IdentityException {
    try {
      return new HibernateIdentityStoreSessionImpl(sessionFactory, lazyStartOfHibernateTransaction);
    } catch (Exception e) {
      throw new IdentityException("Failed to obtain Hibernate SessionFactory", e);
    }
  }

  public IdentityStoreSession createIdentityStoreSession(Map<String, Object> sessionOptions) throws IdentityException {
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
    int size = countIdentityObjectByNameAndType(session, name, identityObjectType.getName(), realm.getName());
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
      throw new IdentityException("Cannot persist new IdentityObject" + io, e);
    }
    return io;
  }

  public void removeIdentityObject(IdentityStoreInvocationContext ctx, IdentityObject identity) throws IdentityException {
    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);

    Session hibernateSession = getHibernateSession(ctx);

    try {
      // Remove all related relationships
      HibernateIdentityObjectRelationship[] from = new HibernateIdentityObjectRelationship[hibernateObject.getFromRelationships()
                                                                                                          .size()];
      for (HibernateIdentityObjectRelationship relationship : hibernateObject.getFromRelationships().toArray(from)) {
        relationship.getFromIdentityObject().getFromRelationships().remove(relationship);
        relationship.getToIdentityObject().getToRelationships().remove(relationship);
        hibernateSession.remove(relationship);
        hibernateSession.flush();
      }

      HibernateIdentityObjectRelationship[] to = new HibernateIdentityObjectRelationship[hibernateObject.getToRelationships()
                                                                                                        .size()];
      for (HibernateIdentityObjectRelationship relationship : hibernateObject.getToRelationships().toArray(to)) {
        relationship.getFromIdentityObject().getFromRelationships().remove(relationship);
        relationship.getToIdentityObject().getToRelationships().remove(relationship);

        hibernateSession.remove(relationship);
        hibernateSession.flush();
      }
      hibernateSession.remove(hibernateObject);
      hibernateSession.flush();
    } catch (Exception e) {
      throw new IdentityException("Cannot remove IdentityObject" + identity, e);
    }
  }

  public int getIdentityObjectsCount(IdentityStoreInvocationContext ctx,
                                     IdentityObjectType identityType) throws IdentityException {
    checkIOType(identityType);
    HibernateIdentityObjectType jpaType = getHibernateIdentityObjectType(ctx, identityType);
    Session hibernateSession = getHibernateSession(ctx);
    try {
      return hibernateSession.createNamedQuery("HibernateIdentityObject.countIdentityObjectsByType", Long.class)
                             .setParameter(IDENTITY_TYPE_NAME, jpaType.getName())
                             .setParameter(REALM_NAME_PARAM, getRealmName(ctx))
                             .setCacheable(true)
                             .uniqueResult()
                             .intValue();
    } catch (Exception e) {
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
    // Check result with case sensitive compare:
    if (isAllowNotCaseSensitiveSearch()) {
      return hibernateObject;
    } else if (hibernateObject != null // NOSONAR
               && hibernateObject.getName().equals(name)) {
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
      return getHibernateSession(ctx).get(HibernateIdentityObject.class, id);
    } catch (Exception e) {
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

      StringBuilder hqlBuilderConditions = new StringBuilder(" where io.realm=:realm and io.identityType=:identityType");
      queryParams.put(REALM_PARAM, realm);
      queryParams.put("identityType", hibernateType);

      hqlBuilderConditions.append(" and io.name like :ioName");
      if (criteria != null && criteria.getFilter() != null) {
        queryParams.put("ioName", criteria.getFilter().replace("\\*", "%").replace("*", "%"));
      } else {
        queryParams.put("ioName", "%");
      }

      if (criteria != null && criteria.isFiltered() && criteria.getValues() != null) {
        int i = 0;
        for (Map.Entry<String, String[]> entry : criteria.getValues().entrySet()) {
          // Resolve attribute name from the store attribute mapping
          String mappedAttributeName = resolveAttributeStoreMapping(hibernateType, entry.getKey());
          List<String> given = Arrays.stream(entry.getValue()).distinct().toList();
          for (String attrValue : given) {
            attrValue = attrValue.replace("\\*", "%").replace("*", "%");

            i++;
            String attrTableJoinName = "attrs" + i;
            String textValuesTableJoinName = "textValues" + i;
            String attrParamName = "attr" + i;
            String textValueParamName = "textValue" + i;

            hqlBuilderSelect.append(" join io.attributes as " + attrTableJoinName);
            hqlBuilderSelect.append(" join " + attrTableJoinName + ".textValues as " + textValuesTableJoinName);
            hqlBuilderConditions.append(" and " + attrTableJoinName + ".name like :" + attrParamName);
            hqlBuilderConditions.append(" and " + textValuesTableJoinName + " like :" + textValueParamName);

            queryParams.put(attrParamName, mappedAttributeName);
            queryParams.put(textValueParamName, attrValue);
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

      Query<IdentityObject> hibernateQuery = hibernateSession.createQuery(
                                                                          hqlBuilderSelect.toString() +
                                                                              hqlBuilderConditions.toString(),
                                                                          IdentityObject.class);
      if (criteria != null && criteria.isPaged()) {
        if (criteria.getMaxResults() > 0) {
          hibernateQuery.setMaxResults(criteria.getMaxResults());
        }
        hibernateQuery.setFirstResult(criteria.getFirstResult());
      }
      // Apply parameters to Hibernate query
      applyQueryParameters(hibernateQuery, queryParams);
      hibernateQuery.setCacheable(true);
      List<IdentityObject> results = hibernateQuery.list();
      Hibernate.initialize(results);
      return results;
    } catch (Exception e) {
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
      Query<Number> q = prepareIdentityObjectQuery(ctx,
                                                   identity,
                                                   relationshipType,
                                                   excludes,
                                                   parent,
                                                   criteria,
                                                   false);

      return q.uniqueResult().intValue();
    } catch (Exception e) {
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
      Query<IdentityObject> q = prepareIdentityObjectQuery(
                                                           ctx,
                                                           identity,
                                                           relationshipType,
                                                           excludes,
                                                           parent,
                                                           criteria,
                                                           false);
      List<IdentityObject> results = q.list();
      Hibernate.initialize(results);
      if (criteria != null && criteria.isFiltered()) {
        filterByAttributesValues(results, criteria.getValues());
        if (criteria.isPaged()) {
          return cutPageFromResults(results, criteria);
        }
      }
      return results;
    } catch (Exception e) {
      throw new IdentityException("Cannot find IdentityObjects", e);
    }
  }

  public <T> Query<T> prepareIdentityObjectQuery(IdentityStoreInvocationContext ctx, // NOSONAR
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
        if (excludes != null && !excludes.isEmpty()) {
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
        if (excludes != null && !excludes.isEmpty()) {
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

      @SuppressWarnings({ "unchecked", "deprecation" })
      Query<T> q = getHibernateSession(ctx).createQuery(hqlString.toString()) // NOSONAR
                                           .setParameter(IDENTITY_PARAM, hibernateObject)
                                           .setParameter(REALM_PARAM, realm)
                                           .setCacheable(true);
      if (relationshipType != null) {
        q.setParameter("relType", relationshipType.getName());
      }
      if (criteria != null && criteria.getFilter() != null) {
        q.setParameter("nameFilter", criteria.getFilter().replace("\\*", "%").replace("*", "%"));
      } else {
        q.setParameter("nameFilter", "%");
      }
      if (excludes != null && !excludes.isEmpty()) {
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
      return q;
    } catch (Exception e) {
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

    if (!getSupportedFeatures().isRelationshipTypeSupported(fromIO.getIdentityType(), toIO.getIdentityType(), relationshipType)
        && !isAllowNotDefinedIdentityObjectTypes()) {
      throw new IdentityException("Relationship not supported. RelationshipType[ " + relationshipType.getName() + " ] " +
          "beetween: [ " + fromIO.getIdentityType().getName() + " ] and [ " + toIO.getIdentityType().getName() + " ]");
    }

    HibernateIdentityObjectRelationship relationship = null;
    HibernateRealm realm = getRealm(getHibernateSession(ctx), ctx);
    if (name != null) {
      HibernateIdentityObjectRelationshipName relationshipName =
                                                               findIdentityObjectRelationshipNameByName(getHibernateSession(ctx),
                                                                                                        name,
                                                                                                        realm.getName());
      if (relationshipName == null) {
        throw new IdentityException("Relationship name " + name + " not present in the store");
      }
      relationship = new HibernateIdentityObjectRelationship(type, fromIO, toIO, relationshipName);
    } else {
      relationship = new HibernateIdentityObjectRelationship(type, fromIO, toIO);
    }

    try {
      Session session = getHibernateSession(ctx);
      session.persist(relationship);
      session.flush();
      return relationship;
    } catch (HibernateException e) {
      throw new IdentityException("Cannot create relationship: ", e);
    }
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

    HibernateIdentityObjectRelationship relationship;
    if (name == null) {
      relationship = findIdentityObjectRelationshipByIdentityByType(getHibernateSession(ctx), fromIO, toIO, type.getName());
    } else {
      relationship = findIdentityObjectRelationshipByAttributes(getHibernateSession(ctx), fromIO, toIO, type.getId(), name);
    }
    if (relationship == null) {
      throw new IdentityException("Relationship not present in the store");
    } else {
      try {
        fromIO.getFromRelationships().remove(relationship);
        toIO.getToRelationships().remove(relationship);
        getHibernateSession(ctx).remove(relationship);
        getHibernateSession(ctx).flush();
      } catch (HibernateException e) {
        throw new IdentityException("Cannot remove relationship");
      }
    }
  }

  public void removeRelationships(IdentityStoreInvocationContext ctx,
                                  IdentityObject identity1,
                                  IdentityObject identity2,
                                  boolean named) throws IdentityException {
    HibernateIdentityObject hio1 = safeGet(ctx, identity1);
    HibernateIdentityObject hio2 = safeGet(ctx, identity2);
    List<HibernateIdentityObjectRelationship> results = findIdentityObjectRelationshipsByIdentities(getHibernateSession(ctx),
                                                                                                    hio1,
                                                                                                    hio2);
    Hibernate.initialize(results);
    for (HibernateIdentityObjectRelationship relationship : results) {
      if ((named && relationship.getName() != null)
          || (!named && relationship.getName() == null)) {
        try {
          relationship.getFromIdentityObject().getFromRelationships().remove(relationship);
          relationship.getToIdentityObject().getToRelationships().remove(relationship);
          getHibernateSession(ctx).remove(relationship);
          getHibernateSession(ctx).flush();
        } catch (HibernateException e) {
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
    List<HibernateIdentityObjectRelationship> results;
    if (relationshipType != null) {
      HibernateIdentityObjectRelationship relationship =
                                                       findIdentityObjectRelationshipByIdentityByType(getHibernateSession(ctx),
                                                                                                      hio1,
                                                                                                      hio2,
                                                                                                      relationshipType.getName());
      results = relationship == null ? Collections.emptyList() : Collections.singletonList(relationship);
    } else {
      results = findIdentityObjectRelationshipsByIdentities(getHibernateSession(ctx), hio1, hio2);
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

    Query<Number> query = prepareResolveRelationshipsCriteria(
                                                              ctx,
                                                              identity,
                                                              type,
                                                              parent,
                                                              named,
                                                              name,
                                                              searchCriteria,
                                                              true);
    Number count = query.uniqueResult();
    return count == null ? 0 : count.intValue();

  }

  public Set<IdentityObjectRelationship> resolveRelationships(IdentityStoreInvocationContext ctx,
                                                              IdentityObject identity,
                                                              IdentityObjectRelationshipType type,
                                                              boolean parent,
                                                              boolean named,
                                                              String name,
                                                              IdentityObjectSearchCriteria searchCriteria) throws IdentityException {
    Query<HibernateIdentityObjectRelationship> query = prepareResolveRelationshipsCriteria(ctx,
                                                                                           identity,
                                                                                           type,
                                                                                           parent,
                                                                                           named,
                                                                                           name,
                                                                                           searchCriteria,
                                                                                           false);
    List<HibernateIdentityObjectRelationship> results = query.list();
    Hibernate.initialize(results);
    return new HashSet<>(results);
  }

  @SuppressWarnings("unchecked")
  public <T> Query<T> prepareResolveRelationshipsCriteria(IdentityStoreInvocationContext ctx, // NOSONAR
                                                          IdentityObject identity,
                                                          IdentityObjectRelationshipType type,
                                                          boolean parent,
                                                          boolean named,
                                                          String name,
                                                          IdentityObjectSearchCriteria searchCriteria,
                                                          boolean count) throws IdentityException {
    HibernateIdentityObject hio = safeGet(ctx, identity);

    StringBuilder queryString = new StringBuilder(count ? "SELECT COUNT(r) FROM HibernateIdentityObjectRelationship r" :
                                                        "SELECT r FROM HibernateIdentityObjectRelationship r");
    queryString.append(" FETCH JOIN r.fromIdentityObject fromIo");
    queryString.append(" FETCH JOIN r.toIdentityObject toIo");
    queryString.append(" WHERE");

    List<String> paramNames = new ArrayList<>();
    List<Object> paramValues = new ArrayList<>();
    if (type != null) {
      HibernateIdentityObjectRelationshipType hibernateType = getHibernateIdentityObjectRelationshipType(ctx, type);
      paramNames.add("type");
      paramValues.add(hibernateType);

      queryString.append(" r.type = :type");
      queryString.append(" AND");
    }

    paramNames.add(IDENTITY_PARAM);
    paramValues.add(hio);
    if (parent) {
      queryString.append(" r.fromIdentityObject = :identity");
      queryString.append(" AND");
    } else {
      queryString.append(" r.toIdentityObject = :identity");
      queryString.append(" AND");
    }

    if (name != null) {
      paramNames.add("name");
      paramValues.add(name);

      queryString.append(" r.name.name = :name");
    } else if (named) {
      queryString.append(" r.name IS NOT NULL");
    } else {
      queryString.append(" r.name.name IS NULL");
    }
    if (searchCriteria != null && searchCriteria.isSorted()) {
      if (parent) {
        if (searchCriteria.isAscending()) {
          queryString.append(" ORDER BY toIo.name ASC");
        } else {
          queryString.append(" ORDER BY toIo.name DESC");
        }
      } else {
        if (searchCriteria.isAscending()) {
          queryString.append(" ORDER BY fromIo.name ASC");
        } else {
          queryString.append(" ORDER BY fromIo.name DESC");
        }
      }
    }
    @SuppressWarnings({ "rawtypes", "deprecation" })
    Query query = getHibernateSession(ctx).createQuery(queryString.toString()); // NOSONAR
    if (searchCriteria != null && searchCriteria.isPaged() && !searchCriteria.isFiltered()) {
      if (searchCriteria.getMaxResults() > 0) {
        query.setMaxResults(searchCriteria.getMaxResults());
      }
      query.setFirstResult(searchCriteria.getFirstResult());
    }
    for (int i = 0; i < paramNames.size(); i++) {
      query.setParameter(paramNames.get(i), paramValues.get(i));
    }
    return query;
  }

  public String createRelationshipName(IdentityStoreInvocationContext ctx, String name) throws IdentityException {
    checkName(name);
    Session hibernateSession = getHibernateSession(ctx);
    HibernateRealm realm = getRealm(hibernateSession, ctx);
    try {
      HibernateIdentityObjectRelationshipName hiorn = findIdentityObjectRelationshipNameByName(hibernateSession,
                                                                                               name,
                                                                                               realm.getName());
      if (hiorn != null) {
        throw new IdentityException("Relationship name already exists");
      }
      hiorn = new HibernateIdentityObjectRelationshipName(name, realm);
      getHibernateSession(ctx).persist(hiorn);
      getHibernateSession(ctx).flush();
      return name;
    } catch (Exception e) {
      throw new IdentityException("Cannot create new relationship name: " + name, e);
    }
  }

  public String removeRelationshipName(IdentityStoreInvocationContext ctx, String name) throws IdentityException {
    checkName(name);

    Session hibernateSession = getHibernateSession(ctx);
    try {
      HibernateIdentityObjectRelationshipName relationshipName = getRelationshipName(ctx, name, hibernateSession);
      removeRelationshipsByName(ctx, relationshipName);
      hibernateSession.remove(relationshipName);
      hibernateSession.flush();
      return name;
    } catch (Exception e) {
      throw new IdentityException("Cannot remove new relationship name: " + name, e);
    }
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx,
                                          IdentityObjectSearchCriteria criteria) throws IdentityException {
    return getRelationshipNames(ctx, criteria, null);
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx) throws IdentityException {
    return getRelationshipNames(ctx, (IdentityObjectSearchCriteria) null);
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx,
                                          IdentityObject identity,
                                          IdentityObjectSearchCriteria criteria) throws IdentityException {
    if (identity == null) {
      throw new IllegalArgumentException("identity is mandatory");
    }
    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);
    return getRelationshipNames(ctx, hibernateObject, criteria);
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx,
                                          IdentityObject identity) throws IdentityException {
    return getRelationshipNames(ctx, identity, null);
  }

  public Map<String, String> getRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                                           String name) throws IdentityException {
    checkName(name);
    Session hibernateSession = getHibernateSession(ctx);
    try {
      HibernateIdentityObjectRelationshipName relationshipName = getRelationshipName(ctx, name, hibernateSession);
      Hibernate.initialize(relationshipName.getProperties());
      return new HashMap<>(relationshipName.getProperties());
    } catch (Exception e) {
      throw new IdentityException("Cannot get relationship name properties: " + name, e);
    }
  }

  public void setRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                            String name,
                                            Map<String, String> properties) throws IdentityException {
    checkName(name);
    Session hibernateSession = getHibernateSession(ctx);
    try {
      HibernateIdentityObjectRelationshipName relationshipName = getRelationshipName(ctx, name, hibernateSession);
      relationshipName.getProperties().putAll(properties);
      hibernateSession.persist(relationshipName);
      hibernateSession.flush();
    } catch (Exception e) {
      throw new IdentityException("Cannot set relationship name properties: " + name, e);
    }
  }

  public void removeRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                               String name,
                                               Set<String> properties) throws IdentityException {
    checkName(name);
    Session hibernateSession = getHibernateSession(ctx);
    try {
      HibernateIdentityObjectRelationshipName relationshipName = getRelationshipName(ctx, name, hibernateSession);
      Hibernate.initialize(relationshipName.getProperties());

      for (String property : properties) {
        relationshipName.getProperties().remove(property);
      }
      hibernateSession.persist(relationshipName);
      hibernateSession.flush();
    } catch (Exception e) {
      throw new IdentityException("Cannot remove relationship name properties: " + name, e);
    }
  }

  public Map<String, String> getRelationshipProperties(IdentityStoreInvocationContext ctx,
                                                       IdentityObjectRelationship relationship) throws IdentityException {
    HibernateIdentityObject fromIO = safeGet(ctx, relationship.getFromIdentityObject());
    HibernateIdentityObject toIO = safeGet(ctx, relationship.getToIdentityObject());
    HibernateIdentityObjectRelationshipType type = getHibernateIdentityObjectRelationshipType(ctx, relationship.getType());
    try {
      HibernateIdentityObjectRelationship hibernateRelationship;
      if (relationship.getName() != null) {
        hibernateRelationship = findIdentityObjectRelationshipByAttributes(getHibernateSession(ctx),
                                                                           fromIO,
                                                                           toIO,
                                                                           type.getId(),
                                                                           relationship.getName());
      } else {
        hibernateRelationship = findIdentityObjectRelationshipByIdentityByType(getHibernateSession(ctx),
                                                                               fromIO,
                                                                               toIO,
                                                                               type.getName());
      }
      Hibernate.initialize(hibernateRelationship.getProperties());
      return new HashMap<>(hibernateRelationship.getProperties());
    } catch (HibernateException e) {
      throw new IdentityException(CANNOT_OBTAIN_RELATIONSHIP_PROPERTIES + relationship, e);
    }
  }

  public void setRelationshipProperties(IdentityStoreInvocationContext ctx,
                                        IdentityObjectRelationship relationship,
                                        Map<String, String> properties) throws IdentityException {
    HibernateIdentityObject fromIO = safeGet(ctx, relationship.getFromIdentityObject());
    HibernateIdentityObject toIO = safeGet(ctx, relationship.getToIdentityObject());
    HibernateIdentityObjectRelationshipType type = getHibernateIdentityObjectRelationshipType(ctx, relationship.getType());
    try {
      HibernateIdentityObjectRelationship hibernateRelationship;
      Session hibernateSession = getHibernateSession(ctx);
      if (relationship.getName() != null) {
        hibernateRelationship = findIdentityObjectRelationshipByAttributes(hibernateSession,
                                                                           fromIO,
                                                                           toIO,
                                                                           type.getId(),
                                                                           relationship.getName());
      } else {
        hibernateRelationship = findIdentityObjectRelationshipByIdentityByType(hibernateSession,
                                                                               fromIO,
                                                                               toIO,
                                                                               type.getName());
      }
      Hibernate.initialize(hibernateRelationship.getProperties());
      hibernateRelationship.getProperties().putAll(properties);
      hibernateSession.persist(hibernateRelationship);
      hibernateSession.flush();
    } catch (HibernateException e) {
      throw new IdentityException(CANNOT_OBTAIN_RELATIONSHIP_PROPERTIES + relationship, e);
    }
  }

  public void removeRelationshipProperties(IdentityStoreInvocationContext ctx,
                                           IdentityObjectRelationship relationship,
                                           Set<String> properties) throws IdentityException {
    HibernateIdentityObject fromIO = safeGet(ctx, relationship.getFromIdentityObject());
    HibernateIdentityObject toIO = safeGet(ctx, relationship.getToIdentityObject());
    HibernateIdentityObjectRelationshipType type = getHibernateIdentityObjectRelationshipType(ctx, relationship.getType());
    try {
      HibernateIdentityObjectRelationship hibernateRelationship;
      Session hibernateSession = getHibernateSession(ctx);
      if (relationship.getName() != null) {
        hibernateRelationship = findIdentityObjectRelationshipByAttributes(hibernateSession,
                                                                           fromIO,
                                                                           toIO,
                                                                           type.getId(),
                                                                           relationship.getName());
      } else {
        hibernateRelationship = findIdentityObjectRelationshipByIdentityByType(hibernateSession,
                                                                               fromIO,
                                                                               toIO,
                                                                               type.getName());
      }
      Hibernate.initialize(hibernateRelationship.getProperties());
      for (String property : properties) {
        hibernateRelationship.getProperties().remove(property);
      }
      hibernateSession.persist(hibernateRelationship);
      hibernateSession.flush();
    } catch (HibernateException e) {
      throw new IdentityException(CANNOT_OBTAIN_RELATIONSHIP_PROPERTIES + relationship, e);
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

    Set<HibernateIdentityObjectAttribute> storeAttributes = hibernateObject.getAttributes();

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

    HibernateIdentityObject hibernateIdentityObject = getHibernateIdentityObject(ctx, identity);
    List<HibernateIdentityObjectAttribute> storeAttributes = findIdentityAttributes(getHibernateSession(ctx),
                                                                                    hibernateIdentityObject);
    // Remap the names
    Map<String, IdentityObjectAttribute> result = new HashMap<>();
    for (HibernateIdentityObjectAttribute attribute : storeAttributes) {
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
    checkAttributes(attributes);

    Map<String, IdentityObjectAttribute> mappedAttributes = new HashMap<>();
    Map<String, IdentityObjectAttributeMetaData> mdMap = attributesMetaData.get(identity.getIdentityType().getName());
    for (IdentityObjectAttribute attribute : attributes) {
      String name = resolveAttributeStoreMapping(identity.getIdentityType(), attribute.getName());
      mappedAttributes.put(name, attribute);

      if ((mdMap == null || !mdMap.containsKey(attribute.getName())) && !isAllowNotDefinedAttributes) {
        throw new IdentityException("Cannot add not defined attribute. Use '" + ALLOW_NOT_DEFINED_ATTRIBUTES +
            OPTION_IF_NEEDED_ATTRIBUTE_NAME + attribute.getName());
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
                  ATTRIBUTE_NAME + name);
            }

            Set<String> v = new HashSet<>();
            for (Object value : attribute.getValues()) {
              v.add(value.toString());
            }

            storeAttribute.setTextValues(v);
          } else if (storeAttribute.getType().equals(HibernateIdentityObjectAttribute.TYPE_BINARY)) {

            if (!type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE)) {
              throw new IdentityException("Wrong attribute mapping. Attribute persisted as binary is mapped with: " + type +
                  ATTRIBUTE_NAME + name);
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

  @SuppressWarnings("unchecked")
  public void addAttributes(IdentityStoreInvocationContext ctx, // NOSONAR
                            IdentityObject identity,
                            IdentityObjectAttribute[] attributes) throws IdentityException {

    checkAttributes(attributes);

    Map<String, IdentityObjectAttribute> mappedAttributes = new HashMap<>();
    Map<String, IdentityObjectAttributeMetaData> mdMap = attributesMetaData.get(identity.getIdentityType().getName());
    for (IdentityObjectAttribute attribute : attributes) {
      String name = resolveAttributeStoreMapping(identity.getIdentityType(), attribute.getName());
      mappedAttributes.put(name, attribute);

      if ((mdMap == null || !mdMap.containsKey(attribute.getName())) &&
          !isAllowNotDefinedAttributes) {
        throw new IdentityException("Cannot add not defined attribute. Use '" + ALLOW_NOT_DEFINED_ATTRIBUTES +
            OPTION_IF_NEEDED_ATTRIBUTE_NAME + attribute.getName());

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

    Hibernate.initialize(hibernateObject.getAttributes());

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
                ATTRIBUTE_NAME + name);
          }

          Set<String> mergedValues = new HashSet<>(hibernateAttribute.getValues());
          for (Object value : attribute.getValues()) {
            mergedValues.add(value.toString());
          }

          hibernateAttribute.setTextValues(mergedValues);
        } else if (hibernateAttribute.getType().equals(HibernateIdentityObjectAttribute.TYPE_BINARY)) {

          if (!type.equals(IdentityObjectAttributeMetaData.BINARY_TYPE)) {
            throw new IdentityException("Wrong attribute mapping. Attribute persisted as binary is mapped with: " + type +
                ATTRIBUTE_NAME + name);
          }

          HibernateIdentityObjectAttributeBinaryValue bv =
                                                         new HibernateIdentityObjectAttributeBinaryValue((byte[]) attribute.getValue());
          getHibernateSession(ctx).persist(bv);
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
          getHibernateSession(ctx).persist(bv);
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
      throw new IllegalArgumentException("attributes are null");
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
              OPTION_IF_NEEDED_ATTRIBUTE_NAME + attributes[i]);
        }
      }

    }

    HibernateIdentityObject hibernateObject = safeGet(ctx, identity);

    Hibernate.initialize(hibernateObject.getAttributes());

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

    StringBuilder queryString =
                              new StringBuilder("select a from HibernateIdentityObjectAttribute a where a.identityObject.identityType = :identityType " +
                                  "and a.name = :attributeName and a.identityObject.realm = :realm");

    if (attrDuctTypeText) {
      for (int i = 0; i < attribute.getValues().size(); i++) {
        String paramName = " :value" + i;
        queryString.append(" and").append(paramName).append(" = any elements(a.textValues)");

      }
    } else {
      queryString.append(" and :value = a.binaryValue");
    }

    Query<HibernateIdentityObjectAttribute> q =
                                              session.createQuery(queryString.toString(), HibernateIdentityObjectAttribute.class);
    q.setParameter("identityType", hiot)
     .setParameter("attributeName", attrMappedName)
     .setParameter(REALM_PARAM, realm);

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
    if (attrs.isEmpty()) {
      return null;
    } else if (attrs.size() > 1) {
      throw new IdentityException("Illegal state - more than one IdentityObject with the same unique attribute value: " +
          attribute);
    } else {
      return attrs.get(0).getIdentityObject();
    }
  }

  public boolean validateCredential(IdentityStoreInvocationContext ctx,
                                    IdentityObject identityObject,
                                    IdentityObjectCredential credential) throws IdentityException {
    if (credential == null) {
      throw new IllegalArgumentException();
    }

    HibernateIdentityObject hibernateObject = safeGet(ctx, identityObject);

    if (supportedFeatures.isCredentialSupported(hibernateObject.getIdentityType(), credential.getType())) {

      HibernateIdentityObjectCredential hibernateCredential =
                                                            getHibernateSession(ctx).createNamedQuery("HibernateIdentityObjectCredential.findCredentialByTypeAndIdentity",
                                                                                                      HibernateIdentityObjectCredential.class)
                                                                                    .setParameter("cTypeName",
                                                                                                  credential.getType().getName())
                                                                                    .setParameter("ioId", hibernateObject.getId())
                                                                                    .uniqueResult();
      if (hibernateCredential == null) {
        return false;
      }

      Object tmpEncodedValue = credential.getEncodedValue();
      Object value = tmpEncodedValue == null ? credential.getValue() : tmpEncodedValue;
      if (value instanceof String valueString && hibernateCredential.getTextValue() != null) {
        return valueString.equals(hibernateCredential.getTextValue());
      } else if (value instanceof byte[] valueBytes && hibernateCredential.getBinaryValue() != null) {
        return Arrays.equals(valueBytes, hibernateCredential.getBinaryValue().getValue());
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

    if (supportedFeatures.isCredentialSupported(hibernateObject.getIdentityType(), credential.getType())) {

      HibernateIdentityObjectCredentialType hibernateCredentialType =
                                                                    getHibernateIdentityObjectCredentialType(ctx,
                                                                                                             credential.getType());

      if (hibernateCredentialType == null) {
        throw new IllegalStateException("Credential type not present in this store: " + credential.getType().getName());
      }

      HibernateIdentityObjectCredential hibernateCredential = hibernateObject.getCredential(credential.getType());

      if (hibernateCredential == null) {
        hibernateCredential = new HibernateIdentityObjectCredential();
        hibernateCredential.setType(hibernateCredentialType);
        hibernateObject.addCredential(hibernateCredential);
      }

      Object tmpEncodedValue = credential.getEncodedValue();
      Object value = tmpEncodedValue == null ? credential.getValue() : tmpEncodedValue;
      if (value instanceof String valueString) {
        hibernateCredential.setTextValue(valueString);
      } else if (value instanceof byte[] valueBytes) {
        HibernateIdentityObjectCredentialBinaryValue bv = new HibernateIdentityObjectCredentialBinaryValue(valueBytes);
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
      HibernateIdentityStoreSessionImpl hbIdentityStoreSession =
                                                               (HibernateIdentityStoreSessionImpl) ctx.getIdentityStoreSession();

      if (lazyStartOfHibernateTransaction) {
        hbIdentityStoreSession.startHibernateTransactionIfNotStartedYet();
      }

      return ((Session) hbIdentityStoreSession.getSessionContext());
    } catch (Exception e) {
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
    if (io instanceof HibernateIdentityObject identityObject) {
      return identityObject;
    } else {
      return getHibernateIdentityObject(ctx, io);
    }
  }

  private void checkIOType(IdentityObjectType iot) throws IdentityException {
    if (iot == null) {
      throw new IllegalArgumentException("IdentityObjectType is null");
    }

    if (!getSupportedFeatures().isIdentityObjectTypeSupported(iot) && !isAllowNotDefinedIdentityObjectTypes()) {
      throw new IdentityException("IdentityType not supported by this IdentityStore implementation: " + iot);
    }
  }

  private HibernateIdentityObjectType getHibernateIdentityObjectType(IdentityStoreInvocationContext ctx,
                                                                     IdentityObjectType type) throws IdentityException {
    checkIOType(type);
    HibernateIdentityObjectType hibernateType;
    String typeName = type.getName();
    try {
      Session hibernateSession = getHibernateSession(ctx);
      hibernateType = findIdentityObjectTypeByName(hibernateSession, typeName);
      if (hibernateType == null) {
        if (isAllowNotDefinedIdentityObjectTypes()) {
          populateObjectTypes(hibernateSession, new String[] { typeName });
        }
        hibernateType = findIdentityObjectTypeByName(hibernateSession, typeName);
      }
    } catch (Exception e) {
      throw new IdentityException("IdentityObjectType[" + typeName + NOT_PRESENT_IN_THE_STORE, e);
    }
    if (hibernateType == null) {
      throw new IdentityException("IdentityObjectType[" + typeName + NOT_PRESENT_IN_THE_STORE);
    }
    return hibernateType;
  }

  private HibernateIdentityObject getHibernateIdentityObject(IdentityStoreInvocationContext ctx,
                                                             IdentityObject io) throws IdentityException {

    Session hibernateSession = getHibernateSession(ctx);
    HibernateIdentityObjectType hibernateType = getHibernateIdentityObjectType(ctx, io.getIdentityType());
    HibernateRealm realm = getRealm(hibernateSession, ctx);
    HibernateIdentityObject identity;
    try {
      identity = findIdentityObjectByNameAndType(hibernateSession, io.getName(), hibernateType.getName(), realm.getName());
    } catch (Exception e) {
      throw new IdentityException("IdentityObject[ " + io.getName() + " | " + io.getIdentityType().getName() +
          NOT_PRESENT_IN_THE_STORE, e);
    }
    if (identity == null) {
      throw new IdentityException("IdentityObject[ " + io.getName() + " | " + io.getIdentityType().getName() +
          NOT_PRESENT_IN_THE_STORE);
    }
    return identity;
  }

  private HibernateIdentityObjectRelationshipType getHibernateIdentityObjectRelationshipType(IdentityStoreInvocationContext ctx,
                                                                                             IdentityObjectRelationshipType iot) throws IdentityException {

    try {
      return findIdentityRelationshipTypeByName(getHibernateSession(ctx), iot.getName());
    } catch (Exception e) {
      throw new IdentityException("IdentityObjectRelationshipType[ " + iot.getName() + NOT_PRESENT_IN_THE_STORE);
    }
  }

  private HibernateIdentityObjectCredentialType getHibernateIdentityObjectCredentialType(IdentityStoreInvocationContext ctx,
                                                                                         IdentityObjectCredentialType credentialType) throws IdentityException {
    try {
      return findIdentityCredentialTypeByName(getHibernateSession(ctx), credentialType.getName());
    } catch (HibernateException e) {
      throw new IdentityException("IdentityObjectCredentialType[ " + credentialType.getName() + NOT_PRESENT_IN_THE_STORE);
    }
  }

  public void populateObjectTypes(Session hibernateSession, String[] typeNames) {
    for (String typeName : typeNames) {
      // Check if present
      HibernateIdentityObjectType hibernateType = findIdentityObjectTypeByName(hibernateSession, typeName);
      if (hibernateType == null) {
        hibernateType = new HibernateIdentityObjectType(typeName);
        hibernateSession.persist(hibernateType);
        hibernateSession.flush();
      }
    }
  }

  public void populateRelationshipTypes(Session hibernateSession, String[] typeNames) {
    for (String typeName : typeNames) {
      HibernateIdentityObjectRelationshipType relationshipType = findIdentityRelationshipTypeByName(hibernateSession, typeName);
      if (relationshipType == null) {
        relationshipType = new HibernateIdentityObjectRelationshipType(typeName);
        hibernateSession.persist(relationshipType);
        hibernateSession.flush();
      }
    }
  }

  public void populateCredentialTypes(Session hibernateSession, String[] typeNames) {
    for (String typeName : typeNames) {
      HibernateIdentityObjectCredentialType hibernateType = findIdentityCredentialTypeByName(hibernateSession, typeName);
      if (hibernateType == null) {
        hibernateType = new HibernateIdentityObjectCredentialType(typeName);
        hibernateSession.persist(hibernateType);
        hibernateSession.flush();
      }
    }
  }

  public void addRealm(Session hibernateSession, String realmName) throws IdentityException {
    try {
      HibernateRealm realm = new HibernateRealm(realmName);
      hibernateSession.persist(realm);
      hibernateSession.flush();
    } catch (Exception e) {
      throw new IdentityException("Failed to create store realm", e);
    }
  }

  private HibernateRealm getRealm(Session hibernateSession, IdentityStoreInvocationContext ctx) throws IdentityException {
    if (getRealmName(ctx) == null) {
      throw new IllegalStateException("Realm Id not present");
    }

    // If store is not realm aware return null to create/get objects accessible
    // from other realms
    if (!isRealmAware()) {
      HibernateRealm realm = getRealmByName(hibernateSession, DEFAULT_REALM_NAME);
      if (realm == null) {
        throw new IdentityException("Default store realm is not present: " + DEFAULT_REALM_NAME);
      } else {
        return realm;
      }
    } else {
      HibernateRealm realm = getRealmByName(hibernateSession, getRealmName(ctx));
      if (realm == null) {
        HibernateRealm newRealm = new HibernateRealm(getRealmName(ctx));
        hibernateSession.persist(newRealm);
        hibernateSession.flush();
        return newRealm;
      } else {
        return realm;
      }
    }
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

  // TODO: this kills performance and is present here only as "quick" hack to // NOSONAR
  // have the feature present and let to add test cases. Needs to be redone at the hibernate query level
  @SuppressWarnings("rawtypes")
  private void filterByAttributesValues(Collection<IdentityObject> objects, Map<String, String[]> attrs) { // NOSONAR
    Set<IdentityObject> toRemove = new HashSet<>();

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

  private void removeRelationshipsByName(IdentityStoreInvocationContext ctx,
                                         HibernateIdentityObjectRelationshipName hiorn) throws IdentityException {
    List<HibernateIdentityObjectRelationship> relationships =
                                                            getHibernateSession(ctx).createNamedQuery("HibernateIdentityObjectRelationship.getRelationshipsByName",
                                                                                                      HibernateIdentityObjectRelationship.class)
                                                                                    .setParameter("nameId", hiorn.getId())
                                                                                    .list();

    Hibernate.initialize(relationships);

    // Remove all present usages
    for (HibernateIdentityObjectRelationship rel : relationships) {
      removeRelationship(ctx, rel.getFromIdentityObject(), rel.getToIdentityObject(), rel.getType(), rel.getName());
    }
  }

  private void checkName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
  }

  private void checkAttributes(IdentityObjectAttribute[] attributes) {
    if (attributes == null) {
      throw new IllegalArgumentException("attributes are null");
    }
  }

  private HibernateIdentityObjectRelationshipName getRelationshipName(IdentityStoreInvocationContext ctx,
                                                                      String name,
                                                                      Session hibernateSession) throws IdentityException {
    HibernateIdentityObjectRelationshipName relationshipName = findIdentityObjectRelationshipNameByName(hibernateSession,
                                                                                                        name,
                                                                                                        getRealmName(ctx));
    checkRelationshipName(name, relationshipName);
    return relationshipName;
  }

  private void checkRelationshipName(String name,
                                     HibernateIdentityObjectRelationshipName relationshipName) throws IdentityException {
    if (relationshipName == null) {
      throw new IdentityException("Relationship name " + name + " doesn't exist");
    }
  }

  private Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx, // NOSONAR
                                           IdentityObjectSearchCriteria criteria,
                                           HibernateIdentityObject hibernateObject) throws IdentityException {
    List<String> paramNames = new ArrayList<>();
    List<Object> paramValues = new ArrayList<>();
    StringBuilder queryString = new StringBuilder();
    if (hibernateObject != null) {
      queryString.append("SELECT rn.name FROM HibernateIdentityObjectRelationshipName rn");
      queryString.append("INNER JOIN r.name rn");
      queryString.append(" WHERE");
    } else {
      queryString.append(" SELECT rn.name FROM HibernateIdentityObjectRelationship r");
      queryString.append(" INNER JOIN r.name rn");
      queryString.append(" WHERE (r.fromIdentityObject = :identity OR r.toIdentityObject = :identity)");
      queryString.append(" AND");
      paramNames.add(IDENTITY_PARAM);
      paramValues.add(hibernateObject);
    }

    try {
      paramNames.add(REALM_NAME_PARAM);
      paramValues.add(getRealmName(ctx));
      queryString.append(" rn.realm.name = :").append(REALM_NAME_PARAM);

      if (criteria != null && criteria.getFilter() != null) {
        paramNames.add(NAME_PARAM);
        paramValues.add(criteria.getFilter().replace("\\*", "%").replace("*", "%"));
        queryString.append(" AND rn.name LIKE :").append(NAME_PARAM);
      } else {
        queryString.append(" AND rn.name IS NOT NULL");
      }

      if (hibernateObject != null) {
        queryString.append(" AND (rn.name IS NOT NULL)");
      }

      if (criteria != null && criteria.isSorted()) {
        if (criteria.isAscending()) {
          queryString.append(" ORDER BY rn.name ASC");
        } else {
          queryString.append(" ORDER BY rn.name DESC");
        }
      }

      Query<String> query = getHibernateSession(ctx).createQuery(queryString.toString(), String.class); // NOSONAR
      if (criteria != null && criteria.isPaged() && !criteria.isFiltered()) {
        if (criteria.getMaxResults() > 0) {
          query.setMaxResults(criteria.getMaxResults());
        }
        query.setFirstResult(criteria.getFirstResult());
      }
      for (int i = 0; i < paramNames.size(); i++) {
        query.setParameter(paramNames.get(i), paramValues.get(i));
      }

      List<String> results = query.list();
      Hibernate.initialize(results);
      return new HashSet<>(results);
    } catch (Exception e) {
      throw new IdentityException("Cannot get relationship names. ", e);
    }
  }

  private HibernateRealm getRealmByName(Session hibernateSession, String name) {
    return hibernateSession.createNamedQuery("HibernateRealm.findRealmByName", HibernateRealm.class)
                           .setParameter(NAME_PARAM, name)
                           .uniqueResult();
  }

  private int countIdentityObjectByNameAndType(Session hibernateSession,
                                               String identityName,
                                               String identityType,
                                               String realmName) {
    Number result = hibernateSession.createNamedQuery("HibernateIdentityObject.countIdentityObjectByNameAndType", Number.class)
                                    .setParameter(REALM_NAME_PARAM, realmName)
                                    .setParameter(NAME_PARAM, identityName)
                                    .setParameter(IDENTITY_TYPE_NAME, identityType)
                                    .uniqueResult();
    return result == null ? 0 : result.intValue();
  }

  private HibernateIdentityObject findIdentityObjectByNameAndType(Session hibernateSession,
                                                                  String identityName,
                                                                  String identityType,
                                                                  String realmName) {
    return hibernateSession.createNamedQuery("HibernateIdentityObject.findIdentityObjectByNameAndType",
                                             HibernateIdentityObject.class)
                           .setParameter(REALM_NAME_PARAM, realmName)
                           .setParameter(NAME_PARAM, identityName)
                           .setParameter(IDENTITY_TYPE_NAME, identityType)
                           .uniqueResult();
  }

  private HibernateIdentityObjectRelationshipName findIdentityObjectRelationshipNameByName(Session hibernateSession,
                                                                                           String relationshipName,
                                                                                           String realmName) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectRelationshipName.findIdentityObjectRelationshipNameByName",
                                             HibernateIdentityObjectRelationshipName.class)
                           .setParameter(REALM_NAME_PARAM, realmName)
                           .setParameter(NAME_PARAM, relationshipName)
                           .uniqueResult();
  }

  private HibernateIdentityObjectRelationship findIdentityObjectRelationshipByIdentityByType(Session hibernateSession,
                                                                                             HibernateIdentityObject fromIdentityObject,
                                                                                             HibernateIdentityObject toIdentityObject,
                                                                                             String typeName) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByIdentityByType",
                                             HibernateIdentityObjectRelationship.class)
                           .setParameter("fromIdentityObject", fromIdentityObject)
                           .setParameter("toIdentityObject", toIdentityObject)
                           .setParameter(TYPE_NAME_PARAM, typeName)
                           .uniqueResult();
  }

  private HibernateIdentityObjectRelationship findIdentityObjectRelationshipByAttributes(Session hibernateSession,
                                                                                         HibernateIdentityObject fromIdentityObject,
                                                                                         HibernateIdentityObject toIdentityObject,
                                                                                         Long typeId,
                                                                                         String relationName) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByAttributes",
                                             HibernateIdentityObjectRelationship.class)
                           .setParameter("fromIdentityObject", fromIdentityObject)
                           .setParameter("toIdentityObject", toIdentityObject)
                           .setParameter("typeId", typeId)
                           .setParameter("name", relationName)
                           .uniqueResult();
  }

  private List<HibernateIdentityObjectRelationship> findIdentityObjectRelationshipsByIdentities(Session hibernateSession,
                                                                                                HibernateIdentityObject hio1,
                                                                                                HibernateIdentityObject hio2) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectRelationship.findIdentityObjectRelationshipsByIdentities",
                                             HibernateIdentityObjectRelationship.class)
                           .setParameter("hio1", hio1)
                           .setParameter("hio2", hio2)
                           .list();
  }

  private List<HibernateIdentityObjectAttribute> findIdentityAttributes(Session hibernateSession,
                                                                        HibernateIdentityObject identityObject) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectAttribute.findIdentityAttributes",
                                             HibernateIdentityObjectAttribute.class)
                           .setParameter("identityObject", identityObject)
                           .list();
  }

  private HibernateIdentityObjectRelationshipType findIdentityRelationshipTypeByName(Session hibernateSession,
                                                                                     String name) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectRelationshipType.findIdentityRelationshipTypeByName",
                                             HibernateIdentityObjectRelationshipType.class)
                           .setParameter(NAME_PARAM, name)
                           .uniqueResult();
  }

  private HibernateIdentityObjectCredentialType findIdentityCredentialTypeByName(Session hibernateSession,
                                                                                 String name) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectCredentialType.findIdentityCredentialTypeByName",
                                             HibernateIdentityObjectCredentialType.class)
                           .setParameter(NAME_PARAM, name)
                           .uniqueResult();
  }

  private HibernateIdentityObjectType findIdentityObjectTypeByName(Session hibernateSession, String typeName) {
    return hibernateSession.createNamedQuery("HibernateIdentityObjectType.findIdentityObjectTypeByName",
                                             HibernateIdentityObjectType.class)
                           .setParameter(NAME_PARAM, typeName)
                           .uniqueResult();
  }

}
