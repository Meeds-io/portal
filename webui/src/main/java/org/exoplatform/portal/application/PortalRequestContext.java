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
package org.exoplatform.portal.application;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.gatein.common.http.QueryStringParser;
import org.w3c.dom.Element;

import org.exoplatform.Constants;
import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.commons.utils.PortalPrinter;
import org.exoplatform.commons.xml.DOMSerializer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.DynamicPortalLayoutService;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.url.PortalURLContext;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageFactory;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.PortalHttpServletResponseWrapper;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.URLBuilder;
import org.exoplatform.web.security.sso.SSOHelper;
import org.exoplatform.web.url.PortalURL;
import org.exoplatform.web.url.ResourceType;
import org.exoplatform.web.url.URLFactory;
import org.exoplatform.web.url.URLFactoryService;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.url.ComponentURL;

import io.meeds.common.performance.model.ServerResponseTime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * This class extends the abstract WebuiRequestContext which itself extends the
 * RequestContext one
 * <p>
 * It mainly implements the abstract methods and overide some.
 */
public class PortalRequestContext extends WebuiRequestContext {

  public static final int                  PUBLIC_ACCESS       = 0;

  public static final int                  PRIVATE_ACCESS      = 1;

  public static final String               UI_COMPONENT_ACTION = ComponentURL.PORTAL_COMPONENT_ACTION;

  public static final String               UI_COMPONENT_ID     = ComponentURL.PORTAL_COMPONENT_ID;

  public static final String               TARGET_NODE         = "portal:targetNode";

  public static final String               CACHE_LEVEL         = "portal:cacheLevel";

  public static final String               REQUEST_TITLE       = "portal:requestTitle".intern();

  public static final String               REQUEST_METADATA    = "portal:requestMetadata".intern();

  private static final String              DO_LOGIN_PATTERN    = "login";

  private static final Log                 LOG                 = ExoLogger.getLogger("portal:PortalRequestContext");

  public static DynamicPortalLayoutService portalLayoutService;                                                     // NOSONAR

  public static UserPortalConfigService    portalConfigService;                                                     // NOSONAR

  public static LayoutService              layoutService;                                                           // NOSONAR

  public static SSOHelper                  ssoHelper;                                                               // NOSONAR

  /** The path decoded from the request. */
  private final String                     nodePath;

  /** . */
  private final String                     portalURI;

  /** . */
  private final String                     contextPath;

  /** . */
  private final SiteKey                    siteKey;

  /** . */
  @Getter
  private final PortalConfig               portalConfig;

  /** The locale from the request. */
  @Getter
  private final Locale                     requestLocale;

  @Getter
  private final HttpServletRequest         request;

  @Getter
  private final HttpServletResponse        response;

  private String                           cacheLevel          = "cacheLevelPortlet";

  private boolean                          ajaxRequest         = true;

  private String                           siteLabel;

  @Setter
  private Boolean                          draftPage;

  @Setter
  private Boolean                          draftSite;

  @Setter
  private Boolean                          noCache;

  private boolean                          forceFullUpdate     = false;

  private Writer                           writer;

  protected JavascriptManager              javascriptManager;

  private List<Element>                    extraMarkupHeaders;

  private final PortalURLBuilder           urlBuilder;

  private Map<String, String[]>            parameterMap;

  @Getter
  @Setter
  private Locale                           locale              = Locale.ENGLISH;

  @Getter
  @Setter
  private Orientation                      orientation         = Orientation.LT;

  private List<Runnable>                   endRequestRunnables;

  /** . */
  private final URLFactoryService          urlFactory;

  /** . */
  private final ControllerContext          controllerContext;

  private UserPortalConfig                 userPortalConfig;

  private PortalConfig                     currentPortalConfig;

  private UIPortal                         uiPortal;

  private UIPage                           uiPage;

  @Getter
  @Setter
  private UIPortlet                        maximizedUIPortlet;

  private ServerResponseTime               serverResponseTime;

  public void setUiPage(UIPage uiPage) {
    this.uiPage = uiPage;
  }

  @Getter
  @Setter
  private List<UIPortlet> uiPortlets;

  @Getter
  @Setter
  private Page            page;

  private UserNode        userNode;

  private String          skin;

  private String          pageTitle = null;

