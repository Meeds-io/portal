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
package io.meeds.portal.mail.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;

import org.exoplatform.portal.branding.BrandingService;

import io.meeds.portal.mail.util.EmailLayout;

public class EmailBrandTest {

  @Test
  public void testFontIsTheFontOfTheBrandingWhenItIsAFontName() {
    assertEquals("Arimo", new EmailBrand("A", "#3f8487").getFontFamily());
    assertEquals("Inter", new EmailBrand("A", "#3f8487", " Inter ", null).getFontFamily());
    assertEquals("Open Sans", new EmailBrand("A", "#3f8487", "Open Sans", null).getFontFamily());
    assertEquals("'Open Sans',Arial,Helvetica,sans-serif", new EmailBrand("A", "#3f8487", "Open Sans", null).getFontStack());
    for (String bad : new String[] { "x';}</style><script>", "Arial, sans-serif", "a;b", "'Arimo'", "", " ", "-Inter", null, "A".repeat(61) }) {
      assertEquals(String.valueOf(bad), EmailBrand.DEFAULT_FONT_FAMILY, new EmailBrand("A", "#3f8487", bad, null).getFontFamily());
    }
  }

  @Test
  public void testEveryColorTheBrandingAcceptsIsUnderstood() {
    assertEquals("#3366cc", new EmailBrand("A", "#36c").getColor());
    assertEquals("#3366cc", new EmailBrand("A", "#36cf").getColor());
    assertEquals("#3f8487", new EmailBrand("A", "#3F8487FF").getColor());
    assertEquals("#c62828", new EmailBrand("A", " #C62828 ").getColor());
    for (String bad : new String[] { "transparent", "initial", "#12", "#12345", "#1234567", "red", "#c62828;x", "", null }) {
      assertEquals(String.valueOf(bad), "#3f8487", new EmailBrand("A", bad).getColor());
    }
    assertEquals("the default color is the one of the platform theme", "#3f8487", EmailBrand.DEFAULT_COLOR);
  }

  @Test
  public void testBrandIsReadOnceForAWhile() {
    BrandingService service = mock(BrandingService.class);
    when(service.getThemeStyle()).thenReturn(Map.of("primaryColor", "#c62828"));
    org.exoplatform.container.PortalContainer container = mock(org.exoplatform.container.PortalContainer.class);
    when(container.getComponentInstanceOfType(BrandingService.class)).thenReturn(service);
    EmailBrand.clearCache();
    try (org.mockito.MockedStatic<org.exoplatform.container.PortalContainer> statics =
        org.mockito.Mockito.mockStatic(org.exoplatform.container.PortalContainer.class)) {
      statics.when(org.exoplatform.container.PortalContainer::getInstance).thenReturn(container);
      for (int i = 0; i < 10; i++) {
        assertEquals("#c62828", EmailBrand.current().getColor());
      }
      verify(service, times(1)).getThemeStyle();
      EmailBrand.clearCache();
      EmailBrand.current();
      verify(service, times(2)).getThemeStyle();
    } finally {
      EmailBrand.clearCache();
    }
  }

  @Test
  public void testBrandIsReadAgainOnceTheDelayHasPassed() {
    BrandingService service = mock(BrandingService.class);
    when(service.getThemeStyle()).thenReturn(Map.of("primaryColor", "#c62828"), Map.of("primaryColor", "#2e7d32"));
    org.exoplatform.container.PortalContainer container = mock(org.exoplatform.container.PortalContainer.class);
    when(container.getComponentInstanceOfType(BrandingService.class)).thenReturn(service);
    EmailBrand.clearCache();
    try (org.mockito.MockedStatic<org.exoplatform.container.PortalContainer> statics =
        org.mockito.Mockito.mockStatic(org.exoplatform.container.PortalContainer.class)) {
      statics.when(org.exoplatform.container.PortalContainer::getInstance).thenReturn(container);
      long start = 1_000_000L;
      assertEquals("#c62828", EmailBrand.current(start).getColor());
      assertEquals("kept just before the delay", "#c62828", EmailBrand.current(start + EmailBrand.CACHE_MILLIS - 1).getColor());
      verify(service, times(1)).getThemeStyle();
      assertEquals("read again once the delay has passed", "#2e7d32", EmailBrand.current(start + EmailBrand.CACHE_MILLIS).getColor());
      verify(service, times(2)).getThemeStyle();
    } finally {
      EmailBrand.clearCache();
    }
  }

  @Test
  public void testOutlineIsThePrimaryColorSeenOnWhite() {
    assertEquals("a color that is seen is kept", "#476a9c", new EmailBrand("A", "#476a9c").getOutlineColor());
    for (String color : new String[] { "#f2c230", "#ffb300", "#e25d5d", "#ffffff", "#d6d6d6" }) {
      assertTrue(color, EmailBrand.contrast("#ffffff", new EmailBrand("A", color).getOutlineColor()) >= 3.0d);
    }
  }

