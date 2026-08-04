/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.picketlink.idm.impl.store.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.organization.idm.ldap.SniAwareLdapSocketFactory;

/**
 * Verifies that the failover/SRV/SNI support only alters the store
 * configuration's execution chain when explicitly enabled via
 * {@link ExoLDAPIdentityStoreImpl#FAILOVER_ENABLED_PROP}, and that once
 * enabled, the wrapped configuration still behaves like the original one
 * except for the specifically overridden methods.
 */
public class ExoLDAPIdentityStoreImplTest {

  @After
  public void clearProperties() {
    System.clearProperty(ExoLDAPIdentityStoreImpl.FAILOVER_ENABLED_PROP);
    System.clearProperty("exo.ldap.failover.urls");
    System.clearProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP);
    PropertyManager.refresh();
  }

  @Test
  public void testDisabledReturnsTheOriginalConfigurationUnchanged() {
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);

    LDAPIdentityStoreConfiguration result = ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, false);

    assertSame("With the feature flag off, the exact same configuration instance must be returned "
        + "(no Proxy created, no behavior change)", original, result);
  }

  @Test
  public void testEnabledReturnsAWrapperDelegatingUnrelatedMethods() {
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);
    when(original.getAdminDN()).thenReturn("cn=admin,dc=example,dc=com");
    when(original.getProviderURL()).thenReturn("ldap://main:389");

    LDAPIdentityStoreConfiguration result = ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, true);

    assertNotSame(original, result);
    assertEquals("cn=admin,dc=example,dc=com", result.getAdminDN());
  }

  @Test
  public void testEnabledResolvesProviderURLThroughTheServerLocator() {
    PropertyManager.setProperty("exo.ldap.failover.urls", "ldap://secondary:389");
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);
    when(original.getProviderURL()).thenReturn("ldap://main:389");

    LDAPIdentityStoreConfiguration result = ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, true);

    assertEquals("ldap://main:389 ldap://secondary:389", result.getProviderURL());
  }

  @Test
  public void testEnabledPassesThroughCustomJNDIParametersWhenSniDisabled() {
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);
    Map<String, String> originalParams = new HashMap<>();
    originalParams.put("com.sun.jndi.ldap.read.timeout", "60000");
    when(original.getCustomJNDIConnectionParameters()).thenReturn(originalParams);

    LDAPIdentityStoreConfiguration result = ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, true);

    assertEquals(originalParams, result.getCustomJNDIConnectionParameters());
  }

  @Test
  public void testEnabledMergesSniSocketFactoryWhenSniEnabled() {
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP, "true");
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);
    Map<String, String> originalParams = new HashMap<>();
    originalParams.put("com.sun.jndi.ldap.read.timeout", "60000");
    when(original.getCustomJNDIConnectionParameters()).thenReturn(originalParams);

    LDAPIdentityStoreConfiguration result = ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, true);
    Map<String, String> merged = result.getCustomJNDIConnectionParameters();

    assertEquals("60000", merged.get("com.sun.jndi.ldap.read.timeout"));
    assertEquals(SniAwareLdapSocketFactory.class.getName(), merged.get(SniAwareLdapSocketFactory.SOCKET_FACTORY_JNDI_PROPERTY));
  }
}
