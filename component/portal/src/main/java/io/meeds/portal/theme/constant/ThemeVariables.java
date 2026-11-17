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
package io.meeds.portal.theme.constant;

import java.util.Set;

/**
 * Theme keys the theme model adds to the declared theme variables.
 */
public final class ThemeVariables {

  /** Suffix of the companion key holding the FileService id of an image key's file. */
  public static final String      FILE_SUFFIX                = "File";

  /** Suffix of a background image key: its value holds only the gradient part. */
  public static final String      BACKGROUND_IMAGE_SUFFIX    = "BackgroundImage";

  public static final String      TOP_BAR_BACKGROUND_IMAGE   = "topBarBackgroundImage";

  public static final String      FONT_FAMILY                = "fontFamily";

  public static final String      TOP_BAR_STICKY             = "topBarSticky";

  public static final String      PAGE_WIDTH                 = "pageWidth";

  public static final String      PAGE_BACKGROUND_COLOR      = "pageBackgroundColor";

  public static final String      PAGE_BACKGROUND_IMAGE      = "pageBackgroundImage";

  public static final String      PAGE_BACKGROUND_SIZE       = "pageBackgroundSize";

  public static final String      PAGE_BACKGROUND_POSITION   = "pageBackgroundPosition";

  public static final String      PAGE_BACKGROUND_REPEAT     = "pageBackgroundRepeat";

  public static final String      PAGE_BACKGROUND_ATTACHMENT = "pageBackgroundAttachment";

  /** The page design, a theme setting (background per version, width shared) not declared in the Less variables. */
  public static final Set<String> PAGE_VARIABLES             = Set.of(PAGE_WIDTH,
                                                                      PAGE_BACKGROUND_COLOR,
                                                                      PAGE_BACKGROUND_IMAGE,
                                                                      PAGE_BACKGROUND_SIZE,
                                                                      PAGE_BACKGROUND_POSITION,
                                                                      PAGE_BACKGROUND_REPEAT,
                                                                      PAGE_BACKGROUND_ATTACHMENT);

  private ThemeVariables() {
    // constants
  }

  /**
   * A setting both versions share (US6): the font, text sizes, bold and
   * italic, and the layout — page width, margins, rounded corners, borders,
   * paddings, the fixed topbar. Colors, images and gradients are per version.
   */
  public static boolean isShared(String key) {
    return FONT_FAMILY.equals(key)
           || TOP_BAR_STICKY.equals(key)
           || PAGE_WIDTH.equals(key)
           || key.endsWith("FontSize")
           || key.endsWith("FontStyle")
           || key.endsWith("FontWeight")
           || key.contains("Margin")
           || key.contains("Radius")
           || "borderRadius".equals(key)
           || key.endsWith("BorderSize")
           || key.contains("BackgroundPadding");
  }

  public static boolean isImageKey(String key) {
    return key.endsWith(BACKGROUND_IMAGE_SUFFIX);
  }

  public static boolean isFileKey(String key) {
    return key.endsWith(BACKGROUND_IMAGE_SUFFIX + FILE_SUFFIX);
  }

  public static String imageKeyOf(String fileKey) {
    return fileKey.substring(0, fileKey.length() - FILE_SUFFIX.length());
  }

}
