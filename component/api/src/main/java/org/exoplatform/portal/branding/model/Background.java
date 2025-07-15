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

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Background extends BrandingFile implements Cloneable {

  private static final long serialVersionUID = 4603820249597850279L;

  public Background(String uploadId, long size, byte[] data, long updatedDate, long fileId) {
    super(uploadId, size, data, updatedDate, fileId);
  }

  @Override
  public Background clone() { // NOSONAR
    return new Background(getUploadId(), getSize(), getData(), getUpdatedDate(), getFileId());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Background cloneFile() {
    return this.clone();
  }
}
