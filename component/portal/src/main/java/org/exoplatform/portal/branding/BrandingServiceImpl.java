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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessCompiler.Configuration;
import com.github.sommeri.less4j.core.ThreadUnsafeLessCompiler;

import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.branding.model.Background;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.BrandingFile;
import org.exoplatform.portal.branding.model.Favicon;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import lombok.SneakyThrows;

@SuppressWarnings("unchecked")
public class BrandingServiceImpl implements BrandingService, Startable {

  private static final Log     LOG                                = ExoLogger.getExoLogger(BrandingServiceImpl.class);

  public static final String   BRANDING_RESET_ATTACHMENT_ID       = "0";

  public static final String   BRANDING_LOGO_BASE_PATH            = "/portal/rest/v1/platform/branding/logo?v=";             // NOSONAR

  public static final String   BRANDING_FAVICON_BASE_PATH         = "/portal/rest/v1/platform/branding/favicon?v=";          // NOSONAR

  public static final String   BRANDING_LOGIN_BG_BASE_PATH        = "/portal/rest/v1/platform/branding/loginBackground?v=";  // NOSONAR

  public static final String   BRANDING_PAGE_BG_BASE_PATH         = "/portal/rest/v1/platform/branding/pageBackground?v=";   // NOSONAR

  public static final String   BRANDING_TOP_BAR_BG_BASE_PATH      = "/portal/rest/v1/platform/branding/topBarBackground?v=";

  public static final String   BRANDING_SIDEBAR_BG_BASE_PATH      = "/portal/rest/v1/platform/branding/sideBarBackground?v=";

  public static final String   BRANDING_DRAWER_BG_BASE_PATH       = "/portal/rest/v1/platform/branding/drawerBackground?v=";

  public static final String   BRANDING_APP_BG_BASE_PATH          = "/portal/rest/v1/platform/branding/appBackground?v=";

  public static final String   BRANDING_APP_TITLE_BG_BASE_PATH    = "/portal/rest/v1/platform/branding/appTextTitleBackground?v=";

  public static final String   BRANDING_APP_HEADER_BG_BASE_PATH   = "/portal/rest/v1/platform/branding/appTextHeaderBackground?v=";

  public static final String   BRANDING_COMPANY_NAME_INIT_PARAM   = "exo.branding.company.name";

  // Will be used in Mail notification Footer by example
  public static final String   BRANDING_SITE_NAME_INIT_PARAM      = "exo.branding.company.siteName";

  // Will be used in Mail notification Footer by example
  public static final String   BRANDING_COMPANY_LINK_INIT_PARAM   = "exo.branding.company.link";

  public static final String   BRANDING_LOGIN_TITLE_PARAM         = "authentication.title";

  public static final String   BRANDING_LOGIN_SUBTITLE_PARAM      = "authentication.subtitle";

  public static final String   BRANDING_LOGIN_BG_INIT_PARAM       = "authentication.background";

  public static final String   BRANDING_LOGO_INIT_PARAM           = "exo.branding.company.logo";

  public static final String   BRANDING_PAGE_WIDTH_INIT_PARAM     = "exo.branding.page.width";

  public static final String   BRANDING_PAGE_BG_COLOR_INIT_PARAM  = "exo.branding.page.backgroundColor";

  public static final String   BRANDING_PAGE_BG_EFFECT_INIT_PARAM = "exo.branding.page.backgroundEffect";

  public static final String   BRANDING_FAVICON_INIT_PARAM        = "exo.branding.company.favicon";

  public static final String   BRANDING_COMPANY_NAME_SETTING_KEY  = "exo.branding.company.name";

  public static final String   BRANDING_SITE_NAME_SETTING_KEY     = "exo.branding.company.siteName";

  public static final String   BRANDING_LOGIN_TEXT_COLOR_KEY      = "authentication.loginBackgroundTextColor";

  public static final String   BRANDING_LOGIN_ALT_TEXT_KEY        = "authentication.loginBackgroundAltText";

  public static final String   BRANDING_TITLE_SETTING_KEY         = "authentication.title.";

  public static final String   BRANDING_SUBTITLE_SETTING_KEY      = "authentication.subtitle.";

  public static final String   BRANDING_COMPANY_LINK_SETTING_KEY  = "exo.branding.company.link";

  public static final String   BRANDING_THEME_LESS_PATH           = "exo.branding.theme.path";

  public static final String   BRANDING_THEME_VARIABLES           = "exo.branding.theme.variables";

  public static final String   BRANDING_LOGO_ID_SETTING_KEY       = "exo.branding.company.id";

  public static final String   BRANDING_FAVICON_ID_SETTING_KEY    = "exo.branding.company.favicon.id";

  public static final String   BRANDING_LOGIN_BG_ID_SETTING_KEY   = "authentication.background";

  public static final String   BRANDING_PAGE_BG_ID_SETTING_KEY    = "page.background";

  public static final String   BRANDING_TOP_BAR_BG_ID_SETTING_KEY = "topBar.background";

  public static final String   BRANDING_SIDEBAR_BG_ID_SETTING_KEY = "sideBar.background";

  public static final String   BRANDING_DRAWER_BG_ID_SETTING_KEY  = "drawer.background";

  public static final String   BRANDING_APP_BG_ID_SETTING_KEY     = "app.background";

  public static final String   BRANDING_APP_TITLE_BG_ID_KEY       = "app.textTitle.background";

  public static final String   BRANDING_APP_HEADER_BG_ID_KEY      = "app.textHeader.background";


  public static final String   TOP_BAR_BG_IMAGE_THEME_STYLE_KEY   = "topBarBackgroundImage";

  public static final String   BRANDING_CUSTOM_CSS                = "page.customCss";

  public static final String   SIDEBAR_BG_IMAGE_THEME_STYLE_KEY   = "sideBarBackgroundImage";

  public static final String   DRAWER_BG_IMAGE_THEME_STYLE_KEY    = "drawerBackgroundImage";

  public static final String   APP_BG_IMAGE_THEME_STYLE_KEY       = "appBackgroundImage";

  public static final String   APP_TITLE_BG_IMAGE_THEME_KEY       = "appTextTitleBackgroundImage";

  public static final String   APP_HEADER_BG_IMAGE_THEME_KEY      = "appTextHeaderBackgroundImage";

  public static final String   TOP_BAR_STICKY_THEME_STYLE_KEY     = "topBarSticky";

  public static final String   TOP_BAR_BG_COLOR_THEME_STYLE_KEY   = "topBarBackgroundColor";

  private static final String  THEME_APP_PREFIX                   = "app";

  private static final String  THEME_PAGE_PREFIX                  = "page";

  private static final Pattern GRADIENT_PATTERN                   = Pattern.compile("(linear|radial|conic)-gradient\\(");

  private static final Pattern URL_PATTERN                        = Pattern.compile("url\\([^)]*\\)");

  private static final Pattern FIRST_COLOR_PATTERN                = Pattern.compile("#[0-9a-fA-F]{3,8}|rgba?\\([0-9.,%\\s]*\\)");

  private static final Pattern THEME_COLOR_PATTERN                = Pattern.compile("^(#[0-9a-fA-F]{3,8}|transparent|initial)$");

  private static final Pattern THEME_SIZE_PATTERN                 = Pattern.compile("^(-?\\d{1,4}(px)?|initial)$");

  private static final Pattern THEME_RADIUS_PATTERN               = Pattern.compile("^(initial|(-?\\d{1,4}(px)?)( -?\\d{1,4}(px)?){0,3})$");

  private static final Pattern THEME_KEYWORD_PATTERN              = Pattern.compile("^[a-zA-Z -]{1,30}$");

  private static final Pattern THEME_BOX_SHADOW_PATTERN           = Pattern.compile("^(initial|none|[0-9a-z\\s(),.-]{1,200})$");

  private static final String  GRADIENT_GRAMMAR                   = "(linear|radial|conic)-gradient\\([#0-9a-zA-Z(),.%\\s-]{1,300}\\)";

  /**
   * A submitted background image is the effect only (none or a gradient): the
   * image URL is added by the server (processBackgroundImage) and any url()
   * layer a client sends is stripped first (Architects Lead decision)
   */
  private static final Pattern THEME_BG_EFFECT_PATTERN            = Pattern.compile("^(initial|none|" + GRADIENT_GRAMMAR + ")$");

  private static final Pattern IMAGE_LAYER_PATTERN                = Pattern.compile("url\\([^)]*\\)\\s*,?\\s*");

  public static final String   BRANDING_PAGE_BG_COLOR_KEY         = "page.backgroundColor";

  public static final String   BRANDING_PAGE_BG_REPEAT_KEY        = "page.backgroundRepeat";

  public static final String   BRANDING_PAGE_BG_EFFECT_KEY        = "page.backgroundEffect";

  public static final String   BRANDING_PAGE_WIDTH_KEY            = "page.width";

