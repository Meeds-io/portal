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
package io.meeds.portal.mop.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.Identity;

import io.meeds.portal.plugin.AclPlugin;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SiteAclPluginTest {

  private static final String SITE_ID = "123";

  private static final String SITE_NAME = "classic";

  @Mock
  private PortalContainer     container;

  @Mock
  private LayoutService       layoutService;

  @Mock
  private UserACL             userAcl;

  @Mock
  private Identity            identity;

  @Mock
  private PortalConfig        portalConfig;

  @InjectMocks
  private SiteAclPlugin       plugin;

  @Before
  public void setUp() {
    when(container.getComponentInstanceOfType(LayoutService.class)).thenReturn(layoutService);
    when(container.getComponentInstanceOfType(UserACL.class)).thenReturn(userAcl);
    plugin.init();
  }

  @Test
  public void testGetObjectType() {
    assertEquals(SiteAclPlugin.OBJECT_TYPE, plugin.getObjectType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionWithNonNumericSiteId() {
    plugin.hasPermission("abc", "view", identity);
  }

  @Test
  public void testHasPermissionWithNullPortalConfigAndAdmin() {
    when(layoutService.getPortalConfig(123L)).thenReturn(null);
    when(userAcl.isAdministrator(identity)).thenReturn(true);
    boolean result = plugin.hasPermission(SITE_ID, AclPlugin.VIEW_PERMISSION_TYPE, identity);
    assertTrue(result);
  }

  @Test
  public void testHasPermissionWithNullPortalConfigAndNotAdmin() {
    when(layoutService.getPortalConfig(123L)).thenReturn(null);
    when(userAcl.isAdministrator(identity)).thenReturn(false);
    boolean result = plugin.hasPermission(SITE_ID, AclPlugin.VIEW_PERMISSION_TYPE, identity);
    assertFalse(result);
  }

  @Test
  public void testHasPermissionViewPermissionHasAccess() {
    mockPortalConfig(SITE_NAME);
    when(userAcl.hasAccessPermission(identity, "type", SITE_NAME, portalConfig.getAccessPermissions())).thenReturn(true);
    when(userAcl.hasEditPermission(identity, "type", SITE_NAME, portalConfig.getEditPermission())).thenReturn(false);
    boolean result = plugin.hasPermission(SITE_ID, AclPlugin.VIEW_PERMISSION_TYPE, identity);
    assertTrue(result);
  }

  @Test
  public void testHasPermissionViewPermissionHasEdit() {
    mockPortalConfig(SITE_NAME);
    when(userAcl.hasAccessPermission(identity, "type", SITE_NAME, portalConfig.getAccessPermissions())).thenReturn(false);
    when(userAcl.hasEditPermission(identity, "type", SITE_NAME, portalConfig.getEditPermission())).thenReturn(true);
    boolean result = plugin.hasPermission(SITE_ID, AclPlugin.VIEW_PERMISSION_TYPE, identity);
    assertTrue(result);
  }

  @Test
  public void testHasPermissionEditPermission() {
    mockPortalConfig(SITE_NAME);
    when(userAcl.hasEditPermission(identity, "type", SITE_NAME, portalConfig.getEditPermission())).thenReturn(true);
    boolean result = plugin.hasPermission(SITE_ID, AclPlugin.EDIT_PERMISSION_TYPE, identity);
    assertTrue(result);
  }

  @Test
  public void testHasPermissionDeletePermission() {
    mockPortalConfig(SITE_NAME);
    when(userAcl.hasEditPermission(identity, "type", SITE_NAME, portalConfig.getEditPermission())).thenReturn(false);
    boolean result = plugin.hasPermission(SITE_ID, AclPlugin.DELETE_PERMISSION_TYPE, identity);
    assertFalse(result);
  }

  @Test
  public void testHasPermissionUnknownPermission() {
    mockPortalConfig(SITE_NAME);

    boolean result = plugin.hasPermission(SITE_ID, "unknown", identity);
    assertFalse(result);
  }

  private void mockPortalConfig(String name) {
    when(layoutService.getPortalConfig(123L)).thenReturn(portalConfig);
    when(portalConfig.getName()).thenReturn(name);
    when(portalConfig.getType()).thenReturn("type");
    when(portalConfig.getEditPermission()).thenReturn("editPerm");
    when(portalConfig.getAccessPermissions()).thenReturn(new String[] { "accessPerm" });
  }
}
