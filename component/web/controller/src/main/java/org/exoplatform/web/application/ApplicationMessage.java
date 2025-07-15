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
import java.util.Arrays;


public class ApplicationMessage extends AbstractApplicationMessage implements Serializable {
    private final String messageKey_;
    private final Object[] messageArgs_;

    public ApplicationMessage(String key, Object[] args) {
        this.messageKey_ = key;
        this.messageArgs_ = args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ApplicationMessage that = (ApplicationMessage) o;

        if (!Arrays.equals(messageArgs_, that.messageArgs_)) {
            return false;
        }
        if (!messageKey_.equals(that.messageKey_)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = messageKey_.hashCode();
        result = 31 * result + (messageArgs_ != null ? Arrays.hashCode(messageArgs_) : 0);
        return result;
    }

    public ApplicationMessage(String key, Object[] args, int type) {
        this.messageKey_ = key;
        this.messageArgs_ = args;
        setType(type);
    }

    public String getMessageKey() {
        return messageKey_;
    }

    public String getMessage() {
        String msg = resolveMessage(messageKey_);
        if (msg != null && messageArgs_ != null) {
            for (int i = 0; i < messageArgs_.length; i++) {
                final Object messageArg = messageArgs_[i];
                if (messageArg != null) {
                    String arg = messageArg.toString();
                    if (isArgsLocalized()) {
                        arg = resolveMessage(arg);
                    }
                    msg = msg.replace("{" + i + "}", arg);
                }
            }
        }

        return msg;
    }
}
