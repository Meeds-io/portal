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
package org.exoplatform.commons.file.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.model.NameSpace;
import org.exoplatform.commons.file.resource.BinaryProvider;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.commons.file.services.NameSpaceService;
import org.exoplatform.commons.file.services.util.FileChecksum;
import org.exoplatform.commons.file.storage.DataStorage;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * File Service which stores the file metadata in a database, and uses a
 * BinaryProvider to store the file binary. Created by The eXo Platform SAS
 * Author : eXoPlatform exo@exoplatform.com
 */
public class FileServiceImpl implements FileService {

  private static final Log    LOG                = ExoLogger.getLogger(FileServiceImpl.class);

  private static final String FILE_CREATED_EVENT = "file.created";

  private static final String FILE_UPDATED_EVENT = "file.updated";

  private static final String FILE_DELETED_EVENT = "file.deleted";

  private DataStorage         dataStorage;

  private BinaryProvider      binaryProvider;

  private NameSpaceService    nameSpaceService;

  private ListenerService     listenerService;

  public FileServiceImpl(DataStorage dataStorage,
                         BinaryProvider resourceProvider,
                         NameSpaceService nameSpaceService,
                         ListenerService listenerService) {
    this.dataStorage = dataStorage;
    this.binaryProvider = resourceProvider;
    this.nameSpaceService = nameSpaceService;
    this.listenerService = listenerService;
  }

  @Override
  public FileInfo getFileInfo(long id) {
    FileInfo fileInfo = dataStorage.getFileInfo(id);
    if (fileInfo != null && fileInfo.isDeleted()) {
      return null;
    } else {
      return fileInfo;
    }
  }

  @Override
  public List<FileInfo> getFileInfoListByChecksum(String checksum) {
    return dataStorage.getFileInfoListByChecksum(checksum);
  }

  @Override
  public FileItem getFile(long id) throws FileStorageException {
    FileInfo fileInfo = getFileInfo(id);
    if (fileInfo == null
        || fileInfo.isDeleted()
        || StringUtils.isEmpty(fileInfo.getChecksum())) {
      return null;
    }
    try {
      FileItem fileItem = new FileItem(fileInfo, null);
      InputStream inputStream = binaryProvider.getStream(fileInfo.getChecksum());
      fileItem.setInputStream(inputStream);
      return fileItem;
    } catch (Exception e) {
      throw new FileStorageException("Cannot get File Item ID=" + id, e);
    }
  }

  @Override
  @ExoTransactional
  public FileItem writeFile(FileItem file) throws FileStorageException, IOException {
    FileInfo fileInfo = file.getFileInfo();
    if (fileInfo.getId() != null && fileInfo.getId() > 0) {
      return updateFile(file);
    } else {
      InputStream inputStream = file.getAsStream();
      NameSpace nSpace = dataStorage.getNameSpace(StringUtils.firstNonBlank(fileInfo.getNameSpace(),
                                                                            nameSpaceService.getDefaultNameSpace()));
      FileInfo createdFileInfo = insertFile(fileInfo, nSpace, inputStream);
      file.setFileInfo(createdFileInfo);
      listenerService.broadcast(FILE_CREATED_EVENT, createdFileInfo, null);
      return file;
    }
  }

  @Override
  @ExoTransactional
  public FileItem updateFile(FileItem file) throws FileStorageException, IOException {
    FileInfo fileInfo = file.getFileInfo();
    if (fileInfo == null || fileInfo.getId() == null) {
      throw new IllegalArgumentException("FileInfo id is required to update the binary");
    }
    FileInfo updatedFileInfo = updateFile(fileInfo, file.getAsStream());
    if (updatedFileInfo != null) {
      file.setFileInfo(updatedFileInfo);
      listenerService.broadcast(FILE_UPDATED_EVENT, updatedFileInfo, null);
      return file;
    }
    return null;
  }

