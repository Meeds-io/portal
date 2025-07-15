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
package org.exoplatform.webui.form.validator;

import java.io.Serializable;

import org.exoplatform.web.application.CompoundApplicationMessage;

/**
 * Created by The eXo Platform SARL Author : Vu Duy Tu duytucntt@gmail.com Jun 22, 2007
 *
 * Validates whether this number is positive or 0
 */

public class PositiveNumberFormatValidator extends NumberFormatValidator implements Serializable {

    @Override
    protected Integer validateInteger(String value, String label, CompoundApplicationMessage messages) {
        Integer integer = super.validateInteger(value, label, messages);

        if (integer == null) {
            return null;
        } else if (integer < 0) {
            messages.addMessage("PositiveNumberFormatValidator.msg.Invalid-number", new Object[] { label });
            return null;
        } else {
            return integer;
        }
    }
}
