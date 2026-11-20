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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.meeds.portal.mail.EmailPreview;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

public class BrandingEmailPreviewTest {

  private static final Map<String, String> LABELS = Map.of("onboarding.email.header", "Create your account and join ",
                                                           "onboarding.email.hello", "Hello",
                                                           "onboarding.email.join", "Join",
                                                           "gatein.forgotPassword.email.header", "Password Reset");

  private static ResourceBundle bundleOf(Map<String, String> labels) {
    return new ResourceBundle() {
      @Override
      protected Object handleGetObject(String key) {
        return labels.get(key);
      }

      @Override
      public Enumeration<String> getKeys() {
        return new Vector<>(labels.keySet()).elements();
      }
    };
  }

  private BrandingService brandingService;

  private UriInfo         uriInfo;

  private String          requestedLang;

  private String          language = "en";

  private BrandingRestResourcesV1 resource;

  private String          previousEmailDomain;

  @Before
  public void setUp() {
    // other suites of the module set the domain configured for the emails: this class decides it itself
    previousEmailDomain = System.getProperty("gatein.email.domain.url");
    System.clearProperty("gatein.email.domain.url");
    brandingService = mock(BrandingService.class);
    when(brandingService.getCompanyName()).thenReturn("Saved Company");
    when(brandingService.getThemeStyle()).thenReturn(Map.of("primaryColor", "#3f8487", "fontFamily", "Inter",
                                                             "borderRadius", "12px"));
    uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/portal/rest/"));
    resource = new BrandingRestResourcesV1(brandingService) {
      @Override
      protected String getLanguage() {
        return language;
      }

      @Override
      protected ResourceBundle getBundle(String lang) {
        requestedLang = lang;
        return bundleOf(LABELS);
      }
    };
  }

  @After
  public void tearDown() {
    if (previousEmailDomain == null) {
      System.clearProperty("gatein.email.domain.url");
    } else {
      System.setProperty("gatein.email.domain.url", previousEmailDomain);
    }
  }

  private static EmailPreviewRequest request(String template) {
    EmailPreviewRequest request = new EmailPreviewRequest();
    request.setTemplate(template);
    return request;
  }

  @Test
  public void testPreviewIsInTheLanguageOfThePlatformWithTheSiteAddressOfTheRequest() {
    language = "fr";
    Response response = resource.previewEmail(uriInfo, request("invitation"));

    assertEquals(200, response.getStatus());
    String html = (String) response.getEntity();
    assertTrue(html, html.contains("Create your account and join Saved Company</h1>"));
    assertTrue(html, html.contains("<html lang=\"fr\""));
    assertEquals("fr", requestedLang);
    assertTrue("the link points to the site of the request", html.contains("href=\"http://localhost:8080/portal/on-boarding?lang=fr&amp;token=sample-token\""));
  }

  @Test
  public void testGreetingIsTheOneOfTheAdministratorWhoPreviews() {
    resource = new BrandingRestResourcesV1(brandingService) {
      @Override
      protected ResourceBundle getBundle(String lang) {
        return bundleOf(LABELS);
      }

      @Override
      protected EmailPreview.Reader getReader() {
        return new EmailPreview.Reader("Amal Haddad", "Amal", "ahaddad");
      }
    };
    String html = (String) resource.previewEmail(uriInfo, request("invitation")).getEntity();
    assertTrue(html, html.contains("Hello Amal Haddad,"));
    assertFalse(html, html.contains("Sam Rivera"));
  }

  @Test
  public void testLanguageIsEnglishWhenThePlatformHasNoDefault() {
    assertEquals("en", new BrandingRestResourcesV1(brandingService).getLanguage());
  }

  @Test
  public void testReaderIsUnknownWithoutAUserAndNothingIsMadeUp() {
    assertNull(resource.getReader().fullName());
    String html = (String) resource.previewEmail(uriInfo, request("invitation")).getEntity();
    assertTrue(html, html.contains("Hello,"));
  }

  @Test
  public void testMissingValuesAreTheSavedBranding() {
    String html = (String) resource.previewEmail(uriInfo, request("reset")).getEntity();
    assertTrue(html, html.contains("Password Reset</h1>"));
    assertTrue(html, html.contains("Saved Company"));
    assertTrue("the font of the saved branding", html.contains("font:15px/1.7 'Inter',Arial,Helvetica,sans-serif"));
    assertTrue(html, html.contains("border-radius:12px"));
    assertEquals("the language is the one of the platform", "en", requestedLang);
  }

  @Test
  public void testRequestCannotCarryBranding() {
    for (String field : new String[] { "companyName", "primaryColor", "fontFamily", "borderRadius" }) {
      assertThrows(field, NoSuchFieldException.class, () -> EmailPreviewRequest.class.getDeclaredField(field));
    }
  }

  @Test
  public void testNothingSavedGivesTheDefaultBranding() {
    when(brandingService.getThemeStyle()).thenReturn(null);
    String html = (String) resource.previewEmail(uriInfo, request("invitation")).getEntity();
    assertTrue(html, html.contains("background-color:" + new io.meeds.portal.mail.EmailBrand("x", null).getFillColor()));
  }

