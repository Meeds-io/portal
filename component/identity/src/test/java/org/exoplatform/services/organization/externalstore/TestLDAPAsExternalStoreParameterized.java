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
package org.exoplatform.services.organization.externalstore;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.organization.externalstore.model.IDMEntityType;
import org.gatein.portal.idm.impl.repository.ExoFallbackIdentityStoreRepository;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.opends.server.tools.LDAPConnectionException;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.api.session.IdentitySessionImpl;
import org.picketlink.idm.impl.store.ldap.SimpleLDAPIdentityObjectTypeConfiguration;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectAttributeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectTypeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityStoreConfigurationMetaData;
import org.picketlink.idm.spi.repository.IdentityStoreRepository;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.PicketLinkIDMServiceImpl;
import org.exoplatform.services.organization.idm.externalstore.PicketLinkIDMExternalStoreService;

import exo.portal.component.identiy.opendsconfig.opends.OpenDSService;

import java.time.LocalDateTime;
import java.util.List;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-ldap-parameterized-configuration.xml") })
public class TestLDAPAsExternalStoreParameterized extends AbstractKernelTest {

  OpenDSService                        openDSService = new OpenDSService(null);

  PicketLinkIDMOrganizationServiceImpl organizationService;

  PicketLinkIDMServiceImpl             idmService;

  PicketLinkIDMExternalStoreService    externalStoreService;

  @Override
  protected void beforeRunBare() {
    try {
      openDSService.start();
      openDSService.initLDAPServer();
    } catch (Exception e) {
      log.error("Error in starting up OPENDS", e);
      e.printStackTrace();
    }
    super.beforeRunBare();
  }

  @Override
  protected void tearDown() throws Exception {
    end();
    super.tearDown();
  }

  public void testParameterizedConfigurationWithDefaultValues() {
    // Given

    // When
    setForceContainerReload(true);
    this.beforeClass();

    idmService = (PicketLinkIDMServiceImpl) getContainer().getComponentInstanceOfType(PicketLinkIDMService.class);

    begin();

    // Then
    IdentityConfigurationMetaData configMD = idmService.getConfigMD();
    IdentityStoreConfigurationMetaData portalLDAPStore = configMD.getIdentityStores().stream()
            .filter(store -> store.getId().equals("PortalLDAPStore"))
            .findFirst()
            .get();

    assertEquals("ldap://localhost:1389", portalLDAPStore.getOptions().get("providerURL").get(0));
    assertEquals("cn=admin", portalLDAPStore.getOptions().get("adminDN").get(0));
    assertEquals("", portalLDAPStore.getOptions().get("adminPassword").get(0));

    IdentityObjectTypeMetaData userIdentityObjectTypeMD = portalLDAPStore.getSupportedIdentityTypes().stream()
            .filter(identityType -> identityType.getName().equals("USER"))
            .findFirst()
            .get();
    List<String> usersCtxDNs = userIdentityObjectTypeMD.getOptions().get(SimpleLDAPIdentityObjectTypeConfiguration.CTX_DNS);
    assertNotNull(usersCtxDNs);
    assertEquals(1, usersCtxDNs.size());
    assertTrue(usersCtxDNs.contains("ou=users,dc=company,dc=org"));

    List<IdentityObjectAttributeMetaData> userAttributes = userIdentityObjectTypeMD.getAttributes();
    assertNotNull(userAttributes);
    assertEquals(3, userAttributes.size());
    assertTrue(userAttributes.stream().anyMatch(attribute -> attribute.getName().equals("firstName") && attribute.getStoreMapping().equals("cn")));
    assertTrue(userAttributes.stream().anyMatch(attribute -> attribute.getName().equals("lastName") && attribute.getStoreMapping().equals("sn")));
    assertTrue(userAttributes.stream().anyMatch(attribute -> attribute.getName().equals("email") && attribute.getStoreMapping().equals("mail")));

    IdentityObjectTypeMetaData groupIdentityObjectTypeMD = portalLDAPStore
            .getSupportedIdentityTypes().stream()
            .filter(identityType -> identityType.getName().equals("GROUP"))
            .findFirst()
            .get();
    List<String> groupsCtxDNs = groupIdentityObjectTypeMD.getOptions().get(SimpleLDAPIdentityObjectTypeConfiguration.CTX_DNS);
    assertNotNull(groupsCtxDNs);
    assertEquals(1, groupsCtxDNs.size());
    assertTrue(groupsCtxDNs.contains("ou=groups,dc=company,dc=org"));
  }

