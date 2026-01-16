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
package io.meeds.portal.thumbnail.plugin;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.thumbnail.ImageThumbnailService;
import io.meeds.portal.thumbnail.model.FileContent;

/**
 * A plugin that will be used by {@link ImageThumbnailService} to get image
 * contents
 */
public abstract class ImageThumbnailPlugin extends BaseComponentPlugin {

  /**
   * @return file type that plugin handles
   */
  public abstract String getFileType();

  /**
   * Get the content of the given file
   *
   * @param fileId file technical unique identifier
   * @param username user name
   * @return @return FileItem of the image related to the file Id
   * @throws ObjectNotFoundException thrown when the object doesn't exists
   */
  public abstract FileContent getImage(String fileId, String username) throws ObjectNotFoundException;

  public boolean hasAccessPermission(String fileId, String username) {
    return true;
  }
}
