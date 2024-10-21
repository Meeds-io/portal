/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.Constants;
import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.commons.utils.PortalPrinter;
import org.exoplatform.commons.xml.DOMSerializer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.*;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig.Builder;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.url.PortalURLContext;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageFactory;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.ControllerContext;
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

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import org.gatein.common.http.QueryStringParser;
import org.w3c.dom.Element;

/**
 * This class extends the abstract WebuiRequestContext which itself extends the
 * RequestContext one
 * <p>
 * It mainly implements the abstract methods and overide some.
 */
public class PortalRequestContext extends WebuiRequestContext {

  protected static Log                     log                 = ExoLogger.getLogger("portal:PortalRequestContext");

  public static final int                  PUBLIC_ACCESS       = 0;

  public static final int                  PRIVATE_ACCESS      = 1;

  public static final String               UI_COMPONENT_ACTION = ComponentURL.PORTAL_COMPONENT_ACTION;

  public static final String               UI_COMPONENT_ID     = ComponentURL.PORTAL_COMPONENT_ID;

  public static final String               TARGET_NODE         = "portal:targetNode";

  public static final String               CACHE_LEVEL         = "portal:cacheLevel";

  public static final String               REQUEST_TITLE       = "portal:requestTitle".intern();

  public static final String               REQUEST_METADATA    = "portal:requestMetadata".intern();

  private static final String              DO_LOGIN_PATTERN    = "login";

  /** The path decoded from the request. */
  private final String                     nodePath_;

  /** . */
  private final String                     requestURI_;

  /** . */
  private final String                     portalURI;

  /** . */
  private final String                     contextPath;

  /** . */
  private final SiteKey                    siteKey;

  /** The locale from the request. */
  private final Locale                     requestLocale;

  /** . */
  private HttpServletRequest               request_;

  /** . */
  private final HttpServletResponse        response_;

  private String                           cacheLevel_         = "cacheLevelPortlet";

  private boolean                          ajaxRequest_        = true;

  @Getter
  @Setter
  private boolean                          showMaxWindow;

  @Getter
  @Setter
  private boolean                          hideSharedLayout;

  @Setter
  private Boolean                          draftPage;

  @Setter
  private Boolean                          noCache;

  private boolean                          forceFullUpdate     = false;

  private Writer                           writer_;

  protected JavascriptManager              jsmanager_;

  private List<Element>                    extraMarkupHeaders;

  private final PortalURLBuilder           urlBuilder;

  private Map<String, String[]>            parameterMap;

  private Locale                           locale              = Locale.ENGLISH;

  private List<Runnable>                   endRequestRunnables;

  /** . */
  private final URLFactoryService          urlFactory;

  /** . */
  private final ControllerContext          controllerContext;

  /** . */
  private final DynamicPortalLayoutService portalLayoutService;

  private final UserPortalConfigService    portalConfigService;

  private final LayoutService              layoutService;

  private UserPortalConfig                 userPortalConfig;

  private PortalConfig                     currentPortalConfig;

  @Getter
  @Setter
  private UIPortal                         uiPortal;

  @Getter
  @Setter
  private UIPage                           uiPage;

  @Getter
  @Setter
  @SuppressWarnings("rawtypes")
  private List<UIPortlet>                  uiPortlets;

  @Getter
  @Setter
  private Page                             page;

  @Getter
  @Setter
  private UserNode                         userNode;

  private String                           skin;

  private String                           pageTitle           = null;

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
    this.jsmanager_ = new JavascriptManager();
    this.portalLayoutService = ExoContainerContext.getService(DynamicPortalLayoutService.class);
    this.layoutService = ExoContainerContext.getService(LayoutService.class);
    this.portalConfigService = ExoContainerContext.getService(UserPortalConfigService.class);

    //
    request_ = controllerContext.getRequest();
    response_ = controllerContext.getResponse();
    response_.setBufferSize(1024 * 100);
    contextPath = request_.getContextPath();
    setSessionId(request_.getSession().getId());

    // The encoding needs to be set before reading any of the parameters since
    // the parameters's encoding
    // is set at the first access.

    // TODO use the encoding from the locale-config.xml file
    response_.setContentType("text/html");
    try {
      request_.setCharacterEncoding("UTF-8");
    } catch (UnsupportedEncodingException e) {
      log.error("Encoding not supported", e);
    }

    // Query parameters from the request will be set in the servlet container
    // url encoding and not
    // necessarly in utf-8 format. So we need to directly parse the parameters
    // from the query string.
    parameterMap = new HashMap<String, String[]>();
    parameterMap.putAll(request_.getParameterMap());
    String queryString = request_.getQueryString();
    if (queryString != null) {
      // The QueryStringParser currently only likes & and not &amp;
      queryString = queryString.replace("&amp;", "&");
      Map<String, String[]> queryParams = QueryStringParser.getInstance().parseQueryString(queryString);
      parameterMap.putAll(queryParams);
    }

