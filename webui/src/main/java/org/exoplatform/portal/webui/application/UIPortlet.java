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
package org.exoplatform.portal.webui.application;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.portlet.MimeResponse;
import javax.portlet.PortletMode;
import javax.portlet.WindowState;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.common.i18n.LocalizedString;
import org.gatein.common.net.media.MediaType;
import org.gatein.common.util.MultiValuedPropertyMap;
import org.gatein.common.util.ParameterValidation;
import org.gatein.pc.api.Mode;
import org.gatein.pc.api.PortletContext;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.PortletInvokerException;
import org.gatein.pc.api.StateString;
import org.gatein.pc.api.StatefulPortletContext;
import org.gatein.pc.api.cache.CacheLevel;
import org.gatein.pc.api.info.EventInfo;
import org.gatein.pc.api.info.MetaInfo;
import org.gatein.pc.api.info.ModeInfo;
import org.gatein.pc.api.info.ParameterInfo;
import org.gatein.pc.api.info.PortletInfo;
import org.gatein.pc.api.invocation.ActionInvocation;
import org.gatein.pc.api.invocation.EventInvocation;
import org.gatein.pc.api.invocation.PortletInvocation;
import org.gatein.pc.api.invocation.RenderInvocation;
import org.gatein.pc.api.invocation.ResourceInvocation;
import org.gatein.pc.api.invocation.response.ContentResponse;
import org.gatein.pc.api.invocation.response.ErrorResponse;
import org.gatein.pc.api.invocation.response.FragmentResponse;
import org.gatein.pc.api.invocation.response.PortletInvocationResponse;
import org.gatein.pc.api.state.PropertyChange;
import org.gatein.pc.portlet.impl.spi.AbstractClientContext;
import org.gatein.pc.portlet.impl.spi.AbstractPortalContext;
import org.gatein.pc.portlet.impl.spi.AbstractRequestContext;
import org.gatein.pc.portlet.impl.spi.AbstractSecurityContext;
import org.gatein.pc.portlet.impl.spi.AbstractWindowContext;
import org.w3c.dom.Element;

import org.exoplatform.Constants;
import org.exoplatform.commons.utils.Text;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.UserProfileLifecycle;
import org.exoplatform.portal.application.state.ContextualPropertyManager;
import org.exoplatform.portal.config.NoSuchDataException;
import org.exoplatform.portal.module.ModuleRegistry;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pc.ExoPortletState;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.portlet.PortletExceptionHandleService;
import org.exoplatform.portal.webui.application.UIPortletActionListener.ChangePortletModeActionListener;
import org.exoplatform.portal.webui.application.UIPortletActionListener.ChangeWindowStateActionListener;
import org.exoplatform.portal.webui.application.UIPortletActionListener.ProcessActionActionListener;
import org.exoplatform.portal.webui.application.UIPortletActionListener.ProcessEventsActionListener;
import org.exoplatform.portal.webui.application.UIPortletActionListener.RenderActionListener;
import org.exoplatform.portal.webui.application.UIPortletActionListener.ServeResourceActionListener;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.portletcontainer.PortletContainerException;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event.Phase;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * This UI component represent a portlet window on a page. <br>
 * Each user request to a portlet will be passed through this class then
 * delegate call to the portlet container<br>
 * UIPortletLifecycle do the main request router: delegate the job to portlet
 * action listeners according to the url parameters<br>
 * ProcessAction, ServeResource, Render action listeners will receive event if
 * request url contain parameter point to them, those event will delegate call
 * to portlet container to call JSR 286 portlet lifecycle method<br>
 * ProcessEvents, ChangePortletMode, ChangeWindowState listener will receive
 * event after the portlet action invocation response. (dispatched during the
 * process of ProcessActionListener)<br>
 * DeleteComponent, EditPortlet action listener is portal specific listener,
 * come from the UI of portal
 *
 * @see UIPortletLifecycle
 * @see UIPortletActionListener
 */
