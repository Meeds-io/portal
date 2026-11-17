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
package io.meeds.portal.theme.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.meeds.portal.theme.constant.ModesOffered;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A publication validated before its transaction: every value checked, every
 * new file already written. The transaction only writes rows; when it does
 * not commit, its {@code newFileIds} are deleted.
 */
@Data
@NoArgsConstructor
public class PublicationPlan {

  private long               version;

  private boolean            companyNameChanged;

  /** Null removes the published name, so the configured one applies. */
  private String             companyName;

  /** Branding file column to its new file id, null removing the file. */
  private Map<String, Long>  brandingFiles = new HashMap<>();

  private List<PlannedTheme> themes        = new ArrayList<>();

  private List<Long>         deletedThemeIds = new ArrayList<>();

  private ThemeReference     themeInUse;

  private ModesOffered       modesOffered;

  /** Files written for this publication, deleted when it does not commit. */
  private List<Long>         newFileIds    = new ArrayList<>();

  /** Uploads read for this publication, removed once it committed or conflicted. */
  private List<String>       consumedUploadIds = new ArrayList<>();

}
