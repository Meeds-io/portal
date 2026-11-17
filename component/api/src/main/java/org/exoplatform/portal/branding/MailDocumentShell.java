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
import java.util.Map;

/**
 * Wraps a mail's HTML body in a document whose head declares the color
 * schemes the mail supports and, for a recipient in Automatic, the dark
 * palette as a media query block overriding the palette's role classes. Mail
 * fragments carry their colours inline, so each rule wins with !important.
 * The caller passes an HTML body only (a plain-text mail is never wrapped);
 * a body that already is a document is left as it is.
 */
public final class MailDocumentShell {

  public static final String BACKGROUND     = "background";

  public static final String SURFACE        = "surface";

  public static final String TEXT           = "text";

  public static final String SECONDARY_TEXT = "secondaryText";

  public static final String LINK           = "link";

  public static final String PRIMARY        = "primary";

  public static final String BORDER         = "border";

  public static final String LOGO_PATH      = "logoPath";

  public static final String LOGO_IS_SVG    = "logoIsSvg";

  public static final String COMPANY_NAME   = "companyName";

  /** The built-in light palette: the platform's light defaults, used before the theme services are ready. */
  public static final Map<String, String> LIGHT_DEFAULTS = Map.of(BACKGROUND,
                                                                  "#F0F0F0",
                                                                  SURFACE,
                                                                  "#FFFFFF",
                                                                  TEXT,
                                                                  "#20282C",
                                                                  SECONDARY_TEXT,
                                                                  "#6B6B6B",
                                                                  LINK,
                                                                  "#3F8487",
                                                                  PRIMARY,
                                                                  "#3F8487",
                                                                  BORDER,
                                                                  "#E1E8EE");

  private MailDocumentShell() {
    // utility
  }

  /**
   * @param body the mail's HTML body
   * @param displayMode the recipient's effective mode: auto, light or dark
   * @param darkPalette the dark palette, used in Automatic only
   * @return the body in a document
   */
  public static String wrap(String body, String displayMode, Map<String, String> darkPalette) {
    if (body == null || isDocument(body)) {
      return body;
    }
    String mode = displayMode == null ? "auto" : displayMode.toLowerCase(Locale.ROOT);
    String scheme = "light".equals(mode) ? "light" : "dark".equals(mode) ? "dark" : "light dark";
    StringBuilder html = new StringBuilder(body.length() + 1024);
    html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
        .append("<meta name=\"color-scheme\" content=\"").append(scheme).append("\">")
        .append("<meta name=\"supported-color-schemes\" content=\"").append(scheme).append("\">");
    if ("light dark".equals(scheme) && darkPalette != null) {
      html.append("<style>").append(darkBlock(darkPalette)).append("</style>");
    }
    return html.append("</head><body>").append(body).append("</body></html>").toString();
  }

  /**
   * @return the media query block that gives the role classes their dark
   *         values, and swaps the light logo for the dark one
   */
  public static String darkBlock(Map<String, String> palette) {
    return "@media (prefers-color-scheme: dark) {"
        + rule(".mail-bg", "background-color", palette.get(BACKGROUND))
        + rule(".mail-surface", "background-color", palette.get(SURFACE))
        + rule(".mail-text", "color", palette.get(TEXT))
        + rule(".mail-secondary-text", "color", palette.get(SECONDARY_TEXT))
        + rule(".mail-link", "color", palette.get(LINK))
        + rule(".mail-border", "border-color", palette.get(BORDER))
        + ".mail-logo-light{display:none !important;}.mail-logo-dark{display:inline-block !important;}"
        + "}";
  }

  private static String rule(String selector, String property, String color) {
    // a palette value is a validated hex colour; anything else is left out rather than written into the head
    return color != null && color.matches("#[0-9a-fA-F]{3,8}") ? selector + "{" + property + ":" + color + " !important;}" : "";
  }

  private static boolean isDocument(String body) {
    String start = body.stripLeading().toLowerCase(Locale.ROOT);
    return start.startsWith("<!doctype") || start.startsWith("<html");
  }

}