@ComponentConfig(lifecycle = UIPortletLifecycle.class, template = "system:/groovy/portal/webui/application/UIPortlet.gtmpl", events = {
  @EventConfig(listeners = RenderActionListener.class, csrfCheck = false),
  @EventConfig(listeners = ChangePortletModeActionListener.class, csrfCheck = false),
  @EventConfig(listeners = ChangeWindowStateActionListener.class, csrfCheck = false),
  @EventConfig(phase = Phase.PROCESS, listeners = ProcessActionActionListener.class, csrfCheck = false),
  @EventConfig(phase = Phase.PROCESS, listeners = ServeResourceActionListener.class, csrfCheck = false),
  @EventConfig(phase = Phase.PROCESS, listeners = ProcessEventsActionListener.class, csrfCheck = false),
})
@SuppressWarnings("unchecked")
@Getter
@Setter
public class UIPortlet extends UIApplication {

  protected static final Log                 LOG                   = ExoLogger.getLogger("portal:UIPortlet");

  public static final String                 DEFAULT_THEME         = "Default:DefaultTheme::Vista:VistaTheme::Mac:MacTheme";

  public static final String                 JAVASCRIPT_DEPENDENCY = "org.gatein.javascript.dependency";

  private static final AbstractPortalContext PORTAL_CONTEXT        = new AbstractPortalContext(Collections.singletonMap(
                                                                                                                        "javax.portlet.markup.head.element.support",
                                                                                                                        "true"));

  private String                             storageId;

  private String                             storageName;

  private ModelAdapter                       adapter;

  private org.gatein.pc.api.Portlet          producedOfferedPortlet;

  private PortletContext                     producerOfferedPortletContext;

  private LocalizedString                    displayName;

  private PortletState                       state;

  private String                             applicationId;

  private String                             theme;

  private String                             portletStyle;

  private List<String>                       supportModes;

  private List<QName>                        supportedProcessingEvents;

  private List<QName>                        supportedPublishingEvents;

  private Map<QName, String>                 supportedPublicParams;

  private StateString                        navigationalState;

  /** A field storing localized value of javax.portlet.title * */
  private String                             configuredTitle;

  private String                             cssClass;

  private boolean                            showPortletMode;

  @Override
  public String getId() {
    return storageId == null ? super.getId() : storageId;
  }

  public String getStorageName() {
    if (storageName == null) {
      storageName = UUID.randomUUID().toString();
    }
    return storageName;
  }

  public String getWindowId() {
    return getStorageName();
  }

  /**
   * Retrieves the skin identifier associated with this portlet or
   * <code>null</code> if there isn't one (for example, it doesn't make any
   * sense in the WSRP scenario).
   *
   * @return the skin identifier associated with this portlet or
   *         <code>null</code> if there isn't one
   */
  public String getSkinId() {
    return applicationId;
  }

  /**
   * @return true if portlet is configured to show control icon that allow to
   *         change portlet mode
   */
  public boolean getShowPortletMode() {
    return showPortletMode;
  }

  /**
   * Theme is composed of map between theme name and skin name. Theme format:
   * {skinName}:{themeName}::{anotherSkin}:{anotherTheme}. For example: the
   * default them is 'Default:DefaultTheme::Vista:VistaTheme::Mac:MacTheme'.
   * Default theme means if portal skin is 'Default', this portlet's theme is
   * 'DefaultTheme. If portal change skin to 'Vista', portlet theme will be
   * change to 'VistaTheme'.
   * 
   * @return current theme setting
   */
  public String getTheme() {
    if (theme == null || theme.trim().length() < 1) {
      return DEFAULT_THEME;
    }
    return theme;
  }

  public PortletMode getCurrentPortletMode() {
    String maximizedPortletMode = getMaximizedPortletMode();
    if (StringUtils.isBlank(maximizedPortletMode)) {
      return PortletRequestContext.getCurrentPortletMode();
    } else if (maximizedPortletMode.equals(PortletMode.VIEW.toString())) {
      return PortletMode.VIEW;
    } else if (maximizedPortletMode.equals(PortletMode.HELP.toString())) {
      return PortletMode.HELP;
    } else if (maximizedPortletMode.equals(PortletMode.EDIT.toString())) {
      return PortletMode.EDIT;
    } else {
      return new PortletMode(maximizedPortletMode);
    }
  }

