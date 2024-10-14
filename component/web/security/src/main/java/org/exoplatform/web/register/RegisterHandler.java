/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.web.register;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.JspBasedWebHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.login.UIParamsExtension;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;

import io.meeds.spring.web.localization.HttpRequestLocaleWrapper;

import nl.captcha.Captcha;
import nl.captcha.servlet.CaptchaServletUtil;
import nl.captcha.text.producer.DefaultTextProducer;
import nl.captcha.text.renderer.DefaultWordRenderer;

public class RegisterHandler extends JspBasedWebHandler {

  private static final Log               LOG                           = ExoLogger.getLogger(RegisterHandler.class);

  public static final String             NAME                          = "register";

  public static final String             REGISTER_EXTENSION_NAME       = "RegisterExtension";

  public static final String             REGISTER_ENABLED              = "registerEnabled";

  public static final String             REGISTER_JSP_PATH_PARAM       = "register.jsp.path";

  public static final String             REGISTER_EXTENSION_JS_MODULES = "RegisterExtension";

  public static final String             REGISTRATION_ERROR_CODE       = "REGISTRATION_ERROR";

  public static final String             EMAIL_PARAM                   = "email";

  public static final String             CAPTCHA_PARAM                 = "captcha";

  public static final UserFieldValidator EMAIL_VALIDATOR               = new UserFieldValidator(EMAIL_PARAM, false, false);

  public static final String             ONBOARDING_EMAIL_SENT_MESSAGE = "onboardingEmailSent";

  public static final String             SUCCESS_MESSAGE_PARAM         = "success";

  public static final String             ERROR_MESSAGE_PARAM           = "error";

  public static final int                CAPTCHA_WIDTH                 = 200;

  public static final int                CAPTCHA_HEIGHT                = 50;

  private PortalContainer                container;

  private PasswordRecoveryService        passwordRecoveryService;

  private OrganizationService            organizationService;

  private ResourceBundleService          resourceBundleService;

  private RegisterUIParamsExtension      registerUIParamsExtension;

  private ServletContext                 servletContext;

  private String                         registerJspPath;

  public RegisterHandler(PortalContainer container, // NOSONAR
                         ResourceBundleService resourceBundleService,
                         PasswordRecoveryService passwordRecoveryService,
                         OrganizationService organizationService,
                         LocaleConfigService localeConfigService,
                         BrandingService brandingService,
                         JavascriptConfigService javascriptConfigService,
                         SkinService skinService,
                         RegisterUIParamsExtension registerUIParamsExtension,
                         InitParams params) {
    super(localeConfigService, brandingService, javascriptConfigService, skinService);
    this.container = container;
    this.passwordRecoveryService = passwordRecoveryService;
    this.organizationService = organizationService;
    this.resourceBundleService = resourceBundleService;
    this.registerUIParamsExtension = registerUIParamsExtension;
    this.servletContext = container.getPortalContext();
    if (params != null && params.containsKey(REGISTER_JSP_PATH_PARAM)) {
      this.registerJspPath = params.getValueParam(REGISTER_JSP_PATH_PARAM).getValue();
    }
  }

