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
package org.exoplatform.portal.branding;

import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_CONTEXT;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_PAGE_BG_COLOR_INIT_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_PAGE_WIDTH_INIT_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SCOPE;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_THEME_LESS_PATH;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_THEME_VARIABLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.upload.UploadService;

/**
 * eXIP 7.3.0.30, platform-wide application styling: the declaration loader
 * (new <code>name=value</code> form, empty default, legacy form), the real
 * Less compilation of the template with the new variables (a mock cannot tell
 * a value the compiler refuses), the Topbar gradient neutralized at read time,
 * the stylesheet version marker, the configured page defaults and the strict
 * grammar of the new keys.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BrandingApplicationStylingTest {

  private static final String BRANDING_LESS_PATH = "../../web/portal/src/main/webapp/WEB-INF/conf/branding/branding.less";

  private static final String LESS_FILE_PATH     = "war:/conf/branding/branding.less";

  private static final String RADIAL_GRADIENT    = "radial-gradient(#AABBCCDD 0%, #AABBCCDD 30%, #112233FF 100%)";

  private static final String CONIC_GRADIENT     =
                                             "conic-gradient(from 90deg at top left, #AABBCCDD 0deg, #AABBCCDD 27.00deg, #112233FF 90deg)";

  private static final String CONIC_GRADIENT_FIRST_COLOR = "#AABBCCDD";

  private static final String LINEAR_GRADIENT    = "linear-gradient(to right, #AABBCCFF 0%, #AABBCCFF 30%, #112233FF 100%)";

  @Test
  public void shouldCompileTemplateWithRealLessCompilerUsingSkinEqualDefaults() throws Exception {
    File lessFile = new File(BRANDING_LESS_PATH);
    assumeTrue("branding.less of web/portal is needed to run the real compilation", lessFile.exists());

    SettingService settingService = mock(SettingService.class);
    stub(settingService, "appMarginTop", "32px");
    stub(settingService, "appBorderColor", "#3F8487FF");
    stub(settingService, "appBorderSize", "2px");
    stub(settingService, "appBoxShadow", "0px 3px 3px -2px rgba(0, 0, 0, 0.2), 0px 3px 4px 0px rgba(0, 0, 0, 0.14)");
    stub(settingService, "appBackgroundImage", "url(/portal/rest/v1/platform/branding/appBackground?v=1), " + RADIAL_GRADIENT);
    stub(settingService, "appBackgroundRepeat", "no-repeat");
    stub(settingService, "appTextTitleColor", "#FF0000FF");
    stub(settingService, "appTextTitleBackgroundPaddingTop", "4px");
    stub(settingService, "sideBarBackgroundImage", CONIC_GRADIENT);
    stub(settingService, "topBarBackgroundScrollColor", "#00FF00FF");
    stub(settingService, "topBarSticky", "true");
    stub(settingService, "pageMarginTop", "30px");
    stub(settingService, "borderRadius", "12px");

    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    when(configurationManager.getInputStream(LESS_FILE_PATH)).thenAnswer(invocation -> new FileInputStream(lessFile));

    BrandingServiceImpl brandingService = newBrandingService(settingService, configurationManager, defaultInitParams());
    brandingService.start();

    String css = brandingService.getThemeCSSContent();
    assertNotNull("The Less template must compile with the new variables", css);
    // stored values, including keywords with '-' and 8-digit colours, travel verbatim
    assertTrue(css, css.contains("--allPagesAppMarginTop: 32px;"));
    assertTrue(css, css.contains("--allPagesAppBackgroundRepeat: no-repeat;"));
    assertTrue(css, css.contains("--allPagesAppBorderColor: #3F8487FF;"));
    assertTrue(css, css.contains("--allPagesAppBoxShadow: 0px 3px 3px -2px rgba(0, 0, 0, 0.2), 0px 3px 4px 0px rgba(0, 0, 0, 0.14);"));
    assertTrue(css, css.contains("--allPagesAppBackgroundImage: url(/portal/rest/v1/platform/branding/appBackground?v=1), " + RADIAL_GRADIENT + ";"));
    assertTrue(css, css.contains("--allPagesAppTextTitleColor: #FF0000FF;"));
    assertTrue(css, css.contains("--allPagesAppTextTitleBackgroundPaddingTop: 4px;"));
    assertTrue(css, css.contains("--allPagesSideBarBackgroundImage: " + CONIC_GRADIENT + ";"));
    assertTrue(css, css.contains("--allPagesTopBarBackgroundScrollColor: #00FF00FF;"));
    // not set: the template default equals the skin's built-in fallback
    assertTrue(css, css.contains("--allPagesAppMarginRight: 20px;"));
    assertTrue(css, css.contains("--allPagesAppBackgroundColor: #FFFFFF;"));
    assertTrue(css, css.contains("--allPagesAppBorderSize: 2px;"));
    assertTrue(css, css.contains("--allPagesAppBoxShadow: 0px 3px"));
    assertTrue(css, css.contains("--allPagesAppTextHeaderBackgroundColor: transparent;"));
    assertTrue(css, css.contains("--allPagesAppTextHeaderBackgroundPaddingTop: 0px;"));
    assertTrue(css, css.contains("--allPagesAppTextHeaderColor: #707070;"));
    assertTrue(css, css.contains("--allPagesAppTextTitleBackgroundColor: transparent;"));
    // chained defaults follow the global variables (borderRadius stored, textColor configured)
    assertTrue(css, css.contains("--allPagesAppBorderRadiusTopLeft: 12px;"));
    assertTrue(css, css.contains("--allPagesAppTextColor: #20282c;"));
    // page margins: unset ones stay 'initial' (two skin defaults), set one travels
    assertTrue(css, css.contains("--allPagesMarginTop: 30px;"));
    assertTrue(css, css.contains("--allPagesMarginRight: initial;"));
    // a set page margin also emits the first/last section companion, unset stays initial
    assertTrue(css, css.contains("--allPagesNoMarginTop: 0px;"));
    assertTrue(css, css.contains("--allPagesNoMarginBottom: initial;"));
    assertTrue(brandingService.isTopBarSticky());
  }

  @Test
  public void shouldLoadDeclarationsInNewLegacyAndEmptyForms() {
    SettingService settingService = mock(SettingService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    BrandingServiceImpl brandingService = newBrandingService(settingService, configurationManager, defaultInitParams());

    Map<String, String> defaults = brandingService.getDefaultThemeStyle();
    // name=value (a property override resolves to the raw value, the name stays in front of '=')
    assertEquals("#20282c", defaults.get("textColor"));
    assertEquals("12px", defaults.get("appMarginLeft"));
    // legacy name:value still accepted
    assertEquals("#3f8487", defaults.get("primaryColor"));
    // an exo.properties override written for the old declaration (X=X:value) keeps working: the name prefix is dropped
    assertEquals("#abcdef", defaults.get("secondaryColor"));
    // empty default = declared but not set: absent from the effective style, present in the declaration
    assertTrue(defaults.containsKey("appMarginTop"));
    assertEquals("", defaults.get("appMarginTop"));
    Map<String, String> themeStyle = brandingService.getThemeStyle();
    assertFalse(themeStyle.containsKey("appMarginTop"));
    assertEquals("12px", themeStyle.get("appMarginLeft"));
    // a malformed configured value is not emitted, the built-in default applies
    assertFalse(themeStyle.containsKey("appBorderRadiusTopLeft"));
    assertEquals("false", themeStyle.get("topBarSticky"));
    assertFalse(brandingService.isTopBarSticky());
  }

  @Test
  public void shouldExposeConfiguredPageDefaults() {
    SettingService settingService = mock(SettingService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    BrandingServiceImpl brandingService = newBrandingService(settingService, configurationManager, defaultInitParams());

    assertEquals("1440px", brandingService.getPageWidth());
    assertEquals("#F0F0F0", brandingService.getPageBackgroundColor());
    Map<String, String> defaults = brandingService.getDefaultThemeStyle();
    assertEquals("1440px", defaults.get("pageWidth"));
    assertEquals("#F0F0F0", defaults.get("pageBackgroundColor"));

    when(settingService.get(Context.GLOBAL, Scope.GLOBAL, BrandingServiceImpl.BRANDING_PAGE_WIDTH_KEY)).thenReturn((SettingValue) SettingValue.create("100%"));
    assertEquals("100%", brandingService.getPageWidth());

    // no init-param: today's behaviour, null unless stored
    BrandingServiceImpl bare = newBrandingService(mock(SettingService.class), configurationManager, new InitParams());
    assertNull(bare.getPageWidth());
    assertNull(bare.getPageBackgroundColor());
  }

  @Test
  public void shouldNeutralizeStoredTopBarGradientAtReadTime() {
    // gradient behind an image, transparent colour: the image URL is kept, the gradient's first colour becomes the colour
    Map<String, String> themeStyle = new HashMap<>();
    themeStyle.put("topBarBackgroundImage", "url(/portal/rest/v1/platform/branding/topBarBackground?v=1), " + LINEAR_GRADIENT);
    themeStyle.put("topBarBackgroundColor", "#FFFFFF00");
    BrandingServiceImpl.neutralizeTopBarGradient(themeStyle);
    assertEquals("url(/portal/rest/v1/platform/branding/topBarBackground?v=1)", themeStyle.get("topBarBackgroundImage"));
    assertEquals("#AABBCCFF", themeStyle.get("topBarBackgroundColor"));

    // gradient only, an explicit stored colour: the picker always stores one (white by default) next to a gradient,
    // so the gradient's first colour still wins (PO decision 5); the image becomes none
    themeStyle = new HashMap<>();
    themeStyle.put("topBarBackgroundImage", CONIC_GRADIENT);
    themeStyle.put("topBarBackgroundColor", "#FFFFFFFF");
    BrandingServiceImpl.neutralizeTopBarGradient(themeStyle);
    assertEquals("none", themeStyle.get("topBarBackgroundImage"));
    assertEquals(CONIC_GRADIENT_FIRST_COLOR, themeStyle.get("topBarBackgroundColor"));

    // gradient without a readable colour, transparent stored colour: dropped, the platform default applies
    themeStyle = new HashMap<>();
    themeStyle.put("topBarBackgroundImage", "linear-gradient(to right, var(--x), var(--y))");
    themeStyle.put("topBarBackgroundColor", "#FFFFFF00");
    BrandingServiceImpl.neutralizeTopBarGradient(themeStyle);
    assertEquals("none", themeStyle.get("topBarBackgroundImage"));
    assertNull(themeStyle.get("topBarBackgroundColor"));

    // no gradient: untouched
    themeStyle = new HashMap<>();
    themeStyle.put("topBarBackgroundImage", "url(/x)");
    themeStyle.put("topBarBackgroundColor", "#FFFFFF00");
    BrandingServiceImpl.neutralizeTopBarGradient(themeStyle);
    assertEquals("url(/x)", themeStyle.get("topBarBackgroundImage"));
    assertEquals("#FFFFFF00", themeStyle.get("topBarBackgroundColor"));

    // through the service read path
    SettingService settingService = mock(SettingService.class);
    stub(settingService, "topBarBackgroundImage", LINEAR_GRADIENT);
    stub(settingService, "topBarBackgroundColor", "transparent");
    BrandingServiceImpl brandingService = newBrandingService(settingService, mock(ConfigurationManager.class), defaultInitParams());
    Map<String, String> effective = brandingService.getThemeStyle();
    assertEquals("none", effective.get("topBarBackgroundImage"));
    assertEquals("#AABBCCFF", effective.get("topBarBackgroundColor"));
  }

  @Test
  public void shouldExposeShippedStylesheetVersionWithoutWriting() {
    // no stored time (fresh instance): the exposed time is never older than the shipped template, nothing is written at startup
    SettingService settingService = mock(SettingService.class);
    BrandingServiceImpl brandingService = newBrandingService(settingService, mock(ConfigurationManager.class), defaultInitParams());
    brandingService.start();
    assertTrue(brandingService.getLastUpdatedTime() >= BrandingServiceImpl.THEME_TEMPLATE_VERSION_TIME);
    verify(settingService, never()).set(eq(Context.GLOBAL), eq(Scope.GLOBAL), eq(BrandingServiceImpl.BRANDING_LAST_UPDATED_TIME_KEY), any());
    verify(settingService, never()).set(eq(Context.GLOBAL), eq(Scope.GLOBAL), eq("branding.themeTemplateVersion"), any());

    // a stored time older than the shipped template (pre-upgrade save): the template time wins, the v= parameter changes
    SettingService older = mock(SettingService.class);
    when(older.get(Context.GLOBAL, Scope.GLOBAL, BrandingServiceImpl.BRANDING_LAST_UPDATED_TIME_KEY))
        .thenReturn((SettingValue) SettingValue.create(String.valueOf(BrandingServiceImpl.THEME_TEMPLATE_VERSION_TIME - 1000)));
    assertEquals(BrandingServiceImpl.THEME_TEMPLATE_VERSION_TIME,
                 newBrandingService(older, mock(ConfigurationManager.class), defaultInitParams()).getLastUpdatedTime());

    // a later save wins over the template time
    SettingService newer = mock(SettingService.class);
    when(newer.get(Context.GLOBAL, Scope.GLOBAL, BrandingServiceImpl.BRANDING_LAST_UPDATED_TIME_KEY))
        .thenReturn((SettingValue) SettingValue.create(String.valueOf(BrandingServiceImpl.THEME_TEMPLATE_VERSION_TIME + 5000)));
    assertEquals(BrandingServiceImpl.THEME_TEMPLATE_VERSION_TIME + 5000,
                 newBrandingService(newer, mock(ConfigurationManager.class), defaultInitParams()).getLastUpdatedTime());
  }

  @Test
  public void shouldNeutralizeValuesTheGrammarAcceptsButLessWouldParse() throws Exception {
    File lessFile = new File(BRANDING_LESS_PATH);
    assumeTrue("branding.less of web/portal is needed to run the real compilation", lessFile.exists());
    // values accepted by the grammar that less4j refuses when written plain: written as Less escapes, they travel
    // verbatim to the CSS (the browser ignores an invalid declaration), the stylesheet never breaks
    SettingService settingService = mock(SettingService.class);
    stub(settingService, "appTextTitleFontWeight", "-");
    stub(settingService, "appBoxShadow", "unit(a)");
    stub(settingService, "appBackgroundImage", "linear-gradient(lighten(x), red)");
    stub(settingService, "sideBarBackgroundImage", "lighten(x)");
    stub(settingService, "drawerBackgroundImage", "data-uri(x)");
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    when(configurationManager.getInputStream(LESS_FILE_PATH)).thenAnswer(invocation -> new FileInputStream(lessFile));
    BrandingServiceImpl brandingService = newBrandingService(settingService, configurationManager, defaultInitParams());
    brandingService.start();

    String css = brandingService.getThemeCSSContent();
    assertNotNull(css);
    assertTrue(css, css.contains("--allPagesAppTextTitleFontWeight: -;"));
    assertTrue(css, css.contains("--allPagesAppBoxShadow: unit(a);"));
    assertTrue(css, css.contains("--allPagesAppBackgroundImage: linear-gradient(lighten(x), red);"));
    assertTrue(css, css.contains("--allPagesSideBarBackgroundImage: lighten(x);"));
    assertTrue(css, css.contains("--allPagesDrawerBackgroundImage: data-uri(x);"));
    // colours and sizes stay plain Less values: the template still derives its shades from them
    assertTrue(css, css.contains("--allPagesPrimaryColor: #3f8487;"));
    // a quote can never close the escape
    assertEquals("~\"a\"", BrandingServiceImpl.toLessEscape("a\"\\\n"));
  }

  @Test
  public void shouldKeepLastCompiledStylesheetWhenAValueDoesNotCompile() throws Exception {
    File lessFile = new File(BRANDING_LESS_PATH);
    assumeTrue("branding.less of web/portal is needed to run the real compilation", lessFile.exists());
    SettingService settingService = mock(SettingService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    when(configurationManager.getInputStream(LESS_FILE_PATH)).thenAnswer(invocation -> new FileInputStream(lessFile));
    BrandingServiceImpl brandingService = newBrandingService(settingService, configurationManager, defaultInitParams());
    brandingService.start();
    String goodCss = brandingService.getThemeCSSContent();
    assertNotNull(goodCss);

    // a stored value of a non-escaped key that breaks the template (e.g. written outside the validated PUT path)
    stub(settingService, "borderRadius", "8px; }");
    brandingService.updateLastUpdatedTime(1);
    assertEquals("the last compiled stylesheet is served, not nothing", goodCss, brandingService.getThemeCSSContent());
  }

  @Test
  public void shouldRefuseAThemeValueThatDoesNotCompileBeforeStoringIt() throws Exception {
    File lessFile = new File(BRANDING_LESS_PATH);
    assumeTrue("branding.less of web/portal is needed to run the real compilation", lessFile.exists());
    SettingService settingService = mock(SettingService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    when(configurationManager.getInputStream(LESS_FILE_PATH)).thenAnswer(invocation -> new FileInputStream(lessFile));
    BrandingServiceImpl brandingService = newBrandingService(settingService, configurationManager, defaultInitParams());
    brandingService.start();

    // passes the character grammar (not an app key) but not the compiler: refused with a message code, nothing stored
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> brandingService.updateBrandingInformation(branding("borderRadius", "8px; }")));
    assertEquals("branding.theme.stylesheetCompilationError", e.getMessage());
    verify(settingService, never()).set(eq(BRANDING_CONTEXT), eq(BRANDING_SCOPE), eq("borderRadius"), any());
    // the grammar refusal carries the key
    e = assertThrows(IllegalArgumentException.class, () -> brandingService.updateBrandingInformation(branding("appMarginTop", "12px; }")));
    assertEquals("branding.theme.invalidValue:appMarginTop", e.getMessage());
  }

  @Test
  public void shouldRejectMalformedApplicationStylingValues() {
    SettingService settingService = mock(SettingService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    BrandingServiceImpl brandingService = newBrandingService(settingService, configurationManager, defaultInitParams());

    assertThrows(IllegalArgumentException.class, () -> brandingService.updateBrandingInformation(branding("appMarginTop", "12px; }")));
    assertThrows(IllegalArgumentException.class, () -> brandingService.updateBrandingInformation(branding("pageMarginTop", "12px; }")));
    assertThrows(IllegalArgumentException.class, () -> brandingService.updateBrandingInformation(branding("appBorderColor", "red")));
    assertThrows(IllegalArgumentException.class, () -> brandingService.updateBrandingInformation(branding("topBarSticky", "yes")));
    assertThrows(IllegalArgumentException.class,
                 () -> brandingService.updateBrandingInformation(branding("appBackgroundImage", "url(javascript:alert(1))")));
    assertThrows(IllegalArgumentException.class,
                 () -> brandingService.updateBrandingInformation(branding("appTextTitleBackgroundImage", "url(javascript:alert(1))")));
    assertThrows(IllegalArgumentException.class,
                 () -> brandingService.updateBrandingInformation(branding("appTextTitleFontWeight", "bold; background: url(x)")));
    assertThrows(IllegalArgumentException.class,
                 () -> brandingService.updateBrandingInformation(branding("appBoxShadow", "0 0 1px red; color: url(x)")));
    assertThrows(IllegalArgumentException.class,
                 () -> brandingService.updateBrandingInformation(branding("appTextTitleBackgroundPaddingTop", "4px; }")));

    // the grammar the UI produces is accepted
    brandingService.updateBrandingInformation(branding("appMarginTop", "32px"));
    brandingService.updateBrandingInformation(branding("pageMarginTop", "30px"));
    brandingService.updateBrandingInformation(branding("appBorderColor", "#3F8487FF"));
    brandingService.updateBrandingInformation(branding("appBackgroundImage", RADIAL_GRADIENT));
    brandingService.updateBrandingInformation(branding("appBackgroundImage", CONIC_GRADIENT));
    brandingService.updateBrandingInformation(branding("appTextTitleBackgroundImage", LINEAR_GRADIENT));
    brandingService.updateBrandingInformation(branding("appTextTitleBackgroundPaddingTop", "4px"));
    brandingService.updateBrandingInformation(branding("appTextTitleBackgroundRadius", "8px"));
    brandingService.updateBrandingInformation(branding("appTextTitleFontWeight", "bold"));
    brandingService.updateBrandingInformation(branding("appBoxShadow",
                                                       "0px 3px 3px -2px rgba(0, 0, 0, 0.2), 0px 3px 4px 0px rgba(0, 0, 0, 0.14), 0px 1px 8px 0px rgba(0, 0, 0, 0.12)"));
    brandingService.updateBrandingInformation(branding("topBarSticky", "true"));
    brandingService.updateBrandingInformation(branding("topBarBackgroundScrollColor", "#00FF00FF"));
  }

  private Branding branding(String key, String value) {
    Branding branding = new Branding();
    Map<String, String> themeStyle = new HashMap<>();
    themeStyle.put(key, value);
    branding.setThemeStyle(themeStyle);
    branding.setLoginTitle(new HashMap<>());
    branding.setLoginSubtitle(new HashMap<>());
    return branding;
  }

  private void stub(SettingService settingService, String key, String value) {
    when(settingService.get(BRANDING_CONTEXT, BRANDING_SCOPE, key)).thenReturn((SettingValue) SettingValue.create(value));
  }

  private InitParams defaultInitParams() {
    InitParams initParams = new InitParams();
    ValuesParam themeVariables = new ValuesParam();
    themeVariables.setName(BRANDING_THEME_VARIABLES);
    List<String> variables = Arrays.asList("primaryColor:#3f8487", // legacy declaration form
                                           "secondaryColor=secondaryColor:#abcdef", // legacy override form (exo.properties X=X:value)
                                           "textColor=#20282c",
                                           "borderRadius=8px",
                                           "topBarBackgroundColor=#FFFFFFFF",
                                           "topBarBackgroundImage=none",
                                           "sideBarBackgroundImage=none",
                                           "drawerBackgroundImage=none",
                                           "pageMarginTop=",
                                           "pageMarginRight=",
                                           "appMarginTop=",
                                           "appMarginRight=",
                                           "appMarginLeft=12px", // configured override
                                           "appBorderColor=",
                                           "appBorderSize=",
                                           "appBoxShadow=",
                                           "appBorderRadiusTopLeft=8px; }", // malformed configured value
                                           "appBackgroundImage=",
                                           "appBackgroundRepeat=",
                                           "appTextTitleColor=",
                                           "appTextHeaderColor=",
                                           "appTextTitleFontWeight=",
                                           "appTextTitleBackgroundPaddingTop=",
                                           "appTextTitleBackgroundImage=",
                                           "appTextTitleBackgroundRadius=",
                                           "topBarSticky=false",
                                           "topBarBackgroundScrollColor=");
    themeVariables.setValues(variables);
    initParams.addParam(themeVariables);

    ValueParam lessPath = new ValueParam();
    lessPath.setName(BRANDING_THEME_LESS_PATH);
    lessPath.setValue(LESS_FILE_PATH);
    initParams.addParam(lessPath);

    ValueParam pageWidth = new ValueParam();
    pageWidth.setName(BRANDING_PAGE_WIDTH_INIT_PARAM);
    pageWidth.setValue("1440px");
    initParams.addParam(pageWidth);

    ValueParam pageBackgroundColor = new ValueParam();
    pageBackgroundColor.setName(BRANDING_PAGE_BG_COLOR_INIT_PARAM);
    pageBackgroundColor.setValue("#F0F0F0");
    initParams.addParam(pageBackgroundColor);
    return initParams;
  }

  private BrandingServiceImpl newBrandingService(SettingService settingService,
                                                 ConfigurationManager configurationManager,
                                                 InitParams initParams) {
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    return new BrandingServiceImpl(mock(PortalContainer.class),
                                   configurationManager,
                                   settingService,
                                   mock(FileService.class),
                                   mock(UploadService.class),
                                   localeConfigService,
                                   mock(ListenerService.class),
                                   mock(UserACL.class),
                                   initParams);
  }

}