  public void setCurrentPortletMode(PortletMode mode) {
    PortletRequestContext.setCurrentPortletMode(mode);
  }

  public WindowState getCurrentWindowState() {
    return PortletRequestContext.getCurrentWindowState();
  }

  public void setCurrentWindowState(WindowState state) {
    PortletRequestContext.setCurrentWindowState(state);
  }

  /**
   * Get localized displayName metadata configured in portlet.xml.<br>
   * If can't find localized displayName, return portlet name.<br>
   * If portlet doesn't exists anymore, return empty string.<br>
   * This value is cached in session, that means it only query to portlet
   * container one time
   * 
   * @return display name
   */
  public String getDisplayName() {
    if (displayName == null) {
      org.gatein.pc.api.Portlet portlet = getProducedOfferedPortlet();
      if (portlet != null) {
        PortletInfo info = portlet.getInfo();
        MetaInfo meta = info.getMeta();
        displayName = meta.getMetaValue(MetaInfo.DISPLAY_NAME);
        String value = null;
        if (displayName != null) {
          RequestContext i = RequestContext.getCurrentInstance();
          Locale locale = i.getLocale();
          value = displayName.getString(locale, true);
        }
        if (value == null || value.length() == 0) {
          value = info.getName();
        }
        return value;
      } else {
        return "";
      }
    } else {
      RequestContext i = RequestContext.getCurrentInstance();
      Locale locale = i.getLocale();
      String value = displayName.getString(locale, true);

      if (ParameterValidation.isNullOrEmpty(value)) {
        org.gatein.pc.api.Portlet portlet = getProducedOfferedPortlet();
        PortletInfo info = portlet.getInfo();
        value = info.getName();
      }

      return value;
    }
  }

  public List<String> getSupportModes() {
    if (supportModes != null) {
      return supportModes;
    }

    this.supportModes = new ArrayList<>();
    org.gatein.pc.api.Portlet portlet = getProducedOfferedPortlet();
    // if we couldn't get the portlet that just return an empty modes list
    if (portlet == null) {
      return supportModes;
    }

    Set<ModeInfo> modes = portlet.getInfo().getCapabilities().getModes(MediaType.create("text/html"));
    for (ModeInfo mode : modes) {
      supportModes.add(mode.getModeName());
    }
    if (!supportModes.isEmpty()) {
      supportModes.remove("view");
    }
    return supportModes;
  }

  /**
   * This methods return the public render parameters names supported by the
   * targeted portlet; in other words, it sorts the full public render params
   * list and only return the ones that the current portlet can handle
   */
  public List<String> getPublicRenderParamNames() {
    UIPortal uiPortal = Util.getUIPortal();
    Map<String, String[]> publicParams = uiPortal.getPublicParameters();

    List<String> publicParamsSupportedByPortlet = new ArrayList<>();
    if (publicParams != null) {
      Set<String> keys = publicParams.keySet();
      for (String key : keys) {
        if (supportsPublicParam(key)) {
          publicParamsSupportedByPortlet.add(key);
        }
      }
      return publicParamsSupportedByPortlet;
    }
    return new ArrayList<>();
  }

