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

import java.util.EnumSet;
import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.DispatcherType;

@Configuration("webPortalRememberMeFilterConfiguration")
public class WebPortalRememberMeFilterConfiguration {

  @Bean
  public FilterRegistrationBean<PortalRememberMeFilter> rememberMeFilter() {
    FilterRegistrationBean<PortalRememberMeFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new PortalRememberMeFilter());
    registrationBean.setUrlPatterns(List.of("/rest/*"));
    registrationBean.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
    registrationBean.setOrder(3);
    return registrationBean;
  }

}
