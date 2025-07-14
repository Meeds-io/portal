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
package org.exoplatform.portal.application.localization;

import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.RequestFailure;
import org.exoplatform.webui.application.WebuiRequestContext;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * This class takes care of loading / initializing / saving the current Locale.
 * Current Locale is used to create properly localized response to current
 * request. At the beginning of request {@link LocalePolicy} is used to
 * determine the initial Locale to be used for processing the request. This
 * Locale is then set on current
 * {@link org.exoplatform.portal.application.PortalRequestContext} (it's
 * presumed that current {@link org.exoplatform.web.application.RequestContext}
 * is of type PortalRequestContext) by calling PortalRequestContext#setLocale.
 * During request processing PortalRequestContext#getLocale is
 * the ultimate reference consulted by any rendering code that needs to know
 * about current Locale. When this Locale is changed during action processing,
 * the new Locale choice is saved into user's profile or into browser's cookie
 * in order to be used by future requests. This Lifecycle depends on
 * UserProfileLifecycle being registered before this one, as it relies on it for
 * loading the user profile. See WEB-INF/webui-configuration.xml in web/portal
 * module.
 *
 */
public class LocalizationLifecycle extends BaseComponentPlugin implements ApplicationLifecycle<WebuiRequestContext> {

  public static final String SAVE_PROFILE_LOCALE_ATTR = "SaveProfileLocale";

  private static final Log    LOG                      = ExoLogger.getLogger("portal:LocalizationLifecycle");

  private static final String LOCALE_COOKIE            = "LOCALE";

  private static final String LOCALE_SESSION_ATTR      = "org.gatein.LOCALE";

  private static final String PREV_LOCALE_SESSION_ATTR = "org.gatein.LAST_LOCALE";

  private ExoContainer        container;

  private LocalePolicy        localePolicy;

  private LocaleConfigService localeConfigService;

  private OrganizationService organizationService;

  /**
   * @see org.exoplatform.web.application.ApplicationLifecycle#onInit
   */
  public void onInit(Application app) throws Exception {
    container = app.getApplicationServiceContainer();
    localePolicy = container.getComponentInstanceOfType(LocalePolicy.class);
    localeConfigService = container.getComponentInstanceOfType(LocaleConfigService.class);
    organizationService = container.getComponentInstanceOfType(OrganizationService.class);
  }

  /**
   * Initialize Locale to be used for the processing of current request
   *
   * @see org.exoplatform.web.application.ApplicationLifecycle#onStartRequest
   */
  public void onStartRequest(Application app, WebuiRequestContext context) throws Exception {
    if (!(context instanceof PortalRequestContext reqCtx)) {
      return;
    }

    Locale requestLocale = reqCtx.getRequestLocale();
    HttpServletRequest request = reqCtx.getRequest();
    LocaleContextInfo localeCtx = LocaleContextInfoUtils.buildLocaleContextInfo(request);
    localeCtx.setRequestLocale(requestLocale);
    Locale locale = localePolicy.determineLocale(localeCtx);
    reqCtx.setLocale(locale);
    if (request.getRemoteUser() != null
        && (localeCtx.getUserProfileLocale() == null || !Objects.equals(locale, localeCtx.getUserProfileLocale()))) {
      reqCtx.setAttribute(SAVE_PROFILE_LOCALE_ATTR, true);
    }
    resetOrientation(reqCtx, locale);
  }

  /**
   * Save any locale change - to cookie for anonymous users, to profile for
   * logged-in users
   *
   * @see org.exoplatform.web.application.ApplicationLifecycle#onEndRequest
   */
  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
    // if onStartRequest survived the cast, this one should as well - no check
    // necessary
    PortalRequestContext reqCtx = PortalRequestContext.getCurrentInstance();
    Locale loc = reqCtx.getLocale();

