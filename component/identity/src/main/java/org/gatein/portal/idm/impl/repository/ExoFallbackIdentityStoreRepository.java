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
package org.gatein.portal.idm.impl.repository;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.repository.AbstractIdentityStoreRepository;
import org.picketlink.idm.impl.store.SimpleIdentityStoreInvocationContext;
import org.picketlink.idm.spi.configuration.IdentityRepositoryConfigurationContext;
import org.picketlink.idm.spi.configuration.IdentityStoreConfigurationContext;
import org.picketlink.idm.spi.configuration.metadata.*;
import org.picketlink.idm.spi.exception.OperationNotSupportedException;
import org.picketlink.idm.spi.model.*;
import org.picketlink.idm.spi.search.IdentityObjectSearchCriteria;
import org.picketlink.idm.spi.store.*;

/**
 * An implementation used by External Store API to manage between internal and
 * external store entities
 */
public class ExoFallbackIdentityStoreRepository extends AbstractIdentityStoreRepository {

  private static final long            serialVersionUID                     = -4857082329385386057L;

  private String                       id                                   = null;

  private String                       externalStoreId                      = null;

  private IdentityStore                externalIdentityStore                = null;

  private AttributeStore               externalAttributeStore               = null;

  private IdentityStoreMappingMetaData externalIdentityStoreMappingMetaData = null;

  private ThreadLocal<Boolean>         useExternalStore                     = new ThreadLocal<>();

  private FeaturesMetaData             featuresMetaData                     = null;

  public ExoFallbackIdentityStoreRepository(String id) {
    this.id = id;
  }

  @Override
  public void bootstrap(IdentityRepositoryConfigurationContext configurationContext,
                        Map<String, IdentityStore> bootstrappedIdentityStores,
                        Map<String, AttributeStore> bootstrappedAttributeStores) throws IdentityException {
    IdentityRepositoryConfigurationMetaData configurationMD = configurationContext.getRepositoryConfigurationMetaData();
    String defaultStoreId = configurationMD.getDefaultIdentityStoreId();
    if (StringUtils.isBlank(defaultStoreId)) {
      throw new IllegalStateException("Default store is not configured");
    }
    Set<String> identityStores = bootstrappedIdentityStores.keySet();
    if ((identityStores.contains(defaultStoreId) && identityStores.size() > 2)
        || (!identityStores.contains(defaultStoreId) && identityStores.size() > 1)) {
      throw new IllegalStateException("More than 2 store are configured, only two stores at most are supported.");
    }

    if ((identityStores.contains(defaultStoreId) && identityStores.size() > 1)
        || (!identityStores.contains(defaultStoreId) && identityStores.size() > 0)) {
      for (String storeId : identityStores) {
        if (defaultStoreId.equals(storeId)) {
          continue;
        }
        externalStoreId = storeId;
        break;
      }
    }

    if (StringUtils.isNotBlank(externalStoreId)) {
      externalIdentityStore = bootstrappedIdentityStores.get(externalStoreId);
      externalAttributeStore = bootstrappedAttributeStores.get(externalStoreId);

      // Keep only Hibernate Mapping
      Iterator<IdentityStoreMappingMetaData> iterator = configurationMD.getIdentityStoreToIdentityObjectTypeMappings().iterator();
      while (iterator.hasNext()) {
        IdentityStoreMappingMetaData mappingMetaData = iterator.next();
        if (!defaultStoreId.equals(mappingMetaData.getIdentityStoreId())) {
          externalIdentityStoreMappingMetaData = mappingMetaData;
          continue;
        }
      }
    }

    featuresMetaData = new FeaturesMetaData() {
      public boolean isNamedRelationshipsSupported() {
        return defaultIdentityStore.getSupportedFeatures().isNamedRelationshipsSupported();
      }

      public boolean isRelationshipPropertiesSupported() {
        return defaultIdentityStore.getSupportedFeatures().isRelationshipPropertiesSupported();
      }

      public boolean isRelationshipNameAddRemoveSupported() {
        return defaultIdentityStore.getSupportedFeatures().isRelationshipNameAddRemoveSupported();
      }

      public boolean isSearchCriteriaTypeSupported(IdentityObjectType identityObjectType,
                                                   IdentityObjectSearchCriteriaType storeSearchConstraint) {
        return getIdentityStore().getSupportedFeatures().isSearchCriteriaTypeSupported(identityObjectType, storeSearchConstraint);
      }

      public Set<String> getSupportedIdentityObjectTypes() {
        return getIdentityStore().getSupportedFeatures().getSupportedRelationshipTypes();
      }

      public boolean isIdentityObjectTypeSupported(IdentityObjectType identityObjectType) {
        return getIdentityStore().getSupportedFeatures().isIdentityObjectTypeSupported(identityObjectType);
      }

      public boolean isRelationshipTypeSupported(IdentityObjectType fromType,
                                                 IdentityObjectType toType,
                                                 IdentityObjectRelationshipType relationshipType) throws IdentityException {
        return getIdentityStore().getSupportedFeatures().isRelationshipTypeSupported(fromType, toType, relationshipType);
      }

      public Set<String> getSupportedRelationshipTypes() {
        return getIdentityStore().getSupportedFeatures().getSupportedRelationshipTypes();
      }

      public boolean isCredentialSupported(IdentityObjectType identityObjectType, IdentityObjectCredentialType credentialType) {
        return getIdentityStore().getSupportedFeatures().isCredentialSupported(identityObjectType, credentialType);
      }

      public boolean isIdentityObjectAddRemoveSupported(IdentityObjectType objectType) {
        return getIdentityStore().getSupportedFeatures().isIdentityObjectAddRemoveSupported(objectType);
      }

      public boolean isRoleNameSearchCriteriaTypeSupported(IdentityObjectSearchCriteriaType constraint) {
        return getIdentityStore().getSupportedFeatures().isRoleNameSearchCriteriaTypeSupported(constraint);
      }
    };

    super.bootstrap(configurationContext, bootstrappedIdentityStores, bootstrappedAttributeStores);
  }

