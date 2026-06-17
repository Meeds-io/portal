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
package org.exoplatform.web.login;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.common.text.EntityEncoder;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.authentication.AuthenticationEventType;
import org.gatein.wci.security.Credentials;
import org.json.JSONObject;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.JspBasedWebHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.exoplatform.web.security.AuthenticationRegistry;
import org.exoplatform.web.security.RedirectUrlValidator;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.sso.SSOHelper;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.service.SecuritySettingService;
import io.meeds.spring.web.localization.HttpRequestLocaleWrapper;

public class LoginHandler extends JspBasedWebHandler {

  public static final String      LOGIN_EXTENSION_NAME       = "LoginExtension";

  public static final String      REGISTER_EXTENSION_NAME    = "RegisterExtension";

  public static final String      USER_AUTHENTICATION_EVENT  = "authentication.user.status";

  private static final Log        LOG                        = ExoLogger.getLogger(LoginHandler.class);

  private static final String     JS_PATHS_PARAM             = "paths";

  private static final String     LOGIN_JSP_PATH_PARAM       = "login.jsp.path";

  private static final String     CASE_INSENSITIVE_PARAM     = "username.case.insensitive";

  private static final String     LOGIN_EXTENSION_JS_MODULES = "LoginExtension";

  public static final String      ERROR_MESSAGE_PARAM        = "error";

  private boolean                 caseInsensitive;

  private PortalContainer         container;

  private AuthenticationRegistry  authenticationRegistry;

  private OrganizationService     organizationService;

  private SecuritySettingService  securitySettingService;

  private PasswordRecoveryService passwordRecoveryService;

  private ListenerService         listenerService;

  private SSOHelper               ssoHelper;

  private ServletContext          servletContext;

  private String                  loginJspPath;

  public LoginHandler(PortalContainer container, // NOSONAR
                      OrganizationService organizationService,
                      PasswordRecoveryService passwordRecoveryService,
                      AuthenticationRegistry authenticationRegistry,
                      LocaleConfigService localeConfigService,
                      BrandingService brandingService,
                      SecuritySettingService securitySettingService,
                      JavascriptConfigService javascriptConfigService,
                      ListenerService listenerService,
                      SkinService skinService,
                      SSOHelper ssoHelper,
                      InitParams params) {
    super(localeConfigService, brandingService, javascriptConfigService, skinService);
    this.container = container;
    this.organizationService = organizationService;
    this.securitySettingService = securitySettingService;
    this.authenticationRegistry = authenticationRegistry;
    this.passwordRecoveryService = passwordRecoveryService;
    this.listenerService = listenerService;
    this.ssoHelper = ssoHelper;
    if (params != null) {
      if (params.containsKey(LOGIN_JSP_PATH_PARAM)) {
        this.loginJspPath = params.getValueParam(LOGIN_JSP_PATH_PARAM).getValue();
      }
      if (params.containsKey(CASE_INSENSITIVE_PARAM)) {
        this.caseInsensitive = Boolean.parseBoolean(params.getValueParam(CASE_INSENSITIVE_PARAM).getValue());
      }
    }
  }

