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

import java.io.IOException;
import java.io.InputStream;


/**
 * Created by The eXo Platform SARL Author : Tuan Nguyen tuan08@users.sourceforge.net Dec 26, 2005
 */
public abstract class DownloadResource {

    private String downloadType_;

    private String downloadName_;

    private String resourceMimeType_;

    private long liveTime_;

    private int limit_;

    private int accessCounter_;

    public DownloadResource(String resourceMimeType) {
        this(null, resourceMimeType);
    }

    public DownloadResource(String downloadType, String resourceMimeType) {
        downloadType_ = downloadType;
        resourceMimeType_ = resourceMimeType;
    }

    public String getDownloadType() {
        return downloadType_;
    }

    public String getDownloadName() {
        return downloadName_;
    }

    public void setDownloadName(String name) {
        downloadName_ = name;
    }

    public String getResourceMimeType() {
        return resourceMimeType_;
    }

    public long getLiveTime() {
        return liveTime_;
    }

    public void setLiveTime(long t) {
        liveTime_ = t;
    }

    public int getLimit() {
        return limit_;
    }

    public void setLimit(int n) {
        limit_ = n;
    }

    public int getAccessCounter() {
        return accessCounter_;
    }

    public void setAccessCounter(int c) {
        accessCounter_ = c;
    }

    public abstract InputStream getInputStream() throws IOException;

}
