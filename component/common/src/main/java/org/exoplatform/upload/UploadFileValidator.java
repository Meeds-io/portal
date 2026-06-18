/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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

import java.io.InputStream;

import org.apache.commons.fileupload2.core.FileUploadException;

/**
 * Validates uploaded files without altering their content.
 * <p>
 * Implementations must be selective in {@link #supports(String, String)} so the
 * centralized upload service does not inspect large unrelated files such as
 * Office documents, PDFs, archives, videos, etc.
 */
public interface UploadFileValidator {

  /**
   * @param fileName uploaded file name
   * @param mimeType detected or enriched MIME type
   * @return true only when this validator must inspect the uploaded file
   */
  boolean supports(String fileName, String mimeType);

  /**
   * Validates the uploaded file in read-only mode.
   * <p>
   * The provided {@link InputStream} is owned by the caller. Implementations must
   * read from it but must not close it.
   * <p>
   * The stream is expected to be positioned at the beginning of the uploaded
   * content. When several validators support the same file, the caller should
   * provide a fresh stream to each validator.
   *
   * @param fileName uploaded file name
   * @param mimeType detected or enriched MIME type
   * @param inputStream upload file {@link InputStream}
   * @throws FileUploadException when the upload must be rejected
   */
  void validate(String fileName, String mimeType, InputStream inputStream) throws FileUploadException;
}
