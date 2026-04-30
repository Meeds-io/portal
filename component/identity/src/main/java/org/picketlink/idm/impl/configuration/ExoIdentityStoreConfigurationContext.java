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
package org.picketlink.idm.impl.configuration;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.picketlink.idm.impl.api.attribute.IdentityObjectAttributeMetaDataImpl;
import org.picketlink.idm.impl.configuration.metadata.IdentityObjectTypeMetaDataImpl;
import org.picketlink.idm.impl.configuration.metadata.IdentityStoreConfigurationMetaDataImpl;
import org.picketlink.idm.impl.store.ldap.SimpleLDAPIdentityObjectTypeConfiguration;
import org.picketlink.idm.spi.configuration.IdentityConfigurationContextRegistry;
import org.picketlink.idm.spi.configuration.IdentityStoreConfigurationContext;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectAttributeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectTypeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityStoreConfigurationMetaData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is a wrapper for PicketLink IdentityStoreConfigurationContext class.
 * It allows to change/enrich the default configuration dynamically at startup.
 * It is used to:
 * <ul>
 *   <li>add custom attributes for users and groups defined by properties:
 *     <ul>
 *       <li>exo.ldap.users.attributes.custom.names: comma-separated list of names of new custom attributes for users</li>
 *       <li>exo.ldap.groups.attributes.custom.names: comma-separated list of names of new custom attributes for groups</li>
 *       <li>exo.ldap.{users|groups}.attributes.custom.{name}.mapping: name of the LDAP attribute mapped with the custom attribute (defaults to the attribute name)</li>
 *       <li>exo.ldap.{users|groups}.attributes.custom.{name}.type: type of the LDAP attribute mapped with the custom attribute, can be "text" or "binary" (defaults to "text")</li>
 *       <li>exo.ldap.{users|groups}.attributes.custom.{name}.isRequired: is the custom attribute mandatory ? (defaults to false)</li>
 *       <li>exo.ldap.{users|groups}.attributes.custom.{name}.isMultivalued: is the custom attribute multi-valued ? (defaults to false)</li>
 *     </ul>
 *   </li>
 *   <li>convert ctxDNs to support semicolon-separated format:
 *     <ul>
 *       <li>multiple {@literal <value>} tags can still be used to define several DNs</li>
 *       <li>for each value, a semicolon-separated list of DNs can be used (for example "ou=users1,dc=company,dc=org;ou=users2,dc=company,dc=org")</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class ExoIdentityStoreConfigurationContext implements IdentityStoreConfigurationContext {

  public static final String LDAP_USERS_ATTRIBUTES_NAMES_PROP = "exo.ldap.users.attributes.custom.names";

  public static final String LDAP_GROUPS_ATTRIBUTES_NAMES_PROP = "exo.ldap.groups.attributes.custom.names";

  public static final String LDAP_USERS_ATTRIBUTES_PROPS_PREFIX = "exo.ldap.users.attributes.";

  public static final String LDAP_GROUPS_ATTRIBUTES_PROPS_PREFIX = "exo.ldap.groups.attributes.";

  private IdentityStoreConfigurationContext identityStoreConfigurationContext;

  public ExoIdentityStoreConfigurationContext(IdentityStoreConfigurationContext identityStoreConfigurationContext) {
    this.identityStoreConfigurationContext = identityStoreConfigurationContext;
  }

  @Override
  public IdentityStoreConfigurationMetaData getStoreConfigurationMetaData() {
    IdentityStoreConfigurationMetaDataImpl storeConfigurationMetaData = (IdentityStoreConfigurationMetaDataImpl) this.identityStoreConfigurationContext.getStoreConfigurationMetaData();

    List<IdentityObjectTypeMetaData> supportedIdentityTypes = storeConfigurationMetaData.getSupportedIdentityTypes();

    List<IdentityObjectTypeMetaData> enrichedSupportedIdentityTypes = new ArrayList<>(supportedIdentityTypes.size());
    for (IdentityObjectTypeMetaData supportedIdentityType : supportedIdentityTypes) {
      List<String> convertedCtxDNs = convertCtxDNs(supportedIdentityType);
      if(convertedCtxDNs != null) {
        supportedIdentityType.getOptions().put(SimpleLDAPIdentityObjectTypeConfiguration.CTX_DNS, convertedCtxDNs);
      }

      List<IdentityObjectAttributeMetaData> enrichedAttributes = enrichAttributes(supportedIdentityType);
      ((IdentityObjectTypeMetaDataImpl) supportedIdentityType).setAttributes(enrichedAttributes);

      enrichedSupportedIdentityTypes.add(supportedIdentityType);
    }

    storeConfigurationMetaData.setSupportedIdentityTypes(enrichedSupportedIdentityTypes);

    return storeConfigurationMetaData;
  }

  protected List<IdentityObjectAttributeMetaData> enrichAttributes(IdentityObjectTypeMetaData supportedIdentityType) {
    // Add custom attributes
    String customNamesPropertyName = LDAP_USERS_ATTRIBUTES_NAMES_PROP;
    String attributesPropertyPrefix = LDAP_USERS_ATTRIBUTES_PROPS_PREFIX;
    if("GROUP".equals(supportedIdentityType.getName()) || "LDAP_MAPPED_GROUP".equals(supportedIdentityType.getName())) {
      customNamesPropertyName = LDAP_GROUPS_ATTRIBUTES_NAMES_PROP;
      attributesPropertyPrefix = LDAP_GROUPS_ATTRIBUTES_PROPS_PREFIX;
    }

    List<IdentityObjectAttributeMetaData> attributes = supportedIdentityType.getAttributes();
    if(attributes == null) {
      attributes = new ArrayList<>();
    }

    String customAttributesNamesProperty = PropertyManager.getProperty(customNamesPropertyName);
    if(StringUtils.isNotBlank(customAttributesNamesProperty)) {
      //
      String[] customAttributesNames = customAttributesNamesProperty.split(",");
      for(String name : customAttributesNames) {
        String mapping = PropertyManager.getProperty(attributesPropertyPrefix + name + ".mapping");
        String type = PropertyManager.getProperty(attributesPropertyPrefix + name + ".type");
        String isRequired = PropertyManager.getProperty(attributesPropertyPrefix + name + ".isRequired");
        String isMultivalued = PropertyManager.getProperty(attributesPropertyPrefix + name + ".isMultivalued");

        IdentityObjectAttributeMetaData attribute = new IdentityObjectAttributeMetaDataImpl(name,
                mapping != null ? mapping : name,
                type != null ? type : IdentityObjectAttributeMetaData.TEXT_TYPE,
                true,
                isMultivalued != null ? Boolean.valueOf(isMultivalued) : false,
                isRequired != null ? Boolean.valueOf(isRequired) : false,
                true);

        attributes.add(attribute);
      }
    }

    return attributes;
  }

  protected List<String> convertCtxDNs(IdentityObjectTypeMetaData supportedIdentityType) {
    Map<String, List<String>> options = supportedIdentityType.getOptions();
    if(options == null) {
      return null;
    }

    List<String> ctxDNs = options.get(SimpleLDAPIdentityObjectTypeConfiguration.CTX_DNS);
    if(ctxDNs == null) {
      return null;
    }

    List<String> convertedCtxDNs = ctxDNs.stream()
            .flatMap(ctxDN -> Arrays.stream(ctxDN.split(";")))
            .collect(Collectors.toList());

    return convertedCtxDNs;
  }

  @Override
  public IdentityConfigurationMetaData getConfigurationMetaData() {
    return this.identityStoreConfigurationContext.getConfigurationMetaData();
  }

  @Override
  public IdentityConfigurationContextRegistry getConfigurationRegistry() {
    return this.identityStoreConfigurationContext.getConfigurationRegistry();
  }
}
