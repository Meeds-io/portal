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
package org.exoplatform.commons.file.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.storage.dao.FileBinaryDAO;
import org.exoplatform.commons.file.storage.entity.FileBinaryEntity;

/**
 * This class provide database implementation of the File RDBMS API.
 */
public class RdbmsResourceProvider implements BinaryProvider {
  private FileBinaryDAO fileBinaryDAO;

  public RdbmsResourceProvider(FileBinaryDAO fileBinaryDAO) {
    this.fileBinaryDAO = fileBinaryDAO;
  }

  @Override
  public void put(String name, InputStream data) throws IOException {
    Date now = Calendar.getInstance().getTime();
    FileBinaryEntity fileBinaryEntity = new FileBinaryEntity();
    fileBinaryEntity.setData(FileUtils.readBytes(data));
    fileBinaryEntity.setName(name);
    fileBinaryEntity.setUpdatedDate(now);
    fileBinaryDAO.create(fileBinaryEntity);
  }

  @Override
  public void put(FileItem fileItem) throws IOException {
    Date now = Calendar.getInstance().getTime();
    FileBinaryEntity fileBinaryEntity = new FileBinaryEntity();
    fileBinaryEntity.setData(FileUtils.readBytes(fileItem.getAsStream()));
    fileBinaryEntity.setName(fileItem.getFileInfo().getChecksum());
    fileBinaryEntity.setUpdatedDate(now);
    fileBinaryDAO.create(fileBinaryEntity);
  }

  @Override
  public void put(String name, byte[] data) throws IOException {
    Date now = Calendar.getInstance().getTime();
    FileBinaryEntity fileBinaryEntity = new FileBinaryEntity();
    fileBinaryEntity.setData(data);
    fileBinaryEntity.setName(name);
    fileBinaryEntity.setUpdatedDate(now);
    fileBinaryDAO.create(fileBinaryEntity);
  }

  @Override
  public InputStream getStream(String name) {
    InputStream stream = null;
    byte[] bytes = fileBinaryDAO.findFileBinaryByName(name).getData();
    if (bytes != null) {
      stream = new ByteArrayInputStream(bytes);
    }
    return stream;
  }

  @Override
  public String getFilePath(FileInfo fileInfo) {
    FileBinaryEntity fileBinaryEntity = fileBinaryDAO.findFileBinaryByName(fileInfo.getChecksum());
    return fileBinaryEntity != null ? "rdbms:" + fileBinaryEntity.getId() : null;
  }

  @Override
  public String getFilePath(String name) {
    FileBinaryEntity fileBinaryEntity = fileBinaryDAO.findFileBinaryByName(name);
    return fileBinaryEntity != null ? "rdbms:" + fileBinaryEntity.getId() : null;
  }

  @Override
  public byte[] getBytes(String name) {
    return fileBinaryDAO.findFileBinaryByName(name).getData();
  }

  @Override
  public void remove(String name) throws IOException {
    FileBinaryEntity fileBinaryEntity = fileBinaryDAO.findFileBinaryByName(name);
    if (fileBinaryEntity != null)
      fileBinaryDAO.delete(fileBinaryEntity);
  }

  @Override
  public boolean remove(FileInfo fileInfo) throws IOException {
    FileBinaryEntity fileBinaryEntity = fileBinaryDAO.findFileBinaryByName(fileInfo.getChecksum());
    if (fileBinaryEntity != null) {
      fileBinaryDAO.delete(fileBinaryEntity);
      return true;
    }
    return false;
  }

  @Override
  public boolean exists(String name) throws IOException {
    FileBinaryEntity fileBinaryEntity = fileBinaryDAO.findFileBinaryByName(name);
    return fileBinaryEntity != null;
  }

  @Override
  public long lastModified(String name) throws IOException {
    FileBinaryEntity fileBinaryEntity = fileBinaryDAO.findFileBinaryByName(name);
    return fileBinaryEntity.getUpdatedDate().getTime();
  }

  @Override
  public URL getURL(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getLocation() {
    return "rdbms";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RdbmsResourceProvider that = (RdbmsResourceProvider) o;
    return getLocation().equals(that.getLocation());
  }

  @Override
  public int hashCode() {
    return getLocation().hashCode();
  }

  @Override
  public String toString() {
    return getLocation();
  }
}
