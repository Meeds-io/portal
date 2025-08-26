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
package org.exoplatform.portal.webui.workspace;

import java.io.IOException;
import java.io.OutputStream;

import org.gatein.portal.controller.resource.ResourceScope;

import org.exoplatform.commons.utils.PortalPrinter;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.javascript.JavascriptConfigParser;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;

public class UIPortalApplicationLifecycle extends Lifecycle<UIPortalApplication> {

  @Override
  public void processDecode(UIPortalApplication uicomponent, WebuiRequestContext context) throws Exception {
    String componentId = context.getRequestParameter(context.getUIComponentIdParameterName());
    if (componentId == null)
      return;
    UIComponent uiTarget = uicomponent.findComponentById(componentId);
    if (uiTarget == null) {
      context.addUIComponentToUpdateByAjax(uicomponent.<UIComponent> getChildById(UIPortalApplication.UI_WORKING_WS_ID));
      PortalRequestContext.getCurrentInstance().ignoreAJAXUpdateOnPortlets(true);
      return;
    }
    if (uiTarget == uicomponent) {
      super.processDecode(uicomponent, context);
    } else {
      uiTarget.processDecode(context);
    }
  }

  /**
   * The processAction() method of the UIPortalApplication is called, as there
   * is no method in the object itself it will call the processAction() of the
   * UIPortalApplicationLifecycle bound to the UI component If no uicomponent
   * object is targeted, which is the case the first time (unless a bookmarked
   * link is used) then nothing is done. Otherwise, the targeted component is
   * extracted and a call of its processAction() method is executed.
   */
  @Override
  public void processAction(UIPortalApplication uicomponent, WebuiRequestContext context) throws Exception {
    String componentId = context.getRequestParameter(context.getUIComponentIdParameterName());
    if (componentId == null)
      return;
    UIComponent uiTarget = uicomponent.findComponentById(componentId);
    if (uiTarget == null)
      return;
    if (uiTarget == uicomponent)
      super.processAction(uicomponent, context);
    uiTarget.processAction(context);
  }

  @Override
  public void processRender(UIPortalApplication uicomponent, WebuiRequestContext context) throws Exception {
    PortalRequestContext portalRequestContext = PortalRequestContext.getCurrentInstance();
    OutputStream responseOutputStream = portalRequestContext.getResponse().getOutputStream();
    PortalPrinter parentWriter = new PortalPrinter(responseOutputStream, true, 5000);

    PortalPrinter childWriter = null;
    if (portalRequestContext.isFullRendering()) {
      JavascriptManager jsManager = portalRequestContext.getJavascriptManager();
      // Add JS resource of current portal
      String portalOwner = portalRequestContext.getPortalOwner();
      jsManager.loadScriptResource(ResourceScope.PORTAL, portalOwner);
      // Support for legacy resource declaration
      jsManager.loadScriptResource(ResourceScope.SHARED, JavascriptConfigParser.LEGACY_JAVA_SCRIPT);
      // Need to add bootstrap as immediate since it contains the loader
      jsManager.loadScriptResource(ResourceScope.SHARED, "bootstrap");

      childWriter = new PortalPrinter(responseOutputStream, true, 25000, true);

      context.setWriter(childWriter);
      processRender(uicomponent, context, "system:/groovy/portal/webui/workspace/UIPortalApplicationChildren.gtmpl");

      context.setWriter(parentWriter);
      processRender(uicomponent, context, "system:/groovy/portal/webui/workspace/UIPortalApplication.gtmpl");
      portalRequestContext.setWriter(parentWriter);
    } else {
      portalRequestContext.setWriter(parentWriter);
      processRender(uicomponent, portalRequestContext, "system:/groovy/portal/webui/workspace/UIApplication.gtmpl");
    }

    try {
      // flush the parent writer to the output stream so that we are really to
      // accept the child content
      portalRequestContext.commitResponse();
      parentWriter.flushOutputStream();
      if (childWriter != null) {
        // now that the parent has been flushed, we can flush the contents of
        // the
        // child to the output
        childWriter.flushOutputStream();
      }
    } catch (IOException e) {
      // We want to ignore the ClientAbortException since this is caused by the
      // users browser closing the connection and is not something we should be
      // logging.
      if (!containsException(e, "ClientAbortException")) {
        throw e;
      }
    }
  }

  public void processRender(UIPortalApplication uicomponent, WebuiRequestContext context, String template) throws Exception {
    // Fail if we have no template
    if (template == null) {
      throw new IllegalStateException("uicomponent " + uicomponent + " with class " + uicomponent.getClass().getName() +
          " has no template for rendering");
    }

    //
    ResourceResolver resolver = uicomponent.getTemplateResourceResolver(context, template);
    WebuiBindingContext bcontext = new WebuiBindingContext(resolver, context.getWriter(), uicomponent, context);
    bcontext.put(UIComponent.UICOMPONENT, uicomponent);
    renderTemplate(template, bcontext);
  }

  private boolean containsException(Throwable e, String pattern) {
    return e != null
           && (e.getClass().toString().contains(pattern)
               || containsException(e.getCause(), pattern));
  }

}
