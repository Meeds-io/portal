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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.exoplatform.container.ExoContainerContext;

import io.meeds.portal.theme.ThemeReadiness;
import io.meeds.portal.theme.constant.DisplayMode;
import io.meeds.portal.theme.service.ThemeService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The display mode string every page shell and mail keys on.
 */
public class BrandingUtilsTest {

  private ThemeService                      themeService;

  private MockedStatic<ExoContainerContext> context;

  @BeforeEach
  void setUp() {
    ThemeReadiness.reset();
    themeService = mock(ThemeService.class);
    context = Mockito.mockStatic(ExoContainerContext.class);
    context.when(() -> ExoContainerContext.getService(ThemeService.class)).thenReturn(themeService);
  }

  @AfterEach
  void tearDown() {
    context.close();
    ThemeReadiness.reset();
  }

  @Test
  void beforeReadinessThePageIsLightWithoutAskingTheService() {
    assertEquals("light", BrandingUtils.getDisplayMode("john"));
    Mockito.verifyNoInteractions(themeService);
  }

  @Test
  void theEffectiveModeIsWrittenAsAutoLightOrDark() {
    ThemeReadiness.setReady();
    when(themeService.getEffectiveDisplayMode("john")).thenReturn(DisplayMode.AUTOMATIC);
    when(themeService.getEffectiveDisplayMode("mary")).thenReturn(DisplayMode.LIGHT);
    when(themeService.getEffectiveDisplayMode("jack")).thenReturn(DisplayMode.DARK);

    assertEquals("auto", BrandingUtils.getDisplayMode("john"));
    assertEquals("light", BrandingUtils.getDisplayMode("mary"));
    assertEquals("dark", BrandingUtils.getDisplayMode("jack"));
  }

  @Test
  void aFailingServiceLetsTheDeviceDecide() {
    ThemeReadiness.setReady();
    when(themeService.getEffectiveDisplayMode("john")).thenThrow(new IllegalStateException("database down"));

    assertEquals("auto", BrandingUtils.getDisplayMode("john"));
  }

  @Test
  void theRequestOverloadSetsTheAttributeAndReadsTheRemoteUser() {
    ThemeReadiness.setReady();
    when(themeService.getEffectiveDisplayMode("john")).thenReturn(DisplayMode.DARK);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("john");

    assertEquals("dark", BrandingUtils.getDisplayMode(request));

    verify(request).setAttribute(BrandingUtils.DISPLAY_MODE_ATTRIBUTE, "dark");
    assertEquals("auto", BrandingUtils.getDisplayMode((HttpServletRequest) null));
  }

  @Test
  void anExplicitLightPageLinksTheLightFaviconOnly() {
    assertEquals("<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"/l.ico\" />",
                 BrandingUtils.getFaviconLinks("light", "/l.ico", "/d.ico"));
  }

  @Test
  void anExplicitDarkPageLinksTheDarkFaviconOnly() {
    assertEquals("<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"/d.ico\" />",
                 BrandingUtils.getFaviconLinks("dark", "/l.ico", "/d.ico"));
  }

  @Test
  void anAutomaticPageLinksTheLightFaviconAndTheDarkOneUnderTheMediaQuery() {
    assertEquals("<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"/l.ico\" />"
        + "<link rel=\"icon\" media=\"(prefers-color-scheme: dark)\" href=\"/d.ico\" />",
                 BrandingUtils.getFaviconLinks("auto", "/l.ico", "/d.ico"));
  }

  @Test
  void withoutADarkFaviconNoDarkLinkIsWritten() {
    String light = "<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"/l.ico\" />";
    assertEquals(light, BrandingUtils.getFaviconLinks("auto", "/l.ico", "/l.ico"));
    assertEquals(light, BrandingUtils.getFaviconLinks("auto", "/l.ico", null));
    assertEquals(light, BrandingUtils.getFaviconLinks("dark", "/l.ico", null));
  }

  @Test
  void pathsAreEscapedAndNoPathWritesNothing() {
    assertEquals("<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"/a&quot; onload=&quot;x\" />",
                 BrandingUtils.getFaviconLinks("light", "/a\" onload=\"x", null));
    assertEquals("", BrandingUtils.getFaviconLinks("auto", null, null));
  }

  @Test
  void theServiceOverloadReadsBothPathsOfTheBrandingService() {
    BrandingService brandingService = mock(BrandingService.class);
    when(brandingService.getFaviconPath()).thenReturn("/l.ico");
    when(brandingService.getDarkFaviconPath()).thenReturn("/d.ico");
    context.when(() -> ExoContainerContext.getService(BrandingService.class)).thenReturn(brandingService);

    assertEquals(BrandingUtils.getFaviconLinks("auto", "/l.ico", "/d.ico"), BrandingUtils.getFaviconLinks("auto"));
  }

}
