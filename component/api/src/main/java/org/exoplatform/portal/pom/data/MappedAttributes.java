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
package org.exoplatform.portal.pom.data;

import org.gatein.mop.api.Key;
import org.gatein.mop.api.ValueType;

/**
 * A class to hold the various attributes mapped between the model and the mop
 * layer.
 *
 */
public class MappedAttributes {

  private MappedAttributes() {
  }

  /** . */
  public static final Key<String>  ID                         = Key.create("id", ValueType.STRING);

  /** . */
  public static final Key<String>  NAME                       = Key.create("name", ValueType.STRING);

  /** . */
  public static final Key<Boolean> SHOW_MAX_WINDOW            = Key.create("show-max-window", ValueType.BOOLEAN);

  public static final Key<Boolean> HIDE_SHARED_LAYOUT         = Key.create("hide-shared-layout", ValueType.BOOLEAN);

  /** . */
  public static final Key<String>  FACTORY_ID                 = Key.create("factory-id", ValueType.STRING);

  /** . */
  public static final Key<String>  CSS_CLASS                  = Key.create("css-class", ValueType.STRING);

  public static final Key<String>  BORDER_COLOR               = Key.create("border-color", ValueType.STRING);

  public static final Key<String>  TEXT_TITLE_COLOR           = Key.create("text-title-color", ValueType.STRING);

  public static final Key<String>  TEXT_TITLE_FONT_SIZE       = Key.create("text-title-font-size", ValueType.STRING);

  public static final Key<String>  TEXT_TITLE_FONT_WEIGHT     = Key.create("text-title-font-weight", ValueType.STRING);

  public static final Key<String>  TEXT_TITLE_FONT_STYLE      = Key.create("text-title-font-style", ValueType.STRING);

  public static final Key<String>  TEXT_HEADER_COLOR          = Key.create("text-header-color", ValueType.STRING);

  public static final Key<String>  TEXT_HEADER_FONT_SIZE      = Key.create("text-header-font-size", ValueType.STRING);

  public static final Key<String>  TEXT_HEADER_FONT_WEIGHT    = Key.create("text-header-font-weight", ValueType.STRING);

  public static final Key<String>  TEXT_HEADER_FONT_STYLE     = Key.create("text-header-font-style", ValueType.STRING);

  public static final Key<String>  TEXT_COLOR                 = Key.create("text-color", ValueType.STRING);

  public static final Key<String>  TEXT_FONT_SIZE             = Key.create("text-font-size", ValueType.STRING);

  public static final Key<String>  TEXT_FONT_WEIGHT           = Key.create("text-font-weight", ValueType.STRING);

  public static final Key<String>  TEXT_FONT_STYLE            = Key.create("text-font-style", ValueType.STRING);

  public static final Key<String>  TEXT_SUBTITLE_COLOR        = Key.create("text-subtitle-color", ValueType.STRING);

  public static final Key<String>  TEXT_SUBTITLE_FONT_SIZE    = Key.create("text-subtitle-font-size", ValueType.STRING);

  public static final Key<String>  TEXT_SUBTITLE_FONT_WEIGHT  = Key.create("text-subtitle-font-weight", ValueType.STRING);

  public static final Key<String>  TEXT_SUBTITLE_FONT_STYLE   = Key.create("text-subtitle-font-style", ValueType.STRING);

  public static final Key<String>  BACKGROUND_COLOR           = Key.create("background-color", ValueType.STRING);

  public static final Key<String>  BACKGROUND_IMAGE           = Key.create("background-image", ValueType.STRING);

  public static final Key<String>  BACKGROUND_EFFECT          = Key.create("background-effect", ValueType.STRING);

  public static final Key<String>  BACKGROUND_POSITION        = Key.create("background-position", ValueType.STRING);

  public static final Key<String>  BACKGROUND_REPEAT          = Key.create("background-repeat", ValueType.STRING);

  public static final Key<String>  BACKGROUND_SIZE            = Key.create("background-size", ValueType.STRING);

  public static final Key<String>  APP_BACKGROUND_COLOR       = Key.create("app-background-color", ValueType.STRING);

  public static final Key<String>  APP_BACKGROUND_IMAGE       = Key.create("app-background-image", ValueType.STRING);

  public static final Key<String>  APP_BACKGROUND_EFFECT      = Key.create("app-background-effect", ValueType.STRING);

  public static final Key<String>  APP_BACKGROUND_POSITION    = Key.create("app-background-position", ValueType.STRING);

  public static final Key<String>  APP_BACKGROUND_REPEAT      = Key.create("app-background-repeat", ValueType.STRING);

  public static final Key<String>  APP_BACKGROUND_SIZE        = Key.create("app-background-size", ValueType.STRING);

  public static final Key<String>  BORDER_SIZE                = Key.create("border-size", ValueType.STRING);

  public static final Key<String>  BOX_SHADOW                 = Key.create("box-shadow", ValueType.STRING);

  public static final Key<String>  MARGIN_TOP                 = Key.create("margin-top", ValueType.STRING);

  public static final Key<String>  MARGIN_BOTTOM              = Key.create("margin-bottom", ValueType.STRING);

  public static final Key<String>  MARGIN_LEFT                = Key.create("margin-left", ValueType.STRING);

  public static final Key<String>  MARGIN_RIGHT               = Key.create("margin-right", ValueType.STRING);

  public static final Key<String>  RADIUS_TOP_RIGHT_SHADOW    = Key.create("radius-top-right", ValueType.STRING);

  public static final Key<String>  RADIUS_TOP_LEFT_SHADOW     = Key.create("radius-top-left", ValueType.STRING);

  public static final Key<String>  RADIUS_BOTTOM_RIGHT_SHADOW = Key.create("radius-bottom-right", ValueType.STRING);

  public static final Key<String>  RADIUS_BOTTOM_LEFT_SHADOW  = Key.create("radius-bottom-left", ValueType.STRING);

  /** . */
  public static final Key<String>  PROFILES                   = Key.create("profiles", ValueType.STRING);

  /** . */
  public static final Key<Integer> PRIORITY                   = Key.create("priority", ValueType.INTEGER);

  /** . */
  public static final Key<String>  ICON                       = Key.create("icon", ValueType.STRING);

  /** . */
  public static final Key<String>  URI                        = Key.create("uri", ValueType.STRING);

  /** . */
  public static final Key<String>  TEMPLATE                   = Key.create("template", ValueType.STRING);

  /** . */
  public static final Key<String>  LOCALE                     = Key.create("locale", ValueType.STRING);

  /** . */
  public static final Key<String>  SKIN                       = Key.create("skin", ValueType.STRING);

  /** . */
  public static final Key<String>  WIDTH                      = Key.create("width", ValueType.STRING);

  /** . */
  public static final Key<String>  HEIGHT                     = Key.create("height", ValueType.STRING);

  /** . */
  public static final Key<String>  TYPE                       = Key.create("type", ValueType.STRING);

  /** . */
  public static final Key<Boolean> SHOW_INFO_BAR              = Key.create("showinfobar", ValueType.BOOLEAN);

  /** . */
  public static final Key<Boolean> SHOW_MODE                  = Key.create("showmode", ValueType.BOOLEAN);

  /** . */
  public static final Key<Boolean> SHOW_WINDOW_STATE          = Key.create("showwindowstate", ValueType.BOOLEAN);

  /** . */
  public static final Key<String>  THEME                      = Key.create("theme", ValueType.STRING);
}
