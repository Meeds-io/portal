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

import org.exoplatform.web.application.CompoundApplicationMessage;
import org.exoplatform.webui.form.UIFormInput;

import java.io.Serializable;

/**
 * Created by The eXo Platform SARL Author : Walid Khessairi wkhessairi@exoplatform.com Jan 02, 2017
 *
 * Validates whether this double is in a correct format
 */
public class DoubleFormatValidator extends MultipleConditionsValidator implements Serializable {
  @Override
  protected void validate(String value, String label, CompoundApplicationMessage messages, UIFormInput uiInput) {
    validateDouble(value, label, messages);
  }

  protected Double validateDouble(String value, String label, CompoundApplicationMessage messages) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      Object[] args = { label };
      messages.addMessage("NumberFormatValidator.msg.Invalid-number", args);
      return null;
    }
  }

}
