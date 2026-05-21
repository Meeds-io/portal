/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.spring.web.security;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Service;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class PortletContainerWebSecurityCustomizer implements WebSecurityCustomizer {

  @Override
  public void customize(WebSecurity web) {
    web.ignoring()
       .requestMatchers(pathPattern("/tomcatgateinservlet"))
       .requestMatchers(jspOrHtmlDispatchMatcher());
  }

  private RequestMatcher jspOrHtmlDispatchMatcher() {
    RequestMatcher forward = new DispatcherTypeRequestMatcher(DispatcherType.FORWARD);
    RequestMatcher include = new DispatcherTypeRequestMatcher(DispatcherType.INCLUDE);
    return request -> (forward.matches(request) || include.matches(request)) && isJspOrHtml(request);
  }

  private boolean isJspOrHtml(HttpServletRequest request) {
    String path = dispatchPath(request);
    return path.endsWith(".jsp")
           || path.endsWith(".jspx")
           || path.endsWith(".html")
           || path.endsWith(".htm");
  }

  private String dispatchPath(HttpServletRequest request) {
    Object forwardUri = request.getAttribute("jakarta.servlet.forward.request_uri");
    if (forwardUri instanceof String uri) {
      return uri;
    }
    Object includeUri = request.getAttribute("jakarta.servlet.include.request_uri");
    if (includeUri instanceof String uri) {
      return uri;
    }
    return request.getRequestURI();
  }

}
