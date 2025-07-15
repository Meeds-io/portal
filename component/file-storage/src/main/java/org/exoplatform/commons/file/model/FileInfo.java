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

import java.util.Date;

/**
 * File information
 */
public class FileInfo {
  protected Long    id;

  protected String  name;

  protected String  mimetype;

  protected String  nameSpace;

  protected long    size;

  protected Date    updatedDate;

  protected String  updater;

  protected String  checksum;

  protected boolean deleted;

  public FileInfo(Long id,
                  String name,
                  String mimetype,
                  String nameSpace,
                  long size,
                  Date updatedDate,
                  String updater,
                  String checksum,
                  boolean deleted) {
    this.id = id;
    this.name = name;
    this.mimetype = mimetype;
    this.nameSpace = nameSpace;
    this.size = size;
    this.updatedDate = updatedDate;
    this.updater = updater;
    this.checksum = checksum;
    this.deleted = deleted;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getMimetype() {
    return mimetype;
  }

  public long getSize() {
    return size;
  }

  public Date getUpdatedDate() {
    return updatedDate;
  }

  public String getUpdater() {
    return updater;
  }

  public String getChecksum() {
    return checksum;
  }

  public String getNameSpace() {
    return nameSpace;
  }

  public void setNameSpace(String nameSpace) {
    this.nameSpace = nameSpace;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setMimetype(String mimetype) {
    this.mimetype = mimetype;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public void setUpdater(String updater) {
    this.updater = updater;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
}
