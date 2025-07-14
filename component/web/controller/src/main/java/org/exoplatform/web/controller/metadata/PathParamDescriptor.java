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
package org.exoplatform.web.controller.metadata;

import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.EncodingMode;

public class PathParamDescriptor extends ParamDescriptor {

    /** . */
    private String pattern;

    /** . */
    private EncodingMode encodingMode;

    /** . */
    private boolean captureGroup;

    public PathParamDescriptor(QualifiedName qualifiedName) {
        super(qualifiedName);

        //
        this.encodingMode = EncodingMode.FORM;
        this.captureGroup = false;
    }

    public PathParamDescriptor(String qualifiedName) {
        super(qualifiedName);

        //
        this.encodingMode = EncodingMode.FORM;
    }

    public PathParamDescriptor matchedBy(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public PathParamDescriptor encodedBy(EncodingMode encodingMode) {
        this.encodingMode = encodingMode;
        return this;
    }

    public PathParamDescriptor captureGroup(boolean capture) {
        this.captureGroup = capture;
        return this;
    }

    public PathParamDescriptor preservePath() {
        return encodedBy(EncodingMode.PRESERVE_PATH);
    }

    public PathParamDescriptor form() {
        return encodedBy(EncodingMode.FORM);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public EncodingMode getEncodingMode() {
        return encodingMode;
    }

    public void setEncodingMode(EncodingMode encodingMode) {
        this.encodingMode = encodingMode;
    }

    public boolean getCaptureGroup() {
        return captureGroup;
    }

    public void setCaptureGroup(boolean captureGroup) {
        this.captureGroup = captureGroup;
    }
}
