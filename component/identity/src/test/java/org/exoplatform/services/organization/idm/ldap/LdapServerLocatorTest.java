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
package org.exoplatform.services.organization.idm.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.naming.NamingException;

import org.junit.After;
import org.junit.Test;

import org.exoplatform.commons.utils.PropertyManager;

public class LdapServerLocatorTest {

  private static final String[] SRV_PROPERTIES = { LdapServerLocator.FAILOVER_URLS_PROP, LdapServerLocator.SRV_ENABLED_PROP,
      LdapServerLocator.SRV_DOMAIN_PROP, LdapServerLocator.SRV_SERVICE_PROP, LdapServerLocator.SRV_SCHEME_PROP,
      LdapServerLocator.SRV_REFRESH_PERIOD_PROP, LdapServerLocator.SRV_TIMEOUT_PROP, LdapServerLocator.SRV_DNS_SERVERS_PROP };

  @After
  public void clearProperties() {
    for (String property : SRV_PROPERTIES) {
      System.clearProperty(property);
    }
    PropertyManager.refresh();
  }

  @Test
  public void testStaticOnlyWhenSrvDisabledByDefault() {
    LdapServerLocator locator = new LdapServerLocator("ldap://host1:389 ldap://host2:389");
    assertEquals("ldap://host1:389 ldap://host2:389", locator.resolveProviderURL());
  }

  @Test
  public void testStaticUrlsAreNormalizedFromCommaSeparatedList() {
    LdapServerLocator locator = new LdapServerLocator("ldap://host1:389,ldap://host2:389");
    assertEquals("ldap://host1:389 ldap://host2:389", locator.resolveProviderURL());
  }

  @Test
  public void testMainServerPlusExplicitSecondaryServers() {
    PropertyManager.setProperty(LdapServerLocator.FAILOVER_URLS_PROP, "ldap://secondary1:389,ldap://secondary2:389");
    LdapServerLocator locator = new LdapServerLocator("ldap://main:389");
    assertEquals("ldap://main:389 ldap://secondary1:389 ldap://secondary2:389", locator.resolveProviderURL());
  }

  @Test
  public void testIsAllLdapsTrueWhenEveryStaticUrlIsLdaps() {
    LdapServerLocator locator = new LdapServerLocator("ldaps://main:636");
    assertTrue(locator.isAllLdaps());
  }

  @Test
  public void testIsAllLdapsFalseWhenAnyStaticUrlIsPlainLdap() {
    PropertyManager.setProperty(LdapServerLocator.FAILOVER_URLS_PROP, "ldap://secondary:389");
    LdapServerLocator locator = new LdapServerLocator("ldaps://main:636");
    assertFalse(locator.isAllLdaps());
  }

  @Test
  public void testIsAllLdapsFalseWhenNothingIsConfigured() {
    // No server known at all: must fail closed, not vacuously true.
    LdapServerLocator locator = new LdapServerLocator("");
    assertFalse(locator.isAllLdaps());
  }

  @Test
  public void testIsAllLdapsTrueWhenSrvSchemeIsLdapsAndNoStaticUrlConfigured() {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    PropertyManager.setProperty(LdapServerLocator.SRV_DOMAIN_PROP, "example.com");
    PropertyManager.setProperty(LdapServerLocator.SRV_SCHEME_PROP, "ldaps");
    LdapServerLocator locator = new LdapServerLocator("");
    assertTrue(locator.isAllLdaps());
  }

  @Test
  public void testIsAllLdapsFalseWhenSrvSchemeIsNotLdapsRegardlessOfStaticUrls() {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    PropertyManager.setProperty(LdapServerLocator.SRV_DOMAIN_PROP, "example.com");
    // SRV_SCHEME_PROP left at its default ("ldap")
    LdapServerLocator locator = new LdapServerLocator("ldaps://main:636");
    assertFalse(locator.isAllLdaps());
  }