  public void bootstrap(IdentityStoreConfigurationContext configurationContext) throws IdentityException {
    // Nothing
  }

  public IdentityStoreSession createIdentityStoreSession() throws IdentityException {
    Map<String, IdentityStoreSession> sessions = new HashMap<String, IdentityStoreSession>();

    sessions.put(getAttributeStore().getId(), getAttributeStore().createIdentityStoreSession());

    if (!sessions.containsKey(getIdentityStore().getId())) {
      sessions.put(getIdentityStore().getId(), getIdentityStore().createIdentityStoreSession());
    }

    return new ExoRepositoryIdentityStoreSessionImpl(sessions, null);
  }

  private AttributeStore getAttributeStore() {
    if (isUseExternalStore()) {
      return externalAttributeStore;
    } else {
      return defaultAttributeStore;
    }
  }

  public IdentityStore getIdentityStore() {
    if (isUseExternalStore()) {
      return externalIdentityStore;
    } else {
      return defaultIdentityStore;
    }
  }

  public IdentityStoreSession createIdentityStoreSession(Map<String, Object> sessionOptions) throws IdentityException {
    Map<String, IdentityStoreSession> sessions = new HashMap<String, IdentityStoreSession>();

    sessions.put(getAttributeStore().getId(), getAttributeStore().createIdentityStoreSession(sessionOptions));

    if (!sessions.containsKey(getIdentityStore().getId())) {
      sessions.put(getIdentityStore().getId(), getIdentityStore().createIdentityStoreSession(sessionOptions));
    }

    return new ExoRepositoryIdentityStoreSessionImpl(sessions, sessionOptions);
  }

