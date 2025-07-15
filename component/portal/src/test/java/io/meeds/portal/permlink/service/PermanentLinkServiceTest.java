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
package io.meeds.portal.permlink.service;

import static io.meeds.portal.permlink.service.PermanentLinkServiceImpl.PERMANENT_LINK_IDS_SCOPE;
import static io.meeds.portal.permlink.service.PermanentLinkServiceImpl.PERMANENT_LINK_SALT_NAME;
import static io.meeds.portal.permlink.service.PermanentLinkServiceImpl.PERMANEN_LINK_CONTEXT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.security.Identity;

import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.plugin.PermanentLinkPlugin;

@RunWith(MockitoJUnitRunner.class)
public class PermanentLinkServiceTest {

  private static final String      PERMANENT_LINK_ID         = "testLinkId";

  private static final String      INTERNAL_PATH_TEST_PREFIX = "internalPath/";

  private static final String      TEST_USER                 = "testUser";

  private static final String      TEST_PLUGIN               = "testPlugin";

  @Mock
  private PortalContainer          container;

  @Mock
  private SettingService           settingService;

  private PermanentLinkServiceImpl permanentLinkService;

  @Before
  public void setup() {
    permanentLinkService = new PermanentLinkServiceImpl(container, settingService);
  }

  @Test
  public void testInit() {
    permanentLinkService.start();
    verify(settingService).set(eq(PERMANEN_LINK_CONTEXT),
                               eq(PERMANENT_LINK_IDS_SCOPE),
                               eq(PERMANENT_LINK_SALT_NAME),
                               argThat(v -> StringUtils.equals(permanentLinkService.getSalt(), v.getValue().toString())));
  }

  @Test
  public void testGetPermannentLinkWhenNoPlugin() {
    mockEmptyPlugins();
    assertNull(permanentLinkService.getPermanentLink(new PermanentLinkObject(TEST_PLUGIN, "2")));
  }

  @Test
  public void testGetPermannentLink() {
    mockPlugins();
    String permanentLink = permanentLinkService.getPermanentLink(new PermanentLinkObject(TEST_PLUGIN, "2"));
    assertNotNull(permanentLink);
    assertTrue(permanentLink.startsWith(PermanentLinkService.PERMANENT_LINK_URL_PREFIX));
    assertFalse(permanentLink.contains(INTERNAL_PATH_TEST_PREFIX));
  }

  @Test
  public void testGetLinkWhenNoPlugin() {
    mockEmptyPlugins();
    assertThrows(ObjectNotFoundException.class, () -> permanentLinkService.getLink(new PermanentLinkObject(TEST_PLUGIN, "2")));
  }

  @Test
  public void testGetLink() throws ObjectNotFoundException {
    mockPlugins();
    String directLink = permanentLinkService.getLink(new PermanentLinkObject(TEST_PLUGIN, "2"));
    assertNotNull(directLink);
    assertFalse(directLink.startsWith(PermanentLinkService.PERMANENT_LINK_URL_PREFIX));
    assertTrue(directLink.contains(INTERNAL_PATH_TEST_PREFIX));
  }

  @Test
  public void testGetDirectAccessUrlWhenNoPlugins() {
    mockEmptyPlugins();
    Identity identity = mock(Identity.class);
    assertThrows(ObjectNotFoundException.class, () -> permanentLinkService.getDirectAccessUrl("", identity));
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testGetDirectAccessUrlWhenAccessDenied() {
    mockPlugins();
    Identity identity = mock(Identity.class);
    when(settingService.get(PERMANEN_LINK_CONTEXT,
                            PERMANENT_LINK_IDS_SCOPE,
                            PERMANENT_LINK_ID)).thenReturn((SettingValue) SettingValue.create(TEST_PLUGIN + "/3?param=value"));
    assertThrows(IllegalAccessException.class,
                 () -> permanentLinkService.getDirectAccessUrl(PERMANENT_LINK_ID, identity));
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testGetDirectAccessUrl() throws IllegalAccessException, ObjectNotFoundException {
    mockPlugins();
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn(TEST_USER);
    when(settingService.get(PERMANEN_LINK_CONTEXT,
                            PERMANENT_LINK_IDS_SCOPE,
                            PERMANENT_LINK_ID)).thenReturn((SettingValue) SettingValue.create(TEST_PLUGIN + "/3?param=value"));
    String directAccessUrl = permanentLinkService.getDirectAccessUrl(PERMANENT_LINK_ID, identity);
    assertNotNull(directAccessUrl);
    assertTrue(directAccessUrl.contains(INTERNAL_PATH_TEST_PREFIX));
  }

  private void mockEmptyPlugins() {
    when(container.getComponentInstancesOfType(PermanentLinkPlugin.class)).thenReturn(Collections.emptyList());
    permanentLinkService.initPlugins();
    permanentLinkService.initSalt();
  }

  private void mockPlugins() {
    when(container.getComponentInstancesOfType(PermanentLinkPlugin.class)).thenReturn(Collections.singletonList(new PermanentLinkPlugin() {

      @Override
      public String getObjectType() {
        return TEST_PLUGIN;
      }

      @Override
      public String getDirectAccessUrl(PermanentLinkObject object) throws ObjectNotFoundException {
        boolean hasParam = object.getParameters() != null && object.getParameters().containsKey("param");
        return INTERNAL_PATH_TEST_PREFIX + object.getObjectType() + "/" + object.getObjectId() +
            (hasParam ? "?param=" + object.getParameters().get("param") : "");
      }

      @Override
      public boolean canAccess(PermanentLinkObject object, Identity identity) throws ObjectNotFoundException {
        return identity != null && TEST_USER.equals(identity.getUserId());
      }
    }));
    permanentLinkService.initPlugins();
    permanentLinkService.initSalt();
  }

}
