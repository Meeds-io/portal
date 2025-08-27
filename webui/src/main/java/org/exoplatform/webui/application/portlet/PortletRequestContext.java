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
package org.exoplatform.webui.application.portlet;

import java.io.Writer;

import javax.portlet.ActionResponse;
import javax.portlet.MimeResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.StateAwareResponse;
import javax.portlet.WindowState;

import org.exoplatform.commons.utils.WriterPrinter;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.URLBuilder;
import org.exoplatform.web.url.PortalURL;
import org.exoplatform.web.url.ResourceType;
import org.exoplatform.web.url.URLFactory;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.EqualsAndHashCode.Exclude;

@Data
@EqualsAndHashCode(callSuper = false)
public class PortletRequestContext extends WebuiRequestContext {

  /**
   * Portlet Window ID
   */
  private String            windowId;

  /**
   * The request
   */
  @Exclude
  private PortletRequest    request;

  /**
   * The response
   */
  @Exclude
  private PortletResponse   response;

  @Exclude
  private Writer            writer;

  @Exclude
  private PortletMode       portletMode           = PortletMode.VIEW;

  @Exclude
  private WindowState       windowState           = WindowState.NORMAL;

  private boolean           portletInPortal       = true;

  private boolean           isAppLifecycleStarted = false;

  /** . */
  private PortletURLBuilder urlBuilder;

  public PortletRequestContext(RequestContext parentAppRequestContext,
                               WebuiApplication app,
                               Writer writer,
                               PortletRequest req,
                               PortletResponse res) {
    super(parentAppRequestContext, app);
    init(writer, req, res);
    setSessionId(req.getPortletSession(true).getId());
  }

  public void init(Writer writer, PortletRequest req, PortletResponse res) {
    this.request = req;
    this.response = res;
    this.writer = new WriterPrinter(writer);
    this.windowId = req.getWindowID();

    if (res instanceof MimeResponse mimeResponse) {
      this.urlBuilder = new PortletURLBuilder(mimeResponse.createActionURL());
    } else {
      this.urlBuilder = null;
    }
  }

  @Override
  public <R, U extends PortalURL<R, U>> U newURL(ResourceType<R, U> resourceType, URLFactory urlFactory) {
    return parentAppRequestContext_.newURL(resourceType, urlFactory);
  }

  @Override
  public void setUIApplication(UIApplication uiApplication) {
    this.uiApplication = uiApplication;
    this.appRes = getApplication().getResourceBundle(getParentAppRequestContext().getLocale());
  }

  @Override
  public final String getRequestParameter(String name) {
    return request.getParameter(name);
  }

  @Override
  public final String[] getRequestParameterValues(String name) {
    return request.getParameterValues(name);
  }

  @Override
  public Orientation getOrientation() {
    return parentAppRequestContext_.getOrientation();
  }

  @Override
  public String getRequestContextPath() {
    return request.getContextPath();
  }

  @Override
  public String getPortalContextPath() {
    if (parentAppRequestContext_ instanceof WebuiRequestContext requestContext) {
      return requestContext.getPortalContextPath();
    } else {
      return null;
    }
  }

  @Override
  public URLFactory getURLFactory() {
    return parentAppRequestContext_.getURLFactory();
  }

  @Override
  public String getRemoteUser() {
    return parentAppRequestContext_.getRemoteUser();
  }

  @Override
  public final boolean isUserInRole(String roleUser) {
    return request.isUserInRole(roleUser);
  }

  @Override
  public final boolean useAjax() {
    return getParentAppRequestContext().useAjax();
  }

  @SneakyThrows
  public void sendRedirect(String url) {
    setResponseComplete(true);
    if (response instanceof ActionResponse actionResponse) {
      actionResponse.sendRedirect(url);
    }
  }

  @Override
  public UserPortal getUserPortal() {
    return getParentAppRequestContext().getUserPortal();
  }

  public URLBuilder<UIComponent> getURLBuilder() {
    if (urlBuilder == null) {
      throw new IllegalStateException("Cannot create portlet URL during action/event phase");
    }
    return urlBuilder;
  }

  public PortletMode getApplicationMode() {
    return request.getPortletMode();
  }

  public void setApplicationMode(PortletMode mode) throws PortletModeException {
    if (response instanceof StateAwareResponse stateResponse) {
      stateResponse.setPortletMode(mode);
    } else {
      throw new PortletModeException("The portlet don't support to set a portlet mode by current runtime environment", mode);
    }
  }

  public boolean isAppLifecycleStarted() {
    return isAppLifecycleStarted;
  }

  public void setAppLifecycleStarted(boolean b) {
    isAppLifecycleStarted = b;
  }

  public static PortletRequestContext getCurrentInstance() {
    RequestContext currentInstance = RequestContext.getCurrentInstance();
    if (currentInstance != null && currentInstance instanceof PortletRequestContext portletRequestContext) {
      return portletRequestContext;
    } else {
      return null;
    }
  }

  public static PortletMode getCurrentPortletMode() {
    PortletRequestContext portletRequestContext = getCurrentInstance();
    return portletRequestContext == null ? PortletMode.VIEW : portletRequestContext.getPortletMode();
  }

  public static void setCurrentPortletMode(PortletMode mode) {
    PortletRequestContext portletRequestContext = getCurrentInstance();
    if (portletRequestContext != null) {
      portletRequestContext.setPortletMode(mode);
    }
  }

  public static WindowState getCurrentWindowState() {
    PortletRequestContext portletRequestContext = getCurrentInstance();
    return portletRequestContext == null ? WindowState.NORMAL : portletRequestContext.getWindowState();
  }

  public static void setCurrentWindowState(WindowState state) {
    PortletRequestContext portletRequestContext = getCurrentInstance();
    if (portletRequestContext != null) {
      portletRequestContext.setWindowState(state);
    }
  }

}
