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

import java.io.Serializable;


/**
 * Created by The eXo Platform SAS . Author : Tuan Nguyen tuan08@users.sourceforge.net Date: May 14, 2004 Time: 1:12:22 PM
 */
@SuppressWarnings("serial")
public class ResourceBundleDescription implements Serializable {

    protected String id_;

    protected String name_;

    protected String language_;

    protected String country_;

    protected String variant_;

    protected String resourceType_;

    public ResourceBundleDescription() {
    }

        public String getId() {
        if (id_ == null) {
            StringBuffer b = new StringBuffer();
            b.append(name_);
            if (language_ != null && !language_.isEmpty())
                b.append('_').append(language_);
            if (country_ != null && !country_.isEmpty())
                b.append('_').append(country_);
            if (variant_ != null && !variant_.isEmpty())
                b.append('_').append(variant_);
            id_ = b.toString();
        }
        return id_;
    }

    public void setId(String id) {
        id_ = id;
    }

    /**
     *
     **/
    public String getName() {
        return name_;
    }

    public void setName(String name) {
        name_ = name;
    }

    /**
     *
     **/
    public String getLanguage() {
        return language_;
    }

    public void setLanguage(String s) {
        language_ = s;
    }

    /**
     *
     **/
    public String getCountry() {
        return country_;
    }

    public void setCountry(String s) {
        country_ = s;
    }

    /**
     *
     **/
    public String getVariant() {
        return variant_;
    }

    public void setVariant(String s) {
        variant_ = s;
    }

    /**
     *
     **/
    public String getResourceType() {
        return resourceType_;
    }

    public void setResourceType(String s) {
        resourceType_ = s;
    }

    public static final String DEFAULT_LANGUAGE = "en";

}