  public void testParameterizedConfigurationWithCustomizedValues() {
    // Given
    System.setProperty("exo.ldap.url", "ldap://localhost:10389");
    System.setProperty("exo.ldap.admin.dn", "cn=Directory Manager");
    System.setProperty("exo.ldap.admin.password", "password");
    System.setProperty("exo.ldap.users.base.dn", "ou=users1,dc=company,dc=org;ou=users2,dc=company,dc=org;ou=users3,dc=company,dc=org");
    System.setProperty("exo.ldap.groups.base.dn", "ou=group1,dc=company,dc=org");
    System.setProperty("exo.ldap.users.attributes.custom.names", "customAttribute1,customAttribute2");
    System.setProperty("exo.ldap.users.attributes.customAttribute1.mapping", "ldapAttribute1");
    System.setProperty("exo.ldap.users.attributes.customAttribute1.type", "text");
    System.setProperty("exo.ldap.users.attributes.customAttribute1.isRequired", "true");
    System.setProperty("exo.ldap.users.attributes.customAttribute1.isMultivalued", "true");
    System.setProperty("exo.ldap.users.attributes.customAttribute2.mapping", "ldapAttribute2");
    System.setProperty("exo.ldap.users.attributes.customAttribute2.type", "text");
    System.setProperty("exo.ldap.users.attributes.customAttribute2.isRequired", "false");
    System.setProperty("exo.ldap.users.attributes.customAttribute2.isMultivalued", "true");

    // When
    setForceContainerReload(true);
    this.beforeClass();

    idmService = (PicketLinkIDMServiceImpl) getContainer().getComponentInstanceOfType(PicketLinkIDMService.class);

    begin();

    // Then
    IdentityConfigurationMetaData configMD = idmService.getConfigMD();
    IdentityStoreConfigurationMetaData portalLDAPStore = configMD.getIdentityStores().stream()
            .filter(store -> store.getId().equals("PortalLDAPStore"))
            .findFirst()
            .get();

    assertEquals("ldap://localhost:10389", portalLDAPStore.getOptions().get("providerURL").get(0));
    assertEquals("cn=Directory Manager", portalLDAPStore.getOptions().get("adminDN").get(0));
    assertEquals("password", portalLDAPStore.getOptions().get("adminPassword").get(0));

    IdentityObjectTypeMetaData userIdentityObjectTypeMD = portalLDAPStore.getSupportedIdentityTypes().stream()
            .filter(identityType -> identityType.getName().equals("USER"))
            .findFirst()
            .get();
    List<String> ctxDNs = userIdentityObjectTypeMD.getOptions().get(SimpleLDAPIdentityObjectTypeConfiguration.CTX_DNS);
    assertNotNull(ctxDNs);
    assertEquals(3, ctxDNs.size());
    assertTrue(ctxDNs.contains("ou=users1,dc=company,dc=org"));
    assertTrue(ctxDNs.contains("ou=users2,dc=company,dc=org"));
    assertTrue(ctxDNs.contains("ou=users3,dc=company,dc=org"));

    List<IdentityObjectAttributeMetaData> userAttributes = userIdentityObjectTypeMD.getAttributes();
    assertNotNull(userAttributes);
    assertEquals(5, userAttributes.size());
    assertTrue(userAttributes.stream().anyMatch(attribute -> attribute.getName().equals("firstName") && attribute.getStoreMapping().equals("cn")));
    assertTrue(userAttributes.stream().anyMatch(attribute -> attribute.getName().equals("lastName") && attribute.getStoreMapping().equals("sn")));
    assertTrue(userAttributes.stream().anyMatch(attribute -> attribute.getName().equals("email") && attribute.getStoreMapping().equals("mail")));
    assertTrue(userAttributes.stream().anyMatch(attribute -> attribute.getName().equals("customAttribute1") && attribute.getStoreMapping().equals("ldapAttribute1")));
    assertTrue(userAttributes.stream().anyMatch(attribute -> attribute.getName().equals("customAttribute2") && attribute.getStoreMapping().equals("ldapAttribute2")));

    IdentityObjectTypeMetaData groupIdentityObjectTypeMD = portalLDAPStore
            .getSupportedIdentityTypes().stream()
            .filter(identityType -> identityType.getName().equals("GROUP"))
            .findFirst()
            .get();
    List<String> groupsCtxDNs = groupIdentityObjectTypeMD.getOptions().get(SimpleLDAPIdentityObjectTypeConfiguration.CTX_DNS);
    assertNotNull(groupsCtxDNs);
    assertEquals(1, groupsCtxDNs.size());
    assertTrue(groupsCtxDNs.contains("ou=group1,dc=company,dc=org"));

    System.clearProperty("exo.ldap.url");
    System.clearProperty("exo.ldap.admin.dn");
    System.clearProperty("exo.ldap.admin.password");
    System.clearProperty("exo.ldap.users.base.dn");
    System.clearProperty("exo.ldap.groups.base.dn");
    System.clearProperty("exo.ldap.users.attributes.custom.names");
    System.clearProperty("exo.ldap.users.attributes.customAttribute1.mapping");
    System.clearProperty("exo.ldap.users.attributes.customAttribute1.type");
    System.clearProperty("exo.ldap.users.attributes.customAttribute1.isRequired");
    System.clearProperty("exo.ldap.users.attributes.customAttribute1.isMultivalued");
    System.clearProperty("exo.ldap.users.attributes.customAttribute2.mapping");
    System.clearProperty("exo.ldap.users.attributes.customAttribute2.type");
    System.clearProperty("exo.ldap.users.attributes.customAttribute2.isRequired");
    System.clearProperty("exo.ldap.users.attributes.customAttribute2.isMultivalued");
    PropertyManager.refresh();
  }

}