  @Override
  public FileInfo deleteFile(long id) {
    FileInfo fileInfo = dataStorage.getFileInfo(id);
    if (fileInfo == null || fileInfo.isDeleted()) {
      return null;
    }
    String checksum = fileInfo.getChecksum();
    if (dataStorage.sharedChecksum(checksum) == 1) {
      try {
        binaryProvider.remove(checksum);
      } catch (IOException e) {
        LOG.warn("Error while effective removal of file with id {} and checksum {}. Continue deleting file definition.",
                 id,
                 checksum,
                 e);
      }
      dataStorage.deleteFileInfo(fileInfo.getId());
    } else {
      // Keep this for retro-compatibility
      // with previous storage strategy
      // since the checksum must be unique for each file
      // Even when same data
      fileInfo.setDeleted(true);
      fileInfo = dataStorage.updateFileInfo(fileInfo);
    }
    listenerService.broadcast(FILE_DELETED_EVENT, fileInfo, null);
    return fileInfo;
  }

  @Override
  public List<FileItem> getFilesByChecksum(String checksum) throws FileStorageException {
    List<FileItem> fileItemList = new ArrayList<>();
    List<FileInfo> fileInfoList = getFileInfoListByChecksum(checksum);
    try {
      for (FileInfo fileInfo : fileInfoList) {
        FileItem fileItem = new FileItem(fileInfo, null);
        InputStream inputStream = binaryProvider.getStream(fileInfo.getChecksum());
        fileItem.setInputStream(inputStream);
        fileItemList.add(fileItem);
      }
    } catch (Exception e) {
      throw new FileStorageException("Cannot get File Item CHECKSUM=" + checksum, e);
    }

    return fileItemList;
  }

  private FileInfo insertFile(FileInfo fileInfo,
                              NameSpace nameSpace,
                              InputStream inputStream) throws FileStorageException, IOException {
    String checksum = generateChecksum(inputStream);
    fileInfo.setChecksum(checksum);
    if (!binaryProvider.exists(checksum)) {
      inputStream.reset();
      binaryProvider.put(checksum, inputStream);
    }
    try {
      return dataStorage.create(fileInfo, nameSpace);
    } catch (Exception e) {
      try {
        binaryProvider.remove(checksum);
      } catch (IOException e1) {
        LOG.error("Error while rollback writing file");
      }
      throw new FileStorageException(String.format("Error while writing file %s", fileInfo.getName()), e);
    }
  }

  private FileInfo updateFile(FileInfo fileInfo, InputStream inputStream) throws FileStorageException, IOException {
    String oldChecksum = getFileChecksum(fileInfo.getId());
    String newChecksum = generateChecksum(inputStream);
    if (!binaryProvider.exists(newChecksum)) {
      inputStream.reset();
      binaryProvider.put(newChecksum, inputStream);
    }

    try {
      fileInfo.setChecksum(newChecksum);
      fileInfo = dataStorage.updateFileInfo(fileInfo);
    } catch (Exception e) {
      try {
        binaryProvider.remove(newChecksum);
      } catch (IOException e1) {
        LOG.warn("Error while rollback written file {}", fileInfo.getId(), e1);
      }
      throw new FileStorageException("Error while writing file " + fileInfo.getName(), e);
    }

    removeFileByChecksum(oldChecksum);
    return fileInfo;
  }

  private void removeFileByChecksum(String checksum) {
    try {
      if (StringUtils.isNotBlank(checksum)
          && binaryProvider.exists(checksum)
          && dataStorage.sharedChecksum(checksum) == 1) {
        binaryProvider.remove(checksum);
      }
    } catch (IOException e) {
      LOG.warn("Error while cleaning replaced file, ignore removing file and continue", e);
    }
  }

  private String getFileChecksum(Long fileId) {
    FileInfo fileInfo = dataStorage.getFileInfo(fileId);
    return fileInfo == null ? null : fileInfo.getChecksum();
  }

  private String generateChecksum(InputStream inputStream) {
    String fileChecksum = FileChecksum.getChecksum(inputStream);
    // Ensure checksum unicity even when the same file exists
    return FileChecksum.getChecksum(fileChecksum + UUID.randomUUID().toString());
  }

}
