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

import org.exoplatform.webui.form.UIFormInput;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minhdv81@yahoo.com Jun 7, 2006
 *
 * Validates whether this value is null
 */
public class NullFieldValidator extends AbstractValidator implements Serializable {

    public void validate(UIFormInput uiInput) throws Exception {
        if ((uiInput.getValue() == null)) {
            throw createMessageException(null, uiInput);
        }
    }

    @Override
    protected String getMessageLocalizationKey() {
        return "EmptyFieldValidator.msg.empty-input";
    }

    @Override
    protected boolean isValid(String value, UIFormInput uiInput) {
        throw new UnsupportedOperationException("Unneeded by this implementation");
    }
}
