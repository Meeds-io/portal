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

import org.exoplatform.commons.serialization.api.annotations.Serialized;

/**
 * Created by The eXo Platform SAS Author : Tan Pham Dinh pdtanit@gmail.com Oct 30, 2008
 */
@Serialized
public class URLValidator extends ExpressionValidator {

    private static final String IP_REGEX = "(((((25[0-5])|(2[0-4][0-9])|([01]?[0-9]?[0-9]))\\.){3}((25[0-4])|(2[0-4][0-9])|((1?[1-9]?[1-9])|([1-9]0))))|(0\\.){3}0)";

    public static final String URL_REGEX = "^((ht|f)tp(s?)://)" // protocol
            + "(\\w+(:\\w+)?@)?" // username:password@
            + "(" + IP_REGEX // ip
            + "|([0-9a-z_!~*'()-]+\\.)*([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\.[a-z]{2,6}" // domain like www.exoplatform.org
            + "|([a-zA-Z][-a-zA-Z0-9]+))" // domain like localhost
            + "(:[0-9]{1,5})?" // port number :8080
            + "((/?)|(/[0-9a-zA-Z_!~*'().;?:@&=+$,%#-]+)+/?)$"; // uri
    private static final String DEFAULT_KEY = "URLValidator.msg.invalid-url";

    public URLValidator() {
        this(DEFAULT_KEY);
    }

    public URLValidator(String key) {
        super(URL_REGEX, key != null ? key : DEFAULT_KEY);
    }
}
