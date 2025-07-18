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
package io.meeds.spring.web.security;

import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.CacheControlConfig;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.ContentTypeOptionsConfig;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.HstsConfig;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.XXssConfig;
import org.springframework.security.config.annotation.web.configurers.JeeConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.ServletContextAware;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import lombok.Setter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true)
public class WebSecurityConfiguration implements ServletContextAware {

  private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfiguration.class);

  @Setter
  private ServletContext      servletContext;

  @Bean
  public static GrantedAuthorityDefaults grantedAuthorityDefaults() {
    // Reset prefix to be empty. By default it adds "ROLE_" prefix
    return new GrantedAuthorityDefaults();
  }

  @Bean
  @SuppressWarnings("removal")
  public SecurityFilterChain filterChain(HttpSecurity http,
                                         PortalAuthenticationManager authenticationProvider,
                                         @Qualifier("restRequestMatcher")
                                         RequestMatcher restRequestMatcher,
                                         @Qualifier("staticResourcesRequestMatcher")
                                         RequestMatcher staticResourcesRequestMatcher,
                                         @Qualifier("accessDeniedHandler")
                                         AccessDeniedHandler accessDeniedHandler,
                                         @Qualifier("requestAuthorizationManager")
                                         AuthorizationManager<RequestAuthorizationContext> requestAuthorizationManager) throws Exception {
    return http.authenticationProvider(authenticationProvider)
               .jee(JeeConfigurer::and) // NOSONAR no method replacement
               .csrf(CsrfConfigurer::disable)
               .headers(headers -> {
                 headers.cacheControl(CacheControlConfig::disable);
                 headers.frameOptions(FrameOptionsConfig::disable);
                 headers.xssProtection(XXssConfig::disable);
                 headers.contentTypeOptions(ContentTypeOptionsConfig::disable);
                 headers.httpStrictTransportSecurity(HstsConfig::disable);
               })
               .authorizeHttpRequests(customizer -> {
                 try {
                   customizer.requestMatchers(restRequestMatcher)
                             .access(requestAuthorizationManager);
                 } catch (Exception e) {
                   LOG.error("Error configuring REST endpoints security manager", e);
                 }
                 customizer.requestMatchers(staticResourcesRequestMatcher)
                           .permitAll();
                 customizer.dispatcherTypeMatchers(DispatcherType.INCLUDE,
                                                   DispatcherType.FORWARD)
                           .permitAll();
               })
               .exceptionHandling(exceptionCustomizer -> exceptionCustomizer.accessDeniedHandler(accessDeniedHandler))
               .build();
  }

  @Bean("restRequestMatcher")
  public RequestMatcher restRequestMatcher() {
    return request -> StringUtils.startsWith(request.getRequestURI(), servletContext.getContextPath() + "/rest/");
  }

  @Bean("staticResourcesRequestMatcher")
  public RequestMatcher staticResourcesRequestMatcher() {
    return request -> !StringUtils.startsWith(request.getRequestURI(), servletContext.getContextPath() + "/rest/");
  }

  @Bean("accessDeniedHandler")
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      LOG.warn("Access denied for path {} and method {}",
               request.getRequestURI(),
               request.getMethod(),
               accessDeniedException);
      if (!response.isCommitted()) {
        // Put exception into request scope (perhaps of use to a view)
        request.setAttribute(WebAttributes.ACCESS_DENIED_403, accessDeniedException);
        // Set the 403 status code.
        response.setStatus(HttpStatus.FORBIDDEN.value());
      }
    };
  }

  @Bean("requestAuthorizationManager")
  public AuthorizationManager<RequestAuthorizationContext> requestAuthorizationManager() {
    return (Supplier<Authentication> authentication, RequestAuthorizationContext object) -> {
      Authentication userAuthentication = authentication.get();
      // Permit anonymous and authentication users to access
      // the REST endpoints and rely on jee & secured permission
      // management
      return userAuthentication.isAuthenticated() ? new AuthorizationDecision(true) : new AuthorizationDecision(false);
    };
  }

}
