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

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("webPortalIdentityFilterConfiguration")
public class WebPortalIdentityFilterConfiguration {

  @Bean
  public FilterRegistrationBean<PortalIdentityFilter> identityFilter() {
    FilterRegistrationBean<PortalIdentityFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new PortalIdentityFilter());
    registrationBean.addUrlPatterns("/rest/*");
    registrationBean.setOrder(2);
    return registrationBean;
  }

}