    ajaxRequest_ = "true".equals(request_.getParameter("ajaxRequest"));
    setShowMaxWindow("true".equals(request_.getParameter("showMaxWindow")));
    setHideSharedLayout("true".equals(request_.getParameter("hideSharedLayout")));
    String cache = request_.getParameter(CACHE_LEVEL);
    if (cache != null) {
      cacheLevel_ = cache;
    }

    requestURI_ = requestPath;
    /*
     * String decodedURI = URLDecoder.decode(requestURI_, "UTF-8"); //
     * req.getPathInfo will already have the encoding set from the server. // We
     * need to use the UTF-8 value since this is how we store the portal name.
     * // Reconstructing the getPathInfo from the non server decoded values.
     * String servletPath = URLDecoder.decode(request_.getServletPath(),
     * "UTF-8"); String contextPath =
     * URLDecoder.decode(request_.getContextPath(), "UTF-8"); String pathInfo =
     * "/"; if (requestURI_.length() > servletPath.length() +
     * contextPath.length()) pathInfo =
     * decodedURI.substring(servletPath.length() + contextPath.length()); int
     * colonIndex = pathInfo.indexOf("/", 1); if (colonIndex < 0) { colonIndex =
     * pathInfo.length(); } portalOwner_ = pathInfo.substring(1, colonIndex);
     * nodePath_ = pathInfo.substring(colonIndex, pathInfo.length());
     */
    //
    this.siteKey = new SiteKey(SiteType.valueOf(requestSiteType.toUpperCase()), requestSiteName);
    this.nodePath_ = requestPath;
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