  IdentityStoreInvocationContext resolveIdentityStoreInvocationContext(IdentityStoreInvocationContext invocationCtx) throws IdentityException {
    return resolveInvocationContext(getIdentityStore().getId(), invocationCtx);
  }

  IdentityStoreInvocationContext resolveAttributeStoreInvocationContext(IdentityStoreInvocationContext invocationCtx) throws IdentityException {
    return resolveInvocationContext(getAttributeStore().getId(), invocationCtx);
  }

  IdentityStoreInvocationContext resolveInvocationContext(String id,
                                                          IdentityStoreInvocationContext invocationCtx) throws IdentityException {
    ExoRepositoryIdentityStoreSessionImpl repoSession =
                                                      (ExoRepositoryIdentityStoreSessionImpl) invocationCtx.getIdentityStoreSession();
    IdentityStoreSession targetSession = repoSession.getIdentityStoreSession(id);
    if (targetSession == null && isUseExternalStore()) {
      targetSession = getIdentityStore().createIdentityStoreSession(repoSession.getSessionOptions());
      repoSession.addIdentityStoreSessionMapping(getIdentityStore().getId(), targetSession);
    }

    return new SimpleIdentityStoreInvocationContext(targetSession, invocationCtx.getRealmId(), String.valueOf(this.hashCode()));
  }

  public String getId() {
    return id;
  }

  public FeaturesMetaData getSupportedFeatures() {
    return featuresMetaData;
  }

  public IdentityObject createIdentityObject(IdentityStoreInvocationContext invocationCtx,
                                             String name,
                                             IdentityObjectType identityObjectType) throws IdentityException {
    return getIdentityStore().createIdentityObject(resolveIdentityStoreInvocationContext(invocationCtx),
                                                   name,
                                                   identityObjectType);
  }

  public IdentityObject createIdentityObject(IdentityStoreInvocationContext invocationCtx,
                                             String name,
                                             IdentityObjectType identityObjectType,
                                             Map<String, String[]> attributes) throws IdentityException {
    return getIdentityStore().createIdentityObject(resolveIdentityStoreInvocationContext(invocationCtx),
                                                   name,
                                                   identityObjectType,
                                                   attributes);
  }

  public void removeIdentityObject(IdentityStoreInvocationContext invocationCtx,
                                   IdentityObject identity) throws IdentityException {
    getIdentityStore().removeIdentityObject(resolveIdentityStoreInvocationContext(invocationCtx), identity);
  }

  public int getIdentityObjectsCount(IdentityStoreInvocationContext invocationCtx,
                                     IdentityObjectType identityType) throws IdentityException {
    return getIdentityStore().getIdentityObjectsCount(resolveIdentityStoreInvocationContext(invocationCtx), identityType);
  }

  public IdentityObject findIdentityObject(IdentityStoreInvocationContext invocationContext,
                                           String name,
                                           IdentityObjectType identityObjectType) throws IdentityException {
    return getIdentityStore().findIdentityObject(resolveIdentityStoreInvocationContext(invocationContext),
                                                 name,
                                                 identityObjectType);
  }

  public IdentityObject findIdentityObject(IdentityStoreInvocationContext invocationContext, String id) throws IdentityException {
    return getIdentityStore().findIdentityObject(resolveIdentityStoreInvocationContext(invocationContext), id);
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext invocationCtx,
                                                       IdentityObjectType identityType,
                                                       IdentityObjectSearchCriteria criteria) throws IdentityException {
    return getIdentityStore().findIdentityObject(resolveIdentityStoreInvocationContext(invocationCtx), identityType, criteria);
  }

  public int getIdentityObjectCount(IdentityStoreInvocationContext invocationCtx,
                                    IdentityObject identity,
                                    IdentityObjectRelationshipType relationshipType,
                                    boolean parent,
                                    IdentityObjectSearchCriteria criteria) throws IdentityException {
    return getIdentityStore().getIdentityObjectCount(resolveIdentityStoreInvocationContext(invocationCtx),
                                                     identity,
                                                     relationshipType,
                                                     parent,
                                                     criteria);
  }

