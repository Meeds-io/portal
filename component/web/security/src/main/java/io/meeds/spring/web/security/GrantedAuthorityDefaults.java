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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.security.access.annotation.Jsr250MethodSecurityMetadataSource;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

@SuppressWarnings("deprecation")
public class GrantedAuthorityDefaults implements BeanPostProcessor, PriorityOrdered {

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    // remove this if you are not using JSR-250
    if (bean instanceof Jsr250MethodSecurityMetadataSource securityBean) {
      securityBean.setDefaultRolePrefix(null);
    }
    if (bean instanceof DefaultMethodSecurityExpressionHandler securityBean) {
      securityBean.setDefaultRolePrefix(null);
    }
    if (bean instanceof DefaultWebSecurityExpressionHandler securityBean) {
      securityBean.setDefaultRolePrefix(null);
    }
    if (bean instanceof SecurityContextHolderAwareRequestFilter securityBean) {
      securityBean.setRolePrefix("");
    }
    return bean;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

}
