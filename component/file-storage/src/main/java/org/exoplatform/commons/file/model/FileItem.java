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
package org.exoplatform.commons.file.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;

/**
 * Object representing a file : information + binary
 */
public class FileItem {
  protected FileInfo fileInfo;

  /**
   * Object representing a file : information + binary
   */
  protected byte[]   data;

  public FileItem(FileInfo fileInfo, InputStream inputStream) throws Exception {
    this.fileInfo = fileInfo;
    if (inputStream != null) {
      this.data = IOUtils.toByteArray(inputStream);
    }
  }

  public FileItem(Long id,
                  String name,
                  String mimetype,
                  String nameSpace,
                  long size,
                  Date updatedDate,
                  String updater,
                  boolean deleted,
                  InputStream inputStream)
      throws Exception {
    this.fileInfo = new FileInfo(id, name, mimetype, nameSpace, size, updatedDate, updater, null, deleted);
    if (inputStream != null) {
      this.data = IOUtils.toByteArray(inputStream);
    }
  }

  public FileInfo getFileInfo() {
    return fileInfo;
  }

  /**
   * {@inheritDoc}
   */
  public InputStream getAsStream() {
    if (data != null) {
      return new ByteArrayInputStream(data);
    } else {
      return null;
    }
  }

  public byte[] getAsByte(){
    return data;
  }

  public void setInputStream(InputStream inputStream) throws Exception {
    if (inputStream != null) {
      this.data = IOUtils.toByteArray(inputStream);
    }
  }

  public void setFileInfo(FileInfo fileInfo) {
    this.fileInfo = fileInfo;
  }

}
