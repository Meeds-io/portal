/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.spring.web.error;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * Error body attributes of the platform's Spring MVC error page. The
 * {@code message} attribute, included by
 * {@code spring.web.error.include-message=always}, is kept for a client error
 * only: a 4xx refusal's reason is the message code the client translates,
 * while a server error's message is an unexpected exception's own text (SQL,
 * file paths...) that must not reach the browser.
 */
@Component
public class PortalErrorAttributes extends DefaultErrorAttributes {

  private static final String MESSAGE_ATTRIBUTE = "message";

  private static final String STATUS_ATTRIBUTE  = "status";

  @Override
  public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
    Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
    if (!(errorAttributes.get(STATUS_ATTRIBUTE) instanceof Integer status) || status >= 500) {
      errorAttributes.remove(MESSAGE_ATTRIBUTE);
    }
    return errorAttributes;
  }

}
