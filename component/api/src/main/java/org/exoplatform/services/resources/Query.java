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
package org.exoplatform.services.resources;

/**
 * Created by The eXo Platform SAS . Author : Tuan Nguyen tuan08@users.sourceforge.net Date: Jun 14, 2003 Time: 1:12:22 PM
 */
public class Query {

    private String name_;

    private String languages_;

    private int maxSize_;

    public Query(String name, String language) {
        name_ = name;
        languages_ = language;
        maxSize_ = 100;
    }

    public String getName() {
        return name_;
    }

    public void setName(String s) {
        name_ = s;
    }

    public String getLanguage() {
        return languages_;
    }

    public void setLanguage(String s) {
        languages_ = s;
    }

    public int getMaxSize() {
        return maxSize_;
    }

    public void setMaxSize(int s) {
        maxSize_ = s;
    }
}
