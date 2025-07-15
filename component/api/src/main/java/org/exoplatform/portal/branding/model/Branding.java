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
package org.exoplatform.portal.branding.model;

import java.io.Serializable;
import java.util.*;

import lombok.Getter;
import lombok.Setter;

public class Branding implements Serializable {

  private static final long   serialVersionUID = 625471892955717717L;

  @Getter
  @Setter
  private String              companyName;

  @Getter
  @Setter
  private String              siteName;

  @Getter
  @Setter
  private String              companyLink;

  @Getter
  @Setter
  private String              defaultLanguage;

  @Getter
  @Setter
  private String              direction;

  @Getter
  @Setter
  private Map<String, String> supportedLanguages;

  @Getter
  @Setter
  private Logo                logo;

  @Getter
  @Setter
  private Favicon             favicon;

  @Getter
  @Setter
  private Background          loginBackground;

  @Getter
  @Setter
  private Background          topBarBackground;

  @Getter
  @Setter
  private Background          sideBarBackground;

  @Getter
  @Setter
  private Background          drawerBackground;

  @Getter
  @Setter
  private String              loginBackgroundTextColor;

  @Getter
  @Setter
  private String              loginBackgroundAltText;

  @Getter
  @Setter
  private Background          pageBackground;

  @Getter
  @Setter
  private String              pageBackgroundColor;

  @Getter
  @Setter
  private String              pageBackgroundSize;

  @Getter
  @Setter
  private String              pageBackgroundRepeat;

  @Getter
  @Setter
  private String              pageBackgroundPosition;

  @Getter
  @Setter
  private String              pageBackgroundEffect;

  @Getter
  @Setter
  private String              pageWidth;

  @Getter
  @Setter
  private String              customCss;

  @Getter
  @Setter
  private Map<String, String> themeStyle       = new HashMap<>();

  @Getter
  @Setter
  private Map<String, String> loginTitle       = new HashMap<>();

  @Getter
  @Setter
  private Map<String, String> loginSubtitle    = new HashMap<>();

  @Getter
  @Setter
  private long                lastUpdatedTime;

}
