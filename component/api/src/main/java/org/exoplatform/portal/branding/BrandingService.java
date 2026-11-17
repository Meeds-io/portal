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

import java.util.Locale;
import java.util.Map;

import org.exoplatform.portal.branding.model.Background;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.Favicon;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.portal.branding.model.ThemeStylesheet;

public interface BrandingService {

  /**
   * Broadcast through the {@code ListenerService} after every branding write,
   * a publication, the custom CSS save and the deprecated writers alike. One
   * payload: the event's source is the username of the administrator who
   * wrote, or null when the writer carries no user (a deprecated writer, the
   * custom style feature's toggle); its data is a {@link Long}: once the theme
   * services are ready, the version of the published branding state after the
   * write (a theme publication and the custom CSS save move it, a deprecated
   * settings-only writer leaves it unchanged), and before they are ready the
   * time of the write. A listener uses it as a change marker only, reads
   * nothing else from the event and fetches the branding it needs from the
   * service.
   */
  static final String BRANDING_UPDATED_EVENT = "branding.updated";

  Branding getBrandingInformation();

  Branding getBrandingInformation(boolean retrieveBinaries);

  /**
   * Update the branding information Missing information in the branding object
   * are not updated.
   * 
   * @param branding The new branding information
   * @deprecated since 7.3, not for removal: use {@code ThemeService.publish(BrandingPublication, String)}, which
   *             validates and publishes the themes and the identity together
   */
  @Deprecated(since = "7.3", forRemoval = false)
  void updateBrandingInformation(Branding branding);

  String getCompanyName();

  String getSiteName();

  String getCompanyLink();

  /**
   * Updates the company name.
   *
   * @param companyName the new company name
   * @deprecated since 7.3, not for removal: use {@code ThemeService.publish(BrandingPublication, String)}, which
   *             validates and publishes the themes and the identity together
   */
  @Deprecated(since = "7.3", forRemoval = false)
  void updateCompanyName(String companyName);

  void updateCompanyLink(String companyLink);

  void updateSiteName(String siteName);

  Long getLogoId();

  /**
   * @return Branding Favicon File technical identifier
   */
  Long getFaviconId();

  /**
   * @return Login Background File technical identifier
   */
  Long getLoginBackgroundId();

  /**
   * @return Page Background File details
   */
  Background getPageBackground();

  /**
   * @return Page Background File technical identifier
   */
  Long getPageBackgroundId();

  /**
   * @return Page Background Color
   */
  String getPageBackgroundColor();

  /**
   * @return true when {@link #getPageBackgroundColor()}, stored or configured
   *         (exo.branding.page.backgroundColor), is #F0F0F0 or #F0F0F0FF in
   *         any case, the built-in default of the Branding and Theme page,
   *         which that page stores when its page styling is applied or reset
   *         without choosing a color and no default is configured; false when
   *         no color is set or for any other color, a configured default other
   *         than #F0F0F0 included
   */
  boolean isDefaultPageBackgroundColor();

  /**
   * @return Page Background Size
   */
  String getPageBackgroundSize();

  /**
   * @return Page Background Position
   */
  String getPageBackgroundPosition();

  /**
   * @return Page Background repeat directive
   */
  String getPageBackgroundRepeat();

  /**
   * @return Page Background attachment directive (fixed or scroll)
   */
  String getPageBackgroundAttachment();

  Logo getLogo();

  /**
   * @return configured custom {@link Favicon} else returns default one
   */
  Favicon getFavicon();

  /**
   * @return configured custom {@link Background} else returns default one if
   *         exists else null
   */
  Background getLoginBackground();

  /**
   * @return Login Text Color displayed on top of the Background
   */
  String getLoginBackgroundTextColor();

  /**
   * @return Login alt Text displayed on the Background
   */
  String getLoginBackgroundAltText();

  /**
   * @return {@link Logo} URL to retrieve logo image
   */
  String getLogoPath();

  /**
   * @return {@link Favicon} URL to retrieve favicon image
   */
  String getFaviconPath();

  /**
   * @return the dark logo's URL, the light one's when no dark logo is set
   */
  default String getDarkLogoPath() {
    return getLogoPath();
  }

  /**
   * @return the dark favicon's URL, the light one's when no dark favicon is
   *         set
   */
  default String getDarkFaviconPath() {
    return getFaviconPath();
  }

  /**
   * @return {@link Background} URL to retrieve login background
   */
  String getLoginBackgroundPath();

  /**
   * @return {@link Background} URL to retrieve page background
   */
  String getPageBackgroundPath();

  /**
   * @return the page background Effect
   */
  String getPageBackgroundEffect();

  /**
   * Returns the URL to retrieve the page background, including the background effect.
   *
   * @return the URL of the page background image
   */
  String getPageBackgroundImageUrl();

  /**
   * @return Page width
   */
  String getPageWidth();