  @Override
  public String getHandlerName() {
    return NAME;
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  @Override
  public boolean execute(ControllerContext controllerContext) throws Exception {
    HttpServletRequest request = new HttpRequestLocaleWrapper(controllerContext.getRequest());
    HttpServletResponse response = controllerContext.getResponse();

    // If user is already authenticated, no registration form is required
    if (request.getRemoteUser() != null) {
      return false;
    }

    if (!registerUIParamsExtension.isRegisterEnabled()) {
      response.setStatus(401);
      return true;
    }

    Locale locale = request.getLocale();
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(resourceBundleService.getSharedResourceBundleNames(),
                                                                            locale);

    String serveCaptcha = request.getParameter("serveCaptcha");
    if ("true".equals(serveCaptcha)) {
      return serveCaptchaImage(request, response);
    }

    Map<String, Object> parameters = new HashMap<>();
    String email = request.getParameter(EMAIL_PARAM);
    if (StringUtils.isNotBlank(email)) {
      String captcha = request.getParameter(CAPTCHA_PARAM);
      if (!isValidCaptch(request.getSession(), captcha)) {
        parameters.put(ERROR_MESSAGE_PARAM, resourceBundle.getString("gatein.forgotPassword.captchaError"));
      } else if (isValidEmail(email, resourceBundle, locale, parameters)) {
        sendOnboardingEmail(email, request);
        parameters.put(SUCCESS_MESSAGE_PARAM, ONBOARDING_EMAIL_SENT_MESSAGE);
      }
      parameters.put(EMAIL_PARAM, email);
    }

    return dispatch(controllerContext, parameters);
  }

  protected boolean dispatch(ControllerContext controllerContext, Map<String, Object> parameters) throws Exception {
    HttpServletRequest request = controllerContext.getRequest();
    HttpServletResponse response = controllerContext.getResponse();

    // Invalidate the Captcha
    request.getSession().removeAttribute(NAME);

    List<String> additionalJSModules = getExtendedJSModules();
    List<String> additionalCSSModules = Collections.singletonList("portal/login");

    super.prepareDispatch(controllerContext,
                          "PORTLET/social/Register",
                          additionalJSModules,
                          additionalCSSModules,
                          params -> extendApplicationParameters(controllerContext, params, parameters));

    servletContext.getRequestDispatcher(registerJspPath).include(request, response);
    return true;
  }

  protected void extendApplicationParameters(ControllerContext controllerContext,
                                             JSONObject params,
                                             Map<String, Object> parameters) {
    try {
      if (parameters != null) {
        parameters.forEach(params::put);
      }

      List<UIParamsExtension> paramsExtensions = this.container.getComponentInstancesOfType(UIParamsExtension.class);
      if (CollectionUtils.isNotEmpty(paramsExtensions)) {
        paramsExtensions.stream()
                        .filter(extension -> extension.getExtensionNames().contains(REGISTER_EXTENSION_NAME))
                        .forEach(paramsExtension -> {
                          Map<String, Object> extendedParams = paramsExtension.extendParameters(controllerContext,
                                                                                                REGISTER_EXTENSION_NAME);
                          if (MapUtils.isNotEmpty(extendedParams)) {
                            extendedParams.forEach((key, value) -> {
                              try {
                                params.put(key, value);
                              } catch (Exception e) {
                                LOG.warn("Error while adding {}/{} in register params map", key, value, e);
                              }
                            });
                          }
                        });
      }
    } catch (Exception e) {
      LOG.warn("Error while computing Register UI parameters", e);
    }
  }

  private List<String> getExtendedJSModules() {
    List<String> additionalJSModules = new ArrayList<>();
    JSONObject jsConfig = javascriptConfigService.getJSConfig();
    if (jsConfig.has(JS_PATHS_PARAM)) {
      JSONObject jsConfigPaths = jsConfig.getJSONObject(JS_PATHS_PARAM);
      Iterator<String> keys = jsConfigPaths.keys();
      while (keys.hasNext()) {
        String module = keys.next();
        if (module.contains(REGISTER_EXTENSION_JS_MODULES)) {
          additionalJSModules.add(module);
        }
      }
    }
    return additionalJSModules;
  }

  private void sendOnboardingEmail(String email, HttpServletRequest request) throws Exception {
    StringBuilder url = getUrl(request);
    Locale locale = request.getLocale();
    passwordRecoveryService.sendExternalRegisterEmail(null, email, locale, null, url, false);
  }

  private boolean isValidEmail(String email,
                               ResourceBundle resourceBundle,
                               Locale locale,
                               Map<String, Object> parameters) throws Exception {
    String errorMessage = EMAIL_VALIDATOR.validate(locale, email);
    if (StringUtils.isNotBlank(errorMessage)) {
      parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
    } else {
      try {
        // Check if mail address is already used
        Query query = new Query();
        query.setEmail(email);
        ListAccess<User> users = organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY);
        if (users != null && users.getSize() > 0) {
          // Must not lack the information anonymously that the user exists
          // Thus, send it as succeeded operation
          parameters.put(SUCCESS_MESSAGE_PARAM, ONBOARDING_EMAIL_SENT_MESSAGE);
        } else {
          User user = organizationService.getUserHandler().findUserByName(email);
          if (user != null) {
            // Must not lack the information anonymously that the user exists
            // Thus, send it as succeeded operation
            parameters.put(SUCCESS_MESSAGE_PARAM, ONBOARDING_EMAIL_SENT_MESSAGE);
          }
        }
      } catch (RuntimeException e) {
        LOG.debug("Error retrieving users list with email {}. Thus, we will consider the email as already used", email, e);
        parameters.put(ERROR_MESSAGE_PARAM, resourceBundle.getString("external.registration.fail.create.user"));
      }
    }
    return !parameters.containsKey(ERROR_MESSAGE_PARAM) && !parameters.containsKey(SUCCESS_MESSAGE_PARAM);
  }

  private boolean isValidCaptch(HttpSession session, String captchaValue) {
    Captcha captcha = (Captcha) session.getAttribute(NAME);
    return ((captcha != null) && (captcha.isCorrect(captchaValue)));
  }

  private boolean serveCaptchaImage(HttpServletRequest req, HttpServletResponse resp) {
    HttpSession session = req.getSession();
    Captcha captcha;
    if (session.getAttribute(NAME) == null) {
      List<Font> textFonts = Arrays.asList(new Font("Arial", Font.BOLD, 40), new Font("Courier", Font.BOLD, 40));
      captcha = new Captcha.Builder(CAPTCHA_WIDTH, CAPTCHA_HEIGHT)
                                                                  .addText(new DefaultTextProducer(5),
                                                                           new DefaultWordRenderer(Color.WHITE, textFonts))
                                                                  .gimp()
                                                                  .addNoise()
                                                                  .addBackground()
                                                                  .build();

      session.setAttribute(NAME, captcha);
      writeImage(resp, captcha.getImage());

    }

    captcha = (Captcha) session.getAttribute(NAME);
    writeImage(resp, captcha.getImage());

    return true;
  }

  private void writeImage(HttpServletResponse response, BufferedImage bi) {
    response.setHeader("Cache-Control", "private,no-cache,no-store");
    response.setContentType("image/png"); // PNGs allow for transparency. JPGs
                                          // do not.
    try {
      CaptchaServletUtil.writeImage(response.getOutputStream(), bi);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  private StringBuilder getUrl(HttpServletRequest request) {
    StringBuilder url = new StringBuilder();
    if (request != null) {
      url.append(request.getScheme()).append("://").append(request.getServerName());
      if (request.getServerPort() != 80 && request.getServerPort() != 443) {
        url.append(':').append(request.getServerPort());
      }
      url.append("/" + PortalContainer.getCurrentPortalContainerName());
    }
    return url;
  }
}
