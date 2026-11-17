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
import org.exoplatform.portal.branding.model.ThemeStylesheet;
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
    io.meeds.portal.theme.ThemeReadiness.reset();
    if (getContainer().getComponentAdapterOfType(io.meeds.portal.theme.service.ThemeService.class) != null) {
      getContainer().unregisterComponent(io.meeds.portal.theme.service.ThemeService.class);
    }
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
    themeServicesReady();
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

  public void testTheDeprecatedPutAnswersUnavailableBeforeReadinessAndWritesNothing() throws Exception {
    io.meeds.portal.theme.ThemeReadiness.reset();
    String path = "/v1/platform/branding/";
    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, new MockHttpServletRequest(path, null, 0, "PUT", null));
    JSONObject jsonBranding = new JSONObject();
    jsonBranding.put("companyName", "test1");
    jsonBranding.put("logo", new JSONObject().put("uploadId", "upload-1"));
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("Content-Type", Arrays.asList("application/json"));

    ContainerResponse resp = launcher.service("PUT", path, "", headers, jsonBranding.toString().getBytes(), envctx);

    assertEquals(503, resp.getStatus());
    verify(brandingService, never()).updateBrandingInformation(any());
    verify(fileService, never()).writeFile(any());
  }

  public void testUpdateBrandingInformationRefusedWithMessageCode() throws Exception {
    themeServicesReady();
    // Given: a value the service refuses (invalid theme value or a stylesheet that does not compile)
    String path = "/v1/platform/branding/";
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "PUT", null);
    envctx.put(HttpServletRequest.class, httpRequest);
    doThrow(new IllegalArgumentException("branding.theme.invalidValue:appMarginTop")).when(brandingService)
                                                                                     .updateBrandingInformation(any());
    JSONObject jsonBranding = new JSONObject();
    jsonBranding.put("companyName", "test1");
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("Content-Type", Arrays.asList("application/json"));

    // When
    ContainerResponse resp = launcher.service("PUT", path, "", headers, jsonBranding.toString().getBytes(), envctx);

    // Then: 400 with the message code as body, not a 500
    assertEquals(400, resp.getStatus());
    assertEquals("branding.theme.invalidValue:appMarginTop", resp.getEntity());
  }

  public void testTheThemesViewShowsTheEffectiveCompanyNameBesideTheStoredOne() throws Exception {
    // Given: no company name stored, the configured one applies
    io.meeds.portal.theme.service.ThemeService themeService = mock(io.meeds.portal.theme.service.ThemeService.class);
    getContainer().registerComponentInstance(io.meeds.portal.theme.service.ThemeService.class, themeService);
    io.meeds.portal.theme.ThemeReadiness.setReady();
    io.meeds.portal.theme.model.BrandingState state = mock(io.meeds.portal.theme.model.BrandingState.class);
    when(state.getVersion()).thenReturn(3L);
    when(themeService.getState()).thenReturn(state);
    io.meeds.portal.theme.model.Theme inUse = mock(io.meeds.portal.theme.model.Theme.class);
    when(inUse.getId()).thenReturn(1L);
    when(themeService.getThemeInUse()).thenReturn(inUse);
    io.meeds.portal.theme.model.Theme defaultTheme = new io.meeds.portal.theme.model.Theme();
    defaultTheme.setId(1L);
    defaultTheme.setName("Default Theme");
    defaultTheme.setOrigin(io.meeds.portal.theme.constant.ThemeOrigin.DEFAULT);
    when(themeService.getThemes()).thenReturn(List.of(defaultTheme));
    io.meeds.portal.theme.model.ThemeSettings configured = new io.meeds.portal.theme.model.ThemeSettings(new HashMap<>(),
                                                                                                         new HashMap<>(),
                                                                                                         new HashMap<>(Map.of("topBarBackgroundColor",
                                                                                                                              "#101010")));
    when(themeService.getConfiguredSettings(defaultTheme)).thenReturn(configured);
    when(brandingService.getCompanyName()).thenReturn("Digital Workplace");
    String path = "/v1/platform/branding/themes";
    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, new MockHttpServletRequest(path, null, 0, "GET", null));

    // When
    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);

    // Then: the page shows the name in use, and knows none was stored
    assertEquals(200, resp.getStatus());
    JSONObject view = new JSONObject(String.valueOf(resp.getEntity()));
    assertEquals("Digital Workplace", view.getString("effectiveCompanyName"));
    assertTrue(view.isNull("companyName"));
    // the Default Theme carries its configured dark values: the page reads them as configured, not Automatic
    assertEquals("#101010",
                 view.getJSONArray("themes")
                     .getJSONObject(0)
                     .getJSONObject("configSettings")
                     .getJSONObject("dark")
                     .getString("topBarBackgroundColor"));
  }

  public void testUpdateBrandingInformationRefusesUnsupportedFontFamily() throws Exception {
    themeServicesReady();
    String path = "/v1/platform/branding/";
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "PUT", null);
    envctx.put(HttpServletRequest.class, httpRequest);
    JSONObject jsonBranding = new JSONObject();
    jsonBranding.put("companyName", "test1");
    jsonBranding.put("themeStyle", new JSONObject().put("fontFamily", "Comic Sans"));
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("Content-Type", Arrays.asList("application/json"));
    doThrow(new IllegalArgumentException("branding.fontFamily.unsupported")).when(brandingService)
                                                                             .updateBrandingInformation(any());

    ContainerResponse resp = launcher.service("PUT", path, "", headers, jsonBranding.toString().getBytes(), envctx);

    assertEquals(400, resp.getStatus());
    assertEquals("branding.fontFamily.unsupported", resp.getEntity());
  }

  public void testGetBrandingCSSIsTaggedWithTheServedStylesheetStamp() throws Exception {
    themeServicesReady();
    when(brandingService.getLastUpdatedTime()).thenReturn(9L);
    when(brandingService.getThemeStylesheet()).thenReturn(new ThemeStylesheet(7L, ".served { color: red; }"));

    // the served stylesheet's own stamp: tagged with it
    ContainerResponse resp = getCss("/v1/platform/branding/css?v=7");
    assertEquals(200, resp.getStatus());
    assertEquals(".served { color: red; }", resp.getEntity());
    assertEquals("\"7\"", String.valueOf(resp.getHttpHeaders().getFirst("ETag")));

    // another stamp than the node can serve: the short-lived answer, never tagged
    resp = getCss("/v1/platform/branding/css?v=9");
    assertEquals(200, resp.getStatus());
    assertEquals(".served { color: red; }", resp.getEntity());
    assertNull(resp.getHttpHeaders().getFirst("ETag"));
    assertNull(resp.getHttpHeaders().getFirst("Expires"));
  }

  private void themeServicesReady() {
    io.meeds.portal.theme.service.ThemeService themeService = mock(io.meeds.portal.theme.service.ThemeService.class);
    getContainer().registerComponentInstance(io.meeds.portal.theme.service.ThemeService.class, themeService);
    io.meeds.portal.theme.ThemeReadiness.setReady();
  }

  private ContainerResponse getCss(String path) throws Exception {
    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, new MockHttpServletRequest(path, null, 0, "GET", null));
    return launcher.service("GET", path, "", null, null, envctx);
  }

  public void testGetBrandingCssLastModifiedNeverInTheFuture() throws Exception {
    // Given: the exposed time carries the template fingerprint and may exceed now by up to 2^32 ms
    themeServicesReady();
    long future = System.currentTimeMillis() + 40L * 24 * 3600 * 1000;
    String path = "/v1/platform/branding/css?v=" + future;
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);
    when(brandingService.getThemeStylesheet()).thenReturn(new ThemeStylesheet(future, ":root {}"));

    // When
    long before = System.currentTimeMillis();
    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);

    // Then: the ETag keeps the fingerprint, Last-Modified is capped to now
    assertEquals(200, resp.getStatus());
    assertEquals("\"" + future + "\"", String.valueOf(resp.getHttpHeaders().getFirst("ETag")));
    Object lastModified = resp.getHttpHeaders().getFirst("Last-Modified");
    assertNotNull(lastModified);
    long lastModifiedTime = lastModified instanceof Date date ? date.getTime()
                                                              : new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US).parse(String.valueOf(lastModified)).getTime();
    assertTrue("Last-Modified must not be in the future: " + lastModified, lastModifiedTime <= System.currentTimeMillis() + 1000);
    assertTrue(lastModifiedTime >= before - 1000);
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