  /**
   * @return Page custom additional css
   */
  String getCustomCss();

  /**
   * Update branding logo. If the logo object contains the image data, they are
   * used, otherwise if the uploadId exists it is used to retrieve the uploaded
   * resource. If there is no data, nor uploadId, the logo is deleted.
   * 
   * @param logo The logo object
   * @deprecated since 7.3, not for removal: use {@code ThemeService.publish(BrandingPublication, String)}, which
   *             validates and publishes the themes and the identity together
   */
  @Deprecated(since = "7.3", forRemoval = false)
  void updateLogo(Logo logo);

  /**
   * Update branding favicon. If the favicon object contains the image data,
   * they are used, otherwise if the uploadId exists it is used to retrieve the
   * uploaded resource. If there is no data, nor uploadId, the favicon is
   * deleted.
   * 
   * @param favicon The {@link Favicon} object to update
   * @deprecated since 7.3, not for removal: use {@code ThemeService.publish(BrandingPublication, String)}, which
   *             validates and publishes the themes and the identity together
   */
  @Deprecated(since = "7.3", forRemoval = false)
  void updateFavicon(Favicon favicon);

  /**
   * Update login background. If the object contains the image data, they are
   * used, otherwise if the uploadId exists it is used to retrieve the uploaded
   * resource. If there is no data, nor uploadId, the login background is
   * deleted.
   * 
   * @param loginBackground The {@link Background} object to update
   */
  void updateLoginBackground(Background loginBackground);

  /**
   * @return CSS content of colors for theme
   */
  String getThemeCSSContent();

  /**
   * @return the compiled branding stylesheet with the branding update time it
   *         was compiled for, the value an HTTP response tags it with. The
   *         stamp is always {@link #getLastUpdatedTime()}: when the theme
   *         values do not compile, the last theme compile that did (the
   *         shipped template's when none did yet) is served under the current
   *         stamp, with the current custom CSS appended.
   */
  ThemeStylesheet getThemeStylesheet();

  /**
   * Updated last updated time of one of Branding properties
   * 
   * @param lastUpdatedTimestamp Timestamp in milliseconds
   */
  void updateLastUpdatedTime(long lastUpdatedTimestamp);

  /**
   * @return last updated time in milliseconds
   */
  long getLastUpdatedTime();

  /**
   * Updates CSS variables that defines the chosen theme and that will be
   * applied and server on UI
   *
   * @param themeStyle {@link Map} with variable name as key and variable value
   *                      as map value
   * @deprecated since 7.3, not for removal: use {@code ThemeService.publish(BrandingPublication, String)}, which
   *             validates and publishes the themes and the identity together
   */
  @Deprecated(since = "7.3", forRemoval = false)
  void updateThemeStyle(Map<String, String> themeStyle);

  /**
   * Updates the login background text color
   *
   * @param textColor text color in hex representation like #fff
   */
  void updateLoginBackgroundTextColor(String textColor);


  void updateLoginBackgroundAltText(String altText);

  /**
   * @return {@link Map} with variable name as key and variable value as map
   *         value
   */
  Map<String, String> getThemeStyle();

  /**
   * @return {@link Map} with variable name as key and variable value as map
   *         value
   */
  Map<String, String> getDefaultThemeStyle();

  /**
   * @param username the signed-in user, null on an anonymous page
   * @return the display mode the page renders in: auto, light or dark, the
   *         value of the data-display-mode attribute of its &lt;html&gt;
   */
  default String getDisplayMode(String username) {
    return "light";
  }

  /**
   * @return the literal value the shipped Less template gives each theme
   *         variable it declares, the skin's built-in default
   */
  default Map<String, String> getTemplateDefaults() {
    return java.util.Collections.emptyMap();
  }

  /**
   * @return the values every theme version resolves from: the template's
   *         built-in literals, overridden by the configured defaults that are
   *         set
   */
  default Map<String, String> getResolutionDefaults() {
    Map<String, String> defaults = new java.util.HashMap<>(getTemplateDefaults());
    getDefaultThemeStyle().forEach((key, value) -> {
      if (value != null && !value.isBlank()) {
        defaults.put(key, value);
      }
    });
    return defaults;
  }

  /**
   * @param dark true for the dark version
   * @return the mail palette of the theme in use's version (role to colour,
   *         see {@link MailDocumentShell}), its logo path and whether that
   *         logo is an SVG; the built-in light palette by default
   */
  default Map<String, String> getMailPalette(boolean dark) {
    return MailDocumentShell.LIGHT_DEFAULTS;
  }

