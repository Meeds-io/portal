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
package org.exoplatform.portal.branding;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.mockito.ArgumentCaptor;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.Favicon;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.test.mock.MockHttpServletRequest;

public class BrandingRestResourcesTest extends BaseRestServicesTestCase {

  private BrandingService originalBrandingService;

  private BrandingService brandingService;

  private FileService     fileService;

  private SettingService  settingService;

  protected Class<?> getComponentClass() {
    return BrandingRestResourcesV1.class;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    brandingService = mock(BrandingService.class);
    fileService = mock(FileService.class);
    settingService = mock(SettingService.class);
    originalBrandingService = (BrandingService) getContainer().unregisterComponent(BrandingService.class).getComponentInstance();
    getContainer().registerComponentInstance(BrandingService.class, brandingService);
    getContainer().registerComponentInstance("FileService", fileService);
    getContainer().registerComponentInstance("SettingService", settingService);
  }

  @Override
  public void tearDown() throws Exception {
    getContainer().unregisterComponent("FileService");
    getContainer().unregisterComponent("SettingService");
    getContainer().unregisterComponent(BrandingService.class);
    getContainer().registerComponentInstance(BrandingService.class, originalBrandingService);
    super.tearDown();
  }

  public void testGetBrandingInformation() throws Exception {
    // Given
    String path = "/v1/platform/branding/";
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    Branding branding = new Branding();
    branding.setCompanyName("test1");
    when(brandingService.getBrandingInformation(false)).thenReturn(branding);

    // When
    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);

    // Then
    assertEquals(200, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof Branding);
    Branding brandingResp = (Branding) entity;
    assertEquals("test1", brandingResp.getCompanyName());
  }

  public void testGetDefaultThemeStyle() throws Exception {
    // Given
    String path = "/v1/platform/branding/default";
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    Map<String, String> themeStyle = new HashMap<>();
    when(brandingService.getDefaultThemeStyle()).thenReturn(themeStyle);

    // When
    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);

    // Then
    assertEquals(200, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
  }

  public void testUpdateBrandingInformation() throws Exception {
    // Given
    String path = "/v1/platform/branding/";
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "PUT", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    Branding branding = new Branding();
    branding.setCompanyName("test1");
    JSONObject jsonBranding = new JSONObject();
    jsonBranding.put("companyName", branding.getCompanyName());

    ArgumentCaptor<Branding> brandingArgumentCaptor = ArgumentCaptor.forClass(Branding.class);

    Map<String, List<String>> headers = new HashMap<>();
    headers.put("Content-Type", Arrays.asList("application/json"));

    // When
    ContainerResponse resp = launcher.service("PUT", path, "", headers, jsonBranding.toString().getBytes(), envctx);

    // Then
    assertEquals(204, resp.getStatus());
    Object entity = resp.getEntity();
    assertNull(entity);

    verify(brandingService, times(1)).updateBrandingInformation(brandingArgumentCaptor.capture());
    assertNotNull(brandingArgumentCaptor);
    Branding caturedBranding = brandingArgumentCaptor.getValue();
    assertEquals("test1", caturedBranding.getCompanyName());
  }

  public void testGetBrandingFavicon() throws Exception {
    // Given
    String path = "/v1/platform/branding/favicon?v=test";
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);
    assertEquals(404, resp.getStatus());

    Favicon favicon = new Favicon(null, 5, new byte[] {
        1, 2, 3
    }, 0, 0);
    when(brandingService.getFavicon()).thenReturn(favicon);

    // When
    resp = launcher.service("GET", path, "", null, null, envctx);

    // Then
    assertEquals(200, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof ByteArrayInputStream);
    assertEquals(3, ((ByteArrayInputStream) entity).available());
  }

  public void testGetBrandingLogo() throws Exception {
    // Given
    String path = "/v1/platform/branding/logo?v=test";
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);
    assertEquals(404, resp.getStatus());

    Logo logo = new Logo(null, 5, new byte[] {
        1, 2, 3
    }, 0, 0);
    when(brandingService.getLogo()).thenReturn(logo);

    // When
    resp = launcher.service("GET", path, "", null, null, envctx);

    // Then
    assertEquals(200, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof ByteArrayInputStream);
    assertEquals(3, ((ByteArrayInputStream) entity).available());
  }

}
