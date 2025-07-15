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
package org.exoplatform.webui.core.lifecycle;

import java.io.Writer;
import java.util.*;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.groovyscript.text.BindingContext;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIContainer;

import groovy.lang.Closure;

@SuppressWarnings("serial")
public class WebuiBindingContext extends BindingContext {

    protected static Log log = ExoLogger.getLogger("portal:WebuiBindingContext");

    private static final boolean DEVELOPPING = PropertyManager.isDevelopping();

    private UIComponent uicomponent_;

    private WebuiRequestContext rcontext_;

    public WebuiBindingContext(ResourceResolver resolver, Writer w, UIComponent uicomponent, final WebuiRequestContext context) {
        super(resolver, w);
        uicomponent_ = uicomponent;
        rcontext_ = context;

        // Closure nodeurl()
        put("nodeurl", new Closure(this) {
            @Override
            public Object call(Object[] args) {
                return context.createURL(NodeURL.TYPE);
            }
        });

        // Add Orientation specific information
        Orientation orientation = context.getOrientation();
        this.put("orientation", orientation);
        this.put("isLT", orientation.isLT());
        this.put("isRT", orientation.isRT());
        this.put("dir", orientation.isLT() ? "ltr" : "rtl");
    }

    public UIComponent getUIComponent() {
        return uicomponent_;
    }

    public WebuiRequestContext getRequestContext() {
        return rcontext_;
    }

    public String getContextPath() {
        return rcontext_.getRequestContextPath();
    }

    public String getPortalContextPath() {
        return rcontext_.getPortalContextPath();
    }

    public BindingContext clone() {
        BindingContext newContext = new WebuiBindingContext(resolver_, writer_, uicomponent_, rcontext_);
        newContext.putAll(this);
        newContext.setGroovyTemplateService(service_);
        return newContext;
    }

    public String appRes(String mesgKey) {
        String value = "";
        try {
            ResourceBundle res = rcontext_.getApplicationResourceBundle();
            value = res.getString(mesgKey);
        } catch (MissingResourceException ex) {
            if (DEVELOPPING)
                log.warn("Can not find resource bundle for key : " + mesgKey);
            if (mesgKey != null)
                value = mesgKey.substring(mesgKey.lastIndexOf('.') + 1);
        }
        return value;
    }

    public void renderChildren() throws Exception {
        if (uicomponent_ instanceof UIComponentDecorator) {
            UIComponentDecorator uiComponentDecorator = (UIComponentDecorator) uicomponent_;
            if (uiComponentDecorator.getUIComponent() == null)
                return;
            uiComponentDecorator.getUIComponent().processRender(rcontext_);
            return;
        }
        UIContainer uicontainer = (UIContainer) uicomponent_;
        List<UIComponent> children = uicontainer.getChildren();
        for (UIComponent child : children) {
            if (child.isRendered()) {
                child.processRender(rcontext_);
            }
        }
    }

    public void renderChild(String id) throws Exception {
        if (!(uicomponent_ instanceof UIContainer))
            return;
        UIContainer uicontainer = (UIContainer) uicomponent_;
        UIComponent uiChild = uicontainer.getChildById(id);
        uiChild.processRender(rcontext_);
    }

    public void renderUIComponent(UIComponent uicomponent) throws Exception {
        uicomponent.processRender(rcontext_);
    }

    public void renderChild(int index) throws Exception {
        if (!(uicomponent_ instanceof UIContainer))
            return;
        UIContainer uicontainer = (UIContainer) uicomponent_;
        UIComponent uiChild = uicontainer.getChild(index);
        uiChild.processRender(rcontext_);
    }

    @SuppressWarnings("unused")
    public void userRes(String mesgKey) {

    }

    public void include(String name, ResourceResolver resourceResolver) throws Exception {
        if (DEVELOPPING) {
          PortalRequestContext rootContext = PortalRequestContext.getCurrentInstance();
          UIApplication uiApplication = rootContext.getUIApplication();
          long lastAccess = uiApplication.getLastAccessApplication();
          if (lastAccess == 0 || resourceResolver.isModified(name, lastAccess)) {
            service_.getTemplatesCache().clearCache();
            uiApplication.setLastAccessApplication(System.currentTimeMillis());
          }
        }
        service_.include(name, clone(), resourceResolver);
    }

    /**
     * Includes the list of gtmpl templates to current
     * {@link WebuiBindingContext} for the parent template identified by its name
     * 
     * @param parentAppName parent template name
     */
    public void includeTemplates(String parentAppName) {
      TemplateService templateService = PortalContainer.getInstance()
                                                       .getComponentInstanceOfType(TemplateService.class);
      Set<String> templateExtensions = templateService.getTemplateExtensions(parentAppName);
      if (templateExtensions == null || templateExtensions.isEmpty()) {
        return;
      }
      for (String templateExtension : templateExtensions) {
        try {
          ResourceResolver resourceResolver = getRequestContext().getResourceResolver(templateExtension);
          if (resourceResolver == null) {
            log.warn("Can't find an adequate resource resolver for template '{}'. Using default.", templateExtension);
            resourceResolver = getResourceResolver();
          }
          include(templateExtension, resourceResolver);
        } catch (Exception e) {
          log.warn("Error while processing template: '{}'", templateExtension, e);
        }
      }
    }
}
