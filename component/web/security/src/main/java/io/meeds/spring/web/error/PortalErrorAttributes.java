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
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.RequestDispatcher;

/**
 * Error body attributes of the platform's Spring MVC error page. The
 * {@code message} attribute, included by
 * {@code spring.web.error.include-message=always}, is kept only for a client
 * error raised with an explicit reason: a {@code ResponseStatusException}
 * reason or a {@code sendError} message, the message code the client
 * translates. Any other message is an exception's own text (a framework 4xx's
 * handler signature or parser error, a 5xx's SQL or file path) and must not
 * reach the browser. An error whose status is not among the attributes keeps
 * no message either.
 */
@Component
public class PortalErrorAttributes extends DefaultErrorAttributes {

  private static final String MESSAGE_ATTRIBUTE = "message";

  private static final String STATUS_ATTRIBUTE  = "status";

  @Override
  public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
    Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
    if (!isClientErrorWithReason(webRequest, errorAttributes)) {
      errorAttributes.remove(MESSAGE_ATTRIBUTE);
    }
    return errorAttributes;
  }

  private boolean isClientErrorWithReason(WebRequest webRequest, Map<String, Object> errorAttributes) {
    return errorAttributes.get(STATUS_ATTRIBUTE) instanceof Integer status
           && status >= 400
           && status < 500
           && !ObjectUtils.isEmpty(webRequest.getAttribute(RequestDispatcher.ERROR_MESSAGE, RequestAttributes.SCOPE_REQUEST));
  }

}
