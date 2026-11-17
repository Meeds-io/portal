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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

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
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
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
import org.exoplatform.portal.branding.model.ThemeStylesheet;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.portal.theme.ThemeReadiness;
import io.meeds.portal.theme.constant.ThemeVariables;
import io.meeds.portal.theme.model.BrandingState;
import io.meeds.portal.theme.model.ImageBytes;
import io.meeds.portal.theme.model.MailPalettes;
import io.meeds.portal.theme.model.ResolvedThemeInUse;
import io.meeds.portal.theme.model.ResolvedTheme;
import io.meeds.portal.theme.model.Theme;
import io.meeds.portal.theme.util.ThemeColorCalculator;
import io.meeds.portal.theme.service.ThemeService;
import io.meeds.portal.theme.util.ThemeValueResolver;
import io.meeds.portal.theme.util.BrandingImageValidator;
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

  public static final String   BRANDING_DARK_LOGO_BASE_PATH       = "/portal/rest/v1/platform/branding/darkLogo?v=";         // NOSONAR

  public static final String   BRANDING_DARK_FAVICON_BASE_PATH    = "/portal/rest/v1/platform/branding/darkFavicon?v=";      // NOSONAR

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

  public static final String   CUSTOM_CSS_FORBIDDEN_MESSAGE       = "branding.customCss.forbiddenValue";

  /** The longest a request waits for another one compiling the stylesheet, in milliseconds. */
  public static final long     STYLESHEET_WAIT_MILLIS             = 2000;

  /** How long a configured login background found missing is not looked up again, in milliseconds. */
  public static final long     MISSING_FILE_RETRY_MILLIS          = 60000;

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

  private static final Pattern UNITLESS_SIZE_PATTERN              = Pattern.compile("^-?\\d{1,4}$");

  private static final Pattern UNITLESS_SIZE_TOKEN_PATTERN        = Pattern.compile("(?<![\\w.-])(-?\\d{1,4})(?![\\w.%])");

  private static final Pattern THEME_RADIUS_PATTERN               = Pattern.compile("^(initial|(-?\\d{1,4}(px)?)( -?\\d{1,4}(px)?){0,3})$");

  private static final Pattern THEME_KEYWORD_PATTERN              = Pattern.compile("^[a-zA-Z -]{1,30}$");

  private static final Pattern THEME_BOX_SHADOW_PATTERN           = Pattern.compile("^(initial|none|[0-9a-zA-Z#\\s(),.-]{1,200})$");

  private static final String  GRADIENT_GRAMMAR                   = "(linear|radial|conic)-gradient\\([#0-9a-zA-Z(),.%\\s-]{1,300}\\)";

  /**
   * A submitted background image is the effect only (none or a gradient): the
   * image URL is added by the server (processBackgroundImage) and any url()
   * layer a client sends is stripped first
   */
  private static final Pattern THEME_BG_EFFECT_PATTERN            = Pattern.compile("^(initial|none|" + GRADIENT_GRAMMAR + ")$");

  private static final Pattern IMAGE_LAYER_PATTERN                = Pattern.compile("url\\([^)]*(\\)|$)\\s*,?\\s*");

  private static final Pattern ROOT_RULE                          = Pattern.compile(":root(?=\\s*\\{)");

  /** A template variable whose value is a literal: a hex color, a size, a keyword. */
  private static final Pattern TEMPLATE_LITERAL                   = Pattern.compile("(?m)^@([a-zA-Z0-9]+):\\s*(#[0-9a-fA-F]{3,8}|-?\\d+(?:px|%)?|[a-zA-Z][a-zA-Z -]*)\\s*;");

  private static final String  IMAGE_FILE_SUFFIX                  = "BackgroundImageFile";

  public static final String   THEME_IMAGE_BASE_PATH              = "/portal/rest/v1/platform/branding/themes/";       // NOSONAR

  private static final String  DARK_SELECTOR                      = ":root[data-display-mode=\"dark\"]";

  private static final String  AUTO_SELECTOR                      = ":root[data-display-mode=\"auto\"]";

  private static final String  LIGHT_SCOPE_SELECTOR               = "[data-display-mode=\"light\"]:not(:root)";

  public static final String   BRANDING_PAGE_BG_COLOR_KEY         = "page.backgroundColor";

  public static final String   BRANDING_PAGE_BG_REPEAT_KEY        = "page.backgroundRepeat";

  public static final String   BRANDING_PAGE_BG_EFFECT_KEY        = "page.backgroundEffect";

  public static final String   BRANDING_PAGE_WIDTH_KEY            = "page.width";

  public static final String   BRANDING_PAGE_BG_POSITION_KEY      = "page.backgroundPosition";

  public static final String   BRANDING_PAGE_BG_SIZE_KEY          = "page.backgroundSize";

  public static final String   BRANDING_PAGE_BG_ATTACHMENT_KEY    = "page.backgroundAttachment";

  public static final String   BRANDING_CUSTOM_STYLE_FEATURE      = "customStylesheet";

  public static final String   BRANDING_LAST_UPDATED_TIME_KEY     = "branding.lastUpdatedTime";

  public static final String   THEME_FONT_FAMILY_KEY              = "fontFamily";

  public static final String   DEFAULT_FONT_FAMILY                = "Arimo";

  /**
   * Built-in default page background color of the Branding and Theme page
   * (social SiteBranding.vue and PageStylingDrawer.vue), stored as is or with
   * an opaque alpha when no default is configured
   * (exo.branding.page.backgroundColor); a configured default other than this
   * color is not matched
   */
  public static final String   DEFAULT_PAGE_BACKGROUND_COLOR      = "#F0F0F0";

  public static final List<String> SUPPORTED_FONT_FAMILIES         = List.of(DEFAULT_FONT_FAMILY, "Inter", "Roboto", "Open Sans", "Public Sans");

  public static final String   FONT_FAMILY_UNSUPPORTED_MESSAGE    = "branding.fontFamily.unsupported";

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

  /** Until when a configured login background found missing is not looked up again. */
  private volatile long        configuredLoginBgRetryAt           = 0;

  private String               lessFilePath                       = null;

  private Map<String, String>  themeVariables                     = null;

  private String               defaultLoginTitle                  = null;

  private String               defaultLoginSubtitle               = null;

  private Map<String, String>  loginTitle                         = null;

  private Map<String, String>  loginSubtitle                      = null;

  private Map<String, String>  supportedLanguages                 = null;

  private final AtomicReference<ThemeStylesheet> themeStylesheet  = new AtomicReference<>();

  /** The last stylesheet compiled, kept when a write clears the current one: served while another request compiles. */
  private final AtomicReference<ThemeStylesheet> lastCompiledStylesheet = new AtomicReference<>();

  /** The pristine template, compiled once: served while the first compile runs. */
  private final AtomicReference<ThemeStylesheet> pristineStylesheet = new AtomicReference<>();

  /** Held by the one request compiling the stylesheet; the others wait at most {@link #STYLESHEET_WAIT_MILLIS}. */
  private final ReentrantLock  themeCSSLock                       = new ReentrantLock();

  private String               lessThemeContent                   = null;

  private String               lastCompiledThemeCSS               = null;

  private String               lastCompiledLightCSS               = null;

  private String               lastCompiledDarkCSS                = null;

  private Map<String, String>  templateDefaults                   = null;

  private ThemeService         themeService                       = null;

  private final AtomicReference<MailPalettes> mailPalettes        = new AtomicReference<>();

  /** The theme in use resolved at one stamp: every page getter reads it, none resolves it again. */
  private final AtomicReference<ResolvedThemeInUse> resolvedThemeInUse = new AtomicReference<>();

  /** The parsed list of each light-only property, keyed by the raw string it was parsed from. */
  private final Map<String, Map.Entry<String, List<String>>> csvProperties = new ConcurrentHashMap<>();

  private Long                 templateHash                       = null;

  private boolean              legacyOverrideFormReported         = false;

  private Logo                 logo                               = null;

  private Favicon              favicon                            = null;

  private Background           loginBackground                    = null;

  private Background           pageBackground                     = null;

  private Background           topBarBackground                   = null;

  private Background           sideBarBackground                  = null;

  private Background           drawerBackground                   = null;

  private Background           appBackground                      = null;

  private Background           appTextTitleBackground             = null;

  private Background           appTextHeaderBackground            = null;

  /**
   * The stored update time the memoised values above were read at: a write
   * on any node of a cluster changes the stored time, and each node reads
   * the values again
   */
  private volatile long        memoisedAt                         = Long.MIN_VALUE;

  /**
   * Delay, in milliseconds, after which the values memoised under a new
   * stored update time are dropped once more. On another node of a cluster,
   * the update time and each value are invalidated apart, in no set order: a
   * value read before its own invalidation arrives would stay memoised under
   * the new update time until the next write. The compiled theme is dropped
   * too, on this node only: a browser that fetched it in between keeps it
   * until the next branding write, since its URL and its ETag carry the update
   * time, which the second drop does not change.
   */
  static final long            SECOND_DROP_DELAY                  = 10000L;

  /**
   * When the values memoised under the current update time are dropped once
   * more, or 0 once done
   */
  private final AtomicLong     secondDropAt                       = new AtomicLong();

  LongSupplier                 clock                              = System::currentTimeMillis;

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
    return getThemeStylesheet().css();
  }

  @Override
  public ThemeStylesheet getThemeStylesheet() {
    long lastUpdatedTime = getLastUpdatedTime();
    ThemeStylesheet stylesheet = themeStylesheet.get();
    if (isCurrent(stylesheet, lastUpdatedTime)) {
      return stylesheet;
    }
    boolean locked = false;
    try {
      locked = themeCSSLock.tryLock(STYLESHEET_WAIT_MILLIS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (!locked) {
      // another request is compiling: the last compiled stylesheet meanwhile, the pristine template before the first
      LOG.debug("The branding stylesheet is being compiled, the last compiled one is served");
      ThemeStylesheet last = lastCompiledStylesheet.get();
      return last == null ? getPristineStylesheet() : last;
    }
    try {
      lastUpdatedTime = getLastUpdatedTime();
      stylesheet = themeStylesheet.get();
      if (isCurrent(stylesheet, lastUpdatedTime)) {
        return stylesheet;
      }
      stylesheet = new ThemeStylesheet(lastUpdatedTime, compileThemeCSS());
      themeStylesheet.set(stylesheet);
      lastCompiledStylesheet.set(stylesheet);
      return stylesheet;
    } finally {
      themeCSSLock.unlock();
    }
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
    ThemeService themeServiceInUse = retrieveBinaries ? null : getThemeService();
    if (themeServiceInUse == null) {
      branding.setLogo(getLogo());
      branding.setFavicon(getFavicon());
      branding.setLoginBackground(getLoginBackground());
    } else {
      // no byte is read for a caller that drops them: the file's reference and metadata only
      branding.setLogo(readThemeFileInfo(getLogoId(), new Logo()));
      branding.setFavicon(readThemeFileInfo(getFaviconId(), new Favicon()));
      Long loginBackgroundId = getLoginBackgroundId();
      branding.setLoginBackground(loginBackgroundId == null ? getLoginBackground() : readThemeFileInfo(loginBackgroundId, new Background()));
    }
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
    branding.setSupportedFontFamilies(SUPPORTED_FONT_FAMILIES);
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
    return storedTime + getTemplateHash() + getThemeStamp();
  }

  /**
   * @param service the theme services, ready
   * @return the theme in use resolved for both versions, computed once per
   *         stamp: callers only read it
   */
  private ResolvedTheme getResolvedThemeInUse(ThemeService service) {
    long stamp = getLastUpdatedTime();
    ResolvedThemeInUse cached = resolvedThemeInUse.get();
    if (cached == null || cached.stamp() != stamp) {
      cached = new ResolvedThemeInUse(stamp, service.resolve(service.getThemeInUse()));
      resolvedThemeInUse.set(cached);
    }
    return cached.resolved();
  }

  /**
   * Once the theme services are ready, every theme write increments the
   * state row's version, and a configuration changed at a restart changes its
   * hash: both move the stylesheet's stamp, its v= parameter and its ETag.
   */
  private long getThemeStamp() {
    ThemeService service = getThemeService();
    return service == null ? 0 : service.getState().getVersion() + service.getConfigurationHash();
  }

  /**
   * A new login background is checked before the form's first write: a
   * refused upload leaves nothing half-saved.
   */
  private void validateLoginBackgroundUpload(Background background) {
    String uploadId = background == null ? null : background.getUploadId();
    if (StringUtils.isBlank(uploadId) || BRANDING_RESET_ATTACHMENT_ID.equals(uploadId)) {
      return;
    }
    UploadResource resource = uploadService.getUploadResource(uploadId);
    if (resource == null) {
      // an expired or already consumed upload is refused here, before anything is written
      throw new IllegalArgumentException("branding.upload.notFound");
    }
    try {
      BrandingImageValidator.validateImage(Files.readAllBytes(new File(resource.getStoreLocation()).toPath()));
    } catch (IOException e) {
      throw new IllegalArgumentException("branding.upload.notFound", e);
    }
  }

  private static void publishLegacy(ThemeService service, Branding branding, boolean themeStyleOnly) {
    try {
      service.publishLegacy(branding, themeStyleOnly);
    } catch (ObjectNotFoundException e) {
      throw new IllegalStateException("The theme in use no longer exists", e);
    }
  }

  /** For tests: the theme service, still used only once the readiness flag is set. */
  void setThemeService(ThemeService themeService) {
    this.themeService = themeService;
  }

  private ThemeService getThemeService() {
    if (!ThemeReadiness.isReady()) {
      return null;
    }
    if (themeService != null) {
      return themeService;
    }
    try {
      return ExoContainerContext.getService(ThemeService.class);
    } catch (Exception e) {
      LOG.debug("The theme service is not reachable, the single-theme stylesheet is served", e);
      return null;
    }
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

  /** Portlets of Time Tracking, Lead Capture Management and Document Cleanup: not adapted to dark */
  static final String DEFAULT_LIGHT_ONLY_APPLICATIONS = "activityManagement,TimeTrackingPortlet,TimeSheetPortlet,leadsManagement,"
      + "mailTemplatesManagement,resourcesManagement,compaignsManagement,leadCaptureSettings,DocumentCleanupAdmin";

  /** Time Tracking's quick action, mounted outside its portlets */
  static final String DEFAULT_LIGHT_ONLY_VUE_APPS     = "Time Tracker Quick Action";

  @Override
  public List<String> getLightOnlyApplications() {
    return csvProperty("exo.branding.lightOnlyApplications", DEFAULT_LIGHT_ONLY_APPLICATIONS);
  }

  @Override
  public List<String> getLightOnlyVueApps() {
    return csvProperty("exo.branding.lightOnlyVueApps", DEFAULT_LIGHT_ONLY_VUE_APPS);
  }

  private List<String> csvProperty(String name, String defaultValue) {
    String raw = PropertyManager.getProperty(name);
    String value = raw == null ? defaultValue : raw;
    Map.Entry<String, List<String>> cached = csvProperties.get(name);
    if (cached == null || !cached.getKey().equals(value)) {
      cached = Map.entry(value,
                         Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList());
      csvProperties.put(name, cached);
    }
    return cached.getValue();
  }

  /**
   * The default comment bubble of each version (the skin's
   * --commentDefaultBackgroundColor and --commentDefaultDrawerBackgroundColor,
   * platform-ui variables.less): the light literal, and in dark the bubble
   * computed on the default dark application and drawer backgrounds.
   */
  private static final String COMMENT_DEFAULT_LIGHT = "#f2f5f8";

  static final String COMMENT_DEFAULTS_LIGHT = "--commentDefaultBackgroundColor: " + COMMENT_DEFAULT_LIGHT
      + "; --commentDefaultDrawerBackgroundColor: " + COMMENT_DEFAULT_LIGHT + "; ";

  static final String COMMENT_DEFAULTS_DARK = "--commentDefaultBackgroundColor: "
      + ThemeColorCalculator.commentBubble(ThemeColorCalculator.DARK_APP_BACKGROUND)
      + "; --commentDefaultDrawerBackgroundColor: "
      + ThemeColorCalculator.commentBubble(ThemeColorCalculator.DARK_DRAWER_BACKGROUND) + "; ";

  @Override
  @Deprecated(since = "7.3", forRemoval = false)
  public void updateBrandingInformation(Branding branding) {
    ThemeService service = getThemeService();
    if (service != null) {
      // the fields that stay in the settings, then the theme values through the theme writer
      // every check before the first write, then the theme values, then the settings. What can still fail after the
      // theme is published: the login background's file write, an upload that vanished meanwhile, and any setting
      // write or removal, each of which throws: the request then answers an error with the theme already published
      validateCSSStyleValue(branding.getCustomCss());
      validateCSSStyleValue(branding.getLoginBackgroundTextColor());
      validateLoginBackgroundUpload(branding.getLoginBackground());
      publishLegacy(service, branding, false);
      try {
        updateSiteName(branding.getSiteName(), false);
        updateCompanyLink(branding.getCompanyLink(), false);
        updateLoginBackground(branding.getLoginBackground(), false);
        updateLoginBackgroundTextColor(branding.getLoginBackgroundTextColor(), false);
        updateLoginBackgroundAltText(branding.getLoginBackgroundAltText(), false);
        updateCustomCss(branding.getCustomCss(), false);
        updateLoginTitle(branding.getLoginTitle());
        updateLoginSubtitle(branding.getLoginSubtitle());
      } finally {
        triggerBrandingUpdated(true, false);
      }
      return;
    }
    stripSubmittedImageLayers(branding.getThemeStyle());
    normalizeSubmittedSizeUnits(branding.getThemeStyle());
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
    ThemeService service = getThemeService();
    if (service != null) {
      return StringUtils.defaultIfBlank(service.getState().getCompanyName(), defaultCompanyName);
    }
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
    ThemeService service = getThemeService();
    return service == null ? getPropertyValue(BRANDING_PAGE_BG_COLOR_KEY, defaultPageBackgroundColor) : themePageValue(service, ThemeVariables.PAGE_BACKGROUND_COLOR);
  }

  @Override
  public boolean isDefaultPageBackgroundColor() {
    String pageBackgroundColor = StringUtils.trim(getPageBackgroundColor());
    return StringUtils.equalsIgnoreCase(pageBackgroundColor, DEFAULT_PAGE_BACKGROUND_COLOR)
           || StringUtils.equalsIgnoreCase(pageBackgroundColor, DEFAULT_PAGE_BACKGROUND_COLOR + "FF");
  }

  @Override
  public String getPageBackgroundRepeat() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValue(BRANDING_PAGE_BG_REPEAT_KEY) : themePageValue(service, ThemeVariables.PAGE_BACKGROUND_REPEAT);
  }

  @Override
  public String getPageBackgroundEffect() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValue(BRANDING_PAGE_BG_EFFECT_KEY, defaultPageBackgroundEffect) : themePageValue(service, ThemeVariables.PAGE_BACKGROUND_IMAGE);
  }

  /**
   * @return the theme in use's light value of a page key, null when blank or
   *         none: the page settings are part of the theme once it is ready
   */
  private String themePageValue(ThemeService service, String key) {
    String value = getResolvedThemeInUse(service).getLight().get(key);
    return StringUtils.isBlank(value) || "none".equals(value) || "initial".equals(value) ? null : value;
  }

  /**
   * @return the file the theme in use shows, in its light version, for an
   *         image key; null when it has none
   */
  private Long getThemeInUseImageFileId(ThemeService service, String imageKey) {
    Theme theme = service.getThemeInUse();
    try {
      return theme == null ? null : service.getThemeImageFileId(theme.getId(), imageKey, false);
    } catch (ObjectNotFoundException e) {
      return null;
    }
  }

  /**
   * @return the theme in use's light image of a key through the node's image
   *         cache, an empty background when it has none
   */
  private Background readThemeInUseBackground(ThemeService service, String imageKey) {
    Background background = readCachedThemeFile(service, getThemeInUseImageFileId(service, imageKey), new Background());
    return background == null ? new Background() : background;
  }

  @Override
  public boolean isPageStyleInStylesheet() {
    return isThemeStylesheet(getThemeService());
  }

  /**
   * @return true when the stylesheet is the theme's, in four blocks: the theme
   *         services are ready and a template and its variables are configured
   */
  private boolean isThemeStylesheet(ThemeService service) {
    return service != null && themeVariables != null && !themeVariables.isEmpty() && StringUtils.isNotBlank(getLessThemeContent());
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
    ThemeService service = getThemeService();
    return service == null ? getPropertyValue(BRANDING_PAGE_WIDTH_KEY, defaultPageWidth) : themePageValue(service, ThemeVariables.PAGE_WIDTH);
  }

  @Override
  public boolean isTopBarSticky() {
    ThemeService service = getThemeService();
    if (service != null) {
      // a shared setting: the theme in use carries it, nothing writes the former setting once ready
      return Boolean.parseBoolean(getResolvedThemeInUse(service).getLight().get(ThemeVariables.TOP_BAR_STICKY));
    }
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
    ThemeService service = getThemeService();
    return service == null ? getPropertyValue(BRANDING_PAGE_BG_POSITION_KEY) : themePageValue(service, ThemeVariables.PAGE_BACKGROUND_POSITION);
  }

  @Override
  public String getPageBackgroundSize() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValue(BRANDING_PAGE_BG_SIZE_KEY) : themePageValue(service, ThemeVariables.PAGE_BACKGROUND_SIZE);
  }

  @Override
  public String getPageBackgroundAttachment() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValue(BRANDING_PAGE_BG_ATTACHMENT_KEY) : themePageValue(service, ThemeVariables.PAGE_BACKGROUND_ATTACHMENT);
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
  @Deprecated(since = "7.3", forRemoval = false)
  public void updateCompanyName(String companyName) {
    ThemeService service = getThemeService();
    if (service != null) {
      Branding branding = new Branding();
      branding.setCompanyName(StringUtils.defaultString(companyName));
      publishLegacy(service, branding, false);
      return;
    }
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
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_LOGO_ID_SETTING_KEY) : service.getState().getLogoFileId();
  }

  @Override
  public Long getFaviconId() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_FAVICON_ID_SETTING_KEY) : service.getState().getFaviconFileId();
  }

  @Override
  public Long getLoginBackgroundId() {
    return getPropertyValueLong(BRANDING_LOGIN_BG_ID_SETTING_KEY);
  }

  @Override
  public Long getPageBackgroundId() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_PAGE_BG_ID_SETTING_KEY) : getThemeInUseImageFileId(service, ThemeVariables.PAGE_BACKGROUND_IMAGE);
  }

  @Override
  public Long getTopBarBackgroundId() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_TOP_BAR_BG_ID_SETTING_KEY) : getThemeInUseImageFileId(service, TOP_BAR_BG_IMAGE_THEME_STYLE_KEY);
  }

  @Override
  public Long getSideBarBackgroundId() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_SIDEBAR_BG_ID_SETTING_KEY) : getThemeInUseImageFileId(service, SIDEBAR_BG_IMAGE_THEME_STYLE_KEY);
  }

  @Override
  public Long getDrawerBackgroundId() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_DRAWER_BG_ID_SETTING_KEY) : getThemeInUseImageFileId(service, DRAWER_BG_IMAGE_THEME_STYLE_KEY);
  }

  @Override
  public Long getAppBackgroundId() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_APP_BG_ID_SETTING_KEY) : getThemeInUseImageFileId(service, APP_BG_IMAGE_THEME_STYLE_KEY);
  }

  @Override
  public Long getAppTextTitleBackgroundId() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_APP_TITLE_BG_ID_KEY) : getThemeInUseImageFileId(service, APP_TITLE_BG_IMAGE_THEME_KEY);
  }

  @Override
  public Long getAppTextHeaderBackgroundId() {
    ThemeService service = getThemeService();
    return service == null ? getPropertyValueLong(BRANDING_APP_HEADER_BG_ID_KEY) : getThemeInUseImageFileId(service, APP_HEADER_BG_IMAGE_THEME_KEY);
  }

  @Override
  public Logo getLogo() {
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      // the state row's file, shared by every node; configured defaults are imported by the theme synchroniser
      return readCachedThemeFile(service, getLogoId(), new Logo());
    }
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
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      return readCachedThemeFile(service, getFaviconId(), new Favicon());
    }
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
    dropStaleMemos();
    if (getThemeService() != null) {
      // read by the id its setting holds, on every node: no per-node copy
      Long fileId = getLoginBackgroundId();
      return fileId == null ? importConfiguredLoginBackground() : readThemeFile(fileId, new Background());
    }
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
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      return readThemeInUseBackground(service, ThemeVariables.PAGE_BACKGROUND_IMAGE);
    }
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
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      return readThemeInUseBackground(service, TOP_BAR_BG_IMAGE_THEME_STYLE_KEY);
    }
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
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      return readThemeInUseBackground(service, SIDEBAR_BG_IMAGE_THEME_STYLE_KEY);
    }
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
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      return readThemeInUseBackground(service, DRAWER_BG_IMAGE_THEME_STYLE_KEY);
    }
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
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      return readThemeInUseBackground(service, APP_BG_IMAGE_THEME_STYLE_KEY);
    }
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
    ThemeService service = getThemeService();
    if (service != null) {
      // addressed by file id, which the image endpoint caches for good
      Long fileId = getThemeInUseImageFileId(service, APP_BG_IMAGE_THEME_STYLE_KEY);
      return fileId == null ? null : BRANDING_APP_BG_BASE_PATH + fileId;
    }
    Background background = getAppBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_APP_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public Background getAppTextTitleBackground() {
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      return readThemeInUseBackground(service, APP_TITLE_BG_IMAGE_THEME_KEY);
    }
    if (this.appTextTitleBackground == null) {
      this.appTextTitleBackground = retrieveBackground(getAppTextTitleBackgroundId(), "application title background");
    }
    return this.appTextTitleBackground;
  }

  @Override
  public String getAppTextTitleBackgroundPath() {
    ThemeService service = getThemeService();
    if (service != null) {
      // addressed by file id, which the image endpoint caches for good
      Long fileId = getThemeInUseImageFileId(service, APP_TITLE_BG_IMAGE_THEME_KEY);
      return fileId == null ? null : BRANDING_APP_TITLE_BG_BASE_PATH + fileId;
    }
    Background background = getAppTextTitleBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_APP_TITLE_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public Background getAppTextHeaderBackground() {
    dropStaleMemos();
    ThemeService service = getThemeService();
    if (service != null) {
      return readThemeInUseBackground(service, APP_HEADER_BG_IMAGE_THEME_KEY);
    }
    if (this.appTextHeaderBackground == null) {
      this.appTextHeaderBackground = retrieveBackground(getAppTextHeaderBackgroundId(), "application header background");
    }
    return this.appTextHeaderBackground;
  }

  @Override
  public String getAppTextHeaderBackgroundPath() {
    ThemeService service = getThemeService();
    if (service != null) {
      // addressed by file id, which the image endpoint caches for good
      Long fileId = getThemeInUseImageFileId(service, APP_HEADER_BG_IMAGE_THEME_KEY);
      return fileId == null ? null : BRANDING_APP_HEADER_BG_BASE_PATH + fileId;
    }
    Background background = getAppTextHeaderBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_APP_HEADER_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  /**
   * The login background stays in the settings: its configured default is
   * imported as it always was, into the setting every node then reads.
   */
  private Background importConfiguredLoginBackground() {
    // every login page asks: a missing or unreadable file is looked up again at most once per retry window
    if (StringUtils.isBlank(defaultConfiguredLoginBgPath) || System.currentTimeMillis() < configuredLoginBgRetryAt) {
      return null;
    }
    Background imported = null;
    try {
      Background background = retrieveDefaultBrandingFile(defaultConfiguredLoginBgPath,
                                                          new Background(),
                                                          LOGIN_BACKGROUND_NAME,
                                                          BRANDING_LOGIN_BG_ID_SETTING_KEY);
      imported = background == null || background.getData() == null ? null : background;
    } catch (Exception e) {
      LOG.warn("The configured login background {} could not be imported", defaultConfiguredLoginBgPath, e);
    }
    if (imported == null) {
      configuredLoginBgRetryAt = System.currentTimeMillis() + MISSING_FILE_RETRY_MILLIS;
    }
    return imported;
  }

  private <T extends BrandingFile> T readThemeFileInfo(Long fileId, T brandingFile) {
    if (fileId == null) {
      return null;
    }
    try {
      FileInfo info = fileService.getFileInfo(fileId);
      if (info != null) {
        brandingFile.setSize(info.getSize());
        brandingFile.setUpdatedDate(info.getUpdatedDate().getTime());
        brandingFile.setFileId(fileId);
      }
      return brandingFile;
    } catch (Exception e) {
      LOG.warn("Branding file {} could not be read", fileId, e);
      return null;
    }
  }

  /**
   * Reads a branding file through the node's image cache, the one the REST
   * endpoints use: a cached file costs no database nor storage read.
   */
  private <T extends BrandingFile> T readCachedThemeFile(ThemeService service, Long fileId, T brandingFile) {
    if (fileId == null) {
      return null;
    }
    ImageBytes image = service.readImage(fileId);
    if (image != null) {
      brandingFile.setData(image.data());
      brandingFile.setSize(image.data().length);
      brandingFile.setUpdatedDate(image.updatedDate());
      brandingFile.setFileId(fileId);
    }
    return brandingFile;
  }

  private <T extends BrandingFile> T readThemeFile(Long fileId, T brandingFile) {
    if (fileId == null) {
      return null;
    }
    try {
      return retrieveStoredBrandingFile(fileId, brandingFile);
    } catch (Exception e) {
      LOG.warn("Branding file {} could not be read", fileId, e);
      return null;
    }
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
    ThemeService service = getThemeService();
    if (service != null) {
      // addressed by file id: a replacement is a new file, so the URL changes with it on every node
      Long fileId = service.getState().getLogoFileId();
      return fileId == null ? null : BRANDING_LOGO_BASE_PATH + fileId;
    }
    Logo brandingLogo = getLogo();
    return brandingLogo == null
           || brandingLogo.getData() == null ? null : BRANDING_LOGO_BASE_PATH + Objects.hash(brandingLogo.getUpdatedDate());
  }

  @Override
  public String getFaviconPath() {
    ThemeService service = getThemeService();
    if (service != null) {
      Long fileId = service.getState().getFaviconFileId();
      return fileId == null ? null : BRANDING_FAVICON_BASE_PATH + fileId;
    }
    Favicon brandingFavicon = getFavicon();
    return brandingFavicon == null
           || brandingFavicon.getData() == null ? null :
                                                BRANDING_FAVICON_BASE_PATH + Objects.hash(brandingFavicon.getUpdatedDate());
  }

  @Override
  public String getDarkLogoPath() {
    ThemeService service = getThemeService();
    Long fileId = service == null ? null : service.getState().getDarkLogoFileId();
    return fileId == null ? getLogoPath() : BRANDING_DARK_LOGO_BASE_PATH + fileId;
  }

  @Override
  public String getDarkFaviconPath() {
    ThemeService service = getThemeService();
    Long fileId = service == null ? null : service.getState().getDarkFaviconFileId();
    return fileId == null ? getFaviconPath() : BRANDING_DARK_FAVICON_BASE_PATH + fileId;
  }

  @Override
  public String getLoginBackgroundPath() {
    if (getThemeService() != null) {
      Long fileId = getLoginBackgroundId();
      if (fileId == null) {
        Background imported = importConfiguredLoginBackground();
        fileId = imported == null ? null : imported.getFileId();
      }
      return fileId == null ? null : BRANDING_LOGIN_BG_BASE_PATH + fileId;
    }
    Background background = getLoginBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_LOGIN_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getPageBackgroundPath() {
    ThemeService service = getThemeService();
    if (service != null) {
      // addressed by file id, which the image endpoint caches for good
      Long fileId = getThemeInUseImageFileId(service, ThemeVariables.PAGE_BACKGROUND_IMAGE);
      return fileId == null ? null : BRANDING_PAGE_BG_BASE_PATH + fileId;
    }
    Background background = getPageBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_PAGE_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getTopBarBackgroundPath() {
    ThemeService service = getThemeService();
    if (service != null) {
      // addressed by file id, which the image endpoint caches for good
      Long fileId = getThemeInUseImageFileId(service, TOP_BAR_BG_IMAGE_THEME_STYLE_KEY);
      return fileId == null ? null : BRANDING_TOP_BAR_BG_BASE_PATH + fileId;
    }
    Background background = getTopBarBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_TOP_BAR_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getSideBarBackgroundPath() {
    ThemeService service = getThemeService();
    if (service != null) {
      // addressed by file id, which the image endpoint caches for good
      Long fileId = getThemeInUseImageFileId(service, SIDEBAR_BG_IMAGE_THEME_STYLE_KEY);
      return fileId == null ? null : BRANDING_SIDEBAR_BG_BASE_PATH + fileId;
    }
    Background background = getSideBarBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_SIDEBAR_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getDrawerBackgroundPath() {
    ThemeService service = getThemeService();
    if (service != null) {
      // addressed by file id, which the image endpoint caches for good
      Long fileId = getThemeInUseImageFileId(service, DRAWER_BG_IMAGE_THEME_STYLE_KEY);
      return fileId == null ? null : BRANDING_DRAWER_BG_BASE_PATH + fileId;
    }
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
    themeStylesheet.set(null);
  }

  @Override
  @Deprecated(since = "7.3", forRemoval = false)
  public void updateLogo(Logo logo) {
    ThemeService service = getThemeService();
    if (service != null) {
      Branding branding = new Branding();
      branding.setLogo(logo);
      publishLegacy(service, branding, false);
      return;
    }
    updateLogo(logo, true);
  }

  @Override
  @Deprecated(since = "7.3", forRemoval = false)
  public void updateFavicon(Favicon favicon) {
    ThemeService service = getThemeService();
    if (service != null) {
      Branding branding = new Branding();
      branding.setFavicon(favicon);
      publishLegacy(service, branding, false);
      return;
    }
    updateFavicon(favicon, true);
  }

  @Override
  public void updateLoginBackground(Background loginBackground) {
    updateLoginBackground(loginBackground, true);
  }

  @Override
  @Deprecated(since = "7.3", forRemoval = false)
  public void updateThemeStyle(Map<String, String> themeStyle) {
    ThemeService service = getThemeService();
    if (service != null) {
      // a merge into the theme in use, validated: an absent key is kept
      Branding branding = new Branding();
      branding.setThemeStyle(themeStyle);
      publishLegacy(service, branding, true);
      return;
    }
    updateThemeStyle(themeStyle, true);
  }

  @Override
  public Map<String, String> getThemeStyle() {
    if (themeVariables == null || themeVariables.isEmpty()) {
      return Collections.emptyMap();
    }
    ThemeService service = getThemeService();
    if (service != null) {
      // the theme in use's resolved light values, under today's keys
      Map<String, String> light = new HashMap<>(getResolvedThemeInUse(service).getLight());
      light.keySet().retainAll(themeVariables.keySet());
      light.values().removeIf(StringUtils::isBlank);
      return light;
    }

    Map<String, String> themeStyleVariables = new HashMap<>();
    Set<String> variables = themeVariables.keySet();
    for (String themeVariable : variables) {
      String styleValue = getThemeStyleValue(themeVariable);
      if (StringUtils.isNotBlank(styleValue)) {
        themeStyleVariables.put(themeVariable, styleValue);
      }
    }
    neutralizeTopBarGradient(themeStyleVariables);
    return themeStyleVariables;
  }

  /**
   * The Topbar background offers no gradient option. A gradient
   * still stored in the Topbar background is ignored at read time (never
   * rewritten): the image URL part is kept and the gradient's first colour
   * becomes the Topbar colour. Only when no colour can be read
   * from the gradient does the stored colour stay, and when that one is
   * transparent or absent it is dropped so that the platform default applies.
   * The picker always stores a plain colour (white by default) next to a
   * gradient, which is why the stored colour cannot take precedence.
   */
  public static void neutralizeTopBarGradient(Map<String, String> themeStyle) {
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
  public String getDisplayMode(String username) {
    return BrandingUtils.getDisplayMode(username);
  }

  @Override
  public Map<String, String> getTemplateDefaults() {
    if (templateDefaults == null) {
      Map<String, String> literals = new HashMap<>();
      String template = getLessThemeContent();
      if (StringUtils.isNotBlank(template)) {
        Matcher matcher = TEMPLATE_LITERAL.matcher(template);
        while (matcher.find()) {
          literals.putIfAbsent(matcher.group(1), matcher.group(2).trim());
        }
      }
      templateDefaults = Collections.unmodifiableMap(literals);
    }
    return templateDefaults;
  }


  @Override
  public Map<String, String> getMailPalette(boolean dark) {
    ThemeService service = getThemeService();
    if (service == null) {
      // before the theme services: the built-in light palette, never cached
      Map<String, String> palette = new HashMap<>(MailDocumentShell.LIGHT_DEFAULTS);
      putIfNotNull(palette, MailDocumentShell.LOGO_PATH, getLogoPath());
      putIfNotNull(palette, MailDocumentShell.COMPANY_NAME, getCompanyName());
      return palette;
    }
    long stamp = getLastUpdatedTime();
    MailPalettes palettes = mailPalettes.get();
    if (palettes == null || palettes.stamp() != stamp) {
      ResolvedTheme resolved = getResolvedThemeInUse(service);
      palettes = new MailPalettes(stamp, mailPalette(service, resolved.getLight(), false), mailPalette(service, resolved.getDark(), true));
      mailPalettes.set(palettes);
    }
    return dark ? palettes.dark() : palettes.light();
  }

  private Map<String, String> mailPalette(ThemeService service, Map<String, String> values, boolean dark) {
    Map<String, String> defaults = dark ? Map.of(MailDocumentShell.BACKGROUND,
                                                 io.meeds.portal.theme.util.ThemeColorCalculator.DARK_PAGE_BACKGROUND,
                                                 MailDocumentShell.SURFACE,
                                                 io.meeds.portal.theme.util.ThemeColorCalculator.DARK_APP_BACKGROUND,
                                                 MailDocumentShell.TEXT,
                                                 io.meeds.portal.theme.util.ThemeColorCalculator.DARK_NEUTRAL,
                                                 MailDocumentShell.SECONDARY_TEXT,
                                                 io.meeds.portal.theme.util.ThemeColorCalculator.DARK_GREY,
                                                 MailDocumentShell.BORDER,
                                                 "#3A4148") :
                                        MailDocumentShell.LIGHT_DEFAULTS;
    Map<String, String> palette = new HashMap<>(defaults);
    putColor(palette, MailDocumentShell.BACKGROUND, values.get("pageBackgroundColor"));
    putColor(palette, MailDocumentShell.SURFACE, values.get("appBackgroundColor"));
    putColor(palette, MailDocumentShell.TEXT, values.getOrDefault("appTextColor", values.get("textColor")));
    putColor(palette, MailDocumentShell.SECONDARY_TEXT, values.get("appTextSubtitleColor"));
    putColor(palette, MailDocumentShell.LINK, values.get(ThemeValueResolver.PRIMARY_TEXT_COLOR));
    putColor(palette, MailDocumentShell.PRIMARY, values.get("primaryColor"));
    putColor(palette, MailDocumentShell.BORDER, values.get("appBorderColor"));
    BrandingState state = service.getState();
    Long logoId = dark ? service.getEffectiveDarkLogoFileId() : state.getLogoFileId();
    putIfNotNull(palette, MailDocumentShell.LOGO_PATH, dark ? getDarkLogoPath() : getLogoPath());
    ImageBytes logo = logoId == null ? null : service.readImage(logoId);
    // most mail clients show no SVG: the header writes the company name instead
    palette.put(MailDocumentShell.LOGO_IS_SVG, String.valueOf(logo != null && BrandingImageValidator.SVG.equals(logo.type())));
    putIfNotNull(palette, MailDocumentShell.COMPANY_NAME, getCompanyName());
    return Collections.unmodifiableMap(palette);
  }

  private static void putColor(Map<String, String> palette, String role, String value) {
    // an opaque hex only: a palette colour is written inline in every mail
    if (value != null && value.matches("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})")) {
      palette.put(role, value);
    }
  }

  private static void putIfNotNull(Map<String, String> map, String key, String value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  @Override
  public String compileThemeStyle(Map<String, String> values) {
    try {
      return compileThemeCSS(toCompiledValues(values));
    } catch (Less4jException e) {
      LOG.debug("Theme values do not compile, refused", e);
      throw new IllegalArgumentException("branding.theme.stylesheetCompilationError");
    }
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
    dropStaleMemos();
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
    dropStaleMemos();
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
          variableValue = normalizeSizeUnits(variableName, variableValue);
          if (!isValidApplicationThemeStyleValue(variableName, variableValue)) {
            // Configuration follows the same grammar as the UI: a malformed value is not emitted in the platform stylesheet
            LOG.warn("Invalid configured value '{}' for theme variable '{}', ignored (the built-in default applies)",
                     variableValue,
                     variableName);
            variableValue = "";
          }
          this.themeVariables.put(variableName, variableValue);
        }
        String configuredFontFamily = this.themeVariables.get(THEME_FONT_FAMILY_KEY);
        if (configuredFontFamily != null && !isSupportedFontFamily(configuredFontFamily)) {
          LOG.warn("Unsupported default font family '{}' in {}, using '{}'. Supported families: {}",
                   configuredFontFamily,
                   BRANDING_THEME_VARIABLES,
                   DEFAULT_FONT_FAMILY,
                   SUPPORTED_FONT_FAMILIES);
          this.themeVariables.put(THEME_FONT_FAMILY_KEY, DEFAULT_FONT_FAMILY);
        }
      }
    }
  }

  /**
   * Drops the memoised values when the stored update time changed since they
   * were read, by a write on this node or on another node of a cluster, then
   * once more {@link #SECOND_DROP_DELAY} milliseconds later
   */
  private void dropStaleMemos() {
    long lastUpdatedTime = getLastUpdatedTime();
    if (lastUpdatedTime != memoisedAt) {
      dropMemos();
      memoisedAt = lastUpdatedTime;
      secondDropAt.set(clock.getAsLong() + SECOND_DROP_DELAY);
    } else {
      long dropAt = secondDropAt.get();
      // Only the deadline read here is cleared: a newer one, set meanwhile, stays
      if (dropAt > 0 && clock.getAsLong() >= dropAt && secondDropAt.compareAndSet(dropAt, 0)) {
        dropMemos();
        themeStylesheet.set(null);
      }
    }
  }

  private void dropMemos() {
    this.loginTitle = null;
    this.loginSubtitle = null;
    this.logo = null;
    this.favicon = null;
    this.loginBackground = null;
    this.pageBackground = null;
    this.topBarBackground = null;
    this.sideBarBackground = null;
    this.drawerBackground = null;
    this.appBackground = null;
    this.appTextTitleBackground = null;
    this.appTextHeaderBackground = null;
  }

  private ThemeStylesheet getPristineStylesheet() {
    ThemeStylesheet pristine = pristineStylesheet.get();
    if (pristine == null) {
      pristine = new ThemeStylesheet(0, compilePristineTemplate());
      pristineStylesheet.compareAndSet(null, pristine);
    }
    return pristineStylesheet.get();
  }

  /**
   * @return the lock the request compiling the stylesheet holds
   */
  ReentrantLock getThemeCSSLock() {
    return themeCSSLock;
  }

  private boolean isCurrent(ThemeStylesheet stylesheet, long lastUpdatedTime) {
    return stylesheet != null && stylesheet.stamp() == lastUpdatedTime;
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

  @Override
  public void validateCustomCss(String customCss) {
    try {
      validateCSSStyleValue(customCss);
    } catch (IllegalArgumentException e) {
      // its own code: a theme value refused in the same publication carries the generic one
      throw new IllegalArgumentException(CUSTOM_CSS_FORBIDDEN_MESSAGE, e);
    }
  }

  @Override
  public void updateCustomCss(String customCss) {
    validateCustomCss(customCss);
    // no event: the theme service moves the stylesheet stamp, evicts its snapshot and broadcasts the new version
    updateCustomCss(customCss, false);
    updateLastUpdatedTime(System.currentTimeMillis());
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
    } catch (IllegalArgumentException e) {
      // a refused upload: answered 400 with its message code
      throw e;
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
    if (BRANDING_LOGIN_BG_ID_SETTING_KEY.equals(settingKey)) {
      // the login background gets the background images' check: type by content, 2 MB
      byte[] data;
      try (InputStream uploaded = inputStream) {
        data = uploaded.readAllBytes();
      }
      BrandingImageValidator.validateImage(data);
      inputStream = new ByteArrayInputStream(data);
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

  private String compileThemeCSS() {
    ThemeService service = getThemeService();
    if (isThemeStylesheet(service)) {
      return withCustomCss(compileThemeVersions(service));
    }
    String css = "";
    if (themeVariables != null && !themeVariables.isEmpty() && StringUtils.isNotBlank(getLessThemeContent())) {
      Map<String, String> effectiveValues = toCompiledValues(getThemeStyle());
      try {
        css = compileThemeCSS(effectiveValues);
        this.lastCompiledThemeCSS = css;
      } catch (Less4jException e) {
        LOG.warn("Error compiling less file content, the last compiled stylesheet is served", e);
        if (this.lastCompiledThemeCSS == null) {
          this.lastCompiledThemeCSS = compilePristineTemplate();
        }
        css = this.lastCompiledThemeCSS;
      }
    }
    return withCustomCss(css);
  }

  private String withCustomCss(String css) {
    String customCss = getCustomCss();
    if (StringUtils.isNotBlank(customCss)
        && getFeatureService() != null
        && getFeatureService().isActiveFeature(BRANDING_CUSTOM_STYLE_FEATURE)) {
      return css + "\n" + customCss;
    }
    return css;
  }

  /**
   * One stylesheet, both versions: the shipped template compiled once with the
   * theme in use's resolved light values and once with its dark values, the
   * dark declarations re-wrapped under the dark and automatic selectors, and
   * the light ones repeated for the light scopes of the add-ons that stay
   * light. Each version falls back to its own last compiled result, never to
   * the other version's.
   */
  String compileThemeVersions(ThemeService service) {
    io.meeds.portal.theme.model.Theme theme = service.getThemeInUse();
    ResolvedTheme resolved = service.resolve(theme);
    long themeId = theme == null ? 0 : theme.getId();
    String light = compileVersion(withImageUrls(resolved.getLight(), themeId, false), false);
    String dark = compileVersion(withImageUrls(resolved.getDark(), themeId, true), true);
    String bubbles = String.format("--allPagesCommentBackgroundColor: %1$s; --allPagesDrawerCommentBackgroundColor: %2$s; "
        + "--themeCommentBackgroundColor: %1$s;",
                                   resolved.getDark().get(ThemeValueResolver.COMMENT_BUBBLE_COLOR),
                                   resolved.getDark().get(ThemeValueResolver.DRAWER_COMMENT_BUBBLE));
    String lightOwn = pageDeclarations(withImageUrls(resolved.getLight(), themeId, false)) + COMMENT_DEFAULTS_LIGHT;
    Map<String, String> darkPage = withImageUrls(resolved.getDark(), themeId, true);
    // no page colour set or configured: the dark page default of the note, below the dark application surface, as
    // the mails use it; in light the skin keeps its own default
    if (StringUtils.isBlank(darkPage.get(ThemeVariables.PAGE_BACKGROUND_COLOR))) {
      darkPage.put(ThemeVariables.PAGE_BACKGROUND_COLOR, ThemeColorCalculator.DARK_PAGE_BACKGROUND);
    }
    String darkOwn = pageDeclarations(darkPage) + COMMENT_DEFAULTS_DARK + bubbles;
    StringBuilder css = new StringBuilder();
    css.append(":root { color-scheme: light; ").append(lightOwn).append(" }\n").append(light).append('\n');
    css.append(DARK_SELECTOR).append(" { color-scheme: dark; }\n")
       .append(rewrap(dark, DARK_SELECTOR))
       .append('\n')
       .append(DARK_SELECTOR).append(" { ").append(darkOwn).append(" }\n");
    css.append("@media (prefers-color-scheme: dark) {\n")
       .append(AUTO_SELECTOR).append(" { color-scheme: dark; }\n")
       .append(rewrap(dark, AUTO_SELECTOR))
       .append('\n')
       .append(AUTO_SELECTOR).append(" { ").append(darkOwn).append(" }\n}\n");
    css.append(LIGHT_SCOPE_SELECTOR)
       .append(" { color-scheme: light; ")
       .append(lightOwn)
       .append(" }\n")
       .append(rewrap(light, LIGHT_SCOPE_SELECTOR));
    return css.toString();
  }

  /**
   * The page background and width of one version, as the custom properties
   * the skin reads on the page (platform-ui helpers.less, reset.less): each
   * version declares every one, initial when unset, so a block never inherits
   * the other version's value. Every value went through the page vocabulary
   * of the theme validator when it was published.
   */
  static String pageDeclarations(Map<String, String> values) {
    StringBuilder declarations = new StringBuilder();
    appendDeclaration(declarations, "--allPagesBackgroundColor", values.get(ThemeVariables.PAGE_BACKGROUND_COLOR));
    appendDeclaration(declarations, "--allPagesBackgroundImage", values.get(ThemeVariables.PAGE_BACKGROUND_IMAGE));
    appendDeclaration(declarations, "--allPagesBackgroundRepeat", values.get(ThemeVariables.PAGE_BACKGROUND_REPEAT));
    appendDeclaration(declarations, "--allPagesBackgroundSize", values.get(ThemeVariables.PAGE_BACKGROUND_SIZE));
    appendDeclaration(declarations, "--allPagesBackgroundPosition", values.get(ThemeVariables.PAGE_BACKGROUND_POSITION));
    // the site background paints on the scroll container, where scroll would mean fixed to that box: local follows the content
    String attachment = values.get(ThemeVariables.PAGE_BACKGROUND_ATTACHMENT);
    appendDeclaration(declarations, "--allPagesBackgroundAttachment", "scroll".equals(attachment) ? "local" : attachment);
    appendDeclaration(declarations, "--allPagesWidth", values.get(ThemeVariables.PAGE_WIDTH));
    return declarations.toString();
  }

  private static void appendDeclaration(StringBuilder declarations, String property, String value) {
    declarations.append(property).append(": ").append(StringUtils.isBlank(value) ? "initial" : value.trim()).append("; ");
  }

  /**
   * An image key keeps only its gradient: the image itself is served by the
   * theme image endpoint, addressed by its file id, and layered first.
   */
  static Map<String, String> withImageUrls(Map<String, String> resolved, long themeId, boolean dark) {
    Map<String, String> values = new HashMap<>(resolved);
    resolved.forEach((key, fileId) -> {
      if (key.endsWith(IMAGE_FILE_SUFFIX) && StringUtils.isNumeric(fileId)) {
        String imageKey = key.substring(0, key.length() - "File".length());
        String url = "url(" + THEME_IMAGE_BASE_PATH + themeId + "/images/" + imageKey + "?version=" + (dark ? "dark" : "light") + "&v="
            + fileId + ")";
        String effect = values.get(imageKey);
        values.put(imageKey, StringUtils.isBlank(effect) || "none".equals(effect) || "initial".equals(effect) ? url : url + ", " + effect);
      }
    });
    return values;
  }

  /**
   * Template constants no theme sets, derived as their dark counterpart: the
   * neutral background of headers, popups and tab items, and the border grey;
   * their light literals would draw near-white surfaces and lines in dark.
   */
  static final Map<String, String> DARK_TEMPLATE_CONSTANTS = Map.of("primaryBackground",
                                                                    ThemeColorCalculator.DARK_PAGE_BACKGROUND,
                                                                    "greyColor",
                                                                    "#3A424A");

  private String compileVersion(Map<String, String> resolved, boolean dark) {
    Map<String, String> values = new HashMap<>(resolved);
    values.keySet().removeIf(key -> key.endsWith(IMAGE_FILE_SUFFIX));
    if (dark) {
      // the resolved values may carry the template's light literal for them: the dark value always wins
      values.putAll(DARK_TEMPLATE_CONSTANTS);
    }
    try {
      String css = compileThemeCSS(toCompiledValues(values));
      if (dark) {
        lastCompiledDarkCSS = css;
      } else {
        lastCompiledLightCSS = css;
      }
      return css;
    } catch (Less4jException e) {
      LOG.warn("Error compiling the {} version of the theme in use, its last compiled version is served", dark ? "dark" : "light", e);
      String last = dark ? lastCompiledDarkCSS : lastCompiledLightCSS;
      if (last == null) {
        Map<String, String> pristine = new HashMap<>();
        if (dark) {
          // the built-in dark defaults, never a light block under the dark selectors
          pristine.putAll(ThemeValueResolver.resolve(getResolutionDefaults(), Collections.emptyMap(), Collections.emptyMap()).getDark());
          pristine.putAll(DARK_TEMPLATE_CONSTANTS);
        }
        try {
          last = compileThemeCSS(toCompiledValues(pristine));
        } catch (Less4jException pristineError) {
          LOG.error("The shipped branding Less template does not compile, no {} stylesheet is served", dark ? "dark" : "light", pristineError);
          last = "";
        }
      }
      return last;
    }
  }

  /**
   * @return the compiled declarations, each :root rule moved under the selector,
   *         the @supports groups kept around them
   */
  static String rewrap(String css, String selector) {
    return ROOT_RULE.matcher(css).replaceAll(Matcher.quoteReplacement(selector));
  }

  /**
   * The steps every compile applies to the effective values, the trial
   * compile included, so that a value passing the trial compiles for real.
   */
  static Map<String, String> toCompiledValues(Map<String, String> values) {
    Map<String, String> effectiveValues = new HashMap<>(values);
    neutralizeTopBarGradient(effectiveValues);
    if (StringUtils.isNotBlank(effectiveValues.get("pageMarginTop"))) {
      effectiveValues.put("pageNoMarginTop", "0px");
    }
    if (StringUtils.isNotBlank(effectiveValues.get("pageMarginBottom"))) {
      effectiveValues.put("pageNoMarginBottom", "0px");
    }
    return effectiveValues;
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
    validateFontFamily(branding.getThemeStyle().get(THEME_FONT_FAMILY_KEY));
    branding.getThemeStyle().forEach(this::validateApplicationThemeStyleValue);
    validateThemeStyleCompiles(branding.getThemeStyle());
  }

  /**
   * The grammar bounds the characters of a value, not its Less validity: the
   * whole stylesheet is trial-compiled with the submitted values before
   * anything is stored, so that a value the compiler refuses is answered 400
   * and never removes the platform stylesheet
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

  private String getThemeStyleValue(String themeVariable) {
    SettingValue<?> storedStyleValue = settingService.get(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable);
    String styleValue = storedStyleValue == null
                        || storedStyleValue.getValue() == null ? themeVariables.get(themeVariable) :
                                                               storedStyleValue.getValue().toString();
    if (THEME_FONT_FAMILY_KEY.equals(themeVariable) && !isSupportedFontFamily(styleValue)) {
      return themeVariables.get(themeVariable);
    }
    return styleValue;
  }

  private void validateFontFamily(String fontFamily) {
    if (StringUtils.isNotBlank(fontFamily) && !isSupportedFontFamily(fontFamily)) {
      throw new IllegalArgumentException(FONT_FAMILY_UNSUPPORTED_MESSAGE);
    }
  }

  private boolean isSupportedFontFamily(String fontFamily) {
    return SUPPORTED_FONT_FAMILIES.contains(fontFamily);
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
    } else if (isSizeThemeKey(key)) {
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

  /**
   * @return the version a branding update broadcasts: the published state's
   *         version when the theme services are ready, else the time of the
   *         update
   */
  private Long currentBrandingVersion() {
    ThemeService service = getThemeService();
    return service == null ? Long.valueOf(System.currentTimeMillis()) : Long.valueOf(service.getState().getVersion());
  }

  private void triggerBrandingUpdated(boolean updateLastUpdatedTime, boolean triggerEvent) {
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
    if (triggerEvent) {
      listenerService.broadcast(BRANDING_UPDATED_EVENT, null, currentBrandingVersion());
    }
  }

  /**
   * A theme key holding one size (a margin, border, font size, padding or
   * corner radius), written in the stylesheet where the skin computes with it
   */
  static boolean isSizeThemeKey(String key) {
    return key.contains("Margin") || key.endsWith("BorderSize") || key.endsWith("FontSize") || key.contains("BackgroundPadding")
           || key.contains("BorderRadius") || "borderRadius".equals(key);
  }

  /**
   * A size without a unit is stored with px: the skin combines these values in
   * calc() and border-radius, where a unitless non-zero number makes the whole
   * declaration invalid and drops it silently
   */
  static String normalizeSizeUnits(String key, String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    } else if (isSizeThemeKey(key) && UNITLESS_SIZE_PATTERN.matcher(value).matches()) {
      return value + "px";
    } else if (key.endsWith("BackgroundRadius")) {
      return UNITLESS_SIZE_TOKEN_PATTERN.matcher(value).replaceAll("$1px");
    } else {
      return value;
    }
  }

  static void normalizeSubmittedSizeUnits(Map<String, String> themeStyles) {
    if (themeStyles == null || themeStyles.isEmpty()) {
      return;
    }
    themeStyles.replaceAll(BrandingServiceImpl::normalizeSizeUnits);
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
    // a stripped layer takes the comma after it; a stripped trailing layer leaves the comma before it
    String effect = IMAGE_LAYER_PATTERN.matcher(value).replaceAll("").trim().replaceAll("(^,\\s*)|(\\s*,$)", "").trim();
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
