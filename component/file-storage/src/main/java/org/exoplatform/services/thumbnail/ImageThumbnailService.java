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
package org.exoplatform.services.thumbnail;

import io.meeds.portal.thumbnail.model.FileContent;
import org.exoplatform.commons.file.model.FileItem;

import io.meeds.portal.thumbnail.plugin.ImageThumbnailPlugin;

public interface ImageThumbnailService {

  /**
   * Retrieves a thumbnail by given width and height or creates a thumbnail
   * image and create it if not exist
   * 
   * @param file Image file
   * @param width target thumbnail width
   * @param height target thumbnail height
   * @return {@link FileItem}
   * @throws Exception
   * @deprecated use {@link ImageThumbnailService#getOrCreateThumbnail(String fileType, String id, String userName, int width, int height)} instead
   */

  @Deprecated(forRemoval = true, since = "7.1.0")
  FileItem getOrCreateThumbnail(FileItem file, int width, int height) throws Exception;

  /**
   * Retrieves a thumbnail by given width and height or creates a thumbnail
   * image and create it if not exist
   * 
   * @param resizeSupplier if resizeSupplier is null then use {@link ImageResizeService}
   * @param file Image file
   * @param width target thumbnail width
   * @param height target thumbnail height
   * @return {@link FileItem}
   * @throws Exception
   * @deprecated use {@link ImageThumbnailService#getOrCreateThumbnail(String fileType, String id, String userName, int width, int height)} instead
   */

  @Deprecated(forRemoval = true, since = "7.1.0")
  FileItem getOrCreateThumbnail(ImageResizeService resizeSupplier, FileItem file, int width, int height) throws Exception;

  /**
   * Retrieves a thumbnail by given fileId, fileType, width and height or creates a thumbnail
   * image and create it if not exist
   *
   * @param fileType file fileType
   * @param id       file id
   * @param width    target thumbnail width
   * @param height   target thumbnail height
   * @return {@link FileItem}
   * @throws Exception
   */


  FileItem getOrCreateThumbnail(String fileType, String id, String userName, int width, int height) throws Exception;


  /**
   * Retrieves a thumbnail by given fileId, width and height or creates a thumbnail
   *
   * @param id       file id
   * @param width    target thumbnail width
   * @param height   target thumbnail height
   * @return {@link FileItem}
   * @throws Exception
   */

  FileItem getThumbnail(String id, int width, int height) throws Exception;

  /**
   * Create a thumbnail for given fileId thith given width and height.
   *
   * @param id       file id
   * @param width    target thumbnail width
   * @param height   target thumbnail height
   * @return {@link FileItem}
   * @throws Exception
   */

  FileItem createThumbnail(String id, FileContent fileContent, String userName, int width, int height) throws Exception;


  /**
   * Delete all thumbnails by fileId
   *
   * @param fileId file id
   * @param fileType file Type
   */
  void deleteThumbnails(String fileType, String fileId);

  /**
   * Delete all thumbnails by fileId
   *
   * @param fileId file id
   */
  void deleteThumbnails(Long fileId);

  /**
   * Add a new {@link ImageThumbnailPlugin} for a given file Type
   *
   * @param imageThumbnailPlugin {@link ImageThumbnailPlugin}
   */
  void addPlugin(ImageThumbnailPlugin imageThumbnailPlugin);

  /**
   * Removes a {@link ImageThumbnailPlugin} identified by its file type
   *
   * @param fileType File type
   */
  void removePlugin(String fileType);

}