  @Test
  public void testPreviewIsDataForAnIframe() {
    Response response = resource.previewEmail(uriInfo, request("invitation"));
    assertEquals(MediaType.TEXT_HTML_TYPE, response.getMetadata().getFirst("Content-Type"));
    assertEquals("default-src 'none'; style-src 'unsafe-inline'; img-src https: http: data:",
                 response.getMetadata().getFirst("Content-Security-Policy"));
    assertEquals("nosniff", response.getMetadata().getFirst("X-Content-Type-Options"));
    assertEquals("no-store", response.getMetadata().getFirst("Cache-Control"));
  }

  @Test
  public void testUnknownOrMissingTemplateIsRefused() {
    for (EmailPreviewRequest bad : new EmailPreviewRequest[] { request("nope"), request(null), request("OTP"), null }) {
      Response response = resource.previewEmail(uriInfo, bad);
      assertEquals(400, response.getStatus());
      assertEquals("unknownEmailTemplate", response.getEntity());
      assertEquals("no-store", response.getMetadata().getFirst("Cache-Control"));
    }
  }

  @Test
  public void testSavedCompanyNameIsEscaped() {
    when(brandingService.getCompanyName()).thenReturn("<script>alert(1)</script>");
    String html = (String) resource.previewEmail(uriInfo, request("invitation")).getEntity();
    assertFalse(html, html.contains("<script>alert(1)"));
    assertTrue(html, html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
  }

  @Test
  public void testSiteAddressIgnoresTheDomainConfiguredForTheEmails() {
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://10.0.0.5:8080/portal/rest/"));
    String previous = System.getProperty("gatein.email.domain.url");
    try {
      System.setProperty("gatein.email.domain.url", "https://hub.acme.example/");
      assertTrue("the preview follows the domain the administrator uses",
                 ((String) resource.previewEmail(uriInfo, request("reset")).getEntity()).contains("href=\"http://10.0.0.5:8080/portal/forgot-password"));
    } finally {
      if (previous == null) {
        System.clearProperty("gatein.email.domain.url");
      } else {
        System.setProperty("gatein.email.domain.url", previous);
      }
    }
  }

  @Test
  public void testSiteAddressDropsThePortOfTheScheme() {
    when(uriInfo.getBaseUri()).thenReturn(URI.create("https://hub.acme.example:443/portal/rest/"));
    String html = (String) resource.previewEmail(uriInfo, request("reset")).getEntity();
    assertTrue(html, html.contains("href=\"https://hub.acme.example/portal/forgot-password"));
    assertFalse("no :443 in an https link", html.contains(":443"));
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://hub.acme.example:80/portal/rest"));
    assertTrue("no :80 in an http link", ((String) resource.previewEmail(uriInfo, request("reset")).getEntity())
                                                                                                                  .contains("href=\"http://hub.acme.example/portal/forgot-password"));
    when(uriInfo.getBaseUri()).thenReturn(URI.create("https://hub.acme.example:8443/portal/rest/"));
    assertTrue("a port that is not the default one is kept", ((String) resource.previewEmail(uriInfo, request("reset")).getEntity())
                                                                                                                                      .contains("href=\"https://hub.acme.example:8443/portal/forgot-password"));
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://hub.acme.example:443/portal/rest/"));
    assertTrue("443 on http is not the default one", ((String) resource.previewEmail(uriInfo, request("reset")).getEntity())
                                                                                                                              .contains("href=\"http://hub.acme.example:443/portal/forgot-password"));
  }

  @Test
  public void testSiteAddressIsFoundWhenTheBaseAddressHasNoTrailingSlashOrIsMissing() {
    when(uriInfo.getBaseUri()).thenReturn(URI.create("https://hub.acme.example/portal/rest"));
    assertTrue("a base address without a trailing slash", ((String) resource.previewEmail(uriInfo, request("reset")).getEntity())
                                                                                                                                .contains("href=\"https://hub.acme.example/portal/forgot-password"));
    when(uriInfo.getBaseUri()).thenReturn(null);
    when(uriInfo.getRequestUri()).thenReturn(URI.create("https://hub.acme.example:8443/portal/rest/v1/platform/branding/email-preview"));
    String html = (String) resource.previewEmail(uriInfo, request("invitation")).getEntity();
    assertTrue("the request address when there is no base address", html.contains("href=\"https://hub.acme.example:8443/portal/on-boarding"));
    assertFalse("never the placeholder when the request is known", html.contains("example.com"));
  }

  @Test
  public void testSiteAddressIsTheOneOfTheRequestOrAnExample() {
    when(uriInfo.getBaseUri()).thenReturn(URI.create("https://hub.acme.example/portal/rest/"));
    assertTrue(((String) resource.previewEmail(uriInfo, request("reset")).getEntity()).contains("href=\"https://hub.acme.example/portal/forgot-password?token=sample-token&amp;lang=en\""));
    assertTrue(((String) resource.previewEmail(null, request("reset")).getEntity()).contains("href=\"https://example.com/forgot-password?token=sample-token&amp;lang=en\""));
    when(uriInfo.getBaseUri()).thenReturn(URI.create("https://hub.acme.example/other/"));
    assertTrue(((String) resource.previewEmail(uriInfo, request("reset")).getEntity()).contains("href=\"https://example.com/forgot-password?token=sample-token&amp;lang=en\""));
  }

  @Test
  public void testOnlyAdministratorsPreview() throws NoSuchMethodException {
    RolesAllowed roles = BrandingRestResourcesV1.class.getMethod("previewEmail", UriInfo.class, EmailPreviewRequest.class)
                                                      .getAnnotation(RolesAllowed.class);
    assertNotNull(roles);
    assertEquals(1, roles.value().length);
    assertEquals("administrators", roles.value()[0]);
  }

  @Test
  public void testBundleOfALanguageAndEnglishWhenThePlatformHasNone() {
    ResourceBundleService bundleService = mock(ResourceBundleService.class);
    String[] names = { "locale.portal.webui" };
    ResourceBundle english = bundleOf(LABELS);
    ResourceBundle arabic = bundleOf(Map.of("onboarding.email.hello", "مرحبا"));
    when(bundleService.getSharedResourceBundleNames()).thenReturn(names);
    when(bundleService.getResourceBundle(names, Locale.forLanguageTag("ar"))).thenReturn(arabic);
    when(bundleService.getResourceBundle(names, Locale.forLanguageTag("xx-YY"))).thenReturn(null);
    when(bundleService.getResourceBundle(names, Locale.ENGLISH)).thenReturn(english);
    BrandingRestResourcesV1 real = new BrandingRestResourcesV1(brandingService);
    try (MockedStatic<ExoContainerContext> container = Mockito.mockStatic(ExoContainerContext.class)) {
      container.when(() -> ExoContainerContext.getService(any())).thenReturn(bundleService);
      assertEquals(arabic, real.getBundle("ar"));
      assertEquals("an underscore is read as a tag separator", english, real.getBundle("xx_YY"));
    }
    try (MockedStatic<ExoContainerContext> container = Mockito.mockStatic(ExoContainerContext.class)) {
      container.when(() -> ExoContainerContext.getService(any())).thenReturn(null);
      assertNull("without a bundle service the labels of the layout are the default ones", real.getBundle("en"));
    }
  }

  @Test
  public void testLanguageIsTheDefaultLanguageOfThePlatformWhenItIsATag() {
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    LocaleConfig config = mock(LocaleConfig.class);
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(config);
    BrandingRestResourcesV1 real = new BrandingRestResourcesV1(brandingService);
    try (MockedStatic<ExoContainerContext> container = Mockito.mockStatic(ExoContainerContext.class)) {
      container.when(() -> ExoContainerContext.getService(LocaleConfigService.class)).thenReturn(localeConfigService);
      when(config.getLocaleName()).thenReturn(" fr ");
      assertEquals("fr", real.getLanguage());
      when(config.getLocaleName()).thenReturn("pt_BR");
      assertEquals("pt_BR", real.getLanguage());
      when(config.getLocaleName()).thenReturn("en<script>");
      assertEquals("a name that is not a tag is English", "en", real.getLanguage());
      when(config.getLocaleName()).thenReturn(null);
      assertEquals("en", real.getLanguage());
      when(localeConfigService.getDefaultLocaleConfig()).thenReturn(null);
      assertEquals("en", real.getLanguage());
    }
  }

  @Test
  public void testReaderIsTheAdministratorWhoPreviews() throws Exception {
    OrganizationService organizationService = mock(OrganizationService.class);
    UserHandler userHandler = mock(UserHandler.class);
    User user = mock(User.class);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.findUserByName("amal")).thenReturn(user);
    when(user.getFirstName()).thenReturn("Amal");
    when(user.getLastName()).thenReturn("Haddad");
    when(user.getUserName()).thenReturn("amal");
    BrandingRestResourcesV1 real = new BrandingRestResourcesV1(brandingService);
    ConversationState previous = ConversationState.getCurrent();
    try (MockedStatic<ExoContainerContext> container = Mockito.mockStatic(ExoContainerContext.class)) {
      container.when(() -> ExoContainerContext.getService(OrganizationService.class)).thenReturn(organizationService);
      ConversationState.setCurrent(new ConversationState(new Identity("amal")));
      assertEquals(new EmailPreview.Reader("Amal Haddad", "Amal", "amal"), real.getReader());
      when(user.getLastName()).thenReturn(null);
      assertEquals("a user without a last name", "Amal", real.getReader().fullName());
      when(userHandler.findUserByName("amal")).thenReturn(null);
      assertNull("an unknown user", real.getReader().fullName());
      ConversationState.setCurrent(null);
      assertNull("nobody is signed in", real.getReader().fullName());
      ConversationState.setCurrent(new ConversationState(new Identity("amal")));
      when(organizationService.getUserHandler()).thenThrow(new IllegalStateException("down"));
      assertNull("a failing directory only gives the samples", real.getReader().fullName());
    } finally {
      ConversationState.setCurrent(previous);
    }
  }

}
