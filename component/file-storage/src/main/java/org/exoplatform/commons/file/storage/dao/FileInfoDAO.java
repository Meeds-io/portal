package org.exoplatform.commons.file.storage.dao;

import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.commons.file.storage.entity.FileInfoEntity;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public interface FileInfoDAO extends GenericDAO<FileInfoEntity, Long> {

  List<FileInfoEntity> findFilesByChecksum(String checksum);

  int countFilesByChecksum(String checksum);

}
