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

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;

public abstract class AbstractValidator implements Validator, Serializable {
    protected boolean exceptionOnMissingMandatory = false;
    protected boolean trimValue = false;

    protected String getLabelFor(UIFormInput uiInput) throws Exception {
        UIComponent uiComponent = (UIComponent) uiInput;
        UIForm uiForm = uiComponent.getAncestorOfType(UIForm.class);
        String label = uiInput.getName();
        if (uiForm != null) {
            label = uiForm.getLabel(label);
            if (label.equals(uiInput.getName()) && uiInput.getLabel() != null) {
                label = uiForm.getLabel(uiInput.getLabel());
            }
        }

        label = label.trim();

        // remove trailing ':' if there is one
        int index = label.indexOf(':');
        if (index != -1) {
            label = label.substring(0, index);
        }
        return label.trim();
    }

    public void validate(UIFormInput uiInput) throws Exception {
        String value = trimmedValueOrNullIfBypassed((String) uiInput.getValue(), uiInput, exceptionOnMissingMandatory,
                trimValue);
        if (value == null) {
            return;
        }

        if (!isValid(value, uiInput)) {
            throw createMessageException(value, uiInput);
        }
    }

    protected MessageException createMessageException(String value, UIFormInput uiInput) throws Exception {
        return createMessageException(value, uiInput, getMessageLocalizationKey());
    }

    protected MessageException createMessageException(String value, UIFormInput uiInput, String localizationKey)
            throws Exception {
        return new MessageException(new ApplicationMessage(localizationKey, getMessageArgs(value, uiInput),
                ApplicationMessage.WARNING));
    }

    protected Object[] getMessageArgs(String value, UIFormInput uiInput) throws Exception {
        return new Object[] { getLabelFor(uiInput) };
    }

    protected abstract String getMessageLocalizationKey();

    protected abstract boolean isValid(String value, UIFormInput uiInput);

    protected String trimmedValueOrNullIfBypassed(String value, UIFormInput uiInput, boolean exceptionOnMissingMandatory,
            boolean trimValue) throws Exception {
        if (value != null) {
            String tmp = value.trim();
            if (trimValue) {
                value = tmp;
            }

            value = tmp.isEmpty() ? null : value;
        }

        if (exceptionOnMissingMandatory && value == null) {
            throw createMessageException(value, uiInput, "EmptyFieldValidator.msg.empty-input");
        }

        return value;
    }
}
