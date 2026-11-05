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
package org.exoplatform.upload;

import org.apache.commons.fileupload2.core.FileUploadException;

/**
 * A refusal of an uploaded file by an {@link UploadFileValidator}, as
 * {@link UploadService} rethrows it. A validator may refuse with a
 * {@link FileUploadException} whose cause is an {@link java.io.IOException} (an
 * unreadable compressed SVG, a file too large to inspect), the shape the
 * multipart parser gives a failed staging write: this type tells a caller that
 * the request was refused, not that the server failed.
 */
public class UploadRefusedException extends FileUploadException {

  private static final long serialVersionUID = 4790385310727405816L;

  /**
   * @param refusal the validator's refusal, whose message is kept
   */
  public UploadRefusedException(FileUploadException refusal) {
    super(refusal.getMessage(), refusal);
  }
}