    // if locale changed since previous request
    Locale sessLocale = getPreviousLocale(reqCtx.getRequest());
    if (reqCtx.getAttribute(SAVE_PROFILE_LOCALE_ATTR) != null
        || (loc != null
            && sessLocale != null
            && !loc.equals(sessLocale))) {
      saveLocale(reqCtx, loc);
      resetOrientation(reqCtx, loc);
      savePreviousLocale(reqCtx, loc);
    } else if (sessLocale == null) {
      savePreviousLocale(reqCtx, loc);
    }
  }

  /**
   * @see org.exoplatform.web.application.ApplicationLifecycle#onFailRequest
   */
  public void onFailRequest(Application app, WebuiRequestContext context, RequestFailure failureType) {
  }

  /**
   * @see org.exoplatform.web.application.ApplicationLifecycle#onDestroy
   */
  public void onDestroy(Application app) throws Exception {
  }

  private UserProfile loadUserProfile(PortalRequestContext context) {
    UserProfile userProfile = null;
    String userName = context.getRemoteUser();
    if (userName != null) {
      try {
        userProfile = organizationService.getUserProfileHandler().findUserProfileByName(userName);
        if (userProfile == null) {
          userProfile = organizationService.getUserProfileHandler().createUserProfileInstance(userName);
        }
      } catch (Exception e) {
        LOG.error("Failed to load UserProfile for user {}", userName, e);
      }
    }
    return userProfile;
  }

  private Locale getPreviousLocale(HttpServletRequest request) {
    return getLocaleFromSession(request, PREV_LOCALE_SESSION_ATTR);
  }

  private Locale getLocaleFromSession(HttpServletRequest request, String attrName) {
    String lang = null;
    HttpSession session = request.getSession(false);
    if (session != null)
      lang = (String) session.getAttribute(attrName);
    return (lang != null) ? LocaleContextInfo.getLocale(lang) : null;
  }

  private void saveLocale(PortalRequestContext context, Locale loc) {
    String user = context.getRemoteUser();
    if (StringUtils.isBlank(user)) {
      saveLocaleToCookie(context, loc);
    } else {
      saveLocaleToUserProfile(context, loc, user);
    }
    saveSessionLocale(context, loc);
  }

  private void resetOrientation(PortalRequestContext context, Locale loc) {
    LocaleConfig localeConfig = localeConfigService.getLocaleConfig(LocaleContextInfo.getLocaleAsString(loc));
    if (localeConfig == null) {
      LOG.warn("Locale changed to unsupported Locale during request processing: " + loc);
      return;
    }
    // we presume PortalRequestContext, and UIPortalApplication
    context.setOrientation(localeConfig.getOrientation());
  }

  private void saveSessionLocale(PortalRequestContext context, Locale loc) {
    saveLocaleToSession(context, LOCALE_SESSION_ATTR, loc);
  }

  private void savePreviousLocale(PortalRequestContext context, Locale loc) {
    saveLocaleToSession(context, PREV_LOCALE_SESSION_ATTR, loc);
  }

  private void saveLocaleToSession(PortalRequestContext context, String attrName, Locale loc) {
    HttpServletRequest res = context.getRequest();
    HttpSession session = res.getSession(false);
    if (session != null)
      session.setAttribute(attrName, LocaleContextInfo.getLocaleAsString(loc));
  }

  private void saveLocaleToCookie(PortalRequestContext context, Locale loc) {
    HttpServletResponse res = context.getResponse();
    Cookie cookie = new Cookie(LOCALE_COOKIE, LocaleContextInfo.getLocaleAsString(loc));
    cookie.setMaxAge(Integer.MAX_VALUE);
    cookie.setPath("/");
    res.addCookie(cookie);
  }

  private void saveLocaleToUserProfile(PortalRequestContext context, Locale loc, String user) {
    RequestLifeCycle.begin(container);
    try {
      // Don't rely on UserProfileLifecycle loaded UserProfile when doing
      // an update to avoid a potential overwrite of other changes
      UserProfile userProfile = loadUserProfile(context);
      if (userProfile == null) {
        LOG.warn("Unable to save locale into profile for user {}", user);
      } else {
        userProfile.getUserInfoMap().put(Constants.USER_LANGUAGE, LocaleContextInfo.getLocaleAsString(loc));
        organizationService.getUserProfileHandler().saveUserProfile(userProfile, false);
      }
    } catch (Exception e) {
      LOG.warn("Failed to save profile for user {}", user, e);
    } finally {
      RequestLifeCycle.end();
    }
  }

}
