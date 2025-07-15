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
package org.exoplatform.portal.application;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.web.security.sso.SSOHelper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * The JSFAjaxRedirectFilter performs a redirect to login page when the HTTP session is expired
 * and the current request contains a <i>"Faces-Request"</i> header.
 *
 */
public class JSFAjaxRedirectFilter extends AbstractFilter {
  
  private static final String DO_LOGIN_PATTERN = "login";
  
  private static final String START_AJAX_REDIRECT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response><redirect url=\"";
  
  private static  String END_AJAX_REDIRECT = "\"/></partial-response>";
  
  private static final String AJAX_JSF_HTTP_HEADER = "Faces-Request";
  
  private static final String AJAX_JSF_HTTP_HEADER_VALUE = "partial/ajax";
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {
    
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    
    if(req.getRemoteUser() == null) {
      if(isJSFAjaxRequest(req)) {
        String loginPath = buildLoginPath(req);
        writeJSFAjaxRedirect(res, loginPath);
        return;
      }
    }
    chain.doFilter(req, res);
  }
  
  @Override
  public void destroy() {
    
  }
  
  private String buildLoginPath(HttpServletRequest request) throws UnsupportedEncodingException {
    StringBuilder initialURI = new StringBuilder();
    initialURI.append(request.getRequestURI());
    if (request.getQueryString() != null) {
      initialURI.append("?").append(request.getQueryString());
    }
    
    StringBuilder loginPath = new StringBuilder();
  
    //Check SSO Enable
    SSOHelper ssoHelper = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SSOHelper.class);
    if(ssoHelper != null && ssoHelper.isSSOEnabled()) {
      loginPath.append(request.getContextPath()).append(ssoHelper.getSSORedirectURLSuffix());
    } else {
      loginPath.append(request.getContextPath()).append("/").append(DO_LOGIN_PATTERN);
    }
    
    loginPath.append("?initialURI=").append(URLEncoder.encode(initialURI.toString(), "UTF-8"));
    
    return loginPath.toString();
  }
  
  private void writeJSFAjaxRedirect(HttpServletResponse response, final String pUrl) throws IOException {
        /*
         * In the case of JSF Ajax redirect, query string of initialUrl doesn't make sense :
         * - JSF components instances doesn't exist anymore
         * - views are expired
         * We cannot do better than remove the query string of the initialURI.
         */
    final String[] splitedUrl = pUrl.split("\\?initialURI=");
    final String requestURI = splitedUrl[0];
    String finalUrl = requestURI;
    if (splitedUrl.length > 1) {
      finalUrl += "?initialURI=" + URLDecoder.decode(splitedUrl[1], "UTF-8").split("\\?")[0];
    }
    try {
      response.reset();
      response.addHeader("Content-Type", "text/xml;charset=UTF-8");
      response.getWriter().append(START_AJAX_REDIRECT);
      response.getWriter().append(finalUrl);
      response.getWriter().append(END_AJAX_REDIRECT);
      response.getWriter().flush();
    } catch (final Exception e) {
            /*
             * rethrow exception to fix exception type to IOException
             * FIXME : Warning - the getWriter() method signature declare throwing Exception
             * but could only throw IOException.
             * Signature should be fixed.
             */
      throw new IOException("Sending Ajax redirect in error.", e);
    }
  }
  
    /**
     * Return true when current request is JSF and Ajax
     * - check JSF header
     *
     * @return true if request is an Ajax request
     */
    private boolean isJSFAjaxRequest(HttpServletRequest req) {
      final String headerValue = req.getHeader(AJAX_JSF_HTTP_HEADER);
      return headerValue != null && AJAX_JSF_HTTP_HEADER_VALUE.contentEquals(headerValue);
    }
}
