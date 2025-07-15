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
package org.exoplatform.portal.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.service.LayoutService;

import lombok.SneakyThrows;

public class DynamicPortalLayoutServiceTest {

  private static final String EXISTING_PORTAL_SITE_NAME     = "testPortal";

  private static final String NON_EXISTING_PORTAL_SITE_NAME = "testPortal2";

  private static final String EXISTING_GROUP_SITE_NAME      = "testGroup";

  private static final String NON_EXISTING_GROUP_SITE_NAME  = "testGroup2";

  private static final String EXISTING_USER_SITE_NAME       = "testUser";

  private static final String NON_EXISTING_USER_SITE_NAME   = "testUser2";

  @Test
  public void testNoLayoutMatchers() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    assertNotNull(dynamicPortalLayoutService.getDynamicLayoutMatchers());
    assertEquals(0, dynamicPortalLayoutService.getDynamicLayoutMatchers().size());
  }

  @Test
  public void testLayoutMatchersPriority() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    DynamicPortalLayoutMatcherPlugin matcher1 = new DynamicPortalLayoutMatcherPlugin(null);
    matcher1.setName("matcher1");
    DynamicPortalLayoutMatcherPlugin matcher2 = new DynamicPortalLayoutMatcherPlugin(null);
    matcher1.setName("matcher2");
    dynamicPortalLayoutService.addDynamicLayoutMatcher(matcher1);
    dynamicPortalLayoutService.addDynamicLayoutMatcher(matcher2);
    List<DynamicPortalLayoutMatcherPlugin> dynamicLayoutMatchers = dynamicPortalLayoutService.getDynamicLayoutMatchers();
    assertEquals(matcher2, dynamicLayoutMatchers.get(0));
    assertEquals(matcher1, dynamicLayoutMatchers.get(1));
  }

  @Test
  public void testCanNotAddNullMatcher() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    try {
      dynamicPortalLayoutService.addDynamicLayoutMatcher(null);
      fail("Should throw IllegalArgumentException when injecting null plugin");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  @SneakyThrows
  public void testThrowExceptionWhenGettingLayoutWithNullSiteKey() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    try {
      dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(null, EXISTING_PORTAL_SITE_NAME);
      fail("Should throw IllegalArgumentException when using null siteKey");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  @SneakyThrows
  public void testWithNonExistingSiteKeyAndNullCurrentPortal() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    PortalConfig result =
                        dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(NON_EXISTING_PORTAL_SITE_NAME),
                                                                                    null);
    assertNull(result);
    result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(NON_EXISTING_GROUP_SITE_NAME), null);
    assertNull(result);
    result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(NON_EXISTING_USER_SITE_NAME), null);
    assertNull(result);
  }

  @Test
  public void testWithNonExistingSiteKeyAndExistingCurrentPortal() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    PortalConfig result = null;
    try {
      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(NON_EXISTING_PORTAL_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNull(result);

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(NON_EXISTING_GROUP_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNull(result);

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(NON_EXISTING_USER_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNull(result);
    } catch (Exception e) {
      fail("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout : " + e.getMessage());
    }
  }

  @Test
  public void testWithExistingSiteKeyAndNullCurrentPortal() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    PortalConfig result = null;
    try {
      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(EXISTING_PORTAL_SITE_NAME), null);
      assertNotNull(result);
      assertEquals(SiteType.PORTAL.getName(), result.getType());
      assertEquals(EXISTING_PORTAL_SITE_NAME, result.getName());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(EXISTING_GROUP_SITE_NAME), null);
      assertNotNull(result);
      assertEquals(SiteType.GROUP.getName(), result.getType());
      assertEquals(EXISTING_GROUP_SITE_NAME, result.getName());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(EXISTING_USER_SITE_NAME), null);
      assertNotNull(result);
      assertEquals(SiteType.USER.getName(), result.getType());
      assertEquals(EXISTING_USER_SITE_NAME, result.getName());
    } catch (Exception e) {
      fail("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout : " + e.getMessage());
    }
  }

  @Test
  public void testWithExistingSiteKeyAndNonExistingCurrentPortal() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    try {
      dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(EXISTING_PORTAL_SITE_NAME),
                                                                  NON_EXISTING_PORTAL_SITE_NAME);
    } catch (IllegalStateException e) {
      fail("Shouldnt throw IllegalStateException since siteKey exists and it's not default");
    } catch (Exception e) {
      fail("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout : " + e.getMessage());
    }
    try {
      PortalConfig result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(EXISTING_GROUP_SITE_NAME),
                                                                                        NON_EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
    } catch (Exception e) {
      fail("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout : " + e.getMessage());
    }
    try {
      PortalConfig result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(EXISTING_USER_SITE_NAME),
                                                                                        NON_EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
    } catch (Exception e) {
      fail("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout : " + e.getMessage());
    }
  }

  @Test
  public void testWithExistingSiteKeyAndExistingCurrentPortal() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, false);
    PortalConfig result = null;
    try {
      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(EXISTING_PORTAL_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.PORTAL.getName(), result.getType());
      assertEquals(EXISTING_PORTAL_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(3, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(EXISTING_GROUP_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.GROUP.getName(), result.getType());
      assertEquals(EXISTING_GROUP_SITE_NAME, result.getName());
      assertTrue(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(1, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(EXISTING_USER_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.USER.getName(), result.getType());
      assertEquals(EXISTING_USER_SITE_NAME, result.getName());
      assertTrue(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(1, result.getPortalLayout().getChildren().size());
    } catch (Exception e) {
      throw new AssertionError("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout", e);
    }
  }

  @Test
  public void testWithExistingSiteKeyAndExistingCurrentPortalAndIgnoredStoredSiteKeyLayout() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(true, true, true, false);
    PortalConfig result = null;
    try {
      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(EXISTING_PORTAL_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.PORTAL.getName(), result.getType());
      assertEquals(EXISTING_PORTAL_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(3, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(EXISTING_GROUP_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.GROUP.getName(), result.getType());
      assertEquals(EXISTING_GROUP_SITE_NAME, result.getName());
      assertTrue(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(1, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(EXISTING_USER_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.USER.getName(), result.getType());
      assertEquals(EXISTING_USER_SITE_NAME, result.getName());
      assertTrue(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(1, result.getPortalLayout().getChildren().size());
    } catch (Exception e) {
      throw new AssertionError("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout", e);
    }
  }

  @Test
  public void testUsingMatcherWithDefaultSiteLayout() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, true, true, true);

    PortalConfig result = null;
    try {
      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(EXISTING_PORTAL_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.PORTAL.getName(), result.getType());
      assertEquals(EXISTING_PORTAL_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals("Last PORTAL site PortalConfig layout should be returned", 3, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(EXISTING_GROUP_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.GROUP.getName(), result.getType());
      assertEquals(EXISTING_GROUP_SITE_NAME, result.getName());
      assertTrue(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals("Last PORTAL site PortalConfig layout should be returned", 3, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(EXISTING_USER_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.USER.getName(), result.getType());
      assertEquals(EXISTING_USER_SITE_NAME, result.getName());
      assertTrue(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(3, result.getPortalLayout().getChildren().size());
    } catch (Exception e) {
      throw new AssertionError("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout", e);
    }
  }

  @Test
  public void testUsingMatcherWithNotDefaultSiteLayout() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(false, false, true, true);

    PortalConfig result = null;
    try {
      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(EXISTING_PORTAL_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.PORTAL.getName(), result.getType());
      assertEquals(EXISTING_PORTAL_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(3, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(EXISTING_GROUP_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.GROUP.getName(), result.getType());
      assertEquals(EXISTING_GROUP_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals("Persisted PortalConfig layout should be returned", 1, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(EXISTING_USER_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.USER.getName(), result.getType());
      assertEquals(EXISTING_USER_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals("Persisted PortalConfig layout should be returned", 1, result.getPortalLayout().getChildren().size());
    } catch (Exception e) {
      throw new AssertionError("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout", e);
    }
  }

  @Test
  public void testUsingMatcherWithNotDefaultSiteLayoutAndForceIgnoreStoredLayout() {
    DynamicPortalLayoutService dynamicPortalLayoutService = mockDynamicLayoutService(true, false, true, true);

    PortalConfig result = null;
    try {
      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.portal(EXISTING_PORTAL_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.PORTAL.getName(), result.getType());
      assertEquals(EXISTING_PORTAL_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals(3, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.group(EXISTING_GROUP_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.GROUP.getName(), result.getType());
      assertEquals(EXISTING_GROUP_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals("Last PORTAL site PortalConfig layout should be returned", 3, result.getPortalLayout().getChildren().size());

      result = dynamicPortalLayoutService.getPortalConfigWithDynamicLayout(SiteKey.user(EXISTING_USER_SITE_NAME),
                                                                           EXISTING_PORTAL_SITE_NAME);
      assertNotNull(result);
      assertEquals(SiteType.USER.getName(), result.getType());
      assertEquals(EXISTING_USER_SITE_NAME, result.getName());
      assertFalse(result.isDefaultLayout());
      assertNotNull(result.getPortalLayout());
      assertEquals("Last PORTAL site PortalConfig layout should be returned", 3, result.getPortalLayout().getChildren().size());
    } catch (Exception e) {
      throw new AssertionError("Unhandled error occurs when invoking getPortalConfigWithDynamicLayout", e);
    }
  }

  private DynamicPortalLayoutService mockDynamicLayoutService(boolean forceIgnoreStoredLayout,
                                                              boolean markPortalConfigAsDefault,
                                                              boolean startService,
                                                              boolean addMatcher) {
    try {
      InitParams params = new InitParams();
      ValueParam ignoreStoredLayoutParam = new ValueParam();
      ignoreStoredLayoutParam.setName("forceIgnoreStoredLayout");
      ignoreStoredLayoutParam.setValue(String.valueOf(forceIgnoreStoredLayout));
      params.addParameter(ignoreStoredLayoutParam);

      ConfigurationManager configurationManager = mock(ConfigurationManager.class);
      LayoutService dataStorage = mock(LayoutService.class);

      PortalConfig portalPortalConfig = new PortalConfig(SiteType.PORTAL.getName(), EXISTING_PORTAL_SITE_NAME);
      Container portalLayout = new Container();
      ArrayList<ModelObject> children = new ArrayList<>();
      children.add(new Container());
      children.add(new PageBody());
      children.add(new Container());
      portalLayout.setChildren(children);
      portalPortalConfig.setPortalLayout(portalLayout);

      when(dataStorage.getPortalConfig(eq(SiteType.PORTAL.getName()),
                                       eq(EXISTING_PORTAL_SITE_NAME))).thenReturn(portalPortalConfig);

      PortalConfig groupPortalConfig = new PortalConfig(SiteType.GROUP.getName(), EXISTING_GROUP_SITE_NAME);
      groupPortalConfig.useMetaPortalLayout();
      groupPortalConfig.setDefaultLayout(markPortalConfigAsDefault);
      when(dataStorage.getPortalConfig(eq(SiteType.GROUP.getName()), eq(EXISTING_GROUP_SITE_NAME))).thenReturn(groupPortalConfig);

      PortalConfig userPortalConfig = new PortalConfig(SiteType.USER.getName(), EXISTING_USER_SITE_NAME);
      userPortalConfig.useMetaPortalLayout();
      userPortalConfig.setDefaultLayout(markPortalConfigAsDefault);
      when(dataStorage.getPortalConfig(eq(SiteType.USER.getName()), eq(EXISTING_USER_SITE_NAME))).thenReturn(userPortalConfig);

      DynamicPortalLayoutService dynamicPortalLayoutService = new DynamicPortalLayoutService(configurationManager,
                                                                                             dataStorage,
                                                                                             params);

      if (addMatcher) {
        DynamicPortalLayoutMatcherPlugin dynamicPortalLayoutMatcherPlugin = mock(DynamicPortalLayoutMatcherPlugin.class);

        PortalConfig dynamicGroupPortalConfig = groupPortalConfig.clone();
        dynamicGroupPortalConfig.setPortalLayout(portalLayout.clone());
        PortalConfig dynamicUserPortalConfig = userPortalConfig.clone();
        dynamicUserPortalConfig.setPortalLayout(portalLayout.clone());

        when(dynamicPortalLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(eq(SiteKey.group(EXISTING_GROUP_SITE_NAME)),
                                                                               eq(groupPortalConfig),
                                                                               eq(portalPortalConfig))).thenReturn(dynamicGroupPortalConfig);
        when(dynamicPortalLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(eq(SiteKey.user(EXISTING_USER_SITE_NAME)),
                                                                               eq(userPortalConfig),
                                                                               eq(portalPortalConfig))).thenReturn(dynamicUserPortalConfig);
        dynamicPortalLayoutService.addDynamicLayoutMatcher(dynamicPortalLayoutMatcherPlugin);
      }

      if (startService) {
        dynamicPortalLayoutService.start();
      }
      return dynamicPortalLayoutService;
    } catch (Exception e) {
      throw new AssertionError("Unknown error occurred while preparing mocks", e);
    }
  }

}
