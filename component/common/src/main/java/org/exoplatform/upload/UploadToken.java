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

/**
 * What a one-time upload token authorizes, as {@link UploadService} stores it:
 * the token value itself is never stored, only its digest, which keys the
 * entry.
 *
 * @param username user the upload is made for, recorded as the owner of the
 *          resulting {@link UploadResource}
 * @param uploadId the only upload id the token can register
 * @param maxSize maximum size of the uploaded file, in bytes
 * @param expirationTime time after which the token is refused, in epoch
 *          milliseconds
 */
public record UploadToken(String username, String uploadId, long maxSize, long expirationTime) {

  /**
   * Tells whether the token can no longer be used.
   *
   * @param now current time in epoch milliseconds
   * @return true when {@code now} is at or past the expiration time
   */
  public boolean isExpired(long now) {
    return now >= expirationTime;
  }

}