  @Test
  public void testSrvEnabledButDomainBlankFallsBackToStaticOnly() {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    LdapServerLocator locator = new LdapServerLocator("ldap://fallback:389");
    assertFalse(locator.isSrvLookupEnabled());
    assertEquals("ldap://fallback:389", locator.resolveProviderURL());
  }

  @Test
  public void testSrvResolvedUrlsAreOrderedByPriorityAndStaticIsAppendedLast() {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    PropertyManager.setProperty(LdapServerLocator.SRV_DOMAIN_PROP, "example.com");

    StubLdapServerLocator locator = new StubLdapServerLocator("ldap://fallback:389");
    locator.setNow(0L);
    locator.setRecordsToReturn(Arrays.asList(new SrvRecord(0, 10, 389, "dc1.example.com"),
                                              new SrvRecord(0, 90, 389, "dc2.example.com"),
                                              new SrvRecord(10, 0, 389, "dc3.example.com")));

    for (int i = 0; i < 20; i++) {
      String[] urls = locator.resolveProviderURL().split(" ");
      assertEquals(4, urls.length);
      // priority 0 tier (dc1/dc2, in either order) comes before priority 10 tier (dc3)
      assertTrue(urls[0].equals("ldap://dc1.example.com:389") || urls[0].equals("ldap://dc2.example.com:389"));
      assertTrue(urls[1].equals("ldap://dc1.example.com:389") || urls[1].equals("ldap://dc2.example.com:389"));
      assertEquals("ldap://dc3.example.com:389", urls[2]);
      // static fallback always appended last
      assertEquals("ldap://fallback:389", urls[3]);
    }
  }

  @Test
  public void testWeightedOrderingFavorsHigherWeight() {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    PropertyManager.setProperty(LdapServerLocator.SRV_DOMAIN_PROP, "example.com");

    StubLdapServerLocator locator = new StubLdapServerLocator("");
    locator.setNow(0L);
    locator.setRecordsToReturn(Arrays.asList(new SrvRecord(0, 10, 389, "low-weight.example.com"),
                                              new SrvRecord(0, 90, 389, "high-weight.example.com")));

    int highWeightFirstCount = 0;
    int iterations = 300;
    for (int i = 0; i < iterations; i++) {
      String firstUrl = locator.resolveProviderURL().split(" ")[0];
      if ("ldap://high-weight.example.com:389".equals(firstUrl)) {
        highWeightFirstCount++;
      }
    }
    // With a 90/10 weight split, the higher-weight entry should come first
    // a large majority of the time; use a generous threshold to avoid flakiness.
    assertTrue("Expected high-weight entry to be picked first most of the time, got " + highWeightFirstCount + "/" + iterations,
               highWeightFirstCount > iterations * 0.7);
  }

  @Test
  public void testResolutionIsCachedWithinRefreshPeriod() {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    PropertyManager.setProperty(LdapServerLocator.SRV_DOMAIN_PROP, "example.com");

    StubLdapServerLocator locator = new StubLdapServerLocator("ldap://fallback:389");
    locator.setNow(0L);
    locator.setRecordsToReturn(Arrays.asList(new SrvRecord(0, 100, 389, "dc1.example.com")));

    for (int i = 0; i < 5; i++) {
      locator.resolveProviderURL();
    }
    assertEquals(1, locator.getLookupCount());

    // Still within the refresh period: no new DNS query
    locator.setNow(LdapServerLocator.DEFAULT_REFRESH_PERIOD_MS - 1);
    locator.resolveProviderURL();
    assertEquals(1, locator.getLookupCount());

    // Past the refresh period: a fresh DNS query is performed
    locator.setNow(LdapServerLocator.DEFAULT_REFRESH_PERIOD_MS + 1);
    locator.resolveProviderURL();
    assertEquals(2, locator.getLookupCount());
  }

