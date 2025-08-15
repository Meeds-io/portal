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
package org.gatein.portal.controller.resource;

import java.io.File;

import org.exoplatform.commons.utils.IOUtil;

import lombok.Getter;
import lombok.SneakyThrows;

public class ScriptContent {

  public static final ScriptContent NOT_FOUND = new ScriptContent(new byte[0], 0);

  @Getter
  private File                      file;

  @Getter
  private long                      hash;

  @Getter
  private byte[]                    bytes;

  /**
   * Used in prod mode
   */
  public ScriptContent(File file, long hash) {
    this.file = file;
    this.hash = hash;
  }

  /**
   * Used in dev mode
   */
  public ScriptContent(byte[] bytes, long hash) {
    this.bytes = bytes;
    this.hash = hash;
  }

  @SneakyThrows
  public byte[] getContentAsBytes() {
    if (this.bytes != null && this.bytes.length > 0) {
      return this.bytes;
    } else if (file != null) {
      return IOUtil.getFileContentAsBytes(file.getAbsolutePath());
    } else {
      return null; // NOSONAR
    }
  }
}
