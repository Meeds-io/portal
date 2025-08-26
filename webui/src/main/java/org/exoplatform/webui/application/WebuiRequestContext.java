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
package org.exoplatform.webui.application;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import org.exoplatform.resolver.ApplicationResourceResolver;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.URLBuilder;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;

import lombok.Getter;
import lombok.Setter;

/**
 * The main class to manage the request context in a webui environment It adds:
 * - some access to the root UI component (UIApplication) - access to the
 * request and response objects - information about the current state of the
 * request - the list of object to be updated in an AJAX way - an access to the
 * ResourceResolver bound to an uri scheme - the reference on the StateManager
 * object
 */
public abstract class WebuiRequestContext extends RequestContext {

  public static final char   NAME_DELIMITER = '-';

  @Getter
  @Setter
  protected String           sessionId;

  @Getter
  @Setter
  protected StateManager     stateManager;

  @Getter
  @Setter
  protected boolean          responseComplete;

  @Getter
  @Setter
  protected boolean          processRender;

  protected UIApplication    uiApplication;

  protected ResourceBundle   appRes;

  protected Set<UIComponent> uicomponentToUpdateByAjax;

  protected WebuiRequestContext(Application application) {
    super(application);
  }

  protected WebuiRequestContext(RequestContext parentAppRequestContext, Application application) {
    super(parentAppRequestContext, application);
  }

  @Override
  public ResourceBundle getApplicationResourceBundle() {
    Application application = getApplication();
    if (appRes == null && application != null) {
      try {
        Locale locale = getLocale();
        appRes = application.getResourceBundle(locale);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    return appRes;
  }

  public void setUIApplication(UIApplication uiApplication) {
    this.uiApplication = uiApplication;
    this.appRes = null;
  }

  public UIApplication getUIApplication() {
    return uiApplication;
  }

  public String getActionParameterName() {
    return ACTION;
  }

  public String getUIComponentIdParameterName() {
    return UIComponent.UICOMPONENT;
  }

  @Override
  public abstract URLBuilder<UIComponent> getURLBuilder();

  public abstract String getRequestContextPath();

  /**
   * Returns the context path of the portal or null if it does not execute in
   * the context of an aggregated portal request.
   *
   * @return the portal context path
   */
  public abstract String getPortalContextPath();

  public abstract <T> T getRequest();

  public abstract <T> T getResponse();

  public abstract void sendRedirect(String url);

  public Set<UIComponent> getUIComponentToUpdateByAjax() {
    return uicomponentToUpdateByAjax;
  }

  public void addUIComponentToUpdateByAjax(UIComponent uicomponent) {
    if (uicomponentToUpdateByAjax == null) {
      uicomponentToUpdateByAjax = new LinkedHashSet<>();
    }
    uicomponentToUpdateByAjax.add(uicomponent);
  }

  public ResourceResolver getResourceResolver(String uri) {
    Application app = getApplication();
    RequestContext pcontext = this;
    while (app != null) {
      ApplicationResourceResolver appResolver = app.getResourceResolver();
      ResourceResolver resolver = appResolver.getResourceResolver(uri);
      if (resolver != null) {
        return resolver;
      }
      pcontext = pcontext.getParentAppRequestContext();
      if (pcontext != null) {
        app = pcontext.getApplication();
      } else {
        app = null;
      }
    }
    return null;
  }

  public JavascriptManager getJavascriptManager() {
    // Yes nasty cast
    return ((WebuiRequestContext) getParentAppRequestContext()).getJavascriptManager();
  }

  public static String generateUUID(String prefix) {
    String uuid = UUID.randomUUID().toString();
    /*
     * The following is equivalent to prefix.length() + 1 + uuid.length() - 4
     * where + 1 is for the additional minus and -4 is for the number of minus
     * signs removed from uuid you may want to look into the source of
     * UUID.toString() to see that there are 4 minus signs in a default UUID
     */
    int uuidLen = uuid.length();
    StringBuilder result = new StringBuilder(prefix.length() + uuidLen - 3);
    result.append(prefix).append(NAME_DELIMITER);
    for (int i = 0; i < uuidLen; i++) {
      char ch = uuid.charAt(i);
      if (ch != NAME_DELIMITER) {
        result.append(ch);
      }
    }
    return result.toString();
  }

  public static String stripUUIDSuffix(String name) {
    int lastMinus = name.lastIndexOf(NAME_DELIMITER);
    if (lastMinus >= 0) {
      return name.substring(0, lastMinus);
    } else {
      return name;
    }
  }
}
