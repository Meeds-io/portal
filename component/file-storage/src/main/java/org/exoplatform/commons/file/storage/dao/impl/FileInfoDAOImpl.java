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
package org.exoplatform.commons.file.storage.dao.impl;

import java.util.List;

import org.exoplatform.commons.file.storage.dao.FileInfoDAO;
import org.exoplatform.commons.file.storage.entity.FileInfoEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

import jakarta.persistence.NoResultException;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public class FileInfoDAOImpl extends GenericDAOJPAImpl<FileInfoEntity, Long> implements FileInfoDAO {

  @Override
  public List<FileInfoEntity> findFilesByChecksum(String checksum) {
    return getEntityManager().createNamedQuery("fileEntity.findByChecksum", FileInfoEntity.class)
                             .setParameter("checksum", checksum)
                             .getResultList();
  }

  @Override
  public int countFilesByChecksum(String checksum) {
    try {
      return getEntityManager().createNamedQuery("fileEntity.countByChecksum", Long.class)
                               .setParameter("checksum", checksum)
                               .getSingleResult()
                               .intValue();
    } catch (NoResultException e) {
      return 0;
    }
  }
}