  /*
   * Adding Map<String, String[]> parameter to support propagation of
   * publicParameters from URL
   */
  public Map<String, String[]> getPublicParameters(Map<String, String[]> portletParameters) {
    Map<String, String[]> publicParamsMap = new HashMap<>();
    UIPortal uiPortal = Util.getUIPortal();
    Map<String, String[]> publicParams = uiPortal.getPublicParameters();
    Set<String> allPublicParamsNames = publicParams.keySet();
    List<String> supportedPublicParamNames = getPublicRenderParamNames();

    for (String oneOfAllParams : allPublicParamsNames) {
      if (supportedPublicParamNames.contains(oneOfAllParams)) {
        publicParamsMap.put(oneOfAllParams, publicParams.get(oneOfAllParams));
        // Propagates public parameter from URL
        if (portletParameters != null && portletParameters.containsKey(oneOfAllParams)) {
          publicParamsMap.put(oneOfAllParams, portletParameters.get(oneOfAllParams));
          /*
           * setRenderParam() in processAction() propagates public render params
           * across pages. UIPortal params are updated to allow same behaviour
           * using URL propagation.
           */
          publicParams.put(oneOfAllParams, portletParameters.get(oneOfAllParams));
        }
      }
    }

    // Case when portlet has not public parameters in UIPortal but there are
    // supported public parameters in URL
    if (supportedPublicParams != null
        && MapUtils.isNotEmpty(portletParameters)
        && allPublicParamsNames.isEmpty()) {
      for (QName qName : supportedPublicParams.keySet()) {
        String prpId = supportsPublicParam(qName);
        if (prpId != null && portletParameters.containsKey(prpId)) {
          publicParamsMap.put(prpId, portletParameters.get(prpId));
          publicParams.put(prpId, portletParameters.get(prpId));
        }
      }
    }

    // Handle exposed portal contextual properties
    ContextualPropertyManager propertyManager = this.getApplicationComponent(ContextualPropertyManager.class);
    Map<QName, String[]> exposedPortalState = propertyManager.getProperties(this);
    for (QName prpQName : exposedPortalState.keySet()) {
      String prpId = supportsPublicParam(prpQName);
      if (prpId != null) {
        publicParamsMap.put(prpId, exposedPortalState.get(prpQName));
      }
    }

    //
    return publicParamsMap;
  }

  public Map<String, String[]> getPublicParameters() {
    return getPublicParameters(null);
  }

  // This is code for integration with PC