  public static final String   BRANDING_PAGE_BG_POSITION_KEY      = "page.backgroundPosition";

  public static final String   BRANDING_PAGE_BG_SIZE_KEY          = "page.backgroundSize";

  public static final String   BRANDING_PAGE_BG_ATTACHMENT_KEY    = "page.backgroundAttachment";

  public static final String   BRANDING_CUSTOM_STYLE_FEATURE      = "customStylesheet";

  public static final String   BRANDING_LAST_UPDATED_TIME_KEY     = "branding.lastUpdatedTime";

  public static final String   FILE_API_NAME_SPACE                = "CompanyBranding";

  public static final String   LOGO_NAME                          = "logo.png";

  public static final String   FAVICON_NAME                       = "favicon.ico";

  public static final String   LOGIN_BACKGROUND_NAME              = "loginBackground.png";

  public static final String   TOP_BAR_BACKGROUND_NAME            = "topBarBackground.png";

  public static final String   SIDEBAR_BACKGROUND_NAME            = "sideBarBackground.png";

  public static final String   DRAWER_BACKGROUND_NAME             = "drawerBackground.png";

  public static final String   APP_BACKGROUND_NAME                = "appBackground.png";

  public static final String   APP_TITLE_BACKGROUND_NAME          = "appTextTitleBackground.png";

  public static final String   APP_HEADER_BACKGROUND_NAME         = "appTextHeaderBackground.png";

  public static final String   PAGE_BACKGROUND_NAME               = "pageBackground.png";

  public static final String   BRANDING_DEFAULT_LOGO_PATH         = "/skin/images/logo/DefaultLogo.png";                     // NOSONAR

  public static final String   BRANDING_DEFAULT_FAVICON_PATH      = "/skin/images/favicon.ico";                              // NOSONAR

  public static final Context  BRANDING_CONTEXT                   = Context.GLOBAL.id("BRANDING");

  public static final Scope    BRANDING_SCOPE                     = Scope.APPLICATION.id("BRANDING");

  public static final long     DEFAULT_LAST_MODIFED               = System.currentTimeMillis();

  private PortalContainer      container;

  private LocaleConfigService  localeConfigService;

  private SettingService       settingService;

  private FileService          fileService;

  private UploadService        uploadService;

  private ConfigurationManager configurationManager;

  private ExoFeatureService    featureService;

  private ListenerService      listenerService;

  private UserACL              userAcl;

  private String               defaultCompanyName                 = "";

  private String               defaultSiteName                    = "";

  private String               defaultCompanyLink                 = "";

  private String               defaultConfiguredLogoPath          = null;

  private String               defaultConfiguredFaviconPath       = null;

  private String               defaultConfiguredLoginBgPath       = null;

  private String               lessFilePath                       = null;

  private Map<String, String>  themeVariables                     = null;

  private String               defaultLoginTitle                  = null;

  private String               defaultLoginSubtitle               = null;

  private Map<String, String>  loginTitle                         = null;

  private Map<String, String>  loginSubtitle                      = null;

  private Map<String, String>  supportedLanguages                 = null;

  private String               lessThemeContent                   = null;

  private String               themeCSSContent                    = null;

  /** Last stylesheet that compiled: served while a newer value does not compile (fail-safe) */
  private String               lastCompiledThemeCSS               = null;

  private Long                 templateHash                       = null;

  private boolean              legacyOverrideFormReported         = false;

  private Logo                 logo                               = null;

  private String               customCss                          = null;

  private Favicon              favicon                            = null;

  private Background           loginBackground                    = null;

  private Background           pageBackground                     = null;

  private Background           topBarBackground                   = null;

  private Background           sideBarBackground                  = null;

  private Background           drawerBackground                   = null;

  private Background           appBackground                      = null;

  private Background           appTextTitleBackground             = null;

  private Background           appTextHeaderBackground            = null;

  private String               defaultPageWidth                   = null;

  private String               defaultPageBackgroundColor         = null;

  private String               defaultPageBackgroundEffect        = null;

  public BrandingServiceImpl(PortalContainer container, // NOSONAR
                             ConfigurationManager configurationManager,
                             SettingService settingService,
                             FileService fileService,
                             UploadService uploadService,
                             LocaleConfigService localeConfigService,
                             ListenerService listenerService,
                             UserACL userAcl,
                             InitParams initParams) {
    this.container = container;
    this.configurationManager = configurationManager;
    this.settingService = settingService;
    this.fileService = fileService;
    this.uploadService = uploadService;
    this.localeConfigService = localeConfigService;
    this.listenerService = listenerService;
    this.userAcl = userAcl;

    this.loadLanguages();
    this.loadInitParams(initParams);
  }

  @Override
  public void start() {
    computeThemeCSS();
    listenerService.addListener(ExoFeatureService.FEATURE_STATUS_CHANGED_EVENT, e -> {
      if (StringUtils.equals(BRANDING_CUSTOM_STYLE_FEATURE, (String) e.getSource())) {
        this.triggerBrandingUpdated(true, true);
      }
    });
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public String getThemeCSSContent() {
    if (themeCSSContent == null) {
      this.computeThemeCSS();
    }
    return themeCSSContent;
  }

  /**
   * Get all the branding information
   * 
   * @return The branding object containing all information
   */
  @Override
  public Branding getBrandingInformation() {
    return getBrandingInformation(true);
  }

  @Override
  public Branding getBrandingInformation(boolean retrieveBinaries) {
    Branding branding = new Branding();
    branding.setDefaultLanguage(getDefaultLanguage());
    branding.setDirection(getDefaultLocaleDirection());
    branding.setSupportedLanguages(loadLanguages());
    branding.setCompanyName(getCompanyName());
    branding.setCompanyLink(getCompanyLink());
    branding.setSiteName(getSiteName());
    branding.setLogo(getLogo());
    branding.setFavicon(getFavicon());
    branding.setLoginBackground(getLoginBackground());
    branding.setTopBarBackground(getTopBarBackground());
    branding.setSideBarBackground(getSideBarBackground());
    branding.setDrawerBackground(getDrawerBackground());
    branding.setAppBackground(getAppBackground());
    branding.setAppTextTitleBackground(getAppTextTitleBackground());
    branding.setAppTextHeaderBackground(getAppTextHeaderBackground());
    branding.setLoginBackgroundTextColor(getLoginBackgroundTextColor());
    branding.setLoginBackgroundAltText(getLoginBackgroundAltText());
    branding.setPageBackground(getPageBackground());
    branding.setPageBackgroundColor(getPageBackgroundColor());
    branding.setPageBackgroundPosition(getPageBackgroundPosition());
    branding.setPageBackgroundSize(getPageBackgroundSize());
    branding.setPageBackgroundRepeat(getPageBackgroundRepeat());
    branding.setPageBackgroundAttachment(getPageBackgroundAttachment());
    branding.setPageBackgroundEffect(getPageBackgroundEffect());
    branding.setPageWidth(getPageWidth());
    branding.setCustomCss(getCustomCss());
    branding.setThemeStyle(getThemeStyle());
    branding.setLoginTitle(getLoginTitle());
    branding.setLoginSubtitle(getLoginSubtitle());
    branding.setLastUpdatedTime(getLastUpdatedTime());
    if (!retrieveBinaries) {
      if (branding.getFavicon() != null && branding.getFavicon().getData() != null) {
        Favicon brandingFile = branding.getFavicon().clone();
        brandingFile.setData(null);
        branding.setFavicon(brandingFile);
      }
      if (branding.getLogo() != null && branding.getLogo().getData() != null) {
        Logo brandingFile = branding.getLogo().clone();
        brandingFile.setData(null);
        branding.setLogo(brandingFile);
      }
      if (branding.getLoginBackground() != null && branding.getLoginBackground().getData() != null) {
        Background brandingFile = branding.getLoginBackground().clone();
        brandingFile.setData(null);
        branding.setLoginBackground(brandingFile);
      }
      if (branding.getPageBackground() != null && branding.getPageBackground().getData() != null) {
        Background brandingFile = branding.getPageBackground().clone();
        brandingFile.setData(null);
        branding.setPageBackground(brandingFile);
      }
      if (branding.getTopBarBackground() != null && branding.getTopBarBackground().getData() != null) {
        Background brandingFile = branding.getTopBarBackground().clone();
        brandingFile.setData(null);
        branding.setTopBarBackground(brandingFile);
      }
      if (branding.getSideBarBackground() != null && branding.getSideBarBackground().getData() != null) {
        Background brandingFile = branding.getSideBarBackground().clone();
        brandingFile.setData(null);
        branding.setSideBarBackground(brandingFile);
      }
      if (branding.getDrawerBackground() != null && branding.getDrawerBackground().getData() != null) {
        Background brandingFile = branding.getDrawerBackground().clone();
        brandingFile.setData(null);
        branding.setDrawerBackground(brandingFile);
      }
      if (branding.getAppBackground() != null && branding.getAppBackground().getData() != null) {
        Background brandingFile = branding.getAppBackground().clone();
        brandingFile.setData(null);
        branding.setAppBackground(brandingFile);
      }
      if (branding.getAppTextTitleBackground() != null && branding.getAppTextTitleBackground().getData() != null) {
        Background brandingFile = branding.getAppTextTitleBackground().clone();
        brandingFile.setData(null);
        branding.setAppTextTitleBackground(brandingFile);
      }
      if (branding.getAppTextHeaderBackground() != null && branding.getAppTextHeaderBackground().getData() != null) {
        Background brandingFile = branding.getAppTextHeaderBackground().clone();
        brandingFile.setData(null);
        branding.setAppTextHeaderBackground(brandingFile);
      }
    }
    return branding;
  }

  @Override
  public long getLastUpdatedTime() {
    String lastUpdatedTime = getPropertyValue(BRANDING_LAST_UPDATED_TIME_KEY);
    long storedTime = lastUpdatedTime == null ? DEFAULT_LAST_MODIFED : Long.parseLong(lastUpdatedTime);
    // The exposed time (v= parameter, ETag) also carries the shipped Less template: an upgrade that changes the
    // template changes the stylesheet URL with no write at startup, and a later save still changes it
    // (Architects Lead decision, eXIP 7.3.0.30)
    return storedTime + getTemplateHash();
  }

  /**
   * @return CRC32 of the shipped branding Less template, computed once; 0 when
   *         no template is configured
   */
  long getTemplateHash() {
    if (templateHash == null) {
      String template = getLessThemeContent();
      if (StringUtils.isBlank(template)) {
        return 0;
      }
      CRC32 crc = new CRC32();
      crc.update(template.getBytes(StandardCharsets.UTF_8));
      templateHash = crc.getValue();
    }
    return templateHash;
  }

  @Override
  public void updateBrandingInformation(Branding branding) {
    stripSubmittedImageLayers(branding.getThemeStyle());
    validateCSSInputs(branding);
    try {
      updateCompanyName(branding.getCompanyName(), false);
      updateSiteName(branding.getSiteName(), false);
      updateCompanyLink(branding.getCompanyLink(), false);
      updateLogo(branding.getLogo(), false);
      updateFavicon(branding.getFavicon(), false);
      updateLoginBackground(branding.getLoginBackground(), false);
      updateTopBarBackground(branding.getTopBarBackground(), false);
      updateSideBarBackground(branding.getSideBarBackground(), false);
      updateDrawerBackground(branding.getDrawerBackground(), false);
      updateAppBackground(branding.getAppBackground(), false);
      updateAppTextTitleBackground(branding.getAppTextTitleBackground(), false);
      updateAppTextHeaderBackground(branding.getAppTextHeaderBackground(), false);
      updateLoginBackgroundTextColor(branding.getLoginBackgroundTextColor(), false);
      updateLoginBackgroundAltText(branding.getLoginBackgroundAltText(), false);
      updatePageBackground(branding.getPageBackground(), false);
      updatePageBackgroundColor(branding.getPageBackgroundColor(), false);
      updatePageBackgroundSize(branding.getPageBackgroundSize(), false);
      updatePageBackgroundPosition(branding.getPageBackgroundPosition(), false);
      updatePageBackgroundEffect(branding.getPageBackgroundEffect(), false);
      updatePageBackgroundRepeat(branding.getPageBackgroundRepeat(), false);
      updatePageBackgroundAttachment(branding.getPageBackgroundAttachment(), false);
      updatePageWidth(branding.getPageWidth(), false);
      updateCustomCss(branding.getCustomCss(), false);
      Map<String, String> themeStyles = branding.getThemeStyle();
      processThemeBackgroundImages(themeStyles);
      updateThemeStyle(themeStyles, false);
      updateLoginTitle(branding.getLoginTitle());
      updateLoginSubtitle(branding.getLoginSubtitle());
    } finally {
      triggerBrandingUpdated(true, true);
    }
  }

  @Override
  public String getCompanyName() {
    SettingValue<String> brandingCompanyName = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                                         Scope.GLOBAL,
                                                                                         BRANDING_COMPANY_NAME_SETTING_KEY);
    if (brandingCompanyName != null && StringUtils.isNotBlank(brandingCompanyName.getValue())) {
      return brandingCompanyName.getValue();
    } else {
      return defaultCompanyName;
    }
  }