  public int getIdentityObjectCount(IdentityStoreInvocationContext ctx,
                                    IdentityObject identity,
                                    IdentityObjectRelationshipType relationshipType,
                                    Collection<IdentityObjectType> excludes,
                                    boolean parent,
                                    IdentityObjectSearchCriteria criteria) throws IdentityException {
    return getIdentityStore().getIdentityObjectCount(resolveIdentityStoreInvocationContext(ctx),
                                                     identity,
                                                     relationshipType,
                                                     excludes,
                                                     parent,
                                                     criteria);
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext invocationCtx,
                                                       IdentityObject identity,
                                                       IdentityObjectRelationshipType relationshipType,
                                                       Collection<IdentityObjectType> excludes,
                                                       boolean parent,
                                                       IdentityObjectSearchCriteria criteria) throws IdentityException {
    return getIdentityStore().findIdentityObject(resolveIdentityStoreInvocationContext(invocationCtx),
                                                 identity,
                                                 relationshipType,
                                                 excludes,
                                                 parent,
                                                 criteria);
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext invocationCtx,
                                                       IdentityObject identity,
                                                       IdentityObjectRelationshipType relationshipType,
                                                       boolean parent,
                                                       IdentityObjectSearchCriteria criteria) throws IdentityException {
    return getIdentityStore().findIdentityObject(resolveIdentityStoreInvocationContext(invocationCtx),
                                                 identity,
                                                 relationshipType,
                                                 parent,
                                                 criteria);
  }

  public IdentityObjectRelationship createRelationship(IdentityStoreInvocationContext invocationCxt,
                                                       IdentityObject fromIdentity,
                                                       IdentityObject toIdentity,
                                                       IdentityObjectRelationshipType relationshipType,
                                                       String relationshipName,
                                                       boolean createNames) throws IdentityException {
    return getIdentityStore().createRelationship(resolveIdentityStoreInvocationContext(invocationCxt),
                                                 fromIdentity,
                                                 toIdentity,
                                                 relationshipType,
                                                 relationshipName,
                                                 createNames);
  }

  public void removeRelationship(IdentityStoreInvocationContext invocationCxt,
                                 IdentityObject fromIdentity,
                                 IdentityObject toIdentity,
                                 IdentityObjectRelationshipType relationshipType,
                                 String relationshipName) throws IdentityException {
    getIdentityStore().removeRelationship(resolveIdentityStoreInvocationContext(invocationCxt),
                                          fromIdentity,
                                          toIdentity,
                                          relationshipType,
                                          relationshipName);
  }

  public void removeRelationships(IdentityStoreInvocationContext invocationCtx,
                                  IdentityObject identity1,
                                  IdentityObject identity2,
                                  boolean named) throws IdentityException {
    getIdentityStore().removeRelationships(resolveIdentityStoreInvocationContext(invocationCtx), identity1, identity2, named);
  }

  public Set<IdentityObjectRelationship> resolveRelationships(IdentityStoreInvocationContext invocationCxt,
                                                              IdentityObject fromIdentity,
                                                              IdentityObject toIdentity,
                                                              IdentityObjectRelationshipType relationshipType) throws IdentityException {
    return getIdentityStore().resolveRelationships(resolveIdentityStoreInvocationContext(invocationCxt),
                                                   fromIdentity,
                                                   toIdentity,
                                                   relationshipType);
  }

  public int getRelationshipsCount(IdentityStoreInvocationContext ctx,
                                   IdentityObject identity,
                                   IdentityObjectRelationshipType type,
                                   boolean parent,
                                   boolean named,
                                   String name,
                                   IdentityObjectSearchCriteria searchCriteria) throws IdentityException {
    return getIdentityStore().getRelationshipsCount(resolveIdentityStoreInvocationContext(ctx),
                                                    identity,
                                                    type,
                                                    parent,
                                                    named,
                                                    name,
                                                    searchCriteria);
  }