  @Override
  public String getHandlerName() {
    return "login";
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  @Override
  public void onInit(WebAppController controller, ServletConfig servletConfig) {
    this.servletContext = container.getPortalContext();
    // Register WCI authentication listener, which is used to bind credentials
    // to temporary authentication registry after each successful login
    ServletContainerFactory.getServletContainer().addAuthenticationListener(event -> {
      if (event.getType() == AuthenticationEventType.LOGIN) {
        authenticationRegistry.setCredentials(event.getRequest(), event.getCredentials());
      }
    });
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception { // NOSONAR
    HttpServletRequest request = new HttpRequestLocaleWrapper(context.getRequest());
    HttpServletResponse response = context.getResponse();
    try {
      // We set the character encoding now to UTF-8 before obtaining parameters
      request.setCharacterEncoding("UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.error("Encoding not supported", e);
    }

    String username = request.getParameter("username");
    if (username != null) {
      username = username.trim();
    }
    String password = request.getParameter("password");

    final String portalContextPath = servletContext.getContextPath();
    HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
      @Override
      public String getContextPath() {
        return portalContextPath;
      }
    };
    StringBuilder loginPath = new StringBuilder(loginJspPath);
    //
    LoginStatus status = LoginStatus.UNAUTHENTICATED;
    if (request.getRemoteUser() == null) {
      if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
        // email authentication
        String realUsername = getExactUserName(username);
        if (username.contains("@")) {
          if (realUsername == null) {
            // no user exist with username=credential
            // check by email
            String userNameByEmail = getUserNameByEmail(username, context, loginPath);
            if (StringUtils.isBlank(userNameByEmail)) {
              return true;
            } else {
              username = userNameByEmail;
            }
          } else {
            // a user exists with username=credential
            // priority to username
            if (caseInsensitive) {
              username = realUsername;
            }
          }
        } else if (caseInsensitive && realUsername != null) {
          username = realUsername;
        }

        Credentials credentials = new Credentials(username, password);
        // This will login or send an AuthenticationException
        try {
          ServletContainerFactory.getServletContainer().login(request, response, credentials);
          LOG.debug("User {} authenticated successfuly.", username);

          // Handle remember me
          addRememberMeCookie(request, response, username);
        } catch (Exception e) {
          LOG.debug("User {} authentication failed.", username, e);
          status = LoginStatus.FAILED;
          String referer = request.getHeader("Referer");
          if (referer != null && !referer.contains("/portal/login?")) {
            // If the request comes from a different page, we redirect to the login page
            // with the error message
            try {
              String redirectUrl = referer.contains("?") ? "&" : "?";
              redirectUrl = "%s%serror=%s".formatted(referer, redirectUrl, status.getErrorCode());
              response.sendRedirect(response.encodeRedirectURL(redirectUrl));
              return true;
            } catch (Exception ex) {
              LOG.error("Failed to redirect to referer page: " + referer, ex);
            }
          } 
        }
      }
      if (request.getRemoteUser() != null) {
        status = LoginStatus.AUTHENTICATED;
        // Delete user forgot-password tokens to invalidate recover password
        // email link
        CookieTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
        tokenService.deleteTokensByUsernameAndType(username, CookieTokenService.FORGOT_PASSWORD_TOKEN);
      }
    } else {
      LOG.debug("User already authenticated. Will redirect to initialURI");
      status = LoginStatus.AUTHENTICATED;
    }

    listenerService.broadcast(USER_AUTHENTICATION_EVENT, username, status);

    String initialURI = getInitalUri(request);

    // Redirect to initialURI
    if (status == LoginStatus.AUTHENTICATED) {
      // Response may be already committed in case of SAML or other SSO
      // providers
      if (!response.isCommitted()) {
        response.sendRedirect(response.encodeRedirectURL(initialURI));
      }
    } else {
      return handleSsoRequest(context, wrappedRequest, loginPath, status, initialURI);
    }
    return true;
  }

  private boolean handleSsoRequest(ControllerContext context,
                                HttpServletRequest request,
                                StringBuilder loginPath,
                                LoginStatus status,
                                String initialURI) throws Exception {
    HttpServletResponse response = context.getResponse();

    // Show login form or redirect to SSO url (/portal/sso) if SSO is enabled
    request.setAttribute("org.gatein.portal.login.initial_uri", initialURI);

    String disabledUser = (String) request.getAttribute(FilterDisabledLoginModule.DISABLED_USER_NAME);
    boolean meetDisabledUser = disabledUser != null;
    if (ssoHelper.skipJSPRedirection() && meetDisabledUser) {
      dispatch(context, loginJspPath, LoginStatus.DISABLED_USER);
      return true;
    } else if (ssoHelper.skipJSPRedirection()) {
      String ssoRedirectUrl = request.getContextPath() + ssoHelper.getSSORedirectURLSuffix();
      ssoRedirectUrl = response.encodeRedirectURL(ssoRedirectUrl);
      if (LOG.isTraceEnabled()) {
        LOG.trace("Redirected to SSO login URL: " + ssoRedirectUrl);
      }
      response.sendRedirect(ssoRedirectUrl);
      return true;
    } else {
      if (meetDisabledUser) {
        status = LoginStatus.DISABLED_USER;
      }
      Object ssoStatus = request.getAttribute("SSO.Login.Status");
      if (ssoStatus != null) {
        status = LoginStatus.valueOf((String) ssoStatus);
      }
      return false;
    }
  }

  private String getUserNameByEmail(String identifier,
                                    ControllerContext context,
                                    StringBuilder loginPath) throws Exception {
    // in login context, we do not allow search by email with wildcard
    identifier = identifier.replace("*", "");
    UserHandler userHandler = organizationService.getUserHandler();
    if (userHandler != null) {
      Query emailQuery = new Query();
      emailQuery.setEmail(identifier);
      ListAccess<User> users;
      try {
        users = userHandler.findUsersByQuery(emailQuery);
        if (users != null && users.getSize() > 0) {
          return users.load(0, 1)[0].getUserName();
        } else {
          return identifier;
        }
      } catch (RuntimeException e) {
        LOG.warn("Can not login with an email associated to many users");
        dispatch(context, loginPath.toString(), LoginStatus.MANY_USERS_WITH_SAME_EMAIL);
      } catch (Exception e) {
        LOG.warn("Can not get users by email", e);
        dispatch(context, loginPath.toString(), LoginStatus.FAILED);
      }
    }
    return null;
  }

  private void addRememberMeCookie(HttpServletRequest request, HttpServletResponse response, String username) {
    String rememberme = request.getParameter(LoginUtils.COOKIE_NAME);
    if ("true".equals(rememberme) || "on".equals(rememberme)) {
      // Create token for credentials
      CookieTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
      String cookieToken = tokenService.createToken(username);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Found a remember me request parameter, created a persistent token " + cookieToken + " for it and set it up " +
            "in the next response");
      }
      Cookie cookie = new Cookie(LoginUtils.COOKIE_NAME, cookieToken);
      cookie.setPath("/");
      cookie.setHttpOnly(true);
      cookie.setMaxAge((int) tokenService.getValidityTime());
      cookie.setSecure(request.isSecure());
      response.addCookie(cookie);
    }
  }

  private String getInitalUri(HttpServletRequest request) {
    // Obtain and validate initial URI
    String initialURI = request.getParameter("initialURI");
    String sanitizedInitialURI = RedirectUrlValidator.sanitizeInitialURI(request, initialURI);

    if (StringUtils.isBlank(initialURI)) {
      LOG.debug("No initial URI found, will use default " + sanitizedInitialURI + " instead ");
    } else if (StringUtils.equals(initialURI, sanitizedInitialURI)) {
      LOG.debug("Found initial URI " + sanitizedInitialURI);
    } else {
      LOG.warn("Unsafe initial URI in login link. Redirecting to the portal context path instead.");
    }

    return sanitizedInitialURI;
  }

  /**
   * Get exact username from database
   *
   * @param username
   * @return
   */
  private String getExactUserName(String username) {
    RequestLifeCycle.begin(container);
    try {
      User user = organizationService.getUserHandler().findUserByName(username);
      if (user != null) {
        return user.getUserName();
      }
    } catch (Exception exception) {
      LOG.warn("Error while retrieving user " + username + " from IDM stores ", exception);
    } finally {
      RequestLifeCycle.end();
    }
    return null;
  }

  private void dispatch(ControllerContext controllerContext, String dispatchPath, LoginStatus status) throws Exception {
    HttpServletRequest request = controllerContext.getRequest();
    HttpServletResponse response = controllerContext.getResponse();

    List<String> additionalJSModules = getExtendedJSModules();
    List<String> additionalCSSModules = Collections.singletonList("portal/login");

    super.prepareDispatch(controllerContext,
                          "PORTLET/social/Login",
                          additionalJSModules,
                          additionalCSSModules,
                          params -> extendUIParameters(controllerContext, status, params));

    request.getRequestDispatcher(dispatchPath).forward(request, response);
  }

  private List<String> getExtendedJSModules() {
    List<String> additionalJSModules = new ArrayList<>();
    JSONObject jsConfig = javascriptConfigService.getJSConfig();
    if (jsConfig.has(JS_PATHS_PARAM)) {
      JSONObject jsConfigPaths = jsConfig.getJSONObject(JS_PATHS_PARAM);
      Iterator<String> keys = jsConfigPaths.keys();
      while (keys.hasNext()) {
        String module = keys.next();
        if (module.contains(LOGIN_EXTENSION_JS_MODULES)) {
          additionalJSModules.add(module);
        }
      }
    }
    return additionalJSModules;
  }

  private void extendUIParameters(ControllerContext controllerContext, LoginStatus status, JSONObject params) {
    HttpServletRequest request = controllerContext.getRequest();
    try {
      String initialURI = getInitalUri(request);
      params.put("initialUri", EntityEncoder.FULL.encode(initialURI));

      String forgotPasswordPath = passwordRecoveryService.getPasswordRecoverURL(null, null);
      params.put("forgotPasswordPath", request.getContextPath() + forgotPasswordPath);

      if (StringUtils.isNotBlank(status.getErrorCode())) {
        params.put(ERROR_MESSAGE_PARAM, status.getErrorCode());
      }

      List<UIParamsExtension> paramsExtensions = this.container.getComponentInstancesOfType(UIParamsExtension.class);
      if (CollectionUtils.isNotEmpty(paramsExtensions)) {
        paramsExtensions.stream()
                        .filter(extension -> extension.getExtensionNames().contains(LOGIN_EXTENSION_NAME))
                        .forEach(paramsExtension -> {
                          Map<String, Object> extendedParams = paramsExtension.extendParameters(controllerContext,
                                                                                                LOGIN_EXTENSION_NAME);
                          if (MapUtils.isNotEmpty(extendedParams)) {
                            extendedParams.forEach((key, value) -> {
                              try {
                                params.put(key, value);
                              } catch (Exception e) {
                                LOG.warn("Error while adding {}/{} in login params map", key, value, e);
                              }
                            });
                          }
                        });
      }
      // Force disabling Register Form when the platform access is restricted
      if (securitySettingService.getRegistrationType() == UserRegistrationType.RESTRICTED) {
        params.put("registerEnabled", false);
      }
    } catch (Exception e) {
      LOG.warn("Error while computing Login UI parameters", e);
    }
  }

}
