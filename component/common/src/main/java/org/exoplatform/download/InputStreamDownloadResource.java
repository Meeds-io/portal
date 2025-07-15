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
package org.exoplatform.download;

import java.io.InputStream;

/**
 * Created by The eXo Platform SARL
 * Author : Tuan Nguyen
 *          tuan08@users.sourceforge.net
 * Dec 26, 2005
 *
 * @deprecated It is better to avoid using this download resource
 */
public class InputStreamDownloadResource extends DownloadResource {
    private InputStream is_;

    public InputStreamDownloadResource(InputStream is, String resourceMimeType) {
        this(null, is, resourceMimeType);
    }

    public InputStreamDownloadResource(String downloadType, InputStream is, String resourceMimeType) {
        super(downloadType, resourceMimeType);
        is_ = is;
    }

    public InputStream getInputStream() {
        return is_;
    }
}