  public Set<IdentityObjectRelationship> resolveRelationships(IdentityStoreInvocationContext invocationCtx,
                                                              IdentityObject identity,
                                                              IdentityObjectRelationshipType relationshipType,
                                                              boolean parent,
                                                              boolean named,
                                                              String name,
                                                              IdentityObjectSearchCriteria criteria) throws IdentityException {
    return getIdentityStore().resolveRelationships(resolveIdentityStoreInvocationContext(invocationCtx),
                                                   identity,
                                                   relationshipType,
                                                   parent,
                                                   named,
                                                   name,
                                                   criteria);
  }

  public String createRelationshipName(IdentityStoreInvocationContext ctx, String name) throws IdentityException,
                                                                                        OperationNotSupportedException {
    return getIdentityStore().createRelationshipName(resolveIdentityStoreInvocationContext(ctx), name);
  }

  public String removeRelationshipName(IdentityStoreInvocationContext ctx, String name) throws IdentityException,
                                                                                        OperationNotSupportedException {
    return getIdentityStore().removeRelationshipName(resolveIdentityStoreInvocationContext(ctx), name);
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx,
                                          IdentityObjectSearchCriteria criteria) throws IdentityException,
                                                                                 OperationNotSupportedException {
    return getIdentityStore().getRelationshipNames(resolveIdentityStoreInvocationContext(ctx), criteria);
  }

  public Set<String> getRelationshipNames(IdentityStoreInvocationContext ctx,
                                          IdentityObject identity,
                                          IdentityObjectSearchCriteria criteria) throws IdentityException,
                                                                                 OperationNotSupportedException {
    return getIdentityStore().getRelationshipNames(resolveIdentityStoreInvocationContext(ctx), identity, criteria);
  }

