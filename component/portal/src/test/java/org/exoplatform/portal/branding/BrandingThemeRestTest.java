/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.portal.theme.ThemeReadiness;
import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.constant.ThemeOrigin;
import io.meeds.portal.theme.model.BrandingState;
import io.meeds.portal.theme.model.Theme;
import io.meeds.portal.theme.model.ThemeSettings;
import io.meeds.portal.theme.rest.util.ThemeRestUtils;
import io.meeds.portal.theme.service.ThemeService;

/**
 * The themes and publication endpoints: the answer before the theme services
 * are ready, the no-store page load, and the status of every refusal.
 */
public class BrandingThemeRestTest {

  private static final String    BODY = "{\"version\": 3}";

  private ThemeService           themeService;

  private BrandingRestResourcesV1 rest;

  private BrandingService         brandingService;

  @BeforeEach
  void setUp() {
    ThemeReadiness.reset();
    themeService = mock(ThemeService.class);
    brandingService = mock(BrandingService.class);
    rest = new BrandingRestResourcesV1(brandingService);
    rest.setThemeService(themeService);
    ConversationState.setCurrent(new ConversationState(new Identity("root")));
  }

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
    ThemeReadiness.reset();
  }

  @Test
  void beforeTheThemeServicesAreReadyBothEndpointsAskToRetry() {
    assertEquals(503, rest.getThemes().getStatus());
    Response response = rest.publish(BODY);
    assertEquals(503, response.getStatus());
    assertEquals("10", String.valueOf(response.getMetadata().getFirst("Retry-After")));
  }

  /**
   * The administrator-only endpoints have no service-level caller check on
   * their reads: the annotation is the whole guard, so it is read back
   * reflectively, where a direct call cannot notice it dropped or loosened.
   */
  @Test
  void theAdministrationEndpointsAreRestrictedToAdministrators() throws Exception {
    String[] administrators = { "administrators" };
    assertArrayEquals(administrators, rolesOf("getThemes"));
    assertArrayEquals(administrators, rolesOf("publish", String.class));
    assertArrayEquals(administrators, rolesOf("saveCustomCss", String.class));
    assertArrayEquals(administrators, rolesOf("resolveTheme", String.class));
    assertArrayEquals(administrators, rolesOf("updateBrandingInformation", Branding.class));
  }

  @Test
  void aDeprecatedUpdateThatFailsOnTheServerIsLoggedAsAWarningWithItsCause() throws Exception {
    ThemeReadiness.setReady();
    Logger logger = (Logger) LoggerFactory.getLogger(BrandingRestResourcesV1.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      RuntimeException cause = new RuntimeException("flush failed");
      doThrow(new IllegalStateException(ThemeService.PUBLISH_FAILED_MESSAGE, cause)).when(brandingService)
                                                                                    .updateBrandingInformation(any());
      Response response = rest.updateBrandingInformation(new Branding());
      assertEquals(500, response.getStatus());
      assertEquals(ThemeService.PUBLISH_FAILED_MESSAGE, response.getEntity());
      assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.WARN), "a server failure leaves a WARN trace");

      appender.list.clear();
      doThrow(new IllegalStateException(ThemeService.PUBLISH_CONFLICT_MESSAGE)).when(brandingService).updateBrandingInformation(any());
      assertEquals(409, rest.updateBrandingInformation(new Branding()).getStatus());
      assertTrue(appender.list.stream().noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN)),
                 "a conflict is an expected refusal");
    } finally {
      logger.detachAppender(appender);
    }
  }

  private static String[] rolesOf(String method, Class<?>... parameters) throws NoSuchMethodException {
    return BrandingRestResourcesV1.class.getMethod(method, parameters).getAnnotation(RolesAllowed.class).value();
  }

  @Test
  void thePageLoadIsNeverStored() {
    ThemeReadiness.setReady();
    when(themeService.getState()).thenReturn(new BrandingState(4, true, "ACME", 1L, null, 2L, null, null, null, null, null, 0));
    Theme defaultTheme = new Theme(1, "Default Theme", false, ThemeOrigin.DEFAULT, null, new ThemeSettings(), null, null);
    when(themeService.getThemeInUse()).thenReturn(defaultTheme);
    when(themeService.getModesOffered()).thenReturn(ModesOffered.LIGHT_AND_DARK);
    when(themeService.getThemes()).thenReturn(List.of(defaultTheme));

    Response response = rest.getThemes();
    assertEquals(200, response.getStatus());
    String cacheControl = String.valueOf(response.getMetadata().getFirst("Cache-Control"));
    assertTrue(cacheControl.contains("private") && cacheControl.contains("no-store"), cacheControl);
    String body = (String) response.getEntity();
    assertTrue(body.contains("\"version\":4"), body);
    assertTrue(body.contains("\"modesOffered\":\"LIGHT_AND_DARK\""), body);
  }

  @Test
  void aPublicationAnswersTheNewVersion() throws Exception {
    ThemeReadiness.setReady();
    when(themeService.publish(any(), eq("root"))).thenReturn(4L);
    Response response = rest.publish(BODY);
    assertEquals(200, response.getStatus());
    assertEquals("{\"version\":4}", response.getEntity());
    verify(themeService).publish(any(), eq("root"));
  }

  @Test
  void everyRefusalHasItsStatus() throws Exception {
    ThemeReadiness.setReady();
    assertStatus(new IllegalAccessException("not an administrator"), 403, null);
    assertStatus(new ObjectNotFoundException("Theme 9 not found"), 404, null);
    assertStatus(new IllegalArgumentException(ThemeService.NAME_ALREADY_EXISTS_MESSAGE), 400, ThemeService.NAME_ALREADY_EXISTS_MESSAGE);
    assertStatus(new IllegalStateException(ThemeService.PUBLISH_CONFLICT_MESSAGE), 409, ThemeService.PUBLISH_CONFLICT_MESSAGE);
    assertStatus(new IllegalStateException(ThemeService.PUBLISH_FAILED_MESSAGE, new RuntimeException("SQL text")),
                 500,
                 ThemeService.PUBLISH_FAILED_MESSAGE);
    assertEquals(ThemeRestUtils.INVALID_BODY_MESSAGE, rest.publish("{").getEntity());
  }

  @Test
  void theCustomCssIsSavedByItsOwnRequest() throws Exception {
    ThemeReadiness.setReady();
    assertEquals(204, rest.saveCustomCss("{\"customCss\": \".a { color: red; }\"}").getStatus());
    verify(themeService).saveCustomCss(".a { color: red; }", "root");
    assertEquals(204, rest.saveCustomCss("{\"customCss\": \"\"}").getStatus());
    verify(themeService).saveCustomCss("", "root");
  }

  @Test
  void theCustomCssSaveAnswersEachRefusalByItsStatus() throws Exception {
    assertEquals(503, rest.saveCustomCss("{\"customCss\": \"a\"}").getStatus());
    ThemeReadiness.setReady();

    doThrow(new IllegalAccessException("not an administrator")).when(themeService).saveCustomCss(any(), any());
    assertEquals(403, rest.saveCustomCss("{\"customCss\": \"a\"}").getStatus());

    doThrow(new IllegalArgumentException("branding.customCss.forbiddenValue")).when(themeService).saveCustomCss(any(), any());
    Response forbidden = rest.saveCustomCss("{\"customCss\": \"a { width: eval(1) }\"}");
    assertEquals(400, forbidden.getStatus());
    assertEquals("branding.customCss.forbiddenValue", forbidden.getEntity());

    doThrow(new IllegalStateException(ThemeService.PUBLISH_FAILED_MESSAGE,
                                      new RuntimeException("SQL text"))).when(themeService).saveCustomCss(any(), any());
    Response failed = rest.saveCustomCss("{\"customCss\": \"a\"}");
    assertEquals(500, failed.getStatus());
    assertEquals(ThemeService.PUBLISH_FAILED_MESSAGE, failed.getEntity());

    assertEquals(400, rest.saveCustomCss("{").getStatus());
    assertEquals(400, rest.saveCustomCss("{\"css\": \"a\"}").getStatus());
  }

  @Test
  void theDisplayModeIsTheCurrentUsersAndNeverStored() {
    ThemeReadiness.setReady();
    when(themeService.getDisplayMode("root")).thenReturn(io.meeds.portal.theme.constant.DisplayMode.DARK);
    when(themeService.getEffectiveDisplayMode("root")).thenReturn(io.meeds.portal.theme.constant.DisplayMode.DARK);
    when(themeService.getModesOffered()).thenReturn(ModesOffered.LIGHT_AND_DARK);
    Response response = rest.getDisplayMode();
    assertEquals(200, response.getStatus());
    assertTrue(String.valueOf(response.getMetadata().getFirst("Cache-Control")).contains("no-store"));
    assertEquals("{\"mode\":\"DARK\",\"effectiveMode\":\"DARK\",\"modesOffered\":\"LIGHT_AND_DARK\"}", response.getEntity());

    assertEquals(204, rest.setDisplayMode("{\"mode\": \"LIGHT\", \"username\": \"mary\"}").getStatus());
    verify(themeService).setDisplayMode("root", io.meeds.portal.theme.constant.DisplayMode.LIGHT);
    assertEquals(400, rest.setDisplayMode("{\"mode\": \"SEPIA\"}").getStatus());
    assertEquals(400, rest.setDisplayMode("{}").getStatus());
  }

  @Test
  void theStylesheetIsAShortLivedAnswerUntilTheStampMatches() {
    BrandingService brandingService = mock(BrandingService.class);
    rest = new BrandingRestResourcesV1(brandingService);
    rest.setThemeService(themeService);
    when(brandingService.getThemeStylesheet()).thenReturn(new org.exoplatform.portal.branding.model.ThemeStylesheet(5L, "css"));
    javax.ws.rs.core.Request request = mock(javax.ws.rs.core.Request.class);

    // before the theme services are ready
    Response response = rest.getBrandingCSS(request, "5");
    assertEquals(60, ((javax.ws.rs.core.CacheControl) response.getMetadata().getFirst("Cache-Control")).getMaxAge());
    assertEquals(null, response.getMetadata().getFirst("Expires"));

    ThemeReadiness.setReady();
    // another stamp than this node's, still another after the re-read
    when(themeService.refreshState()).thenReturn(true);
    response = rest.getBrandingCSS(request, "6");
    assertEquals(null, response.getMetadata().getFirst("Expires"));
    assertEquals(null, response.getMetadata().getFirst("ETag"));
    // the matching stamp gets the long-lived answer
    response = rest.getBrandingCSS(request, "5");
    assertTrue(response.getMetadata().getFirst("Expires") != null);
  }

  @Test
  void aStaleStampRereadsTheStateOnceAndServesTheNewStampForGood() {
    BrandingService brandingService = mock(BrandingService.class);
    rest = new BrandingRestResourcesV1(brandingService);
    rest.setThemeService(themeService);
    ThemeReadiness.setReady();
    when(brandingService.getThemeStylesheet()).thenReturn(new org.exoplatform.portal.branding.model.ThemeStylesheet(5L, "old"),
                                                          new org.exoplatform.portal.branding.model.ThemeStylesheet(6L, "new"));
    when(themeService.refreshState()).thenReturn(true);
    javax.ws.rs.core.Request request = mock(javax.ws.rs.core.Request.class);

    Response response = rest.getBrandingCSS(request, "6");

    verify(themeService).refreshState();
    assertEquals("new", response.getEntity());
    assertTrue(response.getMetadata().getFirst("Expires") != null, "the re-read stamp matches: the long-lived answer");
  }

  @Test
  void aThemePreviewIsResolvedForAnAdministratorOnly() throws Exception {
    ThemeReadiness.setReady();
    when(themeService.resolve(any(io.meeds.portal.theme.model.ThemePublication.class), eq("root")))
                                                                                           .thenReturn(new io.meeds.portal.theme.model.ResolvedTheme());
    Response response = rest.resolveTheme("{\"id\": 1, \"light\": {\"primaryColor\": null}}");
    assertEquals(200, response.getStatus());
    assertTrue(String.valueOf(response.getMetadata().getFirst("Cache-Control")).contains("no-store"));
    doThrow(new IllegalAccessException("not an administrator")).when(themeService).resolve(any(io.meeds.portal.theme.model.ThemePublication.class), any());
    assertEquals(403, rest.resolveTheme("{}").getStatus());
    assertEquals(400, rest.resolveTheme("[").getStatus());
  }

  @Test
  void theLegacyImageEndpointsServeTheThemeInUsesLightImageOnceReady() throws Exception {
    byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    when(brandingService.getTopBarBackgroundId()).thenReturn(12L);
    when(themeService.readImage(12L)).thenReturn(new io.meeds.portal.theme.model.ImageBytes(svg, "image/svg+xml", 0));
    // before readiness the former path stays: nothing from the theme services
    org.exoplatform.portal.branding.model.Background legacy = new org.exoplatform.portal.branding.model.Background();
    legacy.setData(new byte[] { 1 });
    when(brandingService.getTopBarBackground()).thenReturn(legacy);
    javax.ws.rs.core.Request request = mock(javax.ws.rs.core.Request.class);
    assertEquals(200, rest.getTopBarBackground(request, null).getStatus());
    verify(themeService, org.mockito.Mockito.never()).readImage(12L);

    ThemeReadiness.setReady();
    Response current = rest.getTopBarBackground(request, "12");
    assertEquals(200, current.getStatus());
    assertEquals("nosniff", String.valueOf(current.getMetadata().getFirst("X-Content-Type-Options")));
    assertEquals("default-src 'none'; style-src 'unsafe-inline'", String.valueOf(current.getMetadata().getFirst("Content-Security-Policy")));
    assertTrue(current.getMetadata().getFirst("Expires") != null, "the file the v names is cached for good");
    Response old = rest.getTopBarBackground(request, "11");
    assertEquals(200, old.getStatus());
    assertEquals(null, old.getMetadata().getFirst("Expires"), "an old v gets the current file, briefly");

    // the seven endpoints each read the id of their own image
    when(brandingService.getPageBackgroundId()).thenReturn(21L);
    when(brandingService.getSideBarBackgroundId()).thenReturn(22L);
    when(brandingService.getDrawerBackgroundId()).thenReturn(23L);
    when(brandingService.getAppBackgroundId()).thenReturn(24L);
    when(brandingService.getAppTextTitleBackgroundId()).thenReturn(25L);
    when(brandingService.getAppTextHeaderBackgroundId()).thenReturn(26L);
    for (long id = 21L; id <= 26L; id++) {
      when(themeService.readImage(id)).thenReturn(new io.meeds.portal.theme.model.ImageBytes(svg, "image/svg+xml", 0));
    }
    assertTrue(rest.getPageBackground(request, "21").getMetadata().getFirst("Expires") != null);
    assertTrue(rest.getSideBarBackground(request, "22").getMetadata().getFirst("Expires") != null);
    assertTrue(rest.getDrawerBackground(request, "23").getMetadata().getFirst("Expires") != null);
    assertTrue(rest.getAppBackground(request, "24").getMetadata().getFirst("Expires") != null);
    assertTrue(rest.getAppTextTitleBackground(request, "25").getMetadata().getFirst("Expires") != null);
    assertTrue(rest.getAppTextHeaderBackground(request, "26").getMetadata().getFirst("Expires") != null);

    // a theme with no image answers 404
    when(brandingService.getTopBarBackgroundId()).thenReturn(null);
    assertEquals(404, rest.getTopBarBackground(request, "12").getStatus());
  }

  @Test
  void aThemeImageIsServedByItsFileIdWithItsDetectedType() throws Exception {
    assertEquals(404, rest.getThemeImage(1L, "sideBarBackgroundImage", "light", "12").getStatus(), "before readiness");
    ThemeReadiness.setReady();
    byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    when(themeService.getThemeImageFileId(1L, "sideBarBackgroundImage", true)).thenReturn(12L);
    when(themeService.readImage(12L)).thenReturn(new io.meeds.portal.theme.model.ImageBytes(svg, "image/svg+xml", 0));

    Response current = rest.getThemeImage(1L, "sideBarBackgroundImage", "dark", "12");
    assertEquals(200, current.getStatus());
    assertEquals("nosniff", String.valueOf(current.getMetadata().getFirst("X-Content-Type-Options")));
    assertEquals("default-src 'none'; style-src 'unsafe-inline'", String.valueOf(current.getMetadata().getFirst("Content-Security-Policy")));
    assertTrue(current.getMetadata().getFirst("Expires") != null, "the current file is cached for good");

    Response other = rest.getThemeImage(1L, "sideBarBackgroundImage", "dark", "11");
    assertEquals(200, other.getStatus());
    assertEquals(null, other.getMetadata().getFirst("Expires"), "another id gets the current file, briefly");

    doThrow(new ObjectNotFoundException("no key")).when(themeService).getThemeImageFileId(1L, "primaryColor", false);
    assertEquals(404, rest.getThemeImage(1L, "primaryColor", "light", null).getStatus());
    when(themeService.getThemeImageFileId(2L, "sideBarBackgroundImage", false)).thenReturn(null);
    assertEquals(404, rest.getThemeImage(2L, "sideBarBackgroundImage", "light", null).getStatus());
  }

  @Test
  void anImageIsServedAsWhatItsBytesAre() {
    byte[] png = { (byte) 0x89, 'P', 'N', 'G', 0, 0 };
    Response response = BrandingRestResourcesV1.imageResponse(png, null).build();
    assertTrue(String.valueOf(response.getMetadata().getFirst("Content-Type")).contains("image/png"));
    assertEquals("nosniff", String.valueOf(response.getMetadata().getFirst("X-Content-Type-Options")));
    assertEquals(null, response.getMetadata().getFirst("Content-Security-Policy"));
  }

  @Test
  void theDarkLogoFallsBackToTheLightOneAndIsAddressedByFileId() {
    ThemeReadiness.setReady();
    byte[] png = { (byte) 0x89, 'P', 'N', 'G', 0, 0 };
    when(themeService.getEffectiveDarkLogoFileId()).thenReturn(5L);
    when(themeService.getEffectiveDarkFaviconFileId()).thenReturn(8L);
    when(themeService.readImage(5L)).thenReturn(new io.meeds.portal.theme.model.ImageBytes(png, "image/png", 0));
    when(themeService.readImage(8L)).thenReturn(new io.meeds.portal.theme.model.ImageBytes(png, "image/png", 0));
    Response darkLogo = rest.getDarkLogo("5");
    assertEquals(200, darkLogo.getStatus());
    assertTrue(darkLogo.getMetadata().getFirst("Expires") != null, "the light logo's id, cached for good");
    assertEquals(null, rest.getDarkLogo("4").getMetadata().getFirst("Expires"));
    assertEquals(200, rest.getDarkFavicon("8").getStatus());
  }

  private void assertStatus(Exception exception, int status, String body) throws Exception {
    doThrow(exception).when(themeService).publish(any(), any());
    Response response = rest.publish(BODY);
    assertEquals(status, response.getStatus());
    if (body != null) {
      assertEquals(body, response.getEntity());
    }
  }

}
