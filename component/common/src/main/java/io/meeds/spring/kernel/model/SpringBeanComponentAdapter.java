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
package io.meeds.spring.kernel.model;

import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.spi.ComponentAdapter;

import lombok.Getter;

public class SpringBeanComponentAdapter implements ComponentAdapter<Object> {

  private final String           servletContextName;

  @Getter
  private final String           beanName;

  private final Class<Object>    componentKey;

  private final Supplier<Object> getBeanSupplier;

  public SpringBeanComponentAdapter(String servletContextName,
                                    String beanName,
                                    Class<Object> componentKey,
                                    Supplier<Object> getBeanSupplier) {
    this.servletContextName = servletContextName;
    this.beanName = beanName;
    this.componentKey = componentKey;
    this.getBeanSupplier = getBeanSupplier;
  }

  public Object getComponentInstance() {
    return getBeanSupplier.get();
  }

  @Override
  public Class<Object> getComponentImplementation() {
    return componentKey;
  }

  @Override
  public Object getComponentKey() { // NOSONAR
    return componentKey;
  }

  public boolean isSingleton() {
    return true;
  }

  public boolean isIssuedFrom(String servletContextName) {
    return StringUtils.equals(servletContextName, this.servletContextName);
  }

}
