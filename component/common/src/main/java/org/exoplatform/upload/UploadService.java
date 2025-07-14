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
package org.exoplatform.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.ProgressListener;
import org.apache.commons.fileupload2.jakarta.JakartaServletDiskFileUpload;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class UploadService {
  /** . */
  private static final Log            log                    = ExoLogger.getLogger(UploadService.class);

  /**
   * These are list ascii-codes of special characters. We should not enable
   * these characters in fileName. They are control codes - that can not
   * printable (from 0 to 31) or special characters like: *, <, >, \, /, :, ?,
   * ", etc. For mor details about ascii-code please see at:
   * http://www.ascii-code.com/
   */
  private static final int[]          illegalChars           = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
      19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124 };

  private static final Pattern        UPLOAD_ID_PATTERN      = Pattern.compile("[^(\\./\\\\)]*");

  private List<MimeTypeUploadPlugin>  plugins;

  private Map<String, UploadResource> uploadResources        = new LinkedHashMap<>();

  private String                      uploadLocation_;

  private UploadLimit                 defaultUploadLimitMB_;

  private Map<String, UploadLimit>    uploadLimits           = new LinkedHashMap<>();

  public static final String          UPLOAD_RESOURCES_STACK = "uploadResourcesStack";

  public static enum UploadUnit {
    KB, MB, GB
  };

  public UploadService(PortalContainerInfo pinfo, InitParams params) throws Exception {
    String tmpDir = System.getProperty("java.io.tmpdir");
    if (params == null || params.getValueParam("upload.limit.size") == null)
      defaultUploadLimitMB_ = new UploadLimit(0, UploadUnit.MB); // 0 means
                                                                 // unlimited
    else
      defaultUploadLimitMB_ = new UploadLimit(Integer.parseInt(params.getValueParam("upload.limit.size").getValue()),
                                              UploadUnit.MB);
    uploadLocation_ = tmpDir + "/" + pinfo.getContainerName() + "/eXoUpload";
  }

  public void register(MimeTypeUploadPlugin plugin) {
    if (plugins == null)
      plugins = new ArrayList<>();
    plugins.add(plugin);
  }

  /**
   * Create UploadResource for HttpServletRequest
   *
   * @param request the webapp's {@link HttpServletRequest}
   * @throws FileUploadException
   */
  public void createUploadResource(HttpServletRequest request) throws FileUploadException {
    String uploadId = request.getParameter("uploadId");
    createUploadResource(uploadId, request);
  }

  public void createUploadResource(UploadResource uploadResource) {
    uploadResources.put(uploadResource.getUploadId(), uploadResource);
  }

  public void createUploadResource(String uploadId, HttpServletRequest request) throws FileUploadException {
    if (uploadId == null || !UPLOAD_ID_PATTERN.matcher(uploadId).matches()) {
      throw new FileUploadException("Upload id " + uploadId + " is not valid, it cannot be null or contain '.' , '/' or '\\'");
    }

    UploadResource upResource = new UploadResource(uploadId);
    upResource.setFileName("");// Avoid NPE in UploadHandler
    upResource.setStoreLocation(uploadLocation_ + File.separator + uploadId);
    uploadResources.put(upResource.getUploadId(), upResource);

    putToStackInSession(request.getSession(true), uploadId);

    double requestContentLength = request.getContentLength();
    upResource.setEstimatedSize(requestContentLength);
    if (isLimited(upResource, requestContentLength)) {
      upResource.setStatus(UploadResource.FAILED_STATUS);
      return;
    }

    File uploadRootPath = new File(uploadLocation_);
    if (!uploadRootPath.exists()) {
      uploadRootPath.mkdirs();
    }

    DiskFileItem fileItem = getFileItem(request, upResource);
    upResource.setFileName(fileItem.getName());
    upResource.setMimeType(fileItem.getContentType());
    if (upResource.getStatus() == UploadResource.UPLOADED_STATUS) {
      writeFile(upResource, fileItem);
    }
    if (plugins != null) {
      for (MimeTypeUploadPlugin plugin : plugins) {
        String mimeType = plugin.getMimeType(fileItem.getName());
        if (mimeType != null)
          upResource.setMimeType(mimeType);
      }
    }
  }

  /**
   * @deprecated use
   *             {@link #createUploadResource(String, jakarta.servlet.http.HttpServletRequest)}
   *             instead
   */
  public void createUploadResource(String uploadId,
                                   String encoding,
                                   String contentType,
                                   double contentLength,
                                   InputStream inputStream) throws Exception {
    if (uploadId == null || !UPLOAD_ID_PATTERN.matcher(uploadId).matches()) {
      throw new FileUploadException("Upload id can contain only digits and hyphens");
    }

    File uploadDir = new File(uploadLocation_);
    if (!uploadDir.exists())
      uploadDir.mkdirs();
    UploadResource upResource = new UploadResource(uploadId);
    RequestStreamReader reader = new RequestStreamReader(upResource);
    uploadResources.put(upResource.getUploadId(), upResource);
    if (isLimited(upResource, contentLength)) {
      upResource.setStatus(UploadResource.FAILED_STATUS);
      return;
    }

    Map<String, String> headers = reader.parseHeaders(inputStream, encoding);

    String fileName = reader.getFileName(headers);
    if (fileName == null)
      fileName = uploadId;
    fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);

    upResource.setFileName(fileName);
    upResource.setMimeType(headers.get(RequestStreamReader.CONTENT_TYPE));
    upResource.setStoreLocation(uploadLocation_ + "/" + uploadId + "." + fileName);
    upResource.setEstimatedSize(contentLength);
    File fileStore = new File(upResource.getStoreLocation());
    if (!fileStore.exists() && fileStore.createNewFile()) {
      fileStore.deleteOnExit();
    } else {
      throw new IllegalStateException("Wasn't able to create a file with uploadId " + uploadId);
    }
    FileOutputStream output = new FileOutputStream(fileStore);
    reader.readBodyData(inputStream, contentType, output);

    if (upResource.getStatus() == UploadResource.UPLOADING_STATUS) {
      upResource.setStatus(UploadResource.UPLOADED_STATUS);
      return;
    }

    uploadResources.remove(uploadId);
    fileStore.delete();
  }

  @SuppressWarnings("unchecked")
  private void putToStackInSession(HttpSession session, String uploadId) {
    Set<String> uploadResouceIds = (Set<String>) session.getAttribute(UploadService.UPLOAD_RESOURCES_STACK);
    if (uploadResouceIds == null) {
      uploadResouceIds = new HashSet<>();
    }
    uploadResouceIds.add(uploadId);
    session.setAttribute(UploadService.UPLOAD_RESOURCES_STACK, uploadResouceIds);
  }

  /**
   * Get UploadResource by uploadId
   *
   * @param uploadId uploadId of UploadResource
   * @return org.exoplatform.upload.UploadResource of uploadId
   */
  public UploadResource getUploadResource(String uploadId) {
    return uploadResources.get(uploadId);
  }

  /**
   * Clean up temporary files that are uploaded in the Session but not removed
   * yet
   *
   * @param session
   */
  public void cleanUp(HttpSession session) {
    log.debug("Cleaning up uploaded files for temporariness");
    @SuppressWarnings("unchecked")
    Set<String> uploadIds = (Set<String>) session.getAttribute(UploadService.UPLOAD_RESOURCES_STACK);
    if (uploadIds != null) {
      for (String id : uploadIds) {
        removeUploadResource(id);
        uploadLimits.remove(id);
      }
    }
  }

  /**
   * @deprecated use {@link #removeUploadResource(String)} instead
   * @param uploadId
   */
  @Deprecated
  public void removeUpload(String uploadId) {
    removeUploadResource(uploadId);
  }

  /**
   * Remove the UploadResource and its temporary file that associated with given
   * <code>uploadId</code>. <br>
   * If <code>uploadId</code> is null or UploadResource is null, do nothing
   *
   * @param uploadId uploadId of UploadResource will be removed
   */
  public void removeUploadResource(String uploadId) {
    if (uploadId == null)
      return;
    UploadResource upResource = uploadResources.get(uploadId);
    if (upResource != null) {
      uploadResources.remove(uploadId);

      if (upResource.getStoreLocation() != null) {
        File file = new File(upResource.getStoreLocation());
        if (!file.delete()) {
          file.deleteOnExit();
        }
      }
    }
  }

  /**
   * Registry upload limit size for uploadLimitsMB_. If limitMB is null,
   * defaultUploadLimitMB_ will be registried
   *
   * @param uploadId
   * @param limitMB upload limit size
   */
  public void addUploadLimit(String uploadId, Integer limitMB) {
    addUploadLimit(uploadId, limitMB, UploadUnit.MB);
  }

  public void addUploadLimit(String uploadId, Integer limit, UploadUnit unit) {
    if (limit == null) {
      uploadLimits.put(uploadId, defaultUploadLimitMB_);
    } else if (unit == null) {
      uploadLimits.put(uploadId, new UploadLimit(limit, UploadUnit.MB));
    } else {
      uploadLimits.put(uploadId, new UploadLimit(limit, unit));
    }
  }

  public void removeUploadLimit(String uploadId) {
    uploadLimits.remove(uploadId);
  }

  /**
   * Get all upload limit sizes
   *
   * @return all upload limit sizes
   */
  public Map<String, UploadLimit> getUploadLimits() {
    return uploadLimits;
  }

  public String correctFileName(String fileName) {
    if (fileName == null || fileName.isEmpty())
      return "NULL";

    char[] chars = fileName.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      if (Arrays.binarySearch(illegalChars, chars[i]) >= 0) {
        chars[i] = '_';
      }
    }

    return new String(chars);
  }

  public boolean isLimited(UploadResource upResource, double contentLength) {
    // by default, use the limit set in the service
    UploadLimit limit = defaultUploadLimitMB_;
    // if the limit is set in the request (specific for this upload) then use
    // this value instead of the default one
    if (uploadLimits.containsKey(upResource.getUploadId())) {
      limit = uploadLimits.get(upResource.getUploadId());
    }

    double estimatedSize = contentLength / limit.division;
    if (limit.getLimit() > 0 && estimatedSize > limit.getLimit()) { // a limit
                                                                    // set to 0
                                                                    // means
                                                                    // unlimited
      if (log.isDebugEnabled()) {
        log.debug("Upload cancelled because file bigger than size limit : " + estimatedSize + " " + limit.unit + " > "
            + limit.getLimit() + " " + limit.unit);
      }
      return true;
    }
    return false;
  }

  public UploadLimit getLimitForResource(UploadResource upResource) {
    // by default, use the limit set in the service
    UploadLimit limit = defaultUploadLimitMB_;
    // if the limit is set in the request (specific for this upload) then use
    // this value instead of the default one
    if (uploadLimits.containsKey(upResource.getUploadId())) {
      limit = uploadLimits.get(upResource.getUploadId());
    }
    return limit;
  }

  private DiskFileItem getFileItem(HttpServletRequest request, UploadResource upResource) throws FileUploadException {
    List<DiskFileItem> itemList = new ArrayList<>();
    // Create a factory for disk-based file items
    DiskFileItemFactory factory = DiskFileItemFactory.builder()
                                                     .setBufferSize(0)
                                                     .setFile(uploadLocation_)
                                                     .setCharset(StandardCharsets.UTF_8)
                                                     .get();
    // Create a new file upload handler
    JakartaServletDiskFileUpload servletUpload = new JakartaServletDiskFileUpload(factory);
    servletUpload.setHeaderCharset(StandardCharsets.UTF_8);
    ProgressListener listener = (bytesRead, contentLength, items) -> {
      if (bytesRead == contentLength) {
        if (itemList.size() == 1) {
          writeFile(upResource, itemList.get(items));
        }
        upResource.addUploadedBytes(contentLength - upResource.getUploadedSize());
        upResource.setStatus(UploadResource.UPLOADED_STATUS);
      } else {
        upResource.addUploadedBytes(bytesRead - upResource.getUploadedSize());
      }
    };
    servletUpload.setProgressListener(listener);
    List<DiskFileItem> fileItems = servletUpload.parseRequest(request);
    if (fileItems == null || fileItems.size() != 1 || fileItems.get(0).isFormField()) {
      removeUploadResource(upResource.getUploadId());
      throw new FileUploadException("You can upload 1 file per request");
    }
    DiskFileItem fileItem = fileItems.get(0);
    itemList.add(fileItem);
    return fileItem;
  }

  private void writeFile(UploadResource upResource, DiskFileItem diskFileItem) {
    DiskFileItem fileItem = null;
    try {
      fileItem = diskFileItem.write(Paths.get(upResource.getStoreLocation()));
    } catch (IOException e) {
      throw new IllegalStateException("Upload id " + upResource.getUploadId() + " can't be uploaded", e);
    } finally {
      if (fileItem != null
          && fileItem.getPath() != null) {
        fileItem.getPath().toFile().deleteOnExit();
      }
    }
  }

  public static class UploadLimit {
    private int        limit;

    private int        division;

    private UploadUnit unit;

    public UploadLimit(int limit, UploadUnit unit) {
      this.limit = limit;
      this.unit = unit;
      if (unit == UploadUnit.KB) {
        division = 1024;
      } else if (unit == UploadUnit.MB) {
        division = 1024 * 1024;
      } else if (unit == UploadUnit.GB) {
        division = 1024 * 1024 * 1024;
      }
    }

    public int getLimit() {
      return limit;
    }

    public String getUnit() {
      return unit.toString();
    }
  }
}