  @Override
  public String getCompanyLink() {
    return getPropertyValue(BRANDING_COMPANY_LINK_SETTING_KEY, defaultCompanyLink);
  }

  @Override
  public String getSiteName() {
    return getPropertyValue(BRANDING_SITE_NAME_SETTING_KEY, defaultSiteName);
  }

  @Override
  public String getPageBackgroundColor() {
    return getPropertyValue(BRANDING_PAGE_BG_COLOR_KEY, defaultPageBackgroundColor);
  }

  @Override
  public String getPageBackgroundRepeat() {
    return getPropertyValue(BRANDING_PAGE_BG_REPEAT_KEY);
  }

  @Override
  public String getPageBackgroundEffect() {
    return getPropertyValue(BRANDING_PAGE_BG_EFFECT_KEY, defaultPageBackgroundEffect);
  }

  @Override
  public String getPageBackgroundImageUrl() {
    if (getPageBackgroundEffect() == null && getPageBackgroundPath() == null) {
      return null;
    }
    StringBuilder backgroundImageUrl = new StringBuilder();
    if (getPageBackgroundPath() != null) {
      backgroundImageUrl.append("url(");
      backgroundImageUrl.append(getPageBackgroundPath());
      if (getPageBackgroundEffect() != null) {
        backgroundImageUrl.append("), ");
        backgroundImageUrl.append(getPageBackgroundEffect());
        backgroundImageUrl.append(";");
      } else {
        backgroundImageUrl.append(");");
      }
    } else if (getPageBackgroundEffect() != null) {
      return getPageBackgroundEffect().concat("; ");
    }
    return backgroundImageUrl.toString();
  }

  @Override
  public String getPageWidth() {
    return getPropertyValue(BRANDING_PAGE_WIDTH_KEY, defaultPageWidth);
  }

  @Override
  public boolean isTopBarSticky() {
    if (themeVariables == null || !themeVariables.containsKey(TOP_BAR_STICKY_THEME_STYLE_KEY)) {
      return false;
    }
    SettingValue<?> storedValue = settingService.get(BRANDING_CONTEXT, BRANDING_SCOPE, TOP_BAR_STICKY_THEME_STYLE_KEY);
    String value = storedValue == null || storedValue.getValue() == null ? themeVariables.get(TOP_BAR_STICKY_THEME_STYLE_KEY) :
                                                                          storedValue.getValue().toString();
    return Boolean.parseBoolean(value);
  }

  @Override
  public String getCustomCss() {
    return getPropertyValue(BRANDING_CUSTOM_CSS);
  }

  @Override
  public String getPageBackgroundPosition() {
    return getPropertyValue(BRANDING_PAGE_BG_POSITION_KEY);
  }

  @Override
  public String getPageBackgroundSize() {
    return getPropertyValue(BRANDING_PAGE_BG_SIZE_KEY);
  }

  @Override
  public String getPageBackgroundAttachment() {
    return getPropertyValue(BRANDING_PAGE_BG_ATTACHMENT_KEY);
  }

  @Override
  public String getLoginBackgroundTextColor() {
    return getPropertyValue(BRANDING_LOGIN_TEXT_COLOR_KEY);
  }

  @Override
  public String getLoginBackgroundAltText() {
    return getPropertyValue(BRANDING_LOGIN_ALT_TEXT_KEY);
  }

  @Override
  public void updateCompanyName(String companyName) {
    updateCompanyName(companyName, true);
  }

  @Override
  public void updateCompanyLink(String companyLink) {
    updateCompanyLink(companyLink, true);
  }

  @Override
  public void updateSiteName(String siteName) {
    updateSiteName(siteName, true);
  }

  @Override
  public void updateLoginBackgroundTextColor(String textColor) {
    updateLoginBackgroundTextColor(textColor, true);
  }

  @Override
  public void updateLoginBackgroundAltText(String altText) {
    updateLoginBackgroundAltText(altText, true);
  }

  @Override
  public Long getLogoId() {
    return getPropertyValueLong(BRANDING_LOGO_ID_SETTING_KEY);
  }

  @Override
  public Long getFaviconId() {
    return getPropertyValueLong(BRANDING_FAVICON_ID_SETTING_KEY);
  }

  @Override
  public Long getLoginBackgroundId() {
    return getPropertyValueLong(BRANDING_LOGIN_BG_ID_SETTING_KEY);
  }

  @Override
  public Long getPageBackgroundId() {
    return getPropertyValueLong(BRANDING_PAGE_BG_ID_SETTING_KEY);
  }

  @Override
  public Long getTopBarBackgroundId() {
    return getPropertyValueLong(BRANDING_TOP_BAR_BG_ID_SETTING_KEY);
  }