  /**
   * Analyze a request and split this request's URI to get useful information
   * then keep it in following properties of PortalRequestContext :<br>
   * 1. <code>requestURI</code> : The decoded URI of this request <br>
   * 2. <code>portalOwner</code> : The portal name ( "classic" for instance
   * )<br>
   * 3. <code>portalURI</code> : The URI to current portal (
   * "/portal/public/classic/ for instance )<br>
   * 4. <code>nodePath</code> : The path that is used to reflect to a navigation
   * node
   */
  @SneakyThrows
  public PortalRequestContext(WebuiApplication app,
                              ControllerContext controllerContext,
                              String requestSiteType,
                              String requestSiteName,
                              String requestPath,
                              Locale requestLocale) {
    super(app);

    //
    this.urlFactory = (URLFactoryService) PortalContainer.getComponent(URLFactoryService.class);
    this.controllerContext = controllerContext;
    this.javascriptManager = new JavascriptManager();
    if (portalLayoutService == null) {
      portalLayoutService = ExoContainerContext.getService(DynamicPortalLayoutService.class); // NOSONAR
      layoutService = ExoContainerContext.getService(LayoutService.class); // NOSONAR
      portalConfigService = ExoContainerContext.getService(UserPortalConfigService.class); // NOSONAR
      ssoHelper = ExoContainerContext.getService(SSOHelper.class); // NOSONAR
    }

    //
    request = controllerContext.getRequest();
    response = controllerContext.getResponse();
    response.setBufferSize(1024 * 100);
    contextPath = request.getContextPath();
    setSessionId(request.getSession().getId());

    // The encoding needs to be set before reading any of the parameters since
    // the parameters's encoding
    // is set at the first access.

    response.setContentType("text/html");
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());

    // Query parameters from the request will be set in the servlet container
    // url encoding and not
    // necessarly in utf-8 format. So we need to directly parse the parameters
    // from the query string.
    parameterMap = new HashMap<>();
    parameterMap.putAll(request.getParameterMap());
    String queryString = request.getQueryString();
    if (queryString != null) {
      queryString = queryString.replace("&amp;", "&");
      Map<String, String[]> queryParams = QueryStringParser.getInstance().parseQueryString(queryString);
      parameterMap.putAll(queryParams);
    }

    ajaxRequest = "true".equals(request.getParameter("ajaxRequest"));
    String cache = request.getParameter(CACHE_LEVEL);
    if (cache != null) {
      cacheLevel = cache;
    }

    this.siteKey = new SiteKey(SiteType.valueOf(requestSiteType.toUpperCase()), requestSiteName);
    this.portalConfig = layoutService.getPortalConfig(this.siteKey);
    this.nodePath = requestPath;
    this.requestLocale = requestLocale;

    //
    NodeURL url = createURL(NodeURL.TYPE);
    url.setResource(new NavigationResource(siteKey, requestPath));
    portalURI = url.toString();