  @Test
  public void testFailureKeepsPreviousListAndRetriesAfterBackoff() {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    PropertyManager.setProperty(LdapServerLocator.SRV_DOMAIN_PROP, "example.com");

    StubLdapServerLocator locator = new StubLdapServerLocator("ldap://fallback:389");
    locator.setNow(0L);
    List<SrvRecord> initialRecords = Arrays.asList(new SrvRecord(0, 100, 389, "dc1.example.com"));
    locator.setRecordsToReturn(initialRecords);

    String initialResolution = locator.resolveProviderURL();
    assertEquals("ldap://dc1.example.com:389 ldap://fallback:389", initialResolution);
    assertEquals(1, locator.getLookupCount());

    // Refresh period elapsed, DNS now fails: previous list should be kept
    locator.setNow(LdapServerLocator.DEFAULT_REFRESH_PERIOD_MS + 1);
    locator.setFailure(new NamingException("simulated DNS failure"));
    String duringFailure = locator.resolveProviderURL();
    assertEquals(2, locator.getLookupCount());
    assertEquals("ldap://dc1.example.com:389 ldap://fallback:389", duringFailure);

    // Immediately retrying should be suppressed by the failure backoff
    locator.setNow(LdapServerLocator.DEFAULT_REFRESH_PERIOD_MS + 2);
    locator.resolveProviderURL();
    assertEquals(2, locator.getLookupCount());

    // Once the backoff elapses, a retry is attempted again
    locator.setNow(LdapServerLocator.DEFAULT_REFRESH_PERIOD_MS + 1 + LdapServerLocator.FAILURE_RETRY_BACKOFF_MS + 1);
    locator.resolveProviderURL();
    assertEquals(3, locator.getLookupCount());
  }

  @Test
  public void testSuccessfulRefreshesAreNotClampedToTheFailureBackoff() {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    PropertyManager.setProperty(LdapServerLocator.SRV_DOMAIN_PROP, "example.com");
    // Well below FAILURE_RETRY_BACKOFF_MS (30s): a successful refresh must follow this
    // cadence exactly and not be silently floored to the failure backoff.
    PropertyManager.setProperty(LdapServerLocator.SRV_REFRESH_PERIOD_PROP, "5000");

    StubLdapServerLocator locator = new StubLdapServerLocator("ldap://fallback:389");
    locator.setNow(0L);
    locator.setRecordsToReturn(Arrays.asList(new SrvRecord(0, 100, 389, "dc1.example.com")));

    locator.resolveProviderURL();
    assertEquals(1, locator.getLookupCount());

    locator.setNow(5001L);
    locator.resolveProviderURL();
    assertEquals(2, locator.getLookupCount());
  }

  @Test
  public void testBackgroundRefreshServesTheStaleListImmediatelyWithoutBlocking() throws InterruptedException {
    PropertyManager.setProperty(LdapServerLocator.SRV_ENABLED_PROP, "true");
    PropertyManager.setProperty(LdapServerLocator.SRV_DOMAIN_PROP, "example.com");

    StubLdapServerLocator locator = new StubLdapServerLocator("ldap://fallback:389");
    locator.setNow(0L);
    locator.setRecordsToReturn(Arrays.asList(new SrvRecord(0, 100, 389, "dc1.example.com")));
    locator.resolveProviderURL();
    assertEquals(1, locator.getLookupCount());

    CountDownLatch refreshStarted = new CountDownLatch(1);
    CountDownLatch releaseRefresh = new CountDownLatch(1);
    locator.setNow(LdapServerLocator.DEFAULT_REFRESH_PERIOD_MS + 1);
    locator.setRecordsToReturn(Arrays.asList(new SrvRecord(0, 100, 389, "dc2.example.com")));
    locator.blockNextLookupOn(refreshStarted, releaseRefresh);
    locator.runInBackgroundThread();

    // The stale (dc1) list is served immediately, even while the background refresh is
    // still blocked - this is exactly what a login request must never wait on.
    String duringRefresh = locator.resolveProviderURL();
    assertEquals("ldap://dc1.example.com:389 ldap://fallback:389", duringRefresh);

    refreshStarted.await();
    releaseRefresh.countDown();
    locator.awaitBackgroundRefresh();

    String afterRefresh = locator.resolveProviderURL();
    assertEquals("ldap://dc2.example.com:389 ldap://fallback:389", afterRefresh);
  }
}
