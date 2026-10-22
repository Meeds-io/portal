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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import javax.naming.NamingException;

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
    System.clearProperty(ExoLDAPIdentityStoreImpl.MEMBER_OF_GROUP_CHANGE_CHECK_PROP);
    System.clearProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP);
    SniAwareLdapSocketFactory.hintCustomJndiConnectTimeout(null);
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
  public void testEnabledMergesSniSocketFactoryWhenSniEnabledAndEveryUrlIsLdaps() {
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP, "true");
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);
    when(original.getProviderURL()).thenReturn("ldaps://main:636");
    Map<String, String> originalParams = new HashMap<>();
    originalParams.put("com.sun.jndi.ldap.read.timeout", "60000");
    when(original.getCustomJNDIConnectionParameters()).thenReturn(originalParams);

    LDAPIdentityStoreConfiguration result = ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, true);
    Map<String, String> merged = result.getCustomJNDIConnectionParameters();

    assertEquals("60000", merged.get("com.sun.jndi.ldap.read.timeout"));
    assertEquals(SniAwareLdapSocketFactory.class.getName(), merged.get(SniAwareLdapSocketFactory.SOCKET_FACTORY_JNDI_PROPERTY));
  }

  @Test
  public void testEnabledHintsTheConfiguredJndiConnectTimeoutToTheSniFactory() {
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP, "true");
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);
    when(original.getProviderURL()).thenReturn("ldaps://main:636");
    Map<String, String> originalParams = new HashMap<>();
    // This is how the shipped configuration sets it - as a customJNDIConnectionParameters entry.
    originalParams.put(SniAwareLdapSocketFactory.JNDI_LDAP_CONNECT_TIMEOUT_SYSTEM_PROP, "45000");
    when(original.getCustomJNDIConnectionParameters()).thenReturn(originalParams);

    ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, true).getCustomJNDIConnectionParameters();

    assertEquals(45000, SniAwareLdapSocketFactory.connectTimeoutMs());
  }

  @Test
  public void testEnabledDoesNotRegisterSniSocketFactoryWhenAnyUrlIsPlainLdap() {
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP, "true");
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);
    when(original.getProviderURL()).thenReturn("ldap://main:389");
    Map<String, String> originalParams = new HashMap<>();
    originalParams.put("com.sun.jndi.ldap.read.timeout", "60000");
    when(original.getCustomJNDIConnectionParameters()).thenReturn(originalParams);

    LDAPIdentityStoreConfiguration result = ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, true);
    Map<String, String> merged = result.getCustomJNDIConnectionParameters();

    assertEquals(originalParams, merged);
  }

  @Test
  public void testEnabledDoesNotRegisterSniSocketFactoryWhenNoUrlIsConfigured() {
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP, "true");
    LDAPIdentityStoreConfiguration original = mock(LDAPIdentityStoreConfiguration.class);
    // getProviderURL() left unstubbed: returns null, exactly the "nothing configured" case.
    Map<String, String> originalParams = new HashMap<>();
    when(original.getCustomJNDIConnectionParameters()).thenReturn(originalParams);

    LDAPIdentityStoreConfiguration result = ExoLDAPIdentityStoreImpl.applyFailoverIfEnabled(original, true);
    Map<String, String> merged = result.getCustomJNDIConnectionParameters();

    assertEquals(originalParams, merged);
  }

  private static final String GROUP_DN = "CN=app-users,OU=Applications,OU=Groups,DC=example,DC=com";
  private static final String USERS_FILTER = "(&(sAMAccountName={0})(objectClass=user)(mail=*)(memberOf=" + GROUP_DN + "))";
  private static final String SINCE = "20260101000000";

  private ExoLDAPIdentityStoreImpl storeFindingGroups(List<SerializableSearchResult> groupSearchResult) throws Exception {
    ExoLDAPIdentityStoreImpl store = spy(new ExoLDAPIdentityStoreImpl("test"));
    doReturn(groupSearchResult).when(store).searchIdentityObjects(any(), any(), anyString(), any(), any(), anyString(), any());
    return store;
  }

  @Test
  public void testMemberOfCheckIsDisabledByDefault() throws Exception {
    ExoLDAPIdentityStoreImpl store = storeFindingGroups(List.of(mock(SerializableSearchResult.class)));

    assertFalse(store.isMemberOfGroupChangedSince(null, USERS_FILTER, SINCE));
    verify(store, never()).searchIdentityObjects(any(), any(), anyString(), any(), any(), anyString(), any());
  }

  @Test
  public void testMemberOfGroupChangedDropsTheDateCriteria() throws Exception {
    PropertyManager.setProperty(ExoLDAPIdentityStoreImpl.MEMBER_OF_GROUP_CHANGE_CHECK_PROP, "true");
    ExoLDAPIdentityStoreImpl store = storeFindingGroups(List.of(mock(SerializableSearchResult.class)));

    assertTrue(store.isMemberOfGroupChangedSince(null, USERS_FILTER, SINCE));
    // The group is read alone (object scope) with the same timestamps as the users search
    verify(store).searchIdentityObjects(any(),
                                        eq(new String[] { GROUP_DN }),
                                        eq("(|(createTimestamp>=" + SINCE + "Z)(modifyTimestamp>=" + SINCE + "Z)(whenCreated>="
                                            + SINCE + ".0Z)(whenChanged>=" + SINCE + ".0Z))"),
                                        any(),
                                        any(),
                                        eq("object"),
                                        any());
  }

  @Test
  public void testMemberOfGroupUnchangedKeepsTheDateCriteria() throws Exception {
    PropertyManager.setProperty(ExoLDAPIdentityStoreImpl.MEMBER_OF_GROUP_CHANGE_CHECK_PROP, "true");
    ExoLDAPIdentityStoreImpl store = storeFindingGroups(Collections.emptyList());

    assertFalse(store.isMemberOfGroupChangedSince(null, USERS_FILTER, SINCE));
  }

  @Test
  public void testFilterWithoutMemberOfNeverQueriesGroups() throws Exception {
    PropertyManager.setProperty(ExoLDAPIdentityStoreImpl.MEMBER_OF_GROUP_CHANGE_CHECK_PROP, "true");
    ExoLDAPIdentityStoreImpl store = storeFindingGroups(List.of(mock(SerializableSearchResult.class)));

    assertFalse(store.isMemberOfGroupChangedSince(null, "(&(sAMAccountName={0})(objectClass=user))", SINCE));
    assertFalse(store.isMemberOfGroupChangedSince(null, null, SINCE));
    assertFalse(store.isMemberOfGroupChangedSince(null, "", SINCE));
    verify(store, never()).searchIdentityObjects(any(), any(), anyString(), any(), any(), anyString(), any());
  }

  @Test
  public void testNestedMemberOfAndSeveralGroupsAreChecked() throws Exception {
    PropertyManager.setProperty(ExoLDAPIdentityStoreImpl.MEMBER_OF_GROUP_CHANGE_CHECK_PROP, "true");
    ExoLDAPIdentityStoreImpl store = spy(new ExoLDAPIdentityStoreImpl("test"));
    List<SerializableSearchResult> changed = List.of(mock(SerializableSearchResult.class));
    doReturn(Collections.emptyList()).when(store).searchIdentityObjects(any(), eq(new String[] { "CN=a,DC=x" }), anyString(),
                                                                        any(), any(), anyString(), any());
    doReturn(changed).when(store).searchIdentityObjects(any(), eq(new String[] { "CN=b,DC=x" }), anyString(),
                                                        any(), any(), anyString(), any());

    String filter = "(&(memberOf=CN=a,DC=x)(memberOf:1.2.840.113556.1.4.1941:=CN=b,DC=x))";

    assertTrue(store.isMemberOfGroupChangedSince(null, filter, SINCE));
  }

  @Test
  public void testIsMemberOfAttributeNameIsRecognised() throws Exception {
    PropertyManager.setProperty(ExoLDAPIdentityStoreImpl.MEMBER_OF_GROUP_CHANGE_CHECK_PROP, "true");
    ExoLDAPIdentityStoreImpl store = storeFindingGroups(List.of(mock(SerializableSearchResult.class)));

    assertTrue(store.isMemberOfGroupChangedSince(null, "(&(uid={0})(isMemberOf=" + GROUP_DN + "))", SINCE));
  }

  @Test
  public void testSearchFailureFallsBackToFullListing() throws Exception {
    PropertyManager.setProperty(ExoLDAPIdentityStoreImpl.MEMBER_OF_GROUP_CHANGE_CHECK_PROP, "true");
    ExoLDAPIdentityStoreImpl store = spy(new ExoLDAPIdentityStoreImpl("test"));
    NamingException failure = new NamingException("No such object");
    doThrow(failure).when(store).searchIdentityObjects(any(), any(), anyString(), any(), any(), anyString(), any());

    assertTrue(store.isMemberOfGroupChangedSince(null, USERS_FILTER, SINCE));
  }
}