    //
    urlBuilder = new PortalURLBuilder(this, createURL(ComponentURL.TYPE));
  }

  @Override
  public <R, U extends PortalURL<R, U>> U newURL(ResourceType<R, U> resourceType, URLFactory urlFactory) {
    PortalURLContext urlContext = new PortalURLContext(controllerContext, siteKey);
    U url = urlFactory.newURL(resourceType, urlContext);
    if (url != null) {
      url.setAjax(false);
      url.setLocale(requestLocale);
    }
    return url;
  }

  @Override
  public JavascriptManager getJavascriptManager() {
    return javascriptManager;
  }

  @Override
  public final String getRemoteUser() {
    return request.getRemoteUser();
  }

  @Override
  public final boolean isUserInRole(String roleUser) {
    return request.isUserInRole(roleUser);
  }

  @Override
  public final Writer getWriter() throws IOException {
    if (writer == null) {
      writer = new PortalPrinter(response.getOutputStream(), false, 30000);
    }
    return writer;
  }

  @Override
  public final void setWriter(Writer writer) {
    this.writer = writer;
  }

  @Override
  public final boolean useAjax() {
    return ajaxRequest;
  }

  /**
   * @see org.exoplatform.web.application.RequestContext#getFullRender()
   */
  @Override
  public final boolean getFullRender() {
    return forceFullUpdate;
  }

  public String getSkin() {
    if (skin == null) {
      String siteSkin = getUiPortal().getSkin();
      if (siteSkin == null) {
        return ExoContainerContext.getService(SkinService.class).getDefaultSkin();
      } else {
        return siteSkin;
      }
    } else {
      return skin;
    }
  }

  public UserPortal getUserPortal() {
    UserPortalConfig upc = getUserPortalConfig();
    if (upc != null) {
      return upc.getUserPortal();
    } else {
      return null;
    }
  }

  public boolean isNoCache() {
    if (noCache == null) {
      noCache = StringUtils.equals("true", getRequest().getParameter("noCache"));
    }
    return noCache.booleanValue();
  }

  public boolean isDraftSite() {
    if (draftSite == null) {
      draftSite = portalConfig != null && PortalConfig.DRAFT.equals(portalConfig.getType());
    }
    return draftSite.booleanValue();
  }

  public boolean isDraftPage() {
    if (draftPage == null) {
      if (portalConfig != null && PortalConfig.DRAFT.equals(portalConfig.getType())) {
        draftPage = true;
      } else {
        UserNode navigationNode = getNavigationNode();
        draftPage = navigationNode != null && navigationNode.getVisibility() == Visibility.DRAFT;
      }
    }
    return draftPage.booleanValue();
  }

  @SneakyThrows
  public UserNode getNavigationNode() {
    return getNavigationNode(false);
  }

  private UserNode getNavigationNode(boolean noCache) {
    if (!noCache && userNode != null) {
      return userNode;
    }
    if (siteKey.getType() == SiteType.DRAFT) {
      userNode = portalConfigService.getSiteNodeOrGlobalNode(PortalConfig.PORTAL_TYPE,
                                                             getMetaPortal(),
                                                             null,
                                                             request.getRemoteUser());
      if (userNode == null) {
        List<String> siteNames = layoutService.getSiteNames(SiteType.PORTAL, 0, 10);
        userNode = siteNames.stream()
                            .map(siteName -> portalConfigService.getSiteNodeOrGlobalNode(PortalConfig.PORTAL_TYPE,
                                                                                         siteName,
                                                                                         null,
                                                                                         request.getRemoteUser()))
                            .findFirst()
                            .orElseThrow();
      }
    } else {
      UserPortal userPortal = getUserPortalConfig().getUserPortal();
      UserNavigation navigation = userPortal.getNavigation(siteKey);
      if (navigation != null) {
        userNode = portalConfigService.getSiteNodeOrGlobalNode(siteKey.getTypeName(),
                                                               siteKey.getName(),
                                                               nodePath,
                                                               request.getRemoteUser());
      }
    }
    return userNode;
  }

  public UserPortalConfig getUserPortalConfig() {
    if (userPortalConfig == null) {
      String portalName = getCurrentPortalSite();
      try {
        userPortalConfig = portalConfigService.getUserPortalConfig(portalName, getRemoteUser());
      } catch (Exception e) {
        LOG.error("Error retrieving UserPortalConfig with sit {} for user {}", portalName, getRemoteUser(), e);
        return null;
      }
    }

    return userPortalConfig;
  }

  private String getCurrentPortalSite() {
    String portalName = null;
    if (SiteType.PORTAL == getSiteType()) {
      portalName = getSiteName();
    }
    if (portalName == null) {
      portalName = getMetaPortal();
    }
    return portalName;
  }

  public UIPortal getUiPortal() {
    if (uiPortal == null) {
      uiPortal = ((UIPortalApplication) uiApplication_).getUiPortal(siteKey);
    }
    return uiPortal;
  }

  public UIPage getUiPage() {
    if (uiPage != null) {
      return uiPage;
    }
    UserNode navigationNode = getNavigationNode();
    if (navigationNode == null) {
      return null;
    } else {
      if (getUiPortal() == null) {
        return null;
      }
      return buildUiPage(navigationNode, uiPortal);
    }
  }

  public String getInitialURI() {
    return request.getRequestURI();
  }

  public ControllerContext getControllerContext() {
    return controllerContext;
  }

  public void refreshResourceBundle() {
    appRes_ = getApplication().getResourceBundle(getLocale());
  }

  public void requestAuthenticationLogin() {
    requestAuthenticationLogin(null);
  }

  @SneakyThrows
  public void requestAuthenticationLogin(Map<String, String> params) {
    StringBuilder initialURI = new StringBuilder();
    initialURI.append(request.getRequestURI());
    if (request.getQueryString() != null) {
      initialURI.append("?").append(request.getQueryString());
    }

    StringBuilder loginPath = new StringBuilder();

    // . Check SSO Enable
    if (ssoHelper != null && ssoHelper.isSSOEnabled() && ssoHelper.skipJSPRedirection()) {
      loginPath.append(getPortalContextPath()).append(ssoHelper.getSSORedirectURLSuffix());
    } else {
      loginPath.append(getPortalContextPath()).append("/").append(DO_LOGIN_PATTERN);
    }

    loginPath.append("?initialURI=").append(URLEncoder.encode(initialURI.toString(), StandardCharsets.UTF_8));
    if (params != null) {
      for (Map.Entry<String, String> param : params.entrySet()) {
        loginPath.append("&").append(URLEncoder.encode(param.getKey(), "UTF-8"));
        loginPath.append("=").append(URLEncoder.encode(param.getValue(), "UTF-8"));
      }
    }

    sendRedirect(loginPath.toString());
  }

  public void setPageTitle(String title) {
    this.pageTitle = title;
  }

  public PortalConfig getDynamicPortalConfig() throws Exception {
    if (this.currentPortalConfig == null) {
      SiteKey displayingSiteKey = getSiteKey();

      if (portalLayoutService == null) {
        this.currentPortalConfig = layoutService.getPortalConfig(displayingSiteKey.getTypeName(), displayingSiteKey.getName());
      } else {
        this.currentPortalConfig =
                                 portalLayoutService.getPortalConfigWithDynamicLayout(displayingSiteKey, getCurrentPortalSite());
      }
    }
    return this.currentPortalConfig;
  }

  public void addOnRequestEnd(Runnable runnable) {
    if (endRequestRunnables == null) {
      endRequestRunnables = new ArrayList<>();
    }
    endRequestRunnables.add(runnable);
  }

  public void onRequestEnd() {
    if (endRequestRunnables != null) {
      endRequestRunnables.forEach(Runnable::run);
    }
  }

  public String getTitle() {
    if (pageTitle != null) {
      return pageTitle;
    }
    String title = (String) getRequest().getAttribute(REQUEST_TITLE);

    //
    if (title == null) {
      UIPortal uiportal = getUiPortal();

      //
      UserNode node = uiportal.getSelectedUserNode();
      if (node != null) {
        PageContext pageContext = portalConfigService.getPage(node.getPageRef(), getRequest().getRemoteUser());

        //
        if (pageContext != null) {
          title = pageContext.getState().getDisplayName();
          // testing to ensure first that the title is a I18N expression
          if (ExpressionUtil.isResourceBindingExpression(title)) {
            String resolvedTitle = ExpressionUtil.getExpressionValue(this.getApplicationResourceBundle(), title);
            // testing to see if the label was translated correctly
            if (StringUtils.isNotBlank(resolvedTitle) && !resolvedTitle.equals(title)) {
              return resolvedTitle;
            }
          }
        }
        // translating the label using the userNode
        title = node.getResolvedLabel();
      }
    }

    return title == null ? "" : title;
  }

  @Override
  public URLFactory getURLFactory() {
    return urlFactory;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getMetaInformation() {
    return (Map<String, String>) request.getAttribute(REQUEST_METADATA);
  }

  public String getCacheLevel() {
    return cacheLevel;
  }

  public String getRequestParameter(String name) {
    if (parameterMap.get(name) != null && parameterMap.get(name).length > 0) {
      return parameterMap.get(name)[0];
    } else {
      return null;
    }
  }

  public String[] getRequestParameterValues(String name) {
    return parameterMap.get(name);
  }

  public Map<String, String[]> getPortletParameters() {
    Map<String, String[]> unsortedParams = parameterMap;
    Map<String, String[]> sortedParams = new HashMap<>();
    Set<String> keys = unsortedParams.keySet();
    for (String key : keys) {
      if (!key.startsWith(Constants.PARAMETER_ENCODER)) {
        sortedParams.put(key, unsortedParams.get(key));
      }
    }
    return sortedParams;
  }

  public final String getRequestContextPath() {
    return contextPath;
  }

  @Override
  public String getPortalContextPath() {
    return getRequestContextPath();
  }

  @Override
  public String getActionParameterName() {
    return PortalRequestContext.UI_COMPONENT_ACTION;
  }

  @Override
  public String getUIComponentIdParameterName() {
    return ComponentURL.PORTAL_COMPONENT_ID;
  }

  public SiteType getSiteType() {
    return siteKey.getType();
  }

  public String getSiteName() {
    return siteKey.getName();
  }

  public SiteKey getSiteKey() {
    return siteKey;
  }

  public long getSiteId() {
    return portalConfig == null ? 0l : portalConfig.getId();
  }

  public String getSiteLabel() {
    if (siteLabel == null) {
      UIPortal portal = getUiPortal();
      if (portal == null) {
        siteLabel = "";
      } else {
        SiteType siteType = portal.getSiteKey().getType();
        if (siteType == SiteType.PORTAL) {
          siteLabel = portal.getLabel();
          if (ExpressionUtil.isResourceBindingExpression(siteLabel)) {
            ResourceBundleManager bundleManager = ExoContainerContext.getService(ResourceBundleManager.class);
            ResourceBundle navigationResourceBundle = bundleManager.getNavigationResourceBundle(getLocale().toLanguageTag(),
                                                                                                siteKey.getTypeName(),
                                                                                                siteKey.getName());
            String resolvedTitle = ExpressionUtil.getExpressionValue(navigationResourceBundle, siteLabel);
            // testing to see if the label was translated correctly
            if (StringUtils.isNotBlank(resolvedTitle) && !resolvedTitle.equals(siteLabel)) {
              siteLabel = resolvedTitle;
            }
          }
        } else if (siteType == SiteType.GROUP) {
          OrganizationService organizationService = ExoContainerContext.getService(OrganizationService.class);
          try {
            Group siteGroup = organizationService.getGroupHandler().findGroupById(siteKey.getName());
            if (siteGroup != null) {
              siteLabel = siteGroup.getLabel();
            }
          } catch (Exception e) {
            LOG.error("Exception on getting site label: " + e.getMessage(), e);
          }
        }
      }
    }
    return siteLabel;
  }

  public String getPortalOwner() {
    UserPortalConfig portal = getUserPortalConfig();
    if (portal != null && portal.getPortalName() != null) {
      return portal.getPortalName();
    } else {
      return getMetaPortal();
    }
  }

  /**
   * @return meta portal name
   */
  public String getMetaPortal() {
    return portalConfigService.getMetaPortal();
  }

  /**
   * @return global portal name
   */
  public String getGlobalPortal() {
    return portalConfigService.getGlobalPortal();
  }

  public String getNodePath() {
    return nodePath;
  }

  public String getRequestURI() {
    return getNodePath();
  }

  public String getPortalURI() {
    return portalURI;
  }

  public URLBuilder<UIComponent> getURLBuilder() {
    return urlBuilder;
  }

  public int getAccessPath() {
    return request.getRemoteUser() != null ? PRIVATE_ACCESS : PUBLIC_ACCESS;
  }

  /**
   * Call to this method makes sense only in the scope of an AJAX request.
   * Invoking ignoreAJAXUpdateOnPortlets(true) as there is need to update only
   * UI components of portal (ie: the components outside portlet windows) are
   * updated by AJAX. In the request response, all the blocks PortletRespond are
   * empty. The content displayed in portlet windows are retrieved by non-AJAX
   * render request to associated portlet object.
   *
   * @param ignoreAJAXUpdateOnPortlets
   */
  public final void ignoreAJAXUpdateOnPortlets(boolean ignoreAJAXUpdateOnPortlets) {
    this.forceFullUpdate = ignoreAJAXUpdateOnPortlets;
  }

  public final void sendError(int sc) throws IOException {
    setResponseComplete(true);
    response.sendError(sc);
  }

  public final void sendRedirect(String url) throws IOException {
    setResponseComplete(true);
    if (url.contains(getGlobalPortal())) {
      String globalSiteURI = "/" + PortalContainer.getCurrentPortalContainerName() + "/" + getGlobalPortal();
      if (url.startsWith(globalSiteURI)) {
        String metaSiteURI = "/" + PortalContainer.getCurrentPortalContainerName() + "/" + getMetaPortal();
        url = url.replace(globalSiteURI, metaSiteURI);
        LOG.warn("An URI was sent with global site name, it will be replaced by default site to avoid returning HTTP 404");
      }
    }
    if (!response.isCommitted()) {
      response.sendRedirect(url);
    }
  }

  public void setHeaders(Map<String, String> headers) {
    final Set<String> keys = headers.keySet();
    for (final String key : keys) {
      response.setHeader(key, headers.get(key));
    }
  }

  @SneakyThrows
  public List<String> getExtraMarkupHeadersAsStrings() {
    List<String> markupHeaders = new ArrayList<>();
    if (extraMarkupHeaders != null && !extraMarkupHeaders.isEmpty()) {
      for (Element element : extraMarkupHeaders) {
        StringWriter sw = new StringWriter();
        DOMSerializer.serialize(element, sw);
        markupHeaders.add(sw.toString());
      }
    }
    return markupHeaders;
  }

  /**
   * Get the extra markup headers to add to the head of the html.
   *
   * @return The markup to be added.
   */
  public List<Element> getExtraMarkupHeaders() {
    return this.extraMarkupHeaders;
  }

  /**
   * Add an extra markup to the head of the html page.
   *
   * @param element The element to add
   * @param portletWindowId The ID of portlet window contributing markup header
   */
  public void addExtraMarkupHeader(Element element, String portletWindowId) {
    element.setAttribute("class", "ExHead-" + portletWindowId);
    if (this.extraMarkupHeaders == null) {
      this.extraMarkupHeaders = new ArrayList<>();
    }
    this.extraMarkupHeaders.add(element);
  }

  public RequestNavigationData getNavigationData() {
    return new RequestNavigationData(controllerContext.getParameter(RequestNavigationData.REQUEST_SITE_TYPE),
                                     controllerContext.getParameter(RequestNavigationData.REQUEST_SITE_NAME),
                                     controllerContext.getParameter(RequestNavigationData.REQUEST_PATH));
  }

  public boolean isMaximizePortlet() {
    return StringUtils.isNotBlank(getMaximizedPortletId());
  }

  public boolean isFullRendering() {
    return !isMaximizePortlet() || StringUtils.equals(getRequest().getParameter("fullRender"), "true");
  }

  public boolean isShowMaxWindow() {
    return isMaximizePortlet()
           || "true".equals(request.getParameter("showMaxWindow"))
           || (uiPage != null && uiPage.isShowMaxWindow());
  }

  public boolean isHideSharedLayout() {
    return isMaximizePortlet()
           || "true".equals(request.getParameter("hideSharedLayout"))
           || (uiPage != null && uiPage.isHideSharedLayout());
  }

  public String getMaximizedPortletId() {
    return getRequest().getParameter("maximizedPortletId");
  }

  public boolean startServerTime(String name) {
    if (ServerResponseTime.SERVER_TIMING_ENABLED) {
      if (serverResponseTime == null) {
        serverResponseTime = new ServerResponseTime();
      }
      return serverResponseTime.startServerTime(name);
    }
    return false;
  }

  public void endServerTime(String name) {
    if (serverResponseTime != null) {
      serverResponseTime.endServerTime(name);
    }
  }

  @SneakyThrows
  public void commitResponse() {
    if (serverResponseTime != null) {
      serverResponseTime.addHttpHeader(response);
      serverResponseTime = null;
    }
    if (response instanceof PortalHttpServletResponseWrapper responseWrapper) {
      responseWrapper.commit();
      responseWrapper.setWrapMethods(false);
    }
    getWriter().flush();
  }

  public static PortalRequestContext getCurrentInstance() {
    RequestContext currentInstance = RequestContext.getCurrentInstance();
    if (currentInstance == null) {
      return null;
    } else if (currentInstance instanceof PortalRequestContext portalRequestContext) {
      return portalRequestContext;
    } else {
      return (PortalRequestContext) currentInstance.getParentAppRequestContext();
    }
  }

  @SneakyThrows
  private UIPage buildUiPage(UserNode pageNode, UIPortal uiPortal) {
    PageContext pageContext = null;
    String pageReference = null;
    if (pageNode != null && pageNode.getPageRef() != null) {
      pageReference = pageNode.getPageRef().format();
      pageContext = layoutService.getPageContext(pageNode.getPageRef());
    }

    // The page has been deleted
    if (pageContext == null) {
      // Clear the UIPage from cache in UIPortal
      uiPortal.clearUIPage(pageReference);
      this.uiPage = null;
    } else {
      if (getPortalConfig() != null && PortalConfig.DRAFT.equals(portalConfig.getType())) {
        setDraftPage(true);
      } else {
        setDraftPage(pageNode.getVisibility() == Visibility.DRAFT);
      }
      if (this.page == null) {
        this.page = layoutService.getPage(pageReference);
      }
      this.uiPage = uiPortal.getUIPage(pageReference);
      if (this.uiPage == null) {
        UIPageFactory clazz = UIPageFactory.getInstance(pageContext.getState().getFactoryId());
        this.uiPage = clazz.createUIPage(this);
        pageContext.update(this.page);
        PortalDataMapper.toUIPage(this.uiPage, this.page);
      }
    }
    return this.uiPage;
  }

}
