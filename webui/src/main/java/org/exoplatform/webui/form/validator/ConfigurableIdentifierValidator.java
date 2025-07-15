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
import java.util.regex.Pattern;

import org.exoplatform.web.application.CompoundApplicationMessage;
import org.exoplatform.webui.form.UIFormInput;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minhdv81@yahoo.com Jun 7, 2006
 * <br>
 * Validates whether the value is composed of letters, digit , '_' or '-'h First character could not be digit or '-'
 */
public class ConfigurableIdentifierValidator extends MultipleConditionsValidator implements Serializable {
    protected static int DEFAULT_MIN_LENGTH = 3;
    protected static int DEFAULT_MAX_LENGTH = 30;

    protected Integer min = DEFAULT_MIN_LENGTH;
    protected Integer max = DEFAULT_MAX_LENGTH;

    protected static String IDENTIFER_VALIDATOR_REGEX = "^[\\p{L}_][\\p{L}\\d_-]*$";

    public ConfigurableIdentifierValidator() {
    }

    public ConfigurableIdentifierValidator(int min, int max) {
        this.min = min;
        this.max = max;
    }

    static void validate(String value, String label, CompoundApplicationMessage messages, UIFormInput uiInput, Integer min, Integer max) {
        char[] buff = value.toCharArray();
        if (buff.length < min || buff.length > max) {
            messages.addMessage("StringLengthValidator.msg.length-invalid",
                    new Object[]{label, min.toString(), max.toString()});
        }

        if (Character.isDigit(value.charAt(0)) || value.charAt(0) == '-') {
            messages.addMessage("FirstAndSpecialCharacterNameValidator.msg", new Object[]{label, uiInput.getBindingField()});
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_' && c != '-') {
                messages.addMessage("IdentifierValidator.msg.Invalid-char", new Object[]{label});
                break;
            }
        }
    }

    @Override
    protected void validate(String value, String label, CompoundApplicationMessage messages, UIFormInput uiInput) {
        validate(value, label, messages, uiInput, min, max);
    }

}