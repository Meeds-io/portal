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
package org.exoplatform.portal.webui.container;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;

@SuppressWarnings("unchecked")
public abstract class UIComponentFactory<T extends UIComponent> {

  protected static final Map<Class<?>, List<UIComponentFactory<?>>> COMPONENT_FACTORY = new HashMap<>();

  static {
    @SuppressWarnings("rawtypes")
    ServiceLoader<UIComponentFactory> loader = ServiceLoader.load(UIComponentFactory.class);
    for (UIComponentFactory<? extends UIComponent> factory : loader) {
      Type genericSuper = factory.getClass().getGenericSuperclass();

      Class<?> supportedType = UIComponent.class;
      if (genericSuper instanceof ParameterizedType parameterizedType) {
        supportedType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
      }
      COMPONENT_FACTORY.computeIfAbsent(supportedType, k -> new LinkedList<>())
                       .add(factory);
    }
  }

  public static <T extends UIComponent> UIComponentFactory<? extends T> getInstance(Class<T> type) {
    if (type == null) {
      throw new NullPointerException(type + " is null");
    }

    final List<UIComponentFactory<? extends T>> list = new LinkedList<UIComponentFactory<? extends T>>();
    for (Class<?> t : COMPONENT_FACTORY.keySet()) {
      if (type.isAssignableFrom(t)) {
        for (UIComponentFactory<?> factory : COMPONENT_FACTORY.get(t)) {
          list.add((UIComponentFactory<? extends T>) factory);
        }
      }
    }

    return new UIComponentFactory<T>() {
      @Override
      public T createUIComponent(String factoryID, WebuiRequestContext context) {
        T uiComponent = null;
        for (UIComponentFactory<? extends T> f : list) {
          uiComponent = f.createUIComponent(factoryID, context);
          if (uiComponent != null) {
            break;
          }
        }
        return uiComponent;
      }
    };
  }

  protected T create(Class<? extends T> type, WebuiRequestContext context) {
    try {
      if (context == null) {
        return type.newInstance();
      }

      WebuiApplication app = (WebuiApplication) context.getApplication();
      return app.createUIComponent(type, null, null, context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public T createUIComponent(String factoryID) {
    return createUIComponent(factoryID, WebuiRequestContext.<WebuiRequestContext> getCurrentInstance());
  }

  public abstract T createUIComponent(String factoryID, WebuiRequestContext context);
}