  public JavascriptManager getJavascriptManager() {
    return jsmanager_;
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

  public boolean isDraftPage() {
    if (draftPage == null) {
      UserNode navigationNode = getNavigationNode();
      draftPage = navigationNode != null && navigationNode.getVisibility() == Visibility.DRAFT;
    }
    return draftPage.booleanValue();
  }

  @SneakyThrows
  public UserNode getNavigationNode() {
    if (userNode != null) {
      return userNode;
    }
    UserPortal userPortal = getUserPortalConfig().getUserPortal();
    UserNavigation navigation = userPortal.getNavigation(siteKey);
    if (navigation != null) {
      Builder builder = UserNodeFilterConfig.builder().withReadCheck();
      if (StringUtils.isBlank(nodePath_)) {
        userNode = portalConfigService.getPortalSiteRootNode(siteKey.getName(), siteKey.getTypeName(), request_);
        if (userNode != null) {
          userNode = portalConfigService.getFirstAllowedPageNode(Collections.singletonList(userNode));
        }
      } else {
        userNode = userPortal.resolvePath(navigation, builder.build(), nodePath_);
      }
    }
    return userNode;
  }

  public UserPortalConfig getUserPortalConfig() {
    String remoteUser = null;
    if (userPortalConfig == null) {
      ConversationState conversationState = ConversationState.getCurrent();
      if (conversationState != null
          && conversationState.getIdentity() != null
          && !IdentityConstants.ANONIM.equals(conversationState.getIdentity().getUserId())) {
        remoteUser = conversationState.getIdentity().getUserId();
      }

      String portalName = getCurrentPortalSite();
      try {
        userPortalConfig = portalConfigService.getUserPortalConfig(portalName,
                                                                   remoteUser);
      } catch (Exception e) {
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
      portalName = portalConfigService.getMetaPortal();
    }
    return portalName;
  }

  public void refreshPortalConfig() {
    this.userPortalConfig = null;
    this.currentPortalConfig = null;
  }

  public UIPage getUIPage(UserNode pageNode, UIPortal uiPortal) throws Exception {
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
      return null;
    } else {
      setDraftPage(pageNode.getVisibility() == Visibility.DRAFT);
      this.page = layoutService.getPage(pageReference);
      if (uiPortal.getUIPage(pageReference) == null) {
        UIPageFactory clazz = UIPageFactory.getInstance(pageContext.getState().getFactoryId());
        this.uiPage = clazz.createUIPage(this);
        pageContext.update(this.page);
        PortalDataMapper.toUIPage(this.uiPage, this.page);
      }
      return this.uiPage;
    }
  }

  public String getInitialURI() {
    return request_.getRequestURI();
  }

  public ControllerContext getControllerContext() {
    return controllerContext;
  }

  public void refreshResourceBundle() throws Exception {
    appRes_ = getApplication().getResourceBundle(getLocale());
  }

  public void requestAuthenticationLogin() throws Exception {
    requestAuthenticationLogin(null);
  }

  public void requestAuthenticationLogin(Map<String, String> params) throws Exception {
    StringBuilder initialURI = new StringBuilder();
    initialURI.append(request_.getRequestURI());
    if (request_.getQueryString() != null) {
      initialURI.append("?").append(request_.getQueryString());
    }

    StringBuilder loginPath = new StringBuilder();

    // . Check SSO Enable
    ExoContainer container = getApplication().getApplicationServiceContainer();
    SSOHelper ssoHelper = container.getComponentInstanceOfType(SSOHelper.class);
    if (ssoHelper != null && ssoHelper.isSSOEnabled() && ssoHelper.skipJSPRedirection()) {
      loginPath.append(getPortalContextPath()).append(ssoHelper.getSSORedirectURLSuffix());
    } else {
      loginPath.append(getPortalContextPath()).append("/").append(DO_LOGIN_PATTERN);
    }

    loginPath.append("?initialURI=").append(URLEncoder.encode(initialURI.toString(), "UTF-8"));
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

  public String getTitle() throws Exception {
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
        ExoContainer container = getApplication().getApplicationServiceContainer();
        UserPortalConfigService configService = container.getComponentInstanceOfType(UserPortalConfigService.class);
        PageKey pageRef = node.getPageRef();
        PageContext pageContext = configService.getPage(pageRef);

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

  public Orientation getOrientation() {
    return ((UIPortalApplication) uiApplication_).getOrientation();
  }

  public Locale getRequestLocale() {
    return requestLocale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public Locale getLocale() {
    return locale;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getMetaInformation() {
    return (Map<String, String>) request_.getAttribute(REQUEST_METADATA);
  }

  public String getCacheLevel() {
    return cacheLevel_;
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
    Map<String, String[]> sortedParams = new HashMap<String, String[]>();
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

  public String getPortalOwner() {
    UserPortalConfig portalConfig = getUserPortalConfig();
    if (portalConfig != null && portalConfig.getPortalName() != null) {
      return portalConfig.getPortalName();
    } else {
      return portalConfigService.getMetaPortal();
    }
  }

  /**
   * @return meta portal name
   */
  public String getMetaPortal() {
    return portalConfigService.getMetaPortal();
  }

  /**
   * @return default portal name
   * @deprecated notion of 'default' portal doesn't exist anymore
   */
  public String getDefaultPortal() {
    return getMetaPortal();
  }

  public String getNodePath() {
    return nodePath_;
  }

  public String getRequestURI() {
    return requestURI_;
  }

  public String getPortalURI() {
    return portalURI;
  }

  public URLBuilder<UIComponent> getURLBuilder() {
    return urlBuilder;
  }

  public int getAccessPath() {
    return request_.getRemoteUser() != null ? PRIVATE_ACCESS : PUBLIC_ACCESS;
  }

  public final String getRemoteUser() {
    return request_.getRemoteUser();
  }

  public final boolean isUserInRole(String roleUser) {
    return request_.isUserInRole(roleUser);
  }

  public final Writer getWriter() throws IOException {
    if (writer_ == null) {
      writer_ = new PortalPrinter(response_.getOutputStream(), false, 30000);
    }
    return writer_;
  }

  public final void setWriter(Writer writer) {
    this.writer_ = writer;
  }

  public final boolean useAjax() {
    return ajaxRequest_;
  }

  @SuppressWarnings("unchecked")
  public HttpServletRequest getRequest() {
    return request_;
  }

  @SuppressWarnings("unchecked")
  public final HttpServletResponse getResponse() {
    return response_;
  }

  /**
   * @see org.exoplatform.web.application.RequestContext#getFullRender()
   */
  public final boolean getFullRender() {
    return forceFullUpdate;
  }

  /**
   * Sets a boolean value to force whether portal will be fully rendered and it
   * is only effective to an Ajax request. <br>
   * if the value is set to <code>true</code>, it means :<br>
   * 1) Only portal ui components are rendered <br>
   * 2) Portlets will be fully rendered if are inner of the portal ui components
   * being updated
   *
   * @param forceFullUpdate This method is deprecated,
   *          ignoreAJAXUpdateOnPortlets should be used instead
   */
  @Deprecated()
  public final void setFullRender(boolean forceFullUpdate) {
    this.forceFullUpdate = forceFullUpdate;
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
    response_.sendError(sc);
  }

  public final void sendRedirect(String url) throws IOException {
    setResponseComplete(true);
    if (url.contains(portalConfigService.getGlobalPortal())) {
      String globalSiteURI = "/" + PortalContainer.getCurrentPortalContainerName() + "/" + portalConfigService.getGlobalPortal();
      if (url.startsWith(globalSiteURI)) {
        String metaSiteURI = "/" + PortalContainer.getCurrentPortalContainerName() + "/" + portalConfigService.getMetaPortal();
        url = url.replace(globalSiteURI, metaSiteURI);
        log.warn("An URI was sent with global site name, it will be replaced by default site to avoid returning HTTP 404");
      }
    }
    response_.sendRedirect(url);
  }

  public void setHeaders(Map<String, String> headers) {
    final Set<String> keys = headers.keySet();
    for (final String key : keys) {
      response_.setHeader(key, headers.get(key));
    }
  }

  public List<String> getExtraMarkupHeadersAsStrings() throws Exception {
    List<String> markupHeaders = new ArrayList<String>();

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

  public String getMaximizedPortletId() {
    return getRequest().getParameter("maximizedPortletId");
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

}
