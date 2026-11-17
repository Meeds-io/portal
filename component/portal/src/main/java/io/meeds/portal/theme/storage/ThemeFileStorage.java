/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.portal.theme.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.portal.theme.model.ImageBytes;
import io.meeds.portal.theme.util.BrandingImageValidator;

import jakarta.servlet.ServletContext;

/**
 * The binaries of the branding and the themes, in FileService under the
 * branding namespace. A file belongs to one reference only: a copy gets a new
 * id, so replacing or deleting one reference never breaks another.
 */
@Component
public class ThemeFileStorage {

  public static final String FILE_NAME_SPACE      = "CompanyBranding";

  public static final String UPLOAD_NOT_FOUND     = "branding.upload.notFound";

  private static final Log   LOG                  = ExoLogger.getLogger(ThemeFileStorage.class);

  /** Default memory a node gives to branding images (exo.branding.images.cache.maxBytes). */
  public static final long   IMAGE_CACHE_DEFAULT_BYTES = 64L * 1024 * 1024;

  private static final long  IMAGE_CACHE_TTL_MS   = 60_000;

  private final LinkedHashMap<Long, ImageBytes> imageCache = new LinkedHashMap<>(16, 0.75f, true);

  private long               cachedBytes;

  /** For tests: the time source of the cache's 60 s bound. */
  java.util.function.LongSupplier clock = System::currentTimeMillis;

  @Value("${exo.branding.images.cache.maxBytes:" + IMAGE_CACHE_DEFAULT_BYTES + "}")
  private long               imageCacheMaxBytes = IMAGE_CACHE_DEFAULT_BYTES;

  @Autowired
  private FileService        fileService;

  @Autowired
  private UploadService      uploadService;

  @Autowired
  private UserACL            userAcl;

  /**
   * Reads an upload's bytes, leaving the upload in place: a refused
   * publication is resent by the page with the same upload ids.
   *
   * @param uploadId the upload id the page received
   * @return the uploaded bytes
   * @throws IllegalArgumentException when no such upload exists
   */
  public byte[] readUpload(String uploadId) {
    UploadResource resource = uploadService.getUploadResource(uploadId);
    if (resource == null) {
      throw new IllegalArgumentException(UPLOAD_NOT_FOUND);
    }
    try {
      return Files.readAllBytes(new File(resource.getStoreLocation()).toPath());
    } catch (Exception e) {
      throw new IllegalArgumentException(UPLOAD_NOT_FOUND, e);
    }
  }

  /**
   * @param uploadId an upload read by a publication that committed or
   *          conflicted
   */
  public void removeUpload(String uploadId) {
    uploadService.removeUploadResource(uploadId);
  }

  /**
   * @param name the stored file's name
   * @param mimeType its detected type
   * @param data its bytes
   * @return the new file's id
   */
  public long writeFile(String name, String mimeType, byte[] data) {
    try {
      FileItem fileItem = new FileItem(0L,
                                       name,
                                       mimeType,
                                       FILE_NAME_SPACE,
                                       data.length,
                                       new Date(),
                                       userAcl.getSuperUser(),
                                       false,
                                       new ByteArrayInputStream(data));
      return fileService.writeFile(fileItem).getFileInfo().getId();
    } catch (Exception e) {
      throw new IllegalStateException("branding.file.writeError", e);
    }
  }

  /**
   * @param fileId an existing file
   * @return the id of a new file holding the same bytes, null when the source
   *         no longer exists
   */
  public Long copyFile(long fileId) {
    try {
      FileItem source = fileService.getFile(fileId);
      if (source == null || source.getFileInfo() == null) {
        return null;
      }
      return writeFile(source.getFileInfo().getName(), source.getFileInfo().getMimetype(), source.getAsByte());
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("branding.file.copyError", e);
    }
  }

  /**
   * Reads a configured default file: a path on disk, else a resource of the
   * portal webapp, as the configured logo has always been read.
   *
   * @param path the configured path
   * @return its bytes, null when nothing is found there
   */
  public byte[] readConfiguredFile(String path) {
    if (StringUtils.isBlank(path)) {
      return null; // NOSONAR no file configured
    }
    try {
      File file = new File(path);
      if (file.isFile()) {
        return Files.readAllBytes(file.toPath());
      }
      ServletContext portalContext = PortalContainer.getInstance().getPortalContext();
      try (InputStream inputStream = portalContext == null ? null : portalContext.getResourceAsStream(path)) {
        return inputStream == null ? null : inputStream.readAllBytes(); // NOSONAR
      }
    } catch (Exception e) {
      LOG.warn("Configured branding file {} could not be read", path, e);
      return null; // NOSONAR
    }
  }

  /**
   * A branding image's bytes and detected type, cached per node by file id:
   * a file's content never changes (a replacement is a new file), so an entry
   * is only bounded in time (60 s, so a deleted file stops being served) and
   * the cache in bytes (least recently used evicted first).
   *
   * @param fileId a file of the branding namespace
   * @return its bytes and detected type, null when no such branding file
   */
  public ImageBytes readImage(long fileId) {
    long now = clock.getAsLong();
    synchronized (imageCache) {
      ImageBytes cached = imageCache.get(fileId);
      if (cached != null && now - cached.loadedAt() < IMAGE_CACHE_TTL_MS) {
        return cached;
      }
    }
    try {
      FileItem file = fileService.getFile(fileId);
      if (file == null || file.getFileInfo() == null || !FILE_NAME_SPACE.equals(file.getFileInfo().getNameSpace())) {
        return null;
      }
      byte[] data = file.getAsByte();
      String type = BrandingImageValidator.detectType(data);
      long updatedDate = file.getFileInfo().getUpdatedDate() == null ? 0L : file.getFileInfo().getUpdatedDate().getTime();
      ImageBytes image = new ImageBytes(data, type == null ? "application/octet-stream" : type, now, updatedDate);
      synchronized (imageCache) {
        ImageBytes previous = imageCache.put(fileId, image);
        if (previous != null) {
          cachedBytes -= previous.data().length;
        }
        cachedBytes += data.length;
        Iterator<Map.Entry<Long, ImageBytes>> eldest = imageCache.entrySet().iterator();
        while (cachedBytes > imageCacheMaxBytes && eldest.hasNext()) {
          Map.Entry<Long, ImageBytes> entry = eldest.next();
          if (entry.getKey() != fileId) {
            cachedBytes -= entry.getValue().data().length;
            eldest.remove();
          }
        }
      }
      return image;
    } catch (Exception e) {
      LOG.warn("Branding image {} could not be read", fileId, e);
      return null;
    }
  }

  /**
   * Deletes a file no reference uses any more; a failure only leaves an orphan
   * file, never a broken reference.
   */
  public void deleteFile(Long fileId) {
    if (fileId == null || fileId <= 0) {
      return;
    }
    synchronized (imageCache) {
      ImageBytes removed = imageCache.remove(fileId);
      if (removed != null) {
        cachedBytes -= removed.data().length;
      }
    }
    try {
      fileService.deleteFile(fileId);
    } catch (Exception e) {
      LOG.warn("Branding file {} could not be deleted, it is left as an orphan", fileId, e);
    }
  }

}
