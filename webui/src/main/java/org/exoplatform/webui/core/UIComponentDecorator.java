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
package org.exoplatform.webui.core;

import java.util.List;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

@ComponentConfig(lifecycle = UIComponentDecorator.UIComponentDecoratorLifecycle.class)
@Serialized
public class UIComponentDecorator extends UIComponent {
  /**
   * The component being decorated
   */
  protected UIComponent uicomponent_;

  public UIComponent getUIComponent() {
    return uicomponent_;
  }

  public UIComponent setUIComponent(UIComponent uicomponent) {
    UIComponent oldOne = getUIComponent();
    if (oldOne != null)
      oldOne.setParent(null);
    setChildComponent(uicomponent);
    if (uicomponent != null) {
      UIComponent oldParent = uicomponent.getParent();
      if (oldParent != null
          && oldParent != this
          && oldParent instanceof UIComponentDecorator uiComponentDecorator) {
        uiComponentDecorator.setUIComponent(null);
      }
      uicomponent.setParent(this);
    }
    return oldOne;
  }

  protected void setChildComponent(UIComponent uicomponent) {
    uicomponent_ = uicomponent;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends UIComponent> T findComponentById(String id) {
    if (getId().equals(id))
      return (T) this;
    if (getUIComponent() == null)
      return null;
    return (T) getUIComponent().findComponentById(id);
  }

  @Override
  public <T extends UIComponent> T findFirstComponentOfType(Class<T> type) {
    if (type.isInstance(this))
      return type.cast(this);
    if (getUIComponent() == null)
      return null;
    return getUIComponent().findFirstComponentOfType(type);
  }

  @Override
  public <T> void findComponentOfType(List<T> list, Class<T> type) {
    if (type.isInstance(this))
      list.add(type.cast(this));
    if (getUIComponent() == null)
      return;
    getUIComponent().findComponentOfType(list, type);
  }

  public void renderChildren() throws Exception {
    if (getUIComponent() == null)
      return;
    getUIComponent().processRender(RequestContext.getCurrentInstance());
  }

  public static class UIComponentDecoratorLifecycle extends Lifecycle<UIComponentDecorator> {

    @Override
    public void processRender(UIComponentDecorator uicomponent, WebuiRequestContext context) throws Exception {
      context.getWriter()
             .append("<div class=\"")
             .append(uicomponent.getId())
             .append("\" id=\"")
             .append(uicomponent.getId())
             .append("\">");
      if (uicomponent.getUIComponent() != null) {
        uicomponent.getUIComponent().processRender(context);
      }
      context.getWriter().append("</div>");
    }

  }
}
