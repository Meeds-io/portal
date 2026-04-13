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
package io.meeds.spring.kernel.model;

import java.util.function.Supplier;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;

import lombok.Setter;

/**
 * Used to inject Beans from a Spring content to another by avoiding
 * `AutowiredAnnotationBeanPostProcessor` which will attempt to resolve
 * `@Autowired` Bean dependencies again. See Meeds-io/meeds#2469
 */
public class SpringBeanProxyFactoryBean<T> implements FactoryBean<T> {

  @Setter
  private Class<T>              exposedType;

  @Setter
  private Supplier<? extends T> targetSupplier;

  @Override
  @SuppressWarnings("unchecked")
  public T getObject() {
    ProxyFactory proxyFactory = new ProxyFactory();
    boolean isInterface = exposedType.isInterface();
    proxyFactory.setProxyTargetClass(!isInterface);
    if (isInterface) {
      proxyFactory.setInterfaces(exposedType);
    } else {
      proxyFactory.setTargetClass(exposedType);
    }
    proxyFactory.setTargetSource(new TargetSource() {
      private T target;

      @Override
      public Class<?> getTargetClass() {
        return exposedType;
      }

      @Override
      public boolean isStatic() {
        return false;
      }

      @Override
      public T getTarget() {
        T local = target;
        if (local == null) {
          synchronized (this) {
            local = target;
            if (local == null) {
              local = targetSupplier.get();
              if (local == null) {
                throw new IllegalStateException("No shared bean found for " + exposedType.getName());
              }
              target = local;
            }
          }
        }
        return local;
      }
    });
    return (T) proxyFactory.getProxy();
  }

  @Override
  public Class<?> getObjectType() {
    return exposedType;
  }

}