  @Test
  public void testFillIsTheBrandColorItselfWhenTextIsReadableOnIt() {
    for (String color : new String[] { "#476a9c", "#2f5d62", "#c62828", "#6a1b9a", "#f2c230", "#ffffff", "#000000" }) {
      assertEquals("an already readable color is not touched", color, new EmailBrand("A", color).getFillColor());
    }
  }

  @Test
  public void testFillIsMovedAsLittleAsNeededWhenTextWouldNotBeReadable() {
    EmailBrand teal = new EmailBrand("A", "#3f8487");
    assertNotEquals("the Meeds teal needed a nudge", "#3f8487", teal.getFillColor());
    assertTrue(teal.getFillColor(), EmailBrand.contrast(teal.getTextColor(), teal.getFillColor()) >= 4.5d);
    assertEquals("the brand color itself is kept", "#3f8487", teal.getColor());
    for (int i = 0; i < 4096; i += 37) {
      String color = String.format("#%06x", i * 4093 % 0xffffff);
      EmailBrand brand = new EmailBrand("A", color);
      assertTrue(color + " -> " + brand.getFillColor(), EmailBrand.contrast(brand.getTextColor(), brand.getFillColor()) >= 4.5d);
    }
  }

  @Test
  public void testBorderRadiusIsOnlyAcceptedAsASmallPixelSize() {
    for (String ok : new String[] { "0px", "4px", "8px", "12px", "29px" }) {
      assertEquals(ok, new EmailBrand("A", "#3f8487", null, ok).getRadius());
    }
    for (String bad : new String[] { "30px", "50%", "8", "8px;color:red", "-4px", "1em", "", null }) {
      assertEquals(String.valueOf(bad), EmailBrand.DEFAULT_RADIUS, new EmailBrand("A", "#3f8487", null, bad).getRadius());
    }
  }

  @Test
  public void testLinkColorIsThePrimaryColorReadableOnWhite() {
    assertEquals("a readable color is kept", "#476a9c", new EmailBrand("A", "#476a9c").getLinkColor());
    for (String color : new String[] { "#f2c230", "#ffb300", "#3f8487", "#e25d5d", "#ffffff" }) {
      EmailBrand brand = new EmailBrand("A", color);
      assertTrue(color, EmailBrand.contrast("#ffffff", brand.getLinkColor()) >= 4.5d);
    }
    assertEquals(new EmailBrand("A", "#3f8487").getLinkColor(), EmailLayout.linkColor(new EmailBrand("A", "#3f8487")));
    assertEquals("without a container, the default brand", new EmailBrand("A", null).getLinkColor(), EmailLayout.linkColor());
  }

  @Test
  public void testBrandReadsTheThemeOfTheSite() {
    BrandingService branding = mock(BrandingService.class);
    when(branding.getCompanyName()).thenReturn("Acme");
    when(branding.getThemeStyle()).thenReturn(Map.of("primaryColor", "#3f8487", "fontFamily", "Inter", "borderRadius", "12px"));
    EmailBrand brand = EmailBrand.from(branding);
    assertEquals("Acme", brand.getCompanyName());
    assertEquals("#3f8487", brand.getColor());
    assertEquals("Inter", brand.getFontFamily());
    assertEquals("12px", brand.getRadius());
    when(branding.getThemeStyle()).thenReturn(null);
    assertEquals(EmailBrand.DEFAULT_COLOR, EmailBrand.from(branding).getColor());
  }

  @Test
  public void testTheTintOfOutlinedButtonsReadsOnTheDarkPage() {
    assertTrue("the tint reads on the dark page", EmailBrand.contrast("#8ab4f8", "#16191c") >= 4.5d);
  }

  @Test
  public void testALightColorIsDarkenedToBeSeenAndReadOnWhite() {
    EmailBrand light = new EmailBrand("Acme", "#f2c230", null, null);
    assertTrue(EmailBrand.contrast("#ffffff", light.getOutlineColor()) >= 3.0d);
    assertTrue(EmailBrand.contrast("#ffffff", light.getLinkColor()) >= 4.5d);
  }

  @Test
  public void testTextColorHasTheBetterContrast() {
    assertEquals("#ffffff", new EmailBrand("A", "#000000").getTextColor());
    assertEquals("#1d2327", new EmailBrand("A", "#ffffff").getTextColor());
    assertEquals("#ffffff", new EmailBrand("A", "#476a9c").getTextColor());
    assertEquals("#1d2327", new EmailBrand("A", "#f2c230").getTextColor());
    for (String c : new String[] { "#476a9c", "#2f5d62", "#f2c230", "#d6d6d6", "#e53935", "#00897b" }) {
      EmailBrand brand = new EmailBrand("A", c);
      assertTrue(c, EmailBrand.contrast(brand.getTextColor(), brand.getColor()) >= 4.5d || c.equals("#e53935") || c.equals("#00897b"));
    }
  }

}
