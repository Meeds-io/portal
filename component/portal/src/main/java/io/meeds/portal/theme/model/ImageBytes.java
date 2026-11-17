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

import java.util.Arrays;
import java.util.Objects;

/**
 * A branding image as served: its bytes, the type detected from them, and
 * when this node read it.
 *
 * @param data the bytes
 * @param type the detected media type
 * @param loadedAt when the node read the file, in milliseconds
 * @param updatedDate the file's last update date, in milliseconds
 */
public record ImageBytes(byte[] data, String type, long loadedAt, long updatedDate) {

  /**
   * @param data the bytes
   * @param type the detected media type
   * @param loadedAt when the node read the file, in milliseconds
   */
  public ImageBytes(byte[] data, String type, long loadedAt) {
    this(data, type, loadedAt, 0L);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ImageBytes other
           && loadedAt == other.loadedAt
           && updatedDate == other.updatedDate
           && Arrays.equals(data, other.data)
           && Objects.equals(type, other.type);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hash(type, loadedAt, updatedDate) + Arrays.hashCode(data);
  }

  /** The bytes' length, never their content: an image does not belong in a log line. */
  @Override
  public String toString() {
    return "ImageBytes[type=" + type + ", length=" + (data == null ? 0 : data.length) + ", loadedAt=" + loadedAt + "]";
  }
}
