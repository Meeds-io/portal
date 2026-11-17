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

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The files copied outside any lock for the locked import step: the legacy
 * files' copies and the configured default files, each a new FileService id
 * that the step either references or returns as unused.
 */
@Data
@NoArgsConstructor
public class ImportPlan {

  /** Null when the legacy branding was already imported. */
  private LegacyBranding      legacy;

  /** Image key to the copy of its legacy file. */
  private Map<String, Long>   imageCopies    = new HashMap<>();

  private Long                logoCopy;

  private Long                faviconCopy;

  /** Branding file column to the configured path read for it. */
  private Map<String, String> defaultPaths   = new HashMap<>();

  /** Branding file column to the file read from its configured path, null when the path yielded nothing. */
  private Map<String, Long>   defaultCopies  = new HashMap<>();

  public List<Long> getAllCopies() {
    List<Long> copies = new ArrayList<>(imageCopies.values());
    if (logoCopy != null) {
      copies.add(logoCopy);
    }
    if (faviconCopy != null) {
      copies.add(faviconCopy);
    }
    defaultCopies.values().stream().filter(java.util.Objects::nonNull).forEach(copies::add);
    return copies;
  }

}
