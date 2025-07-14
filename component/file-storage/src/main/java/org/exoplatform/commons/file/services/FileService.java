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
package org.exoplatform.commons.file.services;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;

import java.io.IOException;
import java.util.List;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public interface FileService {
  /**
   * Get only the file info of the given id
   *
   * @param id file id
   * @return file info
   */
  public FileInfo getFileInfo(long id);

  /**
   * Get the file (info + binary) of the given id
   *
   * @param id file id
   * @return fileItem
   * @throws FileStorageException signals that an I/O exception of some sort has
   *           occurred.
   */
  public FileItem getFile(long id) throws FileStorageException;

  /**
   * Store the file using the provided DAO and binary provider. This method is
   * transactional, meaning that if the write of the info or of the binary
   * fails, nothing must be persisted.
   *
   * @param file file item
   * @return updated file item
   * @throws IOException signals that an I/O exception of some sort has
   *           occurred.
   * @throws FileStorageException signals that an error occur on save resource.
   */
  public FileItem writeFile(FileItem file) throws FileStorageException, IOException;

  /**
   * Update the stored file using the provided DAO and binary provider. This
   * method is transactional, meaning that if the write of the info or of the
   * binary fails, nothing must be persisted.
   *
   * @param file file item
   * @return updated file item
   * @throws IOException signals that an I/O exception of some sort has
   *           occurred.
   * @throws FileStorageException signals that an error occur on save resource.
   */
  public FileItem updateFile(FileItem file) throws FileStorageException, IOException;

  /**
   * Delete file with the given id The file is not physically deleted, it is
   * only a logical deletion
   *
   * @param id Id of the file to delete
   * @return file Info
   */
  public FileInfo deleteFile(long id);
  
  /**
   * Get the files info of the given checksum
   *
   * @param checksum files checksum
   * @return list file info
   * @throws Exception
   */
  default public List<FileInfo> getFileInfoListByChecksum(String checksum) throws Exception {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Get the files (info + binary) of the given checksum
   *
   * @param checksum files checksum
   * @return list fileItem
   * @throws FileStorageException signals that an I/O exception of some sort has
   *           occurred.
   */
  default public List<FileItem> getFilesByChecksum(String checksum) throws FileStorageException {
    throw new UnsupportedOperationException();
  }
}
