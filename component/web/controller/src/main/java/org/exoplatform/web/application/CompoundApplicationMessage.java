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
package org.exoplatform.web.application;

import java.io.Serializable;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;


public class CompoundApplicationMessage extends AbstractApplicationMessage implements Serializable {
    private Set<AbstractApplicationMessage> messages = new HashSet<AbstractApplicationMessage>(5);

    public CompoundApplicationMessage() {
        this(null);
    }

    public CompoundApplicationMessage(AbstractApplicationMessage initialMessage) {
        if (initialMessage != null) {
            messages.add(initialMessage);
        }
        setType(AbstractApplicationMessage.WARNING);
    }

    @Override
    public void setResourceBundle(ResourceBundle resourceBundle) {
        super.setResourceBundle(resourceBundle);
        for (AbstractApplicationMessage message : messages) {
            message.setResourceBundle(resourceBundle);
        }
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(255);
        for (AbstractApplicationMessage message : messages) {
            sb.append(message.getMessage()).append('\n');
        }

        return sb.toString();
    }

    public void addMessage(String messageKey, Object[] args) {
        final ApplicationMessage message = new ApplicationMessage(messageKey, args, AbstractApplicationMessage.WARNING);
        message.setArgsLocalized(false);
        messages.add(message);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