  @Override
  public Long getSideBarBackgroundId() {
    return getPropertyValueLong(BRANDING_SIDEBAR_BG_ID_SETTING_KEY);
  }

  @Override
  public Long getDrawerBackgroundId() {
    return getPropertyValueLong(BRANDING_DRAWER_BG_ID_SETTING_KEY);
  }

  @Override
  public Long getAppBackgroundId() {
    return getPropertyValueLong(BRANDING_APP_BG_ID_SETTING_KEY);
  }

  @Override
  public Long getAppTextTitleBackgroundId() {
    return getPropertyValueLong(BRANDING_APP_TITLE_BG_ID_KEY);
  }

  @Override
  public Long getAppTextHeaderBackgroundId() {
    return getPropertyValueLong(BRANDING_APP_HEADER_BG_ID_KEY);
  }

  @Override
  public Logo getLogo() {
    if (this.logo == null) {
      try {
        Long imageId = getLogoId();
        if (imageId != null && imageId > 0) {
          this.logo = retrieveStoredBrandingFile(imageId, new Logo());
        } else {
          this.logo = retrieveDefaultBrandingFile(defaultConfiguredLogoPath, new Logo(), LOGO_NAME, BRANDING_LOGO_ID_SETTING_KEY);
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving logo", e);
      }
    }
    return this.logo;
  }

  @Override
  public Favicon getFavicon() {
    if (this.favicon == null) {
      try {
        Long imageId = getFaviconId();
        if (imageId != null) {
          this.favicon = retrieveStoredBrandingFile(imageId, new Favicon());
        } else {
          this.favicon = retrieveDefaultBrandingFile(defaultConfiguredFaviconPath,
                                                     new Favicon(),
                                                     FAVICON_NAME,
                                                     BRANDING_FAVICON_ID_SETTING_KEY);
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving favicon", e);
      }
    }
    return this.favicon;
  }

  @Override
  public Background getLoginBackground() {
    if (this.loginBackground == null) {
      try {
        Long imageId = getLoginBackgroundId();
        if (imageId != null) {
          this.loginBackground = retrieveStoredBrandingFile(imageId, new Background());
        } else if (StringUtils.isNotBlank(defaultConfiguredLoginBgPath)) {
          this.loginBackground = retrieveDefaultBrandingFile(defaultConfiguredLoginBgPath,
                                                             new Background(),
                                                             LOGIN_BACKGROUND_NAME,
                                                             BRANDING_LOGIN_BG_ID_SETTING_KEY);
        } else {
          this.loginBackground = new Background();
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving login background", e);
      }
    }
    return this.loginBackground;
  }

  @Override
  public Background getPageBackground() {
    if (this.pageBackground == null) {
      try {
        Long imageId = getPageBackgroundId();
        if (imageId != null) {
          this.pageBackground = retrieveStoredBrandingFile(imageId, new Background());
        } else {
          this.pageBackground = new Background();
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving page background", e);
      }
    }
    return this.pageBackground;
  }

  @Override
  public Background getTopBarBackground() {
    if (this.topBarBackground == null) {
      try {
        Long imageId = getTopBarBackgroundId();
        if (imageId != null) {
          this.topBarBackground = retrieveStoredBrandingFile(imageId, new Background());
        } else {
          this.topBarBackground = new Background();
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving top bar background", e);
      }
    }
    return this.topBarBackground;
  }

  @Override
  public Background getSideBarBackground() {
    if (this.sideBarBackground == null) {
      try {
        Long imageId = getSideBarBackgroundId();
        if (imageId != null) {
          this.sideBarBackground = retrieveStoredBrandingFile(imageId, new Background());
        } else {
          this.sideBarBackground = new Background();
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving sidebar background", e);
      }
    }
    return this.sideBarBackground;
  }

  @Override
  public Background getDrawerBackground() {
    if (this.drawerBackground == null) {
      try {
        Long imageId = getDrawerBackgroundId();
        if (imageId != null) {
          this.drawerBackground = retrieveStoredBrandingFile(imageId, new Background());
        } else {
          this.drawerBackground = new Background();
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving drawer background", e);
      }
    }
    return this.drawerBackground;
  }

  @Override
  public Background getAppBackground() {
    if (this.appBackground == null) {
      try {
        Long imageId = getAppBackgroundId();
        if (imageId != null) {
          this.appBackground = retrieveStoredBrandingFile(imageId, new Background());
        } else {
          this.appBackground = new Background();
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving application default background", e);
      }
    }
    return this.appBackground;
  }

  @Override
  public String getAppBackgroundPath() {
    Background background = getAppBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_APP_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public Background getAppTextTitleBackground() {
    if (this.appTextTitleBackground == null) {
      this.appTextTitleBackground = retrieveBackground(getAppTextTitleBackgroundId(), "application title background");
    }
    return this.appTextTitleBackground;
  }

  @Override
  public String getAppTextTitleBackgroundPath() {
    Background background = getAppTextTitleBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_APP_TITLE_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public Background getAppTextHeaderBackground() {
    if (this.appTextHeaderBackground == null) {
      this.appTextHeaderBackground = retrieveBackground(getAppTextHeaderBackgroundId(), "application header background");
    }
    return this.appTextHeaderBackground;
  }

  @Override
  public String getAppTextHeaderBackgroundPath() {
    Background background = getAppTextHeaderBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_APP_HEADER_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  private Background retrieveBackground(Long imageId, String label) {
    try {
      return imageId != null ? retrieveStoredBrandingFile(imageId, new Background()) : new Background();
    } catch (Exception e) {
      LOG.warn("Error retrieving {}", label, e);
      return null;
    }
  }

  @Override
  public String getLogoPath() {
    Logo brandingLogo = getLogo();
    return brandingLogo == null
           || brandingLogo.getData() == null ? null : BRANDING_LOGO_BASE_PATH + Objects.hash(brandingLogo.getUpdatedDate());
  }

  @Override
  public String getFaviconPath() {
    Favicon brandingFavicon = getFavicon();
    return brandingFavicon == null
           || brandingFavicon.getData() == null ? null :
                                                BRANDING_FAVICON_BASE_PATH + Objects.hash(brandingFavicon.getUpdatedDate());
  }

  @Override
  public String getLoginBackgroundPath() {
    Background background = getLoginBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_LOGIN_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getPageBackgroundPath() {
    Background background = getPageBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_PAGE_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getTopBarBackgroundPath() {
    Background background = getTopBarBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_TOP_BAR_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getSideBarBackgroundPath() {
    Background background = getSideBarBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_SIDEBAR_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getDrawerBackgroundPath() {
    Background background = getDrawerBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_DRAWER_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public void updateLastUpdatedTime(long lastUpdatedTimestamp) {
    if (lastUpdatedTimestamp <= 0) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_LAST_UPDATED_TIME_KEY);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BRANDING_LAST_UPDATED_TIME_KEY, SettingValue.create(lastUpdatedTimestamp));
    }
    this.themeCSSContent = null;
    this.customCss = null;
  }

  @Override
  public void updateLogo(Logo logo) {
    updateLogo(logo, true);
  }

  @Override
  public void updateFavicon(Favicon favicon) {
    updateFavicon(favicon, true);
  }

  @Override
  public void updateLoginBackground(Background loginBackground) {
    updateLoginBackground(loginBackground, true);
  }

  @Override
  public void updateThemeStyle(Map<String, String> themeStyle) {
    updateThemeStyle(themeStyle, true);
  }

  @Override
  public Map<String, String> getThemeStyle() {
    if (themeVariables == null || themeVariables.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> themeStyleVariables = new HashMap<>();
    Set<String> variables = themeVariables.keySet();
    for (String themeVariable : variables) {
      SettingValue<?> storedStyleValue = settingService.get(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable);
      String styleValue = storedStyleValue == null
                          || storedStyleValue.getValue() == null ? themeVariables.get(themeVariable) :
                                                                 storedStyleValue.getValue().toString();
      if (StringUtils.isNotBlank(styleValue)) {
        themeStyleVariables.put(themeVariable, styleValue);
      }
    }
    neutralizeTopBarGradient(themeStyleVariables);
    return themeStyleVariables;
  }

  /**
   * eXIP 7.3.0.30: the gradient option is removed from the Topbar. A gradient
   * still stored in the Topbar background is ignored at read time (never
   * rewritten): the image URL part is kept and the gradient's first colour
   * becomes the Topbar colour (PO decision 5). Only when no colour can be read
   * from the gradient does the stored colour stay, and when that one is
   * transparent or absent it is dropped so that the platform default applies.
   * The picker always stores a plain colour (white by default) next to a
   * gradient, which is why the stored colour cannot take precedence.
   */
  static void neutralizeTopBarGradient(Map<String, String> themeStyle) {
    String backgroundImage = themeStyle.get(TOP_BAR_BG_IMAGE_THEME_STYLE_KEY);
    if (StringUtils.isBlank(backgroundImage) || !GRADIENT_PATTERN.matcher(backgroundImage).find()) {
      return;
    }
    Matcher urlMatcher = URL_PATTERN.matcher(backgroundImage);
    String urlPart = urlMatcher.find() ? urlMatcher.group() : null;
    String gradientPart = urlPart == null ? backgroundImage : backgroundImage.replace(urlPart, "");
    themeStyle.put(TOP_BAR_BG_IMAGE_THEME_STYLE_KEY, urlPart == null ? "none" : urlPart);
    Matcher colorMatcher = FIRST_COLOR_PATTERN.matcher(gradientPart);
    if (colorMatcher.find()) {
      themeStyle.put(TOP_BAR_BG_COLOR_THEME_STYLE_KEY, colorMatcher.group());
    } else {
      String color = themeStyle.get(TOP_BAR_BG_COLOR_THEME_STYLE_KEY);
      if (StringUtils.isBlank(color) || isTransparent(color)) {
        themeStyle.remove(TOP_BAR_BG_COLOR_THEME_STYLE_KEY);
      }
    }
  }

  private static boolean isTransparent(String color) {
    return "transparent".equalsIgnoreCase(color)
           || (color.length() == 9 && color.startsWith("#") && color.toUpperCase().endsWith("00"));
  }

  @Override
  public Map<String, String> getDefaultThemeStyle() {
    Map<String, String> defaultStyle = new HashMap<>();
    if (themeVariables != null) {
      defaultStyle.putAll(themeVariables);
    }
    // Page design defaults, configurable without the UI (exo.branding.page.*)
    if (StringUtils.isNotBlank(defaultPageWidth)) {
      defaultStyle.put("pageWidth", defaultPageWidth);
    }
    if (StringUtils.isNotBlank(defaultPageBackgroundColor)) {
      defaultStyle.put("pageBackgroundColor", defaultPageBackgroundColor);
    }
    if (StringUtils.isNotBlank(defaultPageBackgroundEffect)) {
      defaultStyle.put("pageBackgroundEffect", defaultPageBackgroundEffect);
    }
    return defaultStyle;
  }

  @Override
  public Map<String, String> getLoginTitle() {
    if (this.loginTitle == null) {
      Map<String, String> valuePerLanguage = new HashMap<>();
      for (String language : supportedLanguages.keySet()) {
        SettingValue<?> storedValue = settingService.get(BRANDING_CONTEXT,
                                                         BRANDING_SCOPE,
                                                         BRANDING_TITLE_SETTING_KEY + language);
        if (storedValue != null && storedValue.getValue() != null) {
          valuePerLanguage.put(language, storedValue.getValue().toString());
        }
      }
      valuePerLanguage.computeIfAbsent(getDefaultLanguage(), key -> this.defaultLoginTitle);
      this.loginTitle = valuePerLanguage;
    }
    return Collections.unmodifiableMap(this.loginTitle);
  }

  @Override
  public Map<String, String> getLoginSubtitle() {
    if (this.loginSubtitle == null) {
      Map<String, String> valuePerLanguage = new HashMap<>();
      for (String language : supportedLanguages.keySet()) {
        SettingValue<?> storedValue = settingService.get(BRANDING_CONTEXT,
                                                         BRANDING_SCOPE,
                                                         BRANDING_SUBTITLE_SETTING_KEY + language);
        if (storedValue != null && storedValue.getValue() != null) {
          valuePerLanguage.put(language, storedValue.getValue().toString());
        }
      }
      valuePerLanguage.computeIfAbsent(getDefaultLanguage(), key -> this.defaultLoginSubtitle);
      this.loginSubtitle = valuePerLanguage;
    }
    return Collections.unmodifiableMap(this.loginSubtitle);
  }

  @Override
  public String getLoginTitle(Locale locale) {
    Map<String, String> valuePerLanguage = getLoginTitle();
    if (valuePerLanguage.containsKey(locale.getLanguage())) {
      return valuePerLanguage.get(locale.getLanguage());
    } else {
      return valuePerLanguage.getOrDefault(getDefaultLanguage(), this.defaultLoginTitle);
    }
  }

  @Override
  public String getLoginSubtitle(Locale locale) {
    Map<String, String> valuePerLanguage = getLoginSubtitle();
    if (valuePerLanguage.containsKey(locale.getLanguage())) {
      return valuePerLanguage.get(locale.getLanguage());
    } else {
      return valuePerLanguage.getOrDefault(getDefaultLanguage(), this.defaultLoginTitle);
    }
  }

  @Override
  public String getDefaultLanguage() {
    return getDefaultLocale().toLanguageTag();
  }

  /**
   * Load init params
   * 
   * @param initParams
   * @throws Exception
   */
  private void loadInitParams(InitParams initParams) { // NOSONAR
    if (initParams != null) {
      ValueParam companyNameParam = initParams.getValueParam(BRANDING_COMPANY_NAME_INIT_PARAM);
      if (companyNameParam != null) {
        this.defaultCompanyName = companyNameParam.getValue();
      }

      ValueParam companyLinkParam = initParams.getValueParam(BRANDING_COMPANY_LINK_INIT_PARAM);
      if (companyLinkParam != null) {
        this.defaultCompanyLink = companyLinkParam.getValue();
      }

      ValueParam siteNameParam = initParams.getValueParam(BRANDING_SITE_NAME_INIT_PARAM);
      if (siteNameParam != null) {
        this.defaultSiteName = siteNameParam.getValue();
      }

      ValueParam logoParam = initParams.getValueParam(BRANDING_LOGO_INIT_PARAM);
      if (logoParam != null) {
        this.defaultConfiguredLogoPath = logoParam.getValue();
      }

      ValueParam favIconParam = initParams.getValueParam(BRANDING_FAVICON_INIT_PARAM);
      if (favIconParam != null) {
        this.defaultConfiguredFaviconPath = favIconParam.getValue();
      }

      ValueParam loginBackgroundParam = initParams.getValueParam(BRANDING_LOGIN_BG_INIT_PARAM);
      if (loginBackgroundParam != null) {
        this.defaultConfiguredLoginBgPath = loginBackgroundParam.getValue();
      }

      ValueParam loginTitleParam = initParams.getValueParam(BRANDING_LOGIN_TITLE_PARAM);
      if (loginTitleParam != null) {
        this.defaultLoginTitle = loginTitleParam.getValue();
      }

      ValueParam loginSubtitleParam = initParams.getValueParam(BRANDING_LOGIN_SUBTITLE_PARAM);
      if (loginSubtitleParam != null) {
        this.defaultLoginSubtitle = loginSubtitleParam.getValue();
      }

      ValueParam pageWidthParam = initParams.getValueParam(BRANDING_PAGE_WIDTH_INIT_PARAM);
      if (pageWidthParam != null && StringUtils.isNotBlank(pageWidthParam.getValue())) {
        this.defaultPageWidth = pageWidthParam.getValue().trim();
      }

      ValueParam pageBackgroundColorParam = initParams.getValueParam(BRANDING_PAGE_BG_COLOR_INIT_PARAM);
      if (pageBackgroundColorParam != null && StringUtils.isNotBlank(pageBackgroundColorParam.getValue())) {
        this.defaultPageBackgroundColor = pageBackgroundColorParam.getValue().trim();
      }

      ValueParam pageBackgroundEffectParam = initParams.getValueParam(BRANDING_PAGE_BG_EFFECT_INIT_PARAM);
      if (pageBackgroundEffectParam != null && StringUtils.isNotBlank(pageBackgroundEffectParam.getValue())) {
        this.defaultPageBackgroundEffect = pageBackgroundEffectParam.getValue().trim();
      }

      ValueParam lessFileParam = initParams.getValueParam(BRANDING_THEME_LESS_PATH);
      if (lessFileParam != null) {
        this.lessFilePath = lessFileParam.getValue();
      }

      ValuesParam lessVariablesParam = initParams.getValuesParam(BRANDING_THEME_VARIABLES);
      if (lessVariablesParam != null) {
        List<String> variables = lessVariablesParam.getValues();
        this.themeVariables = new HashMap<>();
        for (String themeVariable : variables) {
          if (StringUtils.isBlank(themeVariable)) {
            continue;
          }
          // Declaration format: <name>=<value> (the value is the resolved ${exo.branding.theme.<name>:<default>},
          // possibly empty = not set). The legacy <name>:<value> form is still accepted.
          int separator = themeVariable.indexOf('=');
          if (separator < 0) {
            separator = themeVariable.indexOf(':');
          }
          if (separator <= 0) {
            continue;
          }
          String variableName = themeVariable.substring(0, separator).trim();
          String variableValue = themeVariable.substring(separator + 1).trim();
          if (variableValue.startsWith(variableName + ":")) {
            // Override written for the pre-7.3.0.30 declaration (exo.branding.theme.X=X:value): the value is what follows
            variableValue = variableValue.substring(variableName.length() + 1).trim();
            if (!legacyOverrideFormReported) {
              legacyOverrideFormReported = true;
              LOG.info("Theme variable '{}' is overridden with the legacy '<name>:<value>' form; the plain value is now expected (exo.branding.theme.{}=<value>)",
                       variableName,
                       variableName);
            }
          }
          if (!isValidApplicationThemeStyleValue(variableName, variableValue)) {
            // Configuration follows the same grammar as the UI: a malformed value is not emitted in the platform stylesheet
            LOG.warn("Invalid configured value '{}' for theme variable '{}', ignored (the built-in default applies)",
                     variableValue,
                     variableName);
            variableValue = "";
          }
          this.themeVariables.put(variableName, variableValue);
        }
      }
    }
  }

  private Map<String, String> loadLanguages() {
    Locale defaultLocale = getDefaultLocale();
    this.supportedLanguages = localeConfigService.getLocalConfigs()
        == null ?
                Collections.singletonMap(defaultLocale.getLanguage(),
                                         getLocaleDisplayName(defaultLocale,
                                                              defaultLocale)) :
                localeConfigService.getLocalConfigs()
                                   .stream()
                                   .filter(localeConfig -> !StringUtils.equals(localeConfig.getLocaleName(),
                                                                               "ma"))
                                   .collect(Collectors.toMap(LocaleConfig::getLocaleName,
                                                             localeConfig -> getLocaleDisplayName(defaultLocale,
                                                                                                  localeConfig.getLocale())));
    return this.supportedLanguages;
  }

  private Locale getDefaultLocale() {
    return localeConfigService.getDefaultLocaleConfig() == null ? Locale.getDefault() :
                                                                localeConfigService.getDefaultLocaleConfig()
                                                                                   .getLocale();
  }

  private String getDefaultLocaleDirection() {
    LocaleConfig defaultLocaleConfig = localeConfigService.getDefaultLocaleConfig();
    return defaultLocaleConfig == null
           || defaultLocaleConfig.getOrientation() == null
           || !defaultLocaleConfig.getOrientation()
                                  .isRT() ? "ltr" : "rtl";
  }

  private String getLocaleDisplayName(Locale defaultLocale, Locale locale) {
    return defaultLocale.equals(locale) ? defaultLocale.getDisplayName(defaultLocale) :
                                        locale.getDisplayName(defaultLocale) + " / " + locale.getDisplayName(locale);
  }

  private void updateCompanyLink(String companyLink, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_COMPANY_LINK_SETTING_KEY, companyLink, updateLastUpdatedTime);
  }

  private void updateSiteName(String siteName, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_SITE_NAME_SETTING_KEY, siteName, updateLastUpdatedTime);
  }

  private void updateLoginBackgroundTextColor(String textColor, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_LOGIN_TEXT_COLOR_KEY, textColor, updateLastUpdatedTime);
  }

  private void updateLoginBackgroundAltText(String altText, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_LOGIN_ALT_TEXT_KEY, altText, updateLastUpdatedTime);
  }

  private void updatePageBackgroundColor(String color, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_COLOR_KEY, color, updateLastUpdatedTime);
  }

  private void updatePageBackgroundSize(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_SIZE_KEY, value, updateLastUpdatedTime);
  }

  private void updatePageBackgroundPosition(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_POSITION_KEY, value, updateLastUpdatedTime);
  }

  private void updatePageBackgroundRepeat(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_REPEAT_KEY, value, updateLastUpdatedTime);
  }

  private void updatePageBackgroundAttachment(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_ATTACHMENT_KEY, value, updateLastUpdatedTime);
  }

  private void updatePageBackgroundEffect(String effect, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_EFFECT_KEY, effect, updateLastUpdatedTime);
  }

  private void updatePageWidth(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_WIDTH_KEY, value, updateLastUpdatedTime);
  }

  private void updateCustomCss(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_CUSTOM_CSS, value, updateLastUpdatedTime);
  }

  private void updateCompanyName(String companyName, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_COMPANY_NAME_SETTING_KEY, companyName, updateLastUpdatedTime);
  }

  private void updateThemeStyle(Map<String, String> themeStyles, boolean updateLastUpdatedTime) {
    if (themeVariables == null || themeVariables.isEmpty()) {
      return;
    }

    Set<String> variables = themeVariables.keySet();
    for (String themeVariable : variables) {
      if (themeStyles != null && themeStyles.get(themeVariable) != null) {
        String themeStyle = themeStyles.get(themeVariable);
        settingService.set(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable, SettingValue.create(themeStyle));
      } else {
        settingService.remove(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable);
      }
    }

    // Refresh Theme
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateLoginTitle(Map<String, String> titles) {
    for (String language : supportedLanguages.keySet()) {
      if (titles.containsKey(language)) {
        String value = titles.get(language);
        settingService.set(BRANDING_CONTEXT,
                           BRANDING_SCOPE,
                           BRANDING_TITLE_SETTING_KEY + language,
                           SettingValue.create(value == null ? "" : value));
      } else {
        settingService.remove(BRANDING_CONTEXT,
                              BRANDING_SCOPE,
                              BRANDING_TITLE_SETTING_KEY + language);
      }
    }
    this.loginTitle = null;
  }

  private void updateLoginSubtitle(Map<String, String> subtitles) {
    for (String language : supportedLanguages.keySet()) {
      if (subtitles.containsKey(language)) {
        String value = subtitles.get(language);
        settingService.set(BRANDING_CONTEXT,
                           BRANDING_SCOPE,
                           BRANDING_SUBTITLE_SETTING_KEY + language,
                           SettingValue.create(value == null ? "" : value));
      } else {
        settingService.remove(BRANDING_CONTEXT,
                              BRANDING_SCOPE,
                              BRANDING_SUBTITLE_SETTING_KEY + language);
      }
    }
    this.loginSubtitle = null;
  }

  private void updateLogo(Logo logo, boolean updateLastUpdatedTime) { // NOSONAR
    updateBrandingFile(logo, LOGO_NAME, this.getLogoId(), BRANDING_LOGO_ID_SETTING_KEY);
    this.logo = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateFavicon(Favicon favicon, boolean updateLastUpdatedTime) {
    updateBrandingFile(favicon, FAVICON_NAME, this.getFaviconId(), BRANDING_FAVICON_ID_SETTING_KEY);
    this.favicon = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateLoginBackground(Background loginBackground, boolean updateLastUpdatedTime) {
    updateBrandingFile(loginBackground, LOGIN_BACKGROUND_NAME, this.getLoginBackgroundId(), BRANDING_LOGIN_BG_ID_SETTING_KEY);
    this.loginBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateTopBarBackground(Background topBarBackground, boolean updateLastUpdatedTime) {
    updateBrandingFile(topBarBackground,
                       TOP_BAR_BACKGROUND_NAME,
                       this.getTopBarBackgroundId(),
                       BRANDING_TOP_BAR_BG_ID_SETTING_KEY);
    this.topBarBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateSideBarBackground(Background sideBarBackground, boolean updateLastUpdatedTime) {
    updateBrandingFile(sideBarBackground,
                       SIDEBAR_BACKGROUND_NAME,
                       this.getSideBarBackgroundId(),
                       BRANDING_SIDEBAR_BG_ID_SETTING_KEY);
    this.sideBarBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateAppTextTitleBackground(Background background, boolean updateLastUpdatedTime) {
    updateBrandingFile(background, APP_TITLE_BACKGROUND_NAME, this.getAppTextTitleBackgroundId(), BRANDING_APP_TITLE_BG_ID_KEY);
    this.appTextTitleBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateAppTextHeaderBackground(Background background, boolean updateLastUpdatedTime) {
    updateBrandingFile(background, APP_HEADER_BACKGROUND_NAME, this.getAppTextHeaderBackgroundId(), BRANDING_APP_HEADER_BG_ID_KEY);
    this.appTextHeaderBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateAppBackground(Background appBackground, boolean updateLastUpdatedTime) {
    updateBrandingFile(appBackground, APP_BACKGROUND_NAME, this.getAppBackgroundId(), BRANDING_APP_BG_ID_SETTING_KEY);
    this.appBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateDrawerBackground(Background drawerBackground, boolean updateLastUpdatedTime) {
    updateBrandingFile(drawerBackground, DRAWER_BACKGROUND_NAME, this.getDrawerBackgroundId(), BRANDING_DRAWER_BG_ID_SETTING_KEY);
    this.drawerBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updatePageBackground(Background background, boolean updateLastUpdatedTime) {
    updateBrandingFile(background, PAGE_BACKGROUND_NAME, this.getPageBackgroundId(), BRANDING_PAGE_BG_ID_SETTING_KEY);
    this.pageBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateBrandingFile(BrandingFile brandingFile, String fileName, Long fileId, String settingKey) {
    String uploadId = brandingFile == null ? null : brandingFile.getUploadId();
    updateBrandingFile(uploadId,
                       fileName,
                       fileId,
                       settingKey);
    if (fileId != null
        && StringUtils.isNotBlank(uploadId)
        && !StringUtils.equals(BRANDING_RESET_ATTACHMENT_ID, uploadId)) {
      // Cleanup old fileId
      fileService.deleteFile(fileId);
    }
  }

  private void updateBrandingFile(String uploadId, String fileName, Long fileId, String settingKey) {
    try {
      if (StringUtils.equals(BRANDING_RESET_ATTACHMENT_ID, uploadId)) {
        removeBrandingFile(fileId, settingKey);
      } else if (StringUtils.isNotBlank(uploadId)) {
        updateBrandingFileByUploadId(uploadId, fileName, settingKey);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error while updating login background", e);
    }
  }

  private void removeBrandingFile(Long fileId, String settingKey) {
    if (fileId != null && fileId > 0) {
      fileService.deleteFile(fileId);
      settingService.remove(Context.GLOBAL,
                            Scope.GLOBAL,
                            settingKey);
    }
  }

  @SneakyThrows
  private void updateBrandingFileByUploadId(String uploadId,
                                            String fileName,
                                            String settingKey) {
    InputStream inputStream = getUploadDataAsStream(uploadId);
    if (inputStream == null) {
      throw new IllegalArgumentException("Cannot update " + fileName +
          ", the object must contain the image data or an upload id");
    }
    updateBrandingFileByInputStream(inputStream, fileName, settingKey);
  }

  @SneakyThrows
  private FileItem updateBrandingFileByInputStream(InputStream inputStream, String fileName, String settingKey) {
    int size = inputStream.available();
    FileItem fileItem = new FileItem(0l,
                                     fileName,
                                     "image/png",
                                     FILE_API_NAME_SPACE,
                                     size,
                                     new Date(),
                                     userAcl.getSuperUser(),
                                     false,
                                     inputStream);
    fileItem = fileService.writeFile(fileItem);
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL,
                       settingKey,
                       SettingValue.create(String.valueOf(fileItem.getFileInfo().getId())));
    return fileItem;
  }

  private String computeThemeCSS() {// NOSONAR
    if (themeVariables != null && !themeVariables.isEmpty() && StringUtils.isNotBlank(getLessThemeContent())) {
      // Effective values: stored, else configured default, blank = not set (the template default stays);
      // the Topbar gradient is neutralized like on the read path
      Map<String, String> effectiveValues = new HashMap<>(getThemeStyle());
      // A platform page margin also neutralises the first and last section's own 10px padding, as the site and
      // page levels do (--allPagesNoMarginTop/Bottom: 0px next to --allPagesMarginTop/Bottom); both stay 'initial' otherwise
      if (StringUtils.isNotBlank(effectiveValues.get("pageMarginTop"))) {
        effectiveValues.put("pageNoMarginTop", "0px");
      }
      if (StringUtils.isNotBlank(effectiveValues.get("pageMarginBottom"))) {
        effectiveValues.put("pageNoMarginBottom", "0px");
      }
      try {
        this.themeCSSContent = compileThemeCSS(effectiveValues);
        this.lastCompiledThemeCSS = this.themeCSSContent;
      } catch (Less4jException e) {
        // Fail-safe: the stylesheet every user loads is never removed by one value the compiler refuses
        LOG.warn("Error compiling less file content, the last compiled stylesheet is served", e);
        if (this.lastCompiledThemeCSS == null) {
          // nothing compiled yet (a stored or configured value broke the very first compile): the pristine
          // template is the fallback, and it is remembered so that the failing compile does not run on every request
          this.lastCompiledThemeCSS = compilePristineTemplate();
        }
        this.themeCSSContent = this.lastCompiledThemeCSS;
      }
    }
    if (StringUtils.isNotBlank(getCustomCssContent())
        && getFeatureService() != null
        && getFeatureService().isActiveFeature(BRANDING_CUSTOM_STYLE_FEATURE)) {
      this.themeCSSContent = (this.themeCSSContent == null ? "" : this.themeCSSContent) + "\n" + this.customCss;
    }
    return this.themeCSSContent;
  }

  private String compilePristineTemplate() {
    try {
      return compileThemeCSS(Collections.emptyMap());
    } catch (Less4jException e) {
      LOG.error("The shipped branding Less template does not compile, no branding stylesheet is served", e);
      return "";
    }
  }

  /**
   * Substitutes the given values into the shipped Less template and compiles
   * it with the real compiler. Free-form values (keywords, shadows, gradients,
   * background images) are written as Less escapes (~"value") so that a value
   * the grammar accepts but Less would parse (a lone '-', a function name)
   * travels verbatim to the CSS instead of breaking the compilation; colours
   * and sizes stay plain since the template derives other variables from them.
   *
   * @param values effective theme values by variable name
   * @return the compiled CSS
   * @throws Less4jException when the template does not compile with these values
   */
  private String compileThemeCSS(Map<String, String> values) throws Less4jException {
    String content = getLessThemeContent();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      String themeVariable = entry.getKey();
      String value = entry.getValue();
      if (StringUtils.isNotBlank(value)) {
        String lessValue = isFreeFormThemeValue(themeVariable) ? toLessEscape(value) : value;
        // The whole template value is replaced, whatever its grammar (keywords with '-', variable references...)
        content = content.replaceAll("@" + Pattern.quote(themeVariable) + ":[^;\\r\\n]*;?\\r?\\n",
                                     "@" + themeVariable + ": " + Matcher.quoteReplacement(lessValue) + ";\n");
      }
    }
    LessCompiler compiler = new ThreadUnsafeLessCompiler();
    Configuration configuration = new Configuration();
    configuration.getSourceMapConfiguration().setLinkSourceMap(false);
    return compiler.compile(content, configuration).getCss();
  }

  /**
   * @param key theme variable name
   * @return true for values emitted verbatim in the CSS (never used in Less
   *         arithmetic or colour functions): background images and gradients
   *         of every area, the application shadow, and the CSS keywords
   */
  static boolean isFreeFormThemeValue(String key) {
    return key.endsWith("BackgroundImage")
           || "appBoxShadow".equals(key)
           || key.endsWith("FontWeight")
           || key.endsWith("FontStyle")
           || key.endsWith("BackgroundPosition")
           || key.endsWith("BackgroundSize")
           || key.endsWith("BackgroundRepeat")
           || key.endsWith("BackgroundAttachment");
  }

  /**
   * An unclosed function keeps the browser's parser open until the end of the
   * stylesheet and drops every later declaration and rule for every user; the
   * grammar allows parentheses, so their balance is checked separately
   * (Architects Lead decision)
   */
  static boolean hasBalancedParentheses(String value) {
    int depth = 0;
    for (char c : value.toCharArray()) {
      if (c == '(') {
        depth++;
      } else if (c == ')' && --depth < 0) {
        return false;
      }
    }
    return depth == 0;
  }

  static String toLessEscape(String value) {
    // no grammar allows a quote; one is stripped anyway so that the escape cannot be closed from inside the value
    return "~\"" + value.replaceAll("[\"\\\\\\r\\n]", "") + "\"";
  }

  private String getLessThemeContent() {
    if (lessThemeContent == null && StringUtils.isNotBlank(lessFilePath)) {
      try {
        InputStream inputStream = configurationManager.getInputStream(lessFilePath);
        lessThemeContent = IOUtil.getStreamContentAsString(inputStream);
      } catch (Exception e) {
        LOG.warn("Error retrieving less file content", e);
      }
    }
    return lessThemeContent;
  }

  private String getCustomCssContent() {
    if (this.customCss == null) {
      this.customCss = getCustomCss();
      if (this.customCss == null) {
        this.customCss = "";
      }
    }
    return this.customCss;
  }

  private InputStream getUploadDataAsStream(String uploadId) throws FileNotFoundException {
    UploadResource uploadResource = uploadService.getUploadResource(uploadId);
    if (uploadResource == null) {
      return null;
    } else {
      try {// NOSONAR
        return new FileInputStream(new File(uploadResource.getStoreLocation()));
      } finally {
        uploadService.removeUploadResource(uploadId);
      }
    }
  }

  private <T extends BrandingFile> T retrieveStoredBrandingFile(long imageId, T brandingFile) throws FileStorageException {
    FileItem fileItem = fileService.getFile(imageId);
    if (fileItem != null) {
      brandingFile.setData(fileItem.getAsByte());
      brandingFile.setSize(fileItem.getFileInfo().getSize());
      brandingFile.setUpdatedDate(fileItem.getFileInfo().getUpdatedDate().getTime());
      brandingFile.setFileId(imageId);
    }
    return brandingFile;
  }

  private <T extends BrandingFile> T retrieveDefaultBrandingFile(String imagePath,
                                                                 T brandingFile,
                                                                 String fileName,
                                                                 String settingKey) throws IOException {
    if (StringUtils.isNotBlank(imagePath)) {
      byte[] bytes = null;
      long lastModified = DEFAULT_LAST_MODIFED;
      File file = new File(imagePath);
      if (file.exists()) {
        bytes = Files.readAllBytes(file.toPath());
        lastModified = file.lastModified();
      } else {
        try (InputStream is = container.getPortalContext().getResourceAsStream(imagePath)) {
          if (is != null) {
            bytes = IOUtil.getStreamContentAsBytes(is);
          }
        }
      }
      if (bytes != null) {
        FileItem fileItem = updateBrandingFileByInputStream(new ByteArrayInputStream(bytes), fileName, settingKey);
        brandingFile.setFileId(fileItem.getFileInfo().getId());
        brandingFile.setData(bytes);
        brandingFile.setSize(bytes.length);
        brandingFile.setUpdatedDate(lastModified);
      }
    }
    return brandingFile;
  }

  private String getPropertyValue(String key) {
    return getPropertyValue(key, null);
  }

  private Long getPropertyValueLong(String key) {
    String value = getPropertyValue(key, null);
    return StringUtils.isBlank(value) ? null : Long.parseLong(value);
  }

  private String getPropertyValue(String key, String defaultValue) {
    SettingValue<?> value = settingService.get(Context.GLOBAL,
                                               Scope.GLOBAL,
                                               key);
    if (value != null
        && value.getValue() != null
        && StringUtils.isNotBlank(value.getValue().toString())) {
      return value.getValue().toString();
    } else {
      return defaultValue;
    }
  }

  private void updatePropertyValue(String key, String value, boolean updateLastUpdatedTime) {
    if (StringUtils.isBlank(value)) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, key);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, key, SettingValue.create(value));
    }
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void validateCSSInputs(Branding branding) { // NOSONAR
    Arrays.asList(branding.getCustomCss(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundPosition(),
                  branding.getPageBackgroundRepeat(),
                  branding.getPageBackgroundSize(),
                  branding.getPageBackgroundAttachment(),
                  branding.getLoginBackgroundTextColor(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundColor())
          .forEach(this::validateCSSStyleValue);
    branding.getThemeStyle().values().forEach(this::validateCSSStyleValue);
    branding.getThemeStyle().forEach(this::validateApplicationThemeStyleValue);
    validateThemeStyleCompiles(branding.getThemeStyle());
  }

  /**
   * The grammar bounds the characters of a value, not its Less validity: the
   * whole stylesheet is trial-compiled with the submitted values before
   * anything is stored, so that a value the compiler refuses is answered 400
   * and never removes the platform stylesheet (eXIP 7.3.0.30, §2 Security)
   */
  private void validateThemeStyleCompiles(Map<String, String> submittedThemeStyle) {
    if (submittedThemeStyle == null || submittedThemeStyle.isEmpty() || StringUtils.isBlank(getLessThemeContent())) {
      return;
    }
    Map<String, String> effectiveValues = new HashMap<>(getThemeStyle());
    submittedThemeStyle.forEach((key, value) -> {
      if (value != null) {
        effectiveValues.put(key, value);
      }
    });
    // same transformation as the read path, so the trial compiles what will really be compiled
    neutralizeTopBarGradient(effectiveValues);
    try {
      compileThemeCSS(effectiveValues);
    } catch (Less4jException e) {
      LOG.debug("Submitted theme style does not compile, refused", e);
      throw new IllegalArgumentException("branding.theme.stylesheetCompilationError");
    }
  }

  /**
   * Strict grammar for the theme variables introduced with the platform-wide
   * application styling (app* keys, topBarSticky, topBarBackgroundScrollColor):
   * they are written verbatim in the platform stylesheet served to every user
   */
  private void validateApplicationThemeStyleValue(String key, String value) {
    if (!isValidApplicationThemeStyleValue(key, value)) {
      LOG.debug("Invalid css value input '{}' for theme variable '{}', refused", value, key);
      throw new IllegalArgumentException("branding.theme.invalidValue:" + key);
    }
  }

  /**
   * Grammar of a theme value, chosen by the key's suffix whatever its prefix
   * (app*, page*, topBar*, sideBar*, drawer*, the palette): every value is
   * written in the stylesheet served to every user. A key with no known
   * suffix keeps the historical check only.
   */
  private boolean isValidApplicationThemeStyleValue(String key, String value) {
    if (StringUtils.isBlank(value)) {
      return true;
    }
    if (TOP_BAR_STICKY_THEME_STYLE_KEY.equals(key)) {
      return "true".equals(value) || "false".equals(value);
    } else if (key.contains("Color")) {
      return THEME_COLOR_PATTERN.matcher(value).matches();
    } else if (key.endsWith("BackgroundRadius")) {
      // text-background radius of the shared styling input: a border-radius shorthand, one to four corner sizes
      return THEME_RADIUS_PATTERN.matcher(value).matches();
    } else if (key.contains("Margin") || key.endsWith("BorderSize") || key.endsWith("FontSize") || key.contains("BackgroundPadding")
               || key.contains("BorderRadius") || "borderRadius".equals(key)) {
      return THEME_SIZE_PATTERN.matcher(value).matches();
    } else if (key.endsWith("BackgroundImage")) {
      return THEME_BG_EFFECT_PATTERN.matcher(value).matches() && hasBalancedParentheses(value);
    } else if (key.endsWith("BoxShadow")) {
      return THEME_BOX_SHADOW_PATTERN.matcher(value).matches() && hasBalancedParentheses(value);
    } else if (key.endsWith("FontWeight") || key.endsWith("FontStyle") || key.endsWith("BackgroundPosition")
               || key.endsWith("BackgroundSize") || key.endsWith("BackgroundRepeat") || key.endsWith("BackgroundAttachment")) {
      return THEME_KEYWORD_PATTERN.matcher(value).matches();
    }
    return true;
  }

  private void validateCSSStyleValue(String value) {
    if (StringUtils.isNotBlank(value)
        && (value.contains("javascript") || value.contains("eval"))) {
      LOG.debug("Forbidden css value input '{}', refused", value);
      throw new IllegalArgumentException("branding.css.forbiddenValue");
    }
  }

  private void triggerBrandingUpdated(boolean updateLastUpdatedTime, boolean triggerEvent) {
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
    if (triggerEvent) {
      listenerService.broadcast(BRANDING_UPDATED_EVENT, null, getBrandingInformation(false));
    }
  }

  /**
   * The server is the only source of the image URL of a *BackgroundImage
   * value: url(...) layers a client sends back (the stored value round-tripped
   * by the Branding UI, or an external URL) are removed before validation, so
   * that a save is idempotent and a stored value doubled by an older version
   * heals on its next save.
   */
  static void stripSubmittedImageLayers(Map<String, String> themeStyles) {
    if (themeStyles == null) {
      return;
    }
    themeStyles.replaceAll((key, value) -> key.endsWith("BackgroundImage") && StringUtils.isNotBlank(value)
                                                                                   && value.contains("url(") ?
                                                                                                                stripImageLayers(value) :
                                                                                                                value);
  }

  static String stripImageLayers(String value) {
    String effect = IMAGE_LAYER_PATTERN.matcher(value).replaceAll("").trim();
    return StringUtils.isBlank(effect) ? "none" : effect;
  }

  private void processThemeBackgroundImages(Map<String, String> themeStyles) {
    processBackgroundImage(themeStyles, TOP_BAR_BG_IMAGE_THEME_STYLE_KEY, getTopBarBackgroundPath());
    processBackgroundImage(themeStyles, SIDEBAR_BG_IMAGE_THEME_STYLE_KEY, getSideBarBackgroundPath());
    processBackgroundImage(themeStyles, DRAWER_BG_IMAGE_THEME_STYLE_KEY, getDrawerBackgroundPath());
    processBackgroundImage(themeStyles, APP_BG_IMAGE_THEME_STYLE_KEY, getAppBackgroundPath());
    processBackgroundImage(themeStyles, APP_TITLE_BG_IMAGE_THEME_KEY, getAppTextTitleBackgroundPath());
    processBackgroundImage(themeStyles, APP_HEADER_BG_IMAGE_THEME_KEY, getAppTextHeaderBackgroundPath());
  }

  private void processBackgroundImage(Map<String, String> themeStyles, String styleKey, String imagePath) {
    if (StringUtils.isNotBlank(imagePath)) {
      String themeStyleValue = themeStyles.get(styleKey);

      StringBuilder backgroundImageValue = new StringBuilder("url(").append(imagePath).append(")");

      if (StringUtils.isNotBlank(themeStyleValue) && !"none".equals(themeStyleValue)) {
        backgroundImageValue.append(", ").append(themeStyleValue);
      }

      themeStyles.put(styleKey, backgroundImageValue.toString());
    }
  }

  private ExoFeatureService getFeatureService() {
    if (featureService == null) {
      featureService = container.getComponentInstanceOfType(ExoFeatureService.class);
    }
    return featureService;
  }

}
