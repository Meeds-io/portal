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
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;

public abstract class MultipleConditionsValidator extends AbstractValidator implements Serializable {
    public void validate(UIFormInput uiInput) throws Exception {
        String value = trimmedValueOrNullIfBypassed((String) uiInput.getValue(), uiInput, exceptionOnMissingMandatory,
                trimValue);
        if (value == null) {
            return;
        }

        String label = getLabelFor(uiInput);

        CompoundApplicationMessage messages = new CompoundApplicationMessage();

        validate(value, label, messages, uiInput);

        if (!messages.isEmpty()) {
            throw new MessageException(messages);
        }
    }

    protected abstract void validate(String value, String label, CompoundApplicationMessage messages, UIFormInput uiInput);

    @Override
    protected String getMessageLocalizationKey() {
        throw new UnsupportedOperationException("Unneeded by this implementation");
    }

    @Override
    protected boolean isValid(String value, UIFormInput uiInput) {
        throw new UnsupportedOperationException("Unneeded by this implementation");
    }
}