  public Map<String, String> getRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                                           String name) throws IdentityException, OperationNotSupportedException {
    return getIdentityStore().getRelationshipNameProperties(resolveAttributeStoreInvocationContext(ctx), name);
  }

  public void setRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                            String name,
                                            Map<String, String> properties) throws IdentityException,
                                                                            OperationNotSupportedException {
    getIdentityStore().setRelationshipNameProperties(resolveIdentityStoreInvocationContext(ctx), name, properties);
  }

  public void removeRelationshipNameProperties(IdentityStoreInvocationContext ctx,
                                               String name,
                                               Set<String> properties) throws IdentityException, OperationNotSupportedException {
    getIdentityStore().removeRelationshipNameProperties(resolveIdentityStoreInvocationContext(ctx), name, properties);
  }

  public Map<String, String> getRelationshipProperties(IdentityStoreInvocationContext ctx,
                                                       IdentityObjectRelationship relationship) throws IdentityException,
                                                                                                OperationNotSupportedException {
    return getIdentityStore().getRelationshipProperties(resolveIdentityStoreInvocationContext(ctx), relationship);
  }

  public void setRelationshipProperties(IdentityStoreInvocationContext ctx,
                                        IdentityObjectRelationship relationship,
                                        Map<String, String> properties) throws IdentityException, OperationNotSupportedException {
    getIdentityStore().setRelationshipProperties(resolveIdentityStoreInvocationContext(ctx), relationship, properties);
  }

  public void removeRelationshipProperties(IdentityStoreInvocationContext ctx,
                                           IdentityObjectRelationship relationship,
                                           Set<String> properties) throws IdentityException, OperationNotSupportedException {
    getIdentityStore().removeRelationshipProperties(resolveIdentityStoreInvocationContext(ctx), relationship, properties);
  }

  public boolean validateCredential(IdentityStoreInvocationContext ctx,
                                    IdentityObject identityObject,
                                    IdentityObjectCredential credential) throws IdentityException {
    return getIdentityStore().validateCredential(resolveIdentityStoreInvocationContext(ctx), identityObject, credential);
  }

  public void updateCredential(IdentityStoreInvocationContext ctx,
                               IdentityObject identityObject,
                               IdentityObjectCredential credential) throws IdentityException {
    getIdentityStore().updateCredential(resolveIdentityStoreInvocationContext(ctx), identityObject, credential);
  }

  public Set<String> getSupportedAttributeNames(IdentityStoreInvocationContext invocationContext,
                                                IdentityObjectType identityType) throws IdentityException {
    return getAttributeStore().getSupportedAttributeNames(resolveAttributeStoreInvocationContext(invocationContext),
                                                          identityType);
  }

  public Map<String, IdentityObjectAttributeMetaData> getAttributesMetaData(IdentityStoreInvocationContext invocationContext,
                                                                            IdentityObjectType identityType) {
    try {
      return getAttributeStore().getAttributesMetaData(resolveAttributeStoreInvocationContext(invocationContext), identityType);
    } catch (IdentityException e) {
      throw new RuntimeException("Error while retrieving data from store", e);
    }
  }

  public Map<String, IdentityObjectAttribute> getAttributes(IdentityStoreInvocationContext invocationContext,
                                                            IdentityObject identity) throws IdentityException {
    return getAttributeStore().getAttributes(resolveAttributeStoreInvocationContext(invocationContext), identity);
  }

  public IdentityObjectAttribute getAttribute(IdentityStoreInvocationContext invocationContext,
                                              IdentityObject identity,
                                              String name) throws IdentityException {
    return getAttributeStore().getAttribute(resolveAttributeStoreInvocationContext(invocationContext), identity, name);
  }

  public void updateAttributes(IdentityStoreInvocationContext invocationCtx,
                               IdentityObject identity,
                               IdentityObjectAttribute[] attributes) throws IdentityException {
    getAttributeStore().updateAttributes(resolveAttributeStoreInvocationContext(invocationCtx), identity, attributes);
  }

  public void addAttributes(IdentityStoreInvocationContext invocationCtx,
                            IdentityObject identity,
                            IdentityObjectAttribute[] attributes) throws IdentityException {
    getAttributeStore().addAttributes(resolveAttributeStoreInvocationContext(invocationCtx), identity, attributes);
  }

  public void removeAttributes(IdentityStoreInvocationContext invocationCtx,
                               IdentityObject identity,
                               String[] attributeNames) throws IdentityException {
    getAttributeStore().removeAttributes(resolveAttributeStoreInvocationContext(invocationCtx), identity, attributeNames);
  }

  public IdentityObject findIdentityObjectByUniqueAttribute(IdentityStoreInvocationContext invocationCtx,
                                                            IdentityObjectType identityObjectType,
                                                            IdentityObjectAttribute attribute) throws IdentityException {
    return getAttributeStore().findIdentityObjectByUniqueAttribute(resolveAttributeStoreInvocationContext(invocationCtx),
                                                                   identityObjectType,
                                                                   attribute);
  }

  public String getExternalStoreId() {
    return externalStoreId;
  }

  public IdentityStore getExternalIdentityStore() {
    return externalIdentityStore;
  }

  public IdentityStore getDefaultIdentityStore() {
    return defaultIdentityStore;
  }

  public AttributeStore getExternalAttributeStore() {
    return externalAttributeStore;
  }

  public IdentityStoreMappingMetaData getExternalIdentityStoreMappingMetaData() {
    return externalIdentityStoreMappingMetaData;
  }

  public void setUseExternalStore(Boolean useExternalStore) {
    this.useExternalStore.set(useExternalStore);
  }

  public boolean isUseExternalStore() {
    Boolean isUseExternalStore = this.useExternalStore.get();
    return isUseExternalStore != null && isUseExternalStore;
  }

  public boolean hasExternalStore() {
    return externalIdentityStore != null;
  }
}
