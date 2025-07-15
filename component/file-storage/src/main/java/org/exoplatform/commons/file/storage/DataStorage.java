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
package org.exoplatform.commons.file.storage;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.NameSpace;
import org.exoplatform.commons.file.storage.dao.FileInfoDAO;
import org.exoplatform.commons.file.storage.dao.NameSpaceDAO;
import org.exoplatform.commons.file.storage.entity.FileInfoEntity;
import org.exoplatform.commons.file.storage.entity.NameSpaceEntity;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public class DataStorage {

  private FileInfoDAO   fileInfoDAO;

  private NameSpaceDAO  nameSpaceDAO;

  public DataStorage(FileInfoDAO fileInfoDAO,
                     NameSpaceDAO nameSpaceDAO) {
    this.fileInfoDAO = fileInfoDAO;
    this.nameSpaceDAO = nameSpaceDAO;
  }

  public FileInfo getFileInfo(long id) {
    FileInfoEntity fileInfoEntity = fileInfoDAO.find(id);
    return convertFileEntityToFileInfo(fileInfoEntity);
  }

  public List<FileInfo> getFileInfoListByChecksum(String checksum) {
    List<FileInfoEntity> result = fileInfoDAO.findFilesByChecksum(checksum);
    List<FileInfo> fileInfoList = new ArrayList<>();
    for (FileInfoEntity fileInfoEntity : result) {
      fileInfoList.add(convertFileEntityToFileInfo(fileInfoEntity));
    }
    return fileInfoList;
  }

  public int sharedChecksum(String checksum) {
    return fileInfoDAO.countFilesByChecksum(checksum);
  }

  public NameSpace getNameSpace(long id) {
    NameSpaceEntity nameSpaceEntity = nameSpaceDAO.find(id);
    return convertNameSpace(nameSpaceEntity);
  }

  @ExoTransactional
  public NameSpace getNameSpace(String name) {
    NameSpaceEntity nameSpaceEntity = nameSpaceDAO.getNameSpaceByName(name);
    return convertNameSpace(nameSpaceEntity);
  }

  public void createNameSpaces(List<NameSpace> listNameSpace) {
    List<NameSpaceEntity> entityList = new ArrayList<>();
    for (NameSpace s : listNameSpace) {
      NameSpaceEntity n = new NameSpaceEntity(s.getName(), s.getDescription());
      entityList.add(n);
    }
    nameSpaceDAO.createAll(entityList);
  }

  public NameSpace createNameSpace(NameSpace nameSpace) {
    NameSpaceEntity n = new NameSpaceEntity(nameSpace.getName(), nameSpace.getDescription());
    NameSpaceEntity createdNameSpace = nameSpaceDAO.create(n);
    return convertNameSpace(createdNameSpace);
  }

  public FileInfo create(FileInfo fileInfo, NameSpace nameSpace) {
    if (fileInfo == null)
      return null;
    NameSpaceEntity nSpace = new NameSpaceEntity(nameSpace.getId(), nameSpace.getName(), nameSpace.getDescription());
    FileInfoEntity fileInfoEntity = new FileInfoEntity(fileInfo.getName(),
                                                       fileInfo.getMimetype(),
                                                       fileInfo.getSize(),
                                                       fileInfo.getUpdatedDate(),
                                                       fileInfo.getUpdater(),
                                                       fileInfo.getChecksum(),
                                                       fileInfo.isDeleted());
    fileInfoEntity.setNameSpaceEntity(nSpace);
    FileInfoEntity createdFile = fileInfoDAO.create(fileInfoEntity);
    return convertFileEntityToFileInfo(createdFile);
  }

  public FileInfo updateFileInfo(FileInfo fileInfo) {
    if (fileInfo == null)
      return null;
    FileInfoEntity fileInfoEntity = new FileInfoEntity(fileInfo.getId(),
                                                       fileInfo.getName(),
                                                       fileInfo.getMimetype(),
                                                       fileInfo.getSize(),
                                                       fileInfo.getUpdatedDate(),
                                                       fileInfo.getUpdater(),
                                                       fileInfo.getChecksum(),
                                                       fileInfo.isDeleted());
    NameSpaceEntity nsEntity = nameSpaceDAO.getNameSpaceByName(fileInfo.getNameSpace());
    fileInfoEntity.setNameSpaceEntity(nsEntity);

    FileInfoEntity updated = fileInfoDAO.update(fileInfoEntity);
    return convertFileEntityToFileInfo(updated);
  }

  public void deleteFileInfo(long id) {
    FileInfoEntity fileInfoEntity = fileInfoDAO.find(id);
    if (fileInfoEntity == null)
      return;
    fileInfoDAO.delete(fileInfoEntity);
  }

  private FileInfo convertFileEntityToFileInfo(FileInfoEntity fileInfoEntity) {
    if (fileInfoEntity == null || fileInfoEntity.isDeleted()) {
      return null;
    }

    return new FileInfo(fileInfoEntity.getId(),
                        fileInfoEntity.getName(),
                        fileInfoEntity.getMimetype(),
                        fileInfoEntity.getNameSpaceEntity().getName(),
                        fileInfoEntity.getSize(),
                        fileInfoEntity.getUpdatedDate(),
                        fileInfoEntity.getUpdater(),
                        fileInfoEntity.getChecksum(),
                        fileInfoEntity.isDeleted());
  }

  private NameSpace convertNameSpace(NameSpaceEntity nameSpaceEntity) {
    if (nameSpaceEntity == null) {
      return null;
    }
    return new NameSpace(nameSpaceEntity.getId(), nameSpaceEntity.getName(), nameSpaceEntity.getDescription());
  }

}