  /**
   * Create the correct portlet invocation that will target the portlet
   * represented by this UI component.
   *
   * @param type the invocation type
   * @param prc the portal request context
   * @param <I> the invocation type
   * @return the portlet invocation
   * @throws Exception any exception
   */
  public <I extends PortletInvocation> I create(Class<I> type, PortalRequestContext prc) throws Exception {
    ExoPortletInvocationContext pic = new ExoPortletInvocationContext(prc, this);

    //
    I invocation;
    HttpServletRequest servletRequest = prc.getRequest();
    HashMap<String, String[]> allParams = new HashMap<>();
    allParams.putAll(prc.getPortletParameters());
    allParams.putAll(this.getPublicParameters());
    allParams.remove(ExoPortletInvocationContext.NAVIGATIONAL_STATE_PARAM_NAME);
    if (type.equals(ActionInvocation.class)) {
      ActionInvocation actionInvocation = new ActionInvocation(pic);
      actionInvocation.setRequestContext(new AbstractRequestContext(servletRequest));

      String interactionState = servletRequest.getParameter(ExoPortletInvocationContext.INTERACTION_STATE_PARAM_NAME);
      if (interactionState != null) {
        actionInvocation.setInteractionState(StateString.create(interactionState));
        // remove the interaction state from remaining params
        allParams.remove(ExoPortletInvocationContext.INTERACTION_STATE_PARAM_NAME);
      }

      actionInvocation.setForm(allParams);

      invocation = type.cast(actionInvocation);
    } else if (type.equals(ResourceInvocation.class)) {
      ResourceInvocation resourceInvocation = new ResourceInvocation(pic);
      resourceInvocation.setRequestContext(new AbstractRequestContext(servletRequest));

      String resourceId = servletRequest.getParameter(Constants.RESOURCE_ID_PARAMETER);
      if (!ParameterValidation.isNullOrEmpty(resourceId)) {
        resourceInvocation.setResourceId(resourceId);
      } else if (!ParameterValidation.isNullOrEmpty(prc.getRequestParameter(Constants.RESOURCE_ID_PARAMETER))) {
        resourceInvocation.setResourceId(prc.getRequestParameter(Constants.RESOURCE_ID_PARAMETER));
      }

      String cachability = servletRequest.getParameter(Constants.CACHELEVEL_PARAMETER);
      if (!ParameterValidation.isNullOrEmpty(cachability)) {
        // we need to convert the given value to upper case as it might come
        // from WSRP in lower case
        resourceInvocation.setCacheLevel(CacheLevel.create(cachability.toUpperCase(Locale.ENGLISH)));
      }

      String resourceState = servletRequest.getParameter(ExoPortletInvocationContext.RESOURCE_STATE_PARAM_NAME);
      if (!ParameterValidation.isNullOrEmpty(resourceState)) {
        resourceInvocation.setResourceState(StateString.create(resourceState));
      }
      // remove the resource state from remaining params
      allParams.remove(ExoPortletInvocationContext.RESOURCE_STATE_PARAM_NAME);

      resourceInvocation.setForm(allParams);

      invocation = type.cast(resourceInvocation);
    } else if (type.equals(EventInvocation.class)) {
      invocation = type.cast(new EventInvocation(pic));
    } else if (type.equals(RenderInvocation.class)) {
      invocation = type.cast(new RenderInvocation(pic));
    } else {
      throw new AssertionError();
    }

    //
    invocation.setRequest(servletRequest);
    invocation.setResponse(prc.getResponse());

    // Navigational state
    invocation.setNavigationalState(navigationalState);

    /*
     * Public navigational state. Passing portletParameters for public render
     * parameters propagation via URL.
     */
    invocation.setPublicNavigationalState(this.getPublicParameters(prc.getPortletParameters()));

    // Mode
    invocation.setMode(Mode.create(getCurrentPortletMode().toString()));

    // Window state
    invocation.setWindowState(org.gatein.pc.api.WindowState.create(getCurrentWindowState().toString()));

    StatefulPortletContext<ExoPortletState> preferencesPortletContext = getPortletContext();
    if (preferencesPortletContext == null) {
      return null;
    }

    // get the user profile cached in the prc during the start of the request
    UserProfile userProfile = (UserProfile) prc.getAttribute(UserProfileLifecycle.USER_PROFILE_ATTRIBUTE_NAME);

    // client context
    AbstractClientContext clientContext;
    Cookie[] cookies = servletRequest.getCookies();
    if (cookies != null) {
      clientContext = new AbstractClientContext(servletRequest, Arrays.asList(cookies));
    } else {
      clientContext = new AbstractClientContext(servletRequest);
    }
    invocation.setClientContext(clientContext);

    // instance context
    ExoPortletInstanceContext instanceContext;
    instanceContext = new ExoPortletInstanceContext(preferencesPortletContext.getId());
    invocation.setTarget(preferencesPortletContext);
    invocation.setInstanceContext(instanceContext);
    invocation.setServerContext(new ExoServerContext(servletRequest, prc.getResponse()));
    invocation.setUserContext(new ExoUserContext(servletRequest, userProfile));
    invocation.setWindowContext(new AbstractWindowContext(getStorageName()));
    invocation.setPortalContext(PORTAL_CONTEXT);
    invocation.setSecurityContext(new AbstractSecurityContext(servletRequest));

    //
    return invocation;
  }

  public void update(PropertyChange... changes) throws Exception {
    PortletContext portletContext = getPortletContext();

    //
    PortletInvoker portletInvoker = getApplicationComponent(PortletInvoker.class);

    // Get marshalled version
    StatefulPortletContext<ExoPortletState> updatedCtx = (StatefulPortletContext<ExoPortletState>) portletInvoker
                                                                                                                 .setProperties(portletContext,
                                                                                                                                changes);

    //
    ExoPortletState updateState = updatedCtx.getState();

    // Now save it
    update(updateState);
  }

  public PortletState getState() {
    return state;
  }