  /**
   * Compiles the shipped theme template with the values of one version.
   *
   * @param values effective values by theme variable, the template's own
   *          default applying to a variable absent from the map
   * @return the compiled CSS
   * @throws IllegalArgumentException with the message code
   *           branding.theme.stylesheetCompilationError when the template does
   *           not compile with these values
   */
  default String compileThemeStyle(Map<String, String> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return {@link Map} of login titles per language
   */
  Map<String, String> getLoginTitle();

  /**
   * @param  locale user request {@link Locale}
   * @return        corresponding title to the {@link Locale}
   */
  String getLoginTitle(Locale locale);

  /**
   * @return {@link Map} of login subtitles per language
   */
  Map<String, String> getLoginSubtitle();

  /**
   * @param  locale user request {@link Locale}
   * @return        corresponding subtitle to the {@link Locale}
   */
  String getLoginSubtitle(Locale locale);

  /**
   * @return default platform language
   */
  String getDefaultLanguage();

  /**
   * @return Top bar Background File technical identifier
   */
  Long getTopBarBackgroundId();

  /**
   * @return Top bar Background File technical identifier
   */
  Background getTopBarBackground();

  /**
   * @return {@link Background} URL to retrieve top bar background
   */
  String getTopBarBackgroundPath();

  /**
   * @return Application default Background File technical identifier
   *         (Branding and Theme &gt; Pages &amp; Apps style &gt; Application
   *         Styling)
   */
  Long getAppBackgroundId();

  /**
   * @return Application default {@link Background}
   */
  Background getAppBackground();

  /**
   * @return {@link Background} URL to retrieve the application default
   *         background
   */
  String getAppBackgroundPath();

  /**
   * @return true when the platform-wide default is to keep the Topbar fixed
   *         while scrolling (theme variable 'topBarSticky')
   */
  boolean isTopBarSticky();

  /**
   * @return Application title text default {@link Background} file identifier
   */
  Long getAppTextTitleBackgroundId();

  /**
   * @return Application title text default {@link Background}
   */
  Background getAppTextTitleBackground();

  /**
   * @return URL to retrieve the application title text default background
   */
  String getAppTextTitleBackgroundPath();

  /**
   * @return Application header text default {@link Background} file identifier
   */
  Long getAppTextHeaderBackgroundId();

  /**
   * @return Application header text default {@link Background}
   */
  Background getAppTextHeaderBackground();

  /**
   * @return URL to retrieve the application header text default background
   */
  String getAppTextHeaderBackgroundPath();

  /**
   * @return Sidebar Background File technical identifier
   */
  Long getSideBarBackgroundId();

  /**
   * @return Sidebar Background File technical identifier
   */
  Background getSideBarBackground();

  /**
   * @return {@link Background} URL to retrieve sideBar background
   */
  String getSideBarBackgroundPath();

  /**
   * @return Drawer Background File technical identifier
   */
  Long getDrawerBackgroundId();

  /**
   * @return Drawer Background File technical identifier
   */
  Background getDrawerBackground();

  /**
   * @return {@link Background} URL to retrieve drawer background
   */
  String getDrawerBackgroundPath();


  /**
   * @return true when the theme stylesheet declares the page background and
   *         width of each version, so a page does not write them inline
   */
  default boolean isPageStyleInStylesheet() {
    return false;
  }

  /**
   * @return the portlet names whose block, and what it opens, keeps its light
   *         display whatever the user's mode (exo.branding.lightOnlyApplications)
   */
  default java.util.List<String> getLightOnlyApplications() {
    return java.util.Collections.emptyList();
  }

  /**
   * @return the names of the Vue apps, mounted outside a portlet, that keep
   *         their light display whatever the user's mode
   *         (exo.branding.lightOnlyVueApps)
   */
  default java.util.List<String> getLightOnlyVueApps() {
    return java.util.Collections.emptyList();
  }

  /**
   * @param applicationId a portlet's application id, webapp/portlet
   * @return true when that portlet keeps its light display
   */
  default boolean isLightOnlyApplication(String applicationId) {
    if (applicationId == null) {
      return false;
    }
    String portletName = applicationId.substring(applicationId.lastIndexOf('/') + 1);
    return getLightOnlyApplications().contains(portletName);
  }

  /**
   * @param customCss the custom CSS to save
   * @throws IllegalArgumentException {@code branding.customCss.forbiddenValue}
   *           when the CSS holds a forbidden construct
   */
  default void validateCustomCss(String customCss) {
    throw new UnsupportedOperationException();
  }

  /**
   * Writes the custom CSS appended to the theme stylesheet, alone, the former
   * whole-branding writer rewriting every other field. It is the setting
   * write of the theme service's custom CSS save, which checks the
   * administrator, moves the stylesheet stamp and broadcasts the branding
   * update, so this method checks no access and broadcasts nothing.
   *
   * @param customCss the CSS, blank to remove it
   * @throws IllegalArgumentException {@code branding.customCss.forbiddenValue}
   *           when the CSS holds a forbidden construct
   */
  default void updateCustomCss(String customCss) {
    throw new UnsupportedOperationException();
  }

}
