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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.portal.theme.ThemeReadiness;
import io.meeds.portal.theme.constant.DisplayMode;
import io.meeds.portal.theme.service.ThemeService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The display mode a page renders in, for the page shells: written on their
 * &lt;html&gt; as data-display-mode, so the first paint is right.
 */
public final class BrandingUtils {

  /** The request attribute the JSP dispatchers set for their page. */
  public static final String DISPLAY_MODE_ATTRIBUTE = "brandingDisplayMode";

  private static final Log   LOG                    = ExoLogger.getLogger(BrandingUtils.class);

  private BrandingUtils() {
    // utility
  }

  /**
   * @param username the signed-in user, null on an anonymous page
   * @return auto, light or dark; light while the theme services start, as
   *         the single-theme platform rendered
   */
  public static String getDisplayMode(String username) {
    if (!ThemeReadiness.isReady()) {
      return DisplayMode.LIGHT.name().toLowerCase(Locale.ROOT);
    }
    try {
      DisplayMode mode = ExoContainerContext.getService(ThemeService.class).getEffectiveDisplayMode(username);
      return mode == DisplayMode.AUTOMATIC ? "auto" : mode.name().toLowerCase(Locale.ROOT);
    } catch (Exception e) {
      LOG.debug("Display mode of {} could not be computed, the device decides", username, e);
      return "auto";
    }
  }

  /**
   * Computes the request's display mode and sets it as the
   * {@link #DISPLAY_MODE_ATTRIBUTE} request attribute.
   *
   * @param request the request of a JSP page that links the theme stylesheet
   * @return auto, light or dark
   */
  public static String getDisplayMode(HttpServletRequest request) {
    String mode = getDisplayMode(request == null ? null : request.getRemoteUser());
    if (request != null) {
      request.setAttribute(DISPLAY_MODE_ATTRIBUTE, mode);
    }
    return mode;
  }

  /**
   * The favicon link(s) of a page shell, for its display mode, like the portal
   * head: explicit light, the light link; explicit dark, the dark one;
   * automatic, the light link plus the dark one under the dark device media
   * query, only when the dark path differs from the light one.
   *
   * @param mode the page's display mode: auto, light or dark
   * @return the escaped markup, empty when the paths are empty
   */
  public static String getFaviconLinks(String mode) {
    BrandingService brandingService = ExoContainerContext.getService(BrandingService.class);
    return getFaviconLinks(mode, brandingService.getFaviconPath(), brandingService.getDarkFaviconPath());
  }

  /**
   * @param mode the page's display mode: auto, light or dark
   * @param lightPath the light favicon path
   * @param darkPath the dark favicon path, the light one when no dark
   *          favicon is set
   * @return the escaped markup of the favicon link(s), empty when none applies
   */
  public static String getFaviconLinks(String mode, String lightPath, String darkPath) {
    String dark = StringUtils.defaultIfBlank(darkPath, lightPath);
    if ("dark".equals(mode)) {
      return StringUtils.isBlank(dark) ? "" : shortcutIcon(dark);
    }
    StringBuilder links = new StringBuilder();
    if (StringUtils.isNotBlank(lightPath)) {
      links.append(shortcutIcon(lightPath));
    }
    if ("auto".equals(mode) && StringUtils.isNotBlank(dark) && !StringUtils.equals(dark, lightPath)) {
      links.append("<link rel=\"icon\" media=\"(prefers-color-scheme: dark)\" href=\"")
           .append(StringEscapeUtils.escapeHtml4(dark))
           .append("\" />");
    }
    return links.toString();
  }

  private static String shortcutIcon(String path) {
    return "<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"" + StringEscapeUtils.escapeHtml4(path) + "\" />";
  }

}