  public void setState(PortletState state) {
    if (state != null) {
      try {
        PortletInvoker portletInvoker = getApplicationComponent(PortletInvoker.class);
        LayoutService layoutService = getApplicationComponent(LayoutService.class);
        String applicationId = layoutService.getId(state.getApplicationState());
        ModelAdapter adapter = ModelAdapter.getAdapter();
        PortletContext producerOfferedPortletContext = adapter.getProducerOfferedPortletContext(applicationId);

        ModuleRegistry moduleRegistry = getApplicationComponent(ModuleRegistry.class);
        if (moduleRegistry.isPortletActive(applicationId)) {
          try {
            this.producedOfferedPortlet = portletInvoker.getPortlet(producerOfferedPortletContext);
          } catch (Exception e) {
            // Whenever couldn't invoke the portlet object, set the request
            // portlet to null for the error tobe
            // properly handled and displayed when the portlet is rendered
            this.producedOfferedPortlet = null;
            LOG.error(e.getMessage(), e);
          }
        }

        this.adapter = adapter;
        this.producerOfferedPortletContext = producerOfferedPortletContext;
        this.applicationId = applicationId;
      } catch (NoSuchDataException e) {
        LOG.error(e.getMessage());
        throw e;
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    } else {
      this.adapter = null;
      this.producedOfferedPortlet = null;
      this.producerOfferedPortletContext = null;
      this.applicationId = null;
    }
    this.state = state;
  }

  /**
   * Returns the state of the portlet as a set of preferences.
   *
   * @return the preferences of the portlet
   * @throws Exception any exception
   */
  public Portlet getPreferences() throws Exception {
    RequestContext context = RequestContext.getCurrentInstance();
    ExoContainer container = context.getApplication().getApplicationServiceContainer();
    return adapter.getState(container, state.getApplicationState());
  }

  /**
   * Returns the portlet context of the portlet.
   *
   * @return the portlet context
   * @throws Exception any exception
   */
  public StatefulPortletContext<ExoPortletState> getPortletContext() throws Exception {
    RequestContext context = RequestContext.getCurrentInstance();
    ExoContainer container = context.getApplication().getApplicationServiceContainer();
    return adapter.getPortletContext(container, applicationId, state.getApplicationState());
  }

  /**
   * Update the state of the portlet.
   *
   * @param updateState the state update
   * @throws Exception any exception
   */
  public void update(ExoPortletState updateState) throws Exception {
    RequestContext context = RequestContext.getCurrentInstance();
    ExoContainer container = context.getApplication().getApplicationServiceContainer();
    state.setApplicationState(adapter.update(container, updateState, state.getApplicationState()));
    setState(state);
  }

  /**
   * Return modifed portlet stated (after portlet action invovation)
   * 
   * @param modifiedContext
   * @throws Exception
   */
  public ExoPortletState getModifiedState(PortletContext modifiedContext) throws Exception {
    return adapter.getStateFromModifiedContext(this.getPortletContext(), modifiedContext);
  }

  /**
   * Return cloned portlet state (updated after portlet action invocation). This
   * method is used in case WSRP
   * 
   * @param clonedContext
   * @throws Exception
   */
  public ExoPortletState getClonedState(PortletContext clonedContext) throws Exception {
    return adapter.getstateFromClonedContext(this.getPortletContext(), clonedContext);
  }

  /**
   * This is used by the dashboard portlet and should not be used else where. It
   * will be removed some day.
   */
  private static final ThreadLocal<UIPortlet> currentPortlet = new ThreadLocal<>();

  public static UIPortlet getCurrentUIPortlet() {
    return currentPortlet.get();
  }

  /**
   * Performs an invocation on this portlet.
   *
   * @param invocation the portlet invocation
   * @return the portlet invocation response
   * @throws PortletInvokerException any invoker exception
   */
  public PortletInvocationResponse invoke(PortletInvocation invocation) throws PortletInvokerException {
    PortletInvoker portletInvoker = getApplicationComponent(PortletInvoker.class);
    currentPortlet.set(this);
    try {
      return portletInvoker.invoke(invocation);
    } finally {
      currentPortlet.remove();
    }
  }

  /**
   * Parsing response from portlet container. The response contains:<br>
   * html markup, portlet title, response properties:<br>
   * - JS resource dependency (defined in gatein-resources.xml)<br>
   * - html header<br>
   * - cookie<br>
   * - extra markup header<br>
   * If errors occur during portlet lifecycle processing.
   * PortletExceptionHandleService is called. Add plugins to this service to
   * customize portlet error handler
   * 
   * @param pir - response object from portlet container
   * @param context - request context
   * @return markup to render on browser
   * @see PortletExceptionHandleService
   */
  public Text generateRenderMarkup(PortletInvocationResponse pir, WebuiRequestContext context) {
    PortalRequestContext prcontext = PortalRequestContext.getCurrentInstance();

    Text markup = null;
    if (pir instanceof FragmentResponse fragmentResponse) {
      switch (fragmentResponse.getType()) {
      case ContentResponse.TYPE_CHARS:
        markup = Text.create(fragmentResponse.getContent());
        break;
      case ContentResponse.TYPE_BYTES:
        markup = Text.create(fragmentResponse.getBytes(), StandardCharsets.UTF_8);
        break;
      default:
        markup = Text.create("");
        break;
      }
      setConfiguredTitle(fragmentResponse.getTitle());

      // setup portlet properties
      if (fragmentResponse.getProperties() != null) {
        // setup transport headers
        if (fragmentResponse.getProperties().getTransportHeaders() != null) {
          JavascriptManager jsMan = context.getJavascriptManager();
          MultiValuedPropertyMap<String> transportHeaders = fragmentResponse.getProperties().getTransportHeaders();
          for (String key : transportHeaders.keySet()) {
            if (JAVASCRIPT_DEPENDENCY.equals(key)) {
              for (String value : transportHeaders.getValues(key)) {
                jsMan.require(value);
              }
            } else {
              for (String value : transportHeaders.getValues(key)) {
                prcontext.getResponse().setHeader(key, value);
              }
            }
          }
        }

        // setup up portlet cookies
        if (fragmentResponse.getProperties().getCookies() != null) {
          List<Cookie> cookies = fragmentResponse.getProperties().getCookies();
          for (Cookie cookie : cookies) {
            prcontext.getResponse().addCookie(cookie);
          }
        }

        // setup markup headers
        if (fragmentResponse.getProperties().getMarkupHeaders() != null) {
          MultiValuedPropertyMap<Element> markupHeaders = fragmentResponse.getProperties().getMarkupHeaders();

          List<Element> markupElements = markupHeaders.getValues(MimeResponse.MARKUP_HEAD_ELEMENT);
          if (markupElements != null) {
            for (Element element : markupElements) {
              if (!context.useAjax() && "title".equalsIgnoreCase(element.getNodeName())
                  && element.getFirstChild() != null) {
                String title = element.getFirstChild().getNodeValue();
                prcontext.getRequest().setAttribute(PortalRequestContext.REQUEST_TITLE, title);
              } else {
                prcontext.addExtraMarkupHeader(element, getId());
              }
            }
          }
        }
      }
    } else {
      PortletContainerException pcException;
      if (pir instanceof ErrorResponse errorResponse) {
        pcException = new PortletContainerException(errorResponse.getMessage(), errorResponse.getCause());
      } else {
        pcException = new PortletContainerException("Unknown invocation response type [" + pir.getClass() +
            "]. Expected a FragmentResponse or an ErrorResponse");
      }
      PortletExceptionHandleService portletExceptionService = getApplicationComponent(PortletExceptionHandleService.class);
      if (portletExceptionService != null) {
        portletExceptionService.handle(pcException);
      }
      // Log the error
      LOG.error("Portlet render threw an exception in page {}", prcontext.getRequest().getRequestURI(), pcException);
      markup = Text.create(context.getApplicationResourceBundle().getString("UIPortlet.message.RuntimeError"));
    }
    return markup;
  }

  /**
   * Tells, according to the info located in portlet.xml, wether this portlet
   * supports the public render parameter qname given as method argument. If the
   * qname is supported, the public render parameter id is returned otherwise
   * false is returned.
   *
   * @param supportedPublicParam the supported public parameter qname
   * @return the supported public parameter id
   */
  public String supportsPublicParam(QName supportedPublicParam) {
    return getSupportedPublicParams().get(supportedPublicParam);
  }

  /**
   * Tells, according to the info located in portlet.xml, wether this portlet
   * supports the public render parameter given as a method argument
   */
  public boolean supportsPublicParam(String supportedPublicParam) {
    return getSupportedPublicParams().values().contains(supportedPublicParam);
  }

  /**
   * Tells, according to the info located in portlet.xml, wether this portlet
   * can handle a portlet event with the QName given as the method argument
   */
  public boolean supportsProcessingEvent(QName name) {
    return getSupportedProcessingEvents().contains(name);
  }

  public boolean supportsPublishingEvent(QName name) {
    return getSupportedPublishingEvents().contains(name);
  }

  public Map<QName, String> getSupportedPublicParams() {
    if (supportedPublicParams == null) {
      if (producedOfferedPortlet == null) {
        supportedPublicParams = Collections.emptyMap();
      } else {
        Collection<ParameterInfo> parameters = (Collection<ParameterInfo>) producedOfferedPortlet.getInfo()
                                                                                                 .getNavigation()
                                                                                                 .getPublicParameters();
        supportedPublicParams = parameters.stream().collect(Collectors.toMap(ParameterInfo::getName, ParameterInfo::getId));
      }
    }
    return supportedPublicParams;
  }

  public List<QName> getSupportedProcessingEvents() {
    if (supportedProcessingEvents == null) {
      org.gatein.pc.api.Portlet portlet = getProducedOfferedPortlet();
      if (portlet == null) {
        supportedProcessingEvents = Collections.emptyList();
      } else {
        Map<QName, EventInfo> consumedEvents = (Map<QName, EventInfo>) portlet.getInfo().getEventing().getConsumedEvents();
        if (MapUtils.isEmpty(consumedEvents)) {
          supportedProcessingEvents = Collections.emptyList();
        } else {
          supportedProcessingEvents = new ArrayList<>(consumedEvents.keySet());
        }
      }
    }
    return supportedProcessingEvents;
  }

  public List<QName> getSupportedPublishingEvents() {
    if (supportedPublishingEvents == null) {
      org.gatein.pc.api.Portlet portlet = getProducedOfferedPortlet();
      if (portlet == null) {
        supportedPublishingEvents = Collections.emptyList();
      } else {
        Map<QName, EventInfo> producedEvents = (Map<QName, EventInfo>) portlet.getInfo().getEventing().getProducedEvents();
        if (MapUtils.isEmpty(producedEvents)) {
          supportedPublishingEvents = Collections.emptyList();
        } else {
          supportedPublishingEvents = new ArrayList<>(producedEvents.keySet());
        }
      }
    }
    return supportedPublishingEvents;
  }

  /**
   * Returns the title showed on the InfoBar. The title is computed in following
   * manner. <br>
   * 1. First, the method getTitle(), inherited from UIPortalComponent is
   * called. The getTitle() returns what users set in the PortletSetting tab,
   * the current method returns call result if it is not null. <br>
   * 2. configuredTitle, which is the localized value of javax.portlet.title is
   * returned if it is not null. <br>
   * 3. If the method does not terminate at neither (1) nor (2), the configured
   * display name is returned.
   *
   * @return
   */
  public String getDisplayTitle() {
    String displayedTitle = getTitle();
    if (displayedTitle != null && displayedTitle.trim().length() > 0) {
      return displayedTitle;
    }
    if (configuredTitle != null) {
      return configuredTitle;
    }
    return getDisplayName();
  }

  private String getMaximizedPortletMode() {
    PortalRequestContext prContext = Util.getPortalRequestContext();
    HttpServletRequest req = prContext.getRequest();
    return req.getParameter("maximizedPortletMode");
  }

}
