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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;
import org.apache.commons.fileupload2.core.ProgressListener;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class UploadService {
  /**
   * Bytes a token upload's request body may carry above the file size the token
   * declares: the multipart boundaries and part headers.
   */
  public static final long            UPLOAD_TOKEN_MULTIPART_ALLOWANCE      = 16L * 1024;

  /** Validity of an upload token when the service configuration sets none. */
  public static final long            DEFAULT_UPLOAD_TOKEN_VALIDITY_SECONDS = 300;

  /** Message of a refused, expired, already used or mismatched upload token. */
  public static final String          UPLOAD_TOKEN_INVALID                  = "upload.token.invalid";

  /** Message of an upload id that is already registered. */
  public static final String          UPLOAD_ID_ALREADY_IN_USE              = "upload.id.alreadyInUse";

  /** . */
  private static final Log            log                    = ExoLogger.getLogger(UploadService.class);

  private static final String         UPLOAD_TOKEN_VALIDITY_PARAM           = "upload.token.validity.seconds";

  private static final String         UPLOAD_LOCATION_PARAM                 = "upload.location";

  private static final int            UPLOAD_TOKEN_BYTE_LENGTH              = 32;

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

  private List<UploadFileValidator>   uploadFileValidators;

  /**
   * Upload resources by upload id. Synchronized because request threads write
   * it concurrently; it keeps the null-key and insertion-order behaviour of a
   * {@link LinkedHashMap}. Iterating over it requires holding its monitor.
   */
  private Map<String, UploadResource> uploadResources        = Collections.synchronizedMap(new LinkedHashMap<>());

  private String                      uploadLocation_;

  private UploadLimit                 defaultUploadLimitMB_;

  /**
   * Per-upload limits by upload id, synchronized for the same reason as
   * {@link #uploadResources}.
   */
  private Map<String, UploadLimit>    uploadLimits           = Collections.synchronizedMap(new LinkedHashMap<>());

  /** Pending upload tokens, keyed by the SHA-256 digest of the token value. */
  private final Map<String, UploadToken> uploadTokens       = new ConcurrentHashMap<>();

  private final SecureRandom          secureRandom           = new SecureRandom();

  private long                        uploadTokenValidityMillis = DEFAULT_UPLOAD_TOKEN_VALIDITY_SECONDS * 1000;

  public static final String          UPLOAD_RESOURCES_STACK = "uploadResourcesStack";

  public static enum UploadUnit {
    KB, MB, GB
  };

  /**
   * Builds the service from its parameters: {@code upload.limit.size} (default
   * limit in MB, 0 or absent for unlimited), {@code upload.location} (staging
   * root, see {@link #resolveUploadLocation(InitParams, String)}) and
   * {@code upload.token.validity.seconds}.
   *
   * @param pinfo the portal container, whose name separates the staging folders
   *          of several containers
   * @param params the service parameters, may be null
   * @throws Exception when a numeric parameter is not a number
   */
  public UploadService(PortalContainerInfo pinfo, InitParams params) throws Exception {
    if (params == null || params.getValueParam("upload.limit.size") == null)
      defaultUploadLimitMB_ = new UploadLimit(0, UploadUnit.MB); // 0 means
                                                                 // unlimited
    else
      defaultUploadLimitMB_ = new UploadLimit(Integer.parseInt(params.getValueParam("upload.limit.size").getValue()),
                                              UploadUnit.MB);
    uploadLocation_ = resolveUploadLocation(params, pinfo.getContainerName());
    if (params != null && params.getValueParam(UPLOAD_TOKEN_VALIDITY_PARAM) != null) {
      uploadTokenValidityMillis = Long.parseLong(params.getValueParam(UPLOAD_TOKEN_VALIDITY_PARAM).getValue().trim()) * 1000;
    }
  }

  /**
   * Resolves the folder uploads are staged in: {@code <root>/<container
   * name>/eXoUpload}, where the root is the {@code upload.location} parameter
   * when it is set and not blank, else {@code java.io.tmpdir}. The container
   * name and {@code eXoUpload} are kept under a configured root too, so several
   * portal containers sharing one root, or one volume shared by several nodes,
   * never mix their files, and the layout is the same whatever the root. The
   * folder is created at the first upload; a root that exists but is not a
   * writable folder, or whose nearest existing parent is not writable, is
   * logged as an error here, and uploads then fail when they write.
   *
   * @param params the service parameters, may be null
   * @param containerName the portal container name
   * @return the staging folder path
   */
  private static String resolveUploadLocation(InitParams params, String containerName) {
    String root = System.getProperty("java.io.tmpdir");
    ValueParam locationParam = params == null ? null : params.getValueParam(UPLOAD_LOCATION_PARAM);
    if (locationParam != null && locationParam.getValue() != null && !locationParam.getValue().isBlank()) {
      root = locationParam.getValue().trim();
      File existing = new File(root).getAbsoluteFile();
      while (existing != null && !existing.exists()) {
        existing = existing.getParentFile();
      }
      if (existing == null || !existing.isDirectory() || !existing.canWrite()) {
        log.error("Upload staging root '{}' set by '{}' (exo.upload.location) cannot be used: '{}' is not a writable folder. Uploads will fail until it is fixed.",
                  root,
                  UPLOAD_LOCATION_PARAM,
                  existing);
      }
    }
    return root + "/" + containerName + "/eXoUpload";
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

  /**
   * Registers a resource built server side, as given: no
   * {@link UploadFileValidator} runs, and the resource's owner is whatever the
   * caller set on it ({@link UploadResource#setOwner(String)}).
   *
   * @param uploadResource the resource to register under its upload id
   */
  public void createUploadResource(UploadResource uploadResource) {
    uploadResources.put(uploadResource.getUploadId(), uploadResource);
  }

  /**
   * Registers the file of a multipart request under the given upload id, owned
   * by the request's authenticated user ({@link HttpServletRequest#getRemoteUser()},
   * null for an anonymous request).
   *
   * @param uploadId upload id
   * @param request the request carrying one multipart file
   * @throws FileUploadException when the id is invalid, the request carries
   *           other than one file, or a validator refuses the file
   */
  public void createUploadResource(String uploadId, HttpServletRequest request) throws FileUploadException {
    createUploadResource(uploadId, request, request.getRemoteUser());
  }

  /**
   * Issues a one-time token that lets a request without a user session upload
   * one file through the upload handler, as the given user. The token is
   * returned once and never stored: the service keeps its SHA-256 digest with
   * the user, the upload id, the maximum size and the expiry. It is consumed by
   * its first presentation, whatever the outcome. The token's size replaces the
   * platform default upload limit for its upload id, so the caller caps it.
   *
   * @param username user the upload is made for, recorded as the resource's
   *          owner
   * @param uploadId the only upload id the token can register; the caller
   *          generates it unguessable (a random UUID)
   * @param maxSize maximum size of the uploaded file, in bytes
   * @return the token value, to send as {@code Authorization: Bearer <token>}
   * @throws IllegalArgumentException when the username is blank, the upload id
   *           is invalid or already registered, or the size is not positive
   */
  public String createUploadToken(String username, String uploadId, long maxSize) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("upload.token.username.mandatory");
    }
    if (uploadId == null || uploadId.isBlank() || !UPLOAD_ID_PATTERN.matcher(uploadId).matches()) {
      throw new IllegalArgumentException("upload.token.uploadId.invalid");
    }
    if (maxSize <= 0) {
      throw new IllegalArgumentException("upload.token.maxSize.invalid");
    }
    if (uploadResources.containsKey(uploadId)) {
      throw new IllegalArgumentException(UPLOAD_ID_ALREADY_IN_USE);
    }
    long now = System.currentTimeMillis();
    uploadTokens.values().removeIf(token -> token.isExpired(now));

    byte[] randomBytes = new byte[UPLOAD_TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(randomBytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    uploadTokens.put(digest(token), new UploadToken(username, uploadId, maxSize, now + uploadTokenValidityMillis));
    return token;
  }

  /**
   * Registers the file of a multipart request authorized by a one-time upload
   * token instead of a user session. The token is consumed first, whatever the
   * outcome; the request must then name the token's upload id, carry a
   * {@code Content-Length} within the token's size plus
   * {@link #UPLOAD_TOKEN_MULTIPART_ALLOWANCE}, and hold a file no larger than
   * the token's size. The file goes through the same parsing and
   * {@link UploadFileValidator}s as a browser upload, is owned by the token's
   * user, and is tied to the request's HTTP session for cleanup. The upload id
   * is reserved atomically: of two concurrent token uploads of one id, exactly
   * one registers it and the other gets an {@link ObjectAlreadyExistsException}.
   * Any refusal after registration removes this upload's resource only.
   *
   * @param token the token value, as issued by
   *          {@link #createUploadToken(String, String, long)}
   * @param uploadId the upload id the request names
   * @param request the request carrying one multipart file
   * @return the registered resource, in {@link UploadResource#UPLOADED_STATUS}
   * @throws IllegalAccessException when the token is unknown, expired, already
   *           used, or issued for another upload id
   * @throws ObjectAlreadyExistsException when the upload id is already
   *           registered
   * @throws FileUploadSizeException when the request has no
   *           {@code Content-Length} (actual size -1) or exceeds the token's
   *           size
   * @throws FileUploadException when the request is not one multipart file, is
   *           incomplete, or a validator refuses the file
   */
  public UploadResource createUploadResourceWithToken(String token,
                                                      String uploadId,
                                                      HttpServletRequest request) throws IllegalAccessException,
                                                                                  ObjectAlreadyExistsException,
                                                                                  FileUploadException {
    UploadToken uploadToken = consumeUploadToken(token, uploadId);
    long permittedLength = uploadToken.maxSize() + UPLOAD_TOKEN_MULTIPART_ALLOWANCE;
    long contentLength = request.getContentLengthLong();
    if (contentLength < 0) {
      throw new FileUploadSizeException("Content-Length is required for a token upload", permittedLength, contentLength);
    }
    if (contentLength > permittedLength) {
      throw new FileUploadSizeException("The request exceeds the size declared for this upload token",
                                        permittedLength,
                                        contentLength);
    }
    UploadResource upResource = newUploadResource(uploadId, uploadToken.username());
    if (uploadResources.putIfAbsent(uploadId, upResource) != null) {
      throw new ObjectAlreadyExistsException(uploadId, UPLOAD_ID_ALREADY_IN_USE);
    }

    addUploadLimit(uploadId, (int) Math.min(Integer.MAX_VALUE, (permittedLength + 1023) / 1024), UploadUnit.KB);
    boolean registered = false;
    try {
      receiveUpload(upResource, request);
      if (uploadResources.get(uploadId) != upResource || upResource.getStatus() != UploadResource.UPLOADED_STATUS) {
        throw new FileUploadException("The upload did not complete");
      }
      long fileSize = new File(upResource.getStoreLocation()).length();
      if (fileSize > uploadToken.maxSize()) {
        throw new FileUploadSizeException("The file exceeds the size declared for this upload token",
                                          uploadToken.maxSize(),
                                          fileSize);
      }
      registered = true;
      return upResource;
    } finally {
      removeUploadLimit(uploadId);
      if (!registered && uploadResources.remove(uploadId, upResource)) {
        deleteStoredFile(upResource);
      }
    }
  }

  /**
   * Registers the file of a multipart request under the given upload id, owned
   * by the given user, replacing any resource already registered under it, and
   * ties it to the request's HTTP session for cleanup.
   *
   * @param uploadId upload id
   * @param request the request carrying one multipart file
   * @param owner the user recorded as the resource's owner, null for none
   * @throws FileUploadException when the id is invalid, the request carries
   *           other than one file, or a validator refuses the file
   */
  private void createUploadResource(String uploadId, HttpServletRequest request, String owner) throws FileUploadException {
    UploadResource upResource = newUploadResource(uploadId, owner);
    uploadResources.put(upResource.getUploadId(), upResource);
    receiveUpload(upResource, request);
  }

  /**
   * Builds the resource a multipart request is stored in, before it is
   * registered.
   *
   * @param uploadId upload id
   * @param owner the user recorded as the resource's owner, null for none
   * @return the resource, in {@link UploadResource#UPLOADING_STATUS}
   * @throws FileUploadException when the id is null or contains '.', '/' or
   *           '\\'
   */
  private UploadResource newUploadResource(String uploadId, String owner) throws FileUploadException {
    if (uploadId == null || !UPLOAD_ID_PATTERN.matcher(uploadId).matches()) {
      throw new FileUploadException("Upload id " + uploadId + " is not valid, it cannot be null or contain '.' , '/' or '\\'");
    }

    UploadResource upResource = new UploadResource(uploadId);
    upResource.setFileName("");// Avoid NPE in UploadHandler
    upResource.setOwner(owner);
    upResource.setStoreLocation(uploadLocation_ + File.separator + uploadId);
    return upResource;
  }

  /**
   * Receives the file of a multipart request into a registered resource: ties
   * the upload id to the request's HTTP session for cleanup, applies the size
   * limit, streams the file to disk and runs the {@link UploadFileValidator}s.
   *
   * @param upResource the registered resource
   * @param request the request carrying one multipart file
   * @throws FileUploadException when the request carries other than one file,
   *           or a validator refuses the file
   */
  private void receiveUpload(UploadResource upResource, HttpServletRequest request) throws FileUploadException {
    String uploadId = upResource.getUploadId();
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
    applyMimeTypeUploadPlugins(upResource, fileItem.getName());
    if (upResource.getStatus() == UploadResource.UPLOADED_STATUS) {
      writeFile(upResource, fileItem);
      validateUploadedFile(upResource);
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
    applyMimeTypeUploadPlugins(upResource, fileName);
    upResource.setStoreLocation(uploadLocation_ + "/" + uploadId + "." + fileName);
    upResource.setEstimatedSize(contentLength);
    File fileStore = new File(upResource.getStoreLocation());
    if (!fileStore.exists() && fileStore.createNewFile()) {
      fileStore.deleteOnExit();
    } else {
      throw new IllegalStateException("Wasn't able to create a file with uploadId " + uploadId);
    }
    try (FileOutputStream output = new FileOutputStream(fileStore)) {
      reader.readBodyData(inputStream, contentType, output);
    }

    if (upResource.getStatus() == UploadResource.UPLOADING_STATUS) {
      validateUploadedFile(upResource);
      upResource.setStatus(UploadResource.UPLOADED_STATUS);
      return;
    }

    uploadResources.remove(uploadId);
    fileStore.delete();
  }

  /**
   * Consumes a one-time upload token: removes it from the pending tokens on its
   * first presentation, then checks its expiry and upload id. The lookup is by
   * the token's digest and the upload id comparison runs in constant time.
   *
   * @param token the token value presented
   * @param uploadId the upload id the request names
   * @return what the token authorizes
   * @throws IllegalAccessException when the token is blank, unknown, expired,
   *           already used, or issued for another upload id
   */
  private UploadToken consumeUploadToken(String token, String uploadId) throws IllegalAccessException {
    if (token == null || token.isBlank()) {
      throw new IllegalAccessException(UPLOAD_TOKEN_INVALID);
    }
    UploadToken uploadToken = uploadTokens.remove(digest(token));
    if (uploadToken == null
        || uploadToken.isExpired(System.currentTimeMillis())
        || uploadId == null
        || !MessageDigest.isEqual(uploadToken.uploadId().getBytes(StandardCharsets.UTF_8),
                                  uploadId.getBytes(StandardCharsets.UTF_8))) {
      throw new IllegalAccessException(UPLOAD_TOKEN_INVALID);
    }
    return uploadToken;
  }

  /**
   * Computes the key an upload token is stored under.
   *
   * @param token the token value
   * @return the hexadecimal SHA-256 digest of the token
   */
  private static String digest(String token) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  /**
   * Deletes the temporary file of a removed resource, or schedules its deletion
   * at JVM exit when it cannot be deleted now.
   *
   * @param upResource the removed resource
   */
  private void deleteStoredFile(UploadResource upResource) {
    if (upResource.getStoreLocation() != null) {
      File file = new File(upResource.getStoreLocation());
      if (!file.delete()) {
        file.deleteOnExit();
      }
    }
  }

  private void applyMimeTypeUploadPlugins(UploadResource upResource, String fileName) {
    if (plugins == null) {
      return;
    }
    for (MimeTypeUploadPlugin plugin : plugins) {
      String mimeType = plugin.getMimeType(fileName);
      if (mimeType != null)
        upResource.setMimeType(mimeType);
    }
  }

  private void validateUploadedFile(UploadResource upResource) throws FileUploadException {
    if (getUploadFileValidators().isEmpty()
        || upResource == null
        || upResource.getStoreLocation() == null) {
      return;
    }

    for (UploadFileValidator uploadFileValidator : getUploadFileValidators()) {
      if (!uploadFileValidator.supports(upResource.getFileName(), upResource.getMimeType())) {
        continue;
      }
      try (InputStream inputStream = new FileInputStream(upResource.getStoreLocation())) {
        uploadFileValidator.validate(upResource.getFileName(), upResource.getMimeType(), inputStream);
      } catch (FileUploadException e) {
        upResource.setStatus(UploadResource.FAILED_STATUS);
        removeUploadResource(upResource.getUploadId());
        throw e;
      } catch (IOException e) {
        upResource.setStatus(UploadResource.FAILED_STATUS);
        removeUploadResource(upResource.getUploadId());
        throw new FileUploadException("Unable to read uploaded file for validation", e);
      }
    }
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
   * Get UploadResource by uploadId. Answers any caller, whoever uploaded the
   * resource: a consumer acting for a user prefers
   * {@link #getUploadResource(String, String)}.
   *
   * @param uploadId uploadId of UploadResource
   * @return org.exoplatform.upload.UploadResource of uploadId
   */
  public UploadResource getUploadResource(String uploadId) {
    return uploadResources.get(uploadId);
  }

  /**
   * Returns the resource registered under the upload id only when the given
   * user owns it.
   *
   * @param uploadId upload id
   * @param username the user the caller acts for
   * @return the resource, or null when the id is unknown, the resource has no
   *         owner, or another user owns it
   */
  public UploadResource getUploadResource(String uploadId, String username) {
    if (uploadId == null || username == null) {
      return null;
    }
    UploadResource upResource = uploadResources.get(uploadId);
    return upResource != null && username.equals(upResource.getOwner()) ? upResource : null;
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
   * If <code>uploadId</code> is null or UploadResource is null, do nothing.
   * Removes for any caller, whoever uploaded the resource: a consumer acting for
   * a user prefers {@link #removeUploadResource(String, String)}.
   *
   * @param uploadId uploadId of UploadResource will be removed
   */
  public void removeUploadResource(String uploadId) {
    if (uploadId == null)
      return;
    UploadResource upResource = uploadResources.remove(uploadId);
    if (upResource != null) {
      deleteStoredFile(upResource);
    }
  }

  /**
   * Removes the resource registered under the upload id, and its temporary
   * file, only when the given user owns it.
   *
   * @param uploadId upload id
   * @param username the user the caller acts for
   * @return true when the resource was removed; false when the id is unknown,
   *         the resource has no owner, or another user owns it
   */
  public boolean removeUploadResource(String uploadId, String username) {
    UploadResource upResource = getUploadResource(uploadId, username);
    if (upResource == null || !uploadResources.remove(uploadId, upResource)) {
      return false;
    }
    deleteStoredFile(upResource);
    return true;
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
   * Get all upload limit sizes. The map is synchronized: a caller iterating over
   * it holds its monitor ({@code synchronized (map)}).
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

  /**
   * Tells whether a request of the given length exceeds the limit of the
   * resource's upload: its own limit when one was set, else the service's
   * default; a limit of 0 means unlimited.
   *
   * @param upResource the resource being uploaded
   * @param contentLength the request's length in bytes
   * @return true when the length exceeds the limit
   */
  public boolean isLimited(UploadResource upResource, double contentLength) {
    // the limit set for this upload, read in one step, else the service's one
    UploadLimit limit = uploadLimits.get(upResource.getUploadId());
    if (limit == null) {
      limit = defaultUploadLimitMB_;
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

  /**
   * Returns the limit that applies to the resource's upload: its own limit when
   * one was set, else the service's default.
   *
   * @param upResource the resource
   * @return the applicable limit
   */
  public UploadLimit getLimitForResource(UploadResource upResource) {
    // the limit set for this upload, read in one step, else the service's one
    UploadLimit limit = uploadLimits.get(upResource.getUploadId());
    if (limit == null) {
      limit = defaultUploadLimitMB_;
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

  private List<UploadFileValidator> getUploadFileValidators() {
    if (uploadFileValidators == null) {
      List<UploadFileValidator> services = ExoContainerContext.getCurrentContainer()
                                                              .getComponentInstancesOfType(UploadFileValidator.class);
      if (services == null) {
        uploadFileValidators = new ArrayList<>();
      } else {
        uploadFileValidators = new ArrayList<>(services);
      }
    }
    return uploadFileValidators;
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
