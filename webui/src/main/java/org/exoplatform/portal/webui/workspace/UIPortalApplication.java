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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.pc.portlet.impl.info.ContainerPortletInfo;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.script.FetchMap;
import org.gatein.portal.controller.resource.script.FetchMode;
import org.gatein.portal.controller.resource.script.Module;
import org.gatein.portal.controller.resource.script.ScriptResource;
import org.json.JSONObject;

import org.exoplatform.commons.addons.AddOnService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.resource.Skin;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.resource.SkinURL;
import org.exoplatform.portal.resource.SkinVisitor;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.portal.UISharedLayout;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.url.MimeType;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.url.ComponentURL;

import lombok.SneakyThrows;

/**
 * This extends the UIApplication and hence is a sibling of UIPortletApplication
 * (used by any eXo Portlets as the Parent class to build the portlet component
 * tree). The UIPortalApplication is responsible to build its subtree according
 * to some configuration parameters. If all components are displayed it is
 * composed of 2 UI components: -UIWorkingWorkSpace: the right part that can
 * display the normal or webos portal layouts - UIPopupWindow: a popup window
 * that display or not
 */
@ComponentConfig(lifecycle = UIPortalApplicationLifecycle.class, template = "system:/groovy/portal/webui/workspace/UIPortalApplication.gtmpl")
public class UIPortalApplication extends UIApplication {

  private static final long       serialVersionUID          = 4289299002617728318L;

  public static final String      PORTAL_BODY_END_CONTAINER = "body-end-container";

  public static final String      PORTAL_PORTLETS_SKIN_ID   = "portalPortletSkins";

  /**
   * The normal, non-edit mode.
   */
  public static final int         NORMAL_MODE               = 0;

  /**
   * The combination of {@link EditMode#BLOCK} and
   * {@link ComponentTab#APPLICATIONS}.
   */
  public static final int         APP_BLOCK_EDIT_MODE       = 1;

  /**
   * The combination of {@link EditMode#PREVIEW} and
   * {@link ComponentTab#APPLICATIONS}.
   */
  public static final int         APP_VIEW_EDIT_MODE        = 2;

  /**
   * The combination of {@link EditMode#BLOCK} and
   * {@link ComponentTab#CONTAINERS}.
   */
  public static final int         CONTAINER_BLOCK_EDIT_MODE = 3;

  /**
   * The combination of {@link EditMode#PREVIEW} and
   * {@link ComponentTab#CONTAINERS}.
   */
  public static final int         CONTAINER_VIEW_EDIT_MODE  = 4;

  public static final UIComponent EMPTY_COMPONENT           = new UIComponent() {
                                                              @Override
                                                              public String getId() {
                                                                return "_portal:componentId_";
                                                              }
                                                            };

  public static final String      UI_WORKING_WS_ID          = "UIWorkingWorkspace";

  public static final String      UI_VIEWING_WS_ID          = "UIViewWS";

  public static final String      UI_MASK_WS_ID             = "UIMaskWorkspace";

  private static final Log        LOG                       = ExoLogger.getLogger("portal:UIPortalApplication");

  public enum EditMode {
    /**
     * Edit mode with plain rectangles in place of portlets.
     */
    BLOCK,
    /**
     * Edit mode with portlets rendered.
     */
    PREVIEW,

    NO_EDIT
  }

  public enum ComponentTab {
    /**
     * For situations when Applications Tab of Page Editor dialog is selected.
     */
    APPLICATIONS,
    /**
     * For situations when Containers Tab of Page Editor dialog is selected.
     */
    CONTAINERS,

    NO_EDIT
  }

  public enum EditLevel {
    NO_EDIT, EDIT_SITE, EDIT_PAGE
  }

  private static SkinService             skinService;                       // NOSONAR

  private static AddOnService            addOnService;                      // NOSONAR

  private static UserPortalConfigService portalConfigService;               // NOSONAR

  private static LayoutService           layoutService;                     // NOSONAR

  private static SkinVisitor             skinVisitor;                       // NOSONAR

  protected UIWorkingWorkspace           uiWorkingWorkspace;                // NOSONAR

  private Map<SiteKey, UIPortal>         all_UIPortals;                     // NOSONAR

  private UIComponentDecorator           uiViewWorkingWorkspace;            // NOSONAR

  private List<String>                   bodyEndContainerPortletContentIds; // NOSONAR

  /**
   * The constructor of this class is used to build the tree of UI components
   * that will be aggregated in the portal page.<br>
   * 1) The component is stored in the current PortalRequestContext
   * ThreadLocal<br>
   * 2) The configuration for the portal associated with the current user
   * request is extracted from the PortalRequestContext<br>
   * 3) Then according to the context path, either a public or private portal is
   * initiated. Usually a public portal does not contain the left column and
   * only the private one has it.<br>
   * 4) The skin to use is setup <br>
   * 5) Finally, the current component is associated with the current portal
   * owner
   *
   * @throws Exception
   */
  public UIPortalApplication() throws Exception {
    PortalRequestContext context = getPortalRequestContext();
    if (skinService == null) {
      skinService = getApplicationComponent(SkinService.class); // NOSONAR
      skinVisitor = getApplicationComponent(SkinVisitor.class); // NOSONAR
      layoutService = getApplicationComponent(LayoutService.class); // NOSONAR
      portalConfigService = getApplicationComponent(UserPortalConfigService.class); // NOSONAR
      addOnService = getApplicationComponent(AddOnService.class); // NOSONAR
    }
    context.setUIApplication(this);

    this.all_UIPortals = new HashMap<>();
    initWorkspaces(context.getPortalOwner());
    // Listen to storage to update cached pages when updated
    ListenerService listenerService = ExoContainerContext.getService(ListenerService.class);
    listenerService.addListener(LayoutService.PORTAL_CONFIG_UPDATED, new RefreshUIPortalListener());
  }

  /**
   * Returns current UIPortal which being showed in normal mode
   *
   * @return
   */
  public UIPortal getCurrentSite() {
    return getPortalRequestContext().getUiPortal();
  }

  @SneakyThrows
  public UIPortal getUiPortal(SiteKey siteKey) {
    UIPortal cachedUIPortal = getCachedUIPortal(siteKey);
    if (cachedUIPortal == null) {
      PortalRequestContext portalRequestContext = getPortalRequestContext();
      PortalConfig portalConfig = portalRequestContext.getDynamicPortalConfig();
      Container layout = portalConfig.getPortalLayout();
      if (layout != null) {
        portalRequestContext.getUserPortalConfig().setPortalConfig(portalConfig);
      }
      cachedUIPortal = createUIComponent(UIPortal.class, null, null);
      // Reset selected navigation on userPortalConfig
      PortalDataMapper.toUIPortalWithMetaLayout(cachedUIPortal, portalRequestContext.getDynamicPortalConfig());
      putCachedUIPortal(cachedUIPortal);
    }
    return cachedUIPortal;
  }

  public UIPortal getCachedUIPortal(SiteKey key) {
    if (key == null || isDraftSite() || isNoCache()) {
      return null;
    } else {
      return this.all_UIPortals.get(key);
    }
  }

  /**
   * Associates the specified UIPortal to a cache map with specified key which
   * bases on OwnerType and OwnerId
   *
   * @param uiPortal
   */
  public void putCachedUIPortal(UIPortal uiPortal) {
    if (!isDraftSite()) {
      SiteKey siteKey = uiPortal.getSiteKey();
      if (siteKey != null) {
        this.all_UIPortals.put(siteKey, uiPortal);
      }
    }
  }

  /**
   * Remove the UIPortal from the cache map
   *
   * @param ownerType
   * @param ownerId
   */
  public void removeCachedUIPortal(String ownerType, String ownerId) {
    if (ownerType != null && ownerId != null) {
      this.all_UIPortals.remove(new SiteKey(ownerType, ownerId));
    }
  }

  /**
   * Invalidate any UIPage cache object associated to UIPortal objects
   *
   * @param pageRef
   */
  public void invalidateUIPage(String pageRef) {
    for (UIPortal tmp : all_UIPortals.values()) {
      tmp.clearUIPage(pageRef);
    }
  }

  public void refreshCachedUI() {
    all_UIPortals.clear();
  }

  public Orientation getOrientation() {
    return getPortalRequestContext().getOrientation();
  }

  public Locale getLocale() {
    return Util.getPortalRequestContext().getLocale();
  }

  public int getModeState() {
    return NORMAL_MODE;
  }

  /**
   * @return false all time
   * @deprecated no more WebUI based layout management, thus useless
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public boolean isEditing() {
    return false;
  }

  /**
   * @return EditMode.NO_EDIT all time
   * @deprecated no more WebUI based layout management, thus useless
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public static EditMode getDefaultEditMode() {
    return EditMode.NO_EDIT;
  }

  /**
   * Return a map of JS resource ids (required to be load for current page) and
   * boolean: true if that script should be push on the header before html.
   * false if that script should be load lazily after html has been loaded <br>
   * JS resources always contains SHARED/bootstrap required to be loaded eagerly
   * and optionally (by configuration) contains: portal js, portlet js, and
   * resouces registered to be load through JavascriptManager
   *
   * @return
   */
  public Map<String, Boolean> getScripts() {
    PortalRequestContext prc = getPortalRequestContext();
    JavascriptManager jsMan = prc.getJavascriptManager();

    //
    FetchMap<ResourceId> requiredResources = jsMan.getScriptResources();

    //
    JavascriptConfigService service = getApplicationComponent(JavascriptConfigService.class);
    Map<String, Boolean> ret = new LinkedHashMap<>();
    Map<String, Boolean> tmp = new LinkedHashMap<>();
    Map<ScriptResource, FetchMode> resolved = service.resolveIds(requiredResources);
    for (ScriptResource rs : resolved.keySet()) {
      ResourceId id = rs.getId();
      // SHARED/bootstrap should be loaded first
      if (ResourceScope.SHARED.equals(id.getScope()) && "bootstrap".equals(id.getName())) {
        ret.put(id.toString(), false);
      } else {
        boolean isRemote = !rs.isEmpty() && rs.getModules().get(0) instanceof Module.Remote;
        tmp.put(id.toString(), isRemote);
      }
    }
    ret.putAll(tmp);
    for (String url : jsMan.getExtendedScriptURLs()) {
      ret.put(url, true);
    }
    return ret;
  }

  /**
   * Return a map of GMD resource ids and their URLs that point to
   * ResourceRequestHandler. this map will be used by GateIn JS module loader
   * (currently, it is requirejs)
   */
  public JSONObject getJSConfig() {
    return getApplicationComponent(JavascriptConfigService.class).getJSConfig();
  }

  public Collection<SkinConfig> getPortalSkins(SkinVisitor visitor) {
    if (visitor != null) {
      return skinService.findSkins(visitor);
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Return corresponding collection of Skin objects depends on current skin
   * name, this object help to build URL that point to
   * SkinResourceRequestHandler. this handler is responsible to serves for css
   * files <br>
   * The collection contains: - portal skin modules <br>
   * - skin for specific site<br>
   */
  public Collection<SkinConfig> getPortalSkins() {
    String skin = getSkin();
    List<SkinConfig> skins = null;
    if (skinVisitor == null) {
      skins = new ArrayList<>(skinService.getPortalSkins(skin));
    } else {
      skins = new ArrayList<>(getPortalSkins(skinVisitor));
    }

    //
    SkinConfig skinConfig = skinService.getSkin(getCurrentSite().getName(), skin);
    if (skinConfig != null) {
      skins.add(skinConfig);
    }
    Collections.sort(skins, (s1, s2) -> s1.getCSSPriority() - s2.getCSSPriority());
    return skins;
  }

  public Collection<SkinConfig> getCustomSkins() {
    return skinService.getCustomPortalSkins(getSkin());
  }

  public String getBrandingUrl() {
    BrandingService brandingService = getApplicationComponent(BrandingService.class);
    long lastUpdatedTime = brandingService.getLastUpdatedTime();
    return "/" + PortalContainer.getCurrentPortalContainerName() + "/" + PortalContainer.getCurrentRestContextName() +
        "/v1/platform/branding/css?v=" + lastUpdatedTime;
  }

  public String getSkin() {
    return getPortalRequestContext().getSkin();
  }

  /**
   * Returns a set of portlets skin that have to be added in the HTML head tag.
   * Those portlets doesn't belongs to portal
   *
   * @return the portlet skins
   */
  public Set<Skin> getPortletSkins() {
    String skin = getSkin();
    List<SkinConfig> portletSkins = getCurrentPortlets().stream()
                                                        .map(this::getPortletSkinConfig)
                                                        .filter(Objects::nonNull)
                                                        .toList();
    // Load static body-end-container applications added on UIPortalApplication
    // Body independently from SharedLayout Display State and from displayed
    // Page/Site
    List<SkinConfig> bodyEndPortletSkins = getBodyEndContainerContentIds().stream()
                                                                          .map(this::getPortletSkinConfig)
                                                                          .filter(Objects::nonNull)
                                                                          .toList();
    List<SkinConfig> additionalSkins = Stream.concat(portletSkins.stream(), bodyEndPortletSkins.stream())
                                             .filter(portletSkin -> portletSkin instanceof SkinConfig skinConfig
                                                                    && CollectionUtils.isNotEmpty(skinConfig.getAdditionalModules()))
                                             .map(SkinConfig::getAdditionalModules)
                                             .flatMap(List::stream)
                                             .distinct()
                                             .map(module -> skinService.getPortalSkin(module, skin))
                                             .filter(Objects::nonNull)
                                             .toList();
    return Stream.concat(Stream.concat(portletSkins.stream(), bodyEndPortletSkins.stream()), additionalSkins.stream())
                 .filter(Objects::nonNull)
                 .filter(c -> !(c instanceof SkinConfig skinConfig) || skinConfig.getCSSPath() != null)
                 .sorted((s1, s2) -> s1.getCSSPriority() - s2.getCSSPriority())
                 .collect(Collectors.toSet());
  }

  /**
   * @return a set of current page portlet names
   */
  public Set<String> getPortletNames() {
    return getPagePortletInfos().stream()
                                .map(ContainerPortletInfo::getName)
                                .collect(Collectors.toSet());
  }

  /**
   * @return a set of current page portlet resource bundle names to preload
   */
  public Set<String> getPortletBundles() {
    return getInitParamsOfPagePortlets("preload.resource.bundles");
  }

  /**
   * @return a set of current page portlet additonal stylesheet files to preload
   */
  public Set<String> getPortletStylesheets() {
    return getInitParamsOfPagePortlets("preload.resource.stylesheet");
  }

  public Set<String> getInitParamsOfPagePortlets(String paramName) {
    List<ContainerPortletInfo> portletInfos = getPagePortletInfos();
    Set<String> result = new HashSet<>();
    for (ContainerPortletInfo portletInfo : portletInfos) {
      String separator = portletInfo.getInitParameter("separator");
      String valuesString = portletInfo.getInitParameter(paramName);
      String[] valuesArray;
      if (StringUtils.isNotBlank(valuesString)) {
        if (StringUtils.isBlank(separator)) {
          valuesArray = valuesString.contains("|") ? StringUtils.split(valuesString, '|') : StringUtils.split(valuesString, ',');
        } else {
          valuesArray = StringUtils.split(valuesString, separator);
        }
        for (String value : valuesArray) {
          if (StringUtils.isNotBlank(value)) {
            result.add(value.trim());
          }
        }
      }
    }
    return result;
  }

  /**
   * Find portlets visible on the page
   * 
   * @return {@link List} of {@link ContainerPortletInfo} corresponding to
   *         portlet info on the page
   */
  public List<ContainerPortletInfo> getPagePortletInfos() {
    return getCurrentPortlets().stream()
                               .filter(p -> p.getProducedOfferedPortlet() != null)
                               .map(pinfo -> pinfo.getProducedOfferedPortlet().getInfo())
                               .filter(ContainerPortletInfo.class::isInstance)
                               .map(p -> (ContainerPortletInfo) p)
                               .toList();
  }

  private SkinConfig getPortletSkinConfig(UIPortlet portlet) {
    String portletId = portlet.getSkinId();
    return getPortletSkinConfig(portletId);
  }

  private SkinConfig getPortletSkinConfig(String contentId) {
    if (contentId != null) {
      return skinService.getSkin(contentId, getSkin());
    } else {
      return null;
    }
  }

  private List<String> getBodyEndContainerContentIds() {
    if (bodyEndContainerPortletContentIds == null) {
      List<Application> applications = addOnService.getApplications(PORTAL_BODY_END_CONTAINER);
      bodyEndContainerPortletContentIds = applications.stream()
                                                      .map(app -> ((TransientApplicationState) app.getState()).getContentId())
                                                      .toList();
    }
    return bodyEndContainerPortletContentIds;
  }

  /**
   * The central area is called the WorkingWorkspace. It is composed of: 1) A
   * UIPortal child which is filled with portal data using the PortalDataMapper
   * helper tool 2) A UIPortalToolPanel which is not rendered by default A
   * UIMaskWorkspace is also added to provide powerfull focus only popups
   *
   * @throws Exception
   */
  private void initWorkspaces(String portalName) throws Exception {
    if (this.getChildById(UIPortalApplication.UI_WORKING_WS_ID) != null) {
      this.removeChildById(UIPortalApplication.UI_WORKING_WS_ID);
    }
    this.uiWorkingWorkspace = this.addChild(UIWorkingWorkspace.class, UIPortalApplication.UI_WORKING_WS_ID, null);
    this.uiViewWorkingWorkspace = this.uiWorkingWorkspace.addChild(UIComponentDecorator.class, null, UI_VIEWING_WS_ID);
    initSharedLayout(portalName);
  }

  private void initSharedLayout(String portalName) throws Exception {
    Container container = layoutService.getSharedLayout(portalName);
    if (container != null) {
      UISharedLayout uiContainer = createUIComponent(UISharedLayout.class, null, null);
      uiContainer.setStorageId(container.getStorageId());
      PortalDataMapper.toUIContainer(uiContainer, container);
      uiContainer.setRendered(true);
      this.uiViewWorkingWorkspace.setUIComponent(uiContainer);
    }
  }

  /**
   * The processAction() method is doing 3 actions: <br>
   * 1) if this is a non ajax request and the last is an ajax one, then we check
   * if the requested nodePath is equal to last non ajax nodePath and is not
   * equal to the last nodePath, the server performs a 302 redirect on the last
   * nodePath.<br>
   * 2) if the nodePath exist but is equals to the current one then we also call
   * super and stops here.<br>
   * 3) if the requested nodePath is not equals to the current one or current
   * page no longer exists, then an event of type PageNodeEvent.CHANGE_NODE is
   * sent to the associated EventListener; a call to super is then done.
   */
  @Override
  public void processAction(WebuiRequestContext context) throws Exception {
    PortalRequestContext pcontext = PortalRequestContext.getCurrentInstance();
    UserNode targetNode = pcontext.getNavigationNode();
    if (targetNode == null) {
      // If unauthenticated users have no permission on PORTAL node and
      // URL is valid, they will be required to login
      if (pcontext.getRemoteUser() == null) {
        pcontext.requestAuthenticationLogin();
        return;
      } else {
        // If path to node is invalid, get the default node instead of.
        pcontext.sendRedirect("/portal/" + portalConfigService.getMetaPortal() + "/page-not-found");
        return;
      }
    }

    if (isRefreshPage()) {
      StringBuilder js = new StringBuilder("eXo.env.server.portalBaseURL=\"");
      js.append(getBaseURL()).append("\";\n");

      String url = getPortalURLTemplate();
      js.append("eXo.env.server.portalURLTemplate=\"");
      js.append(url).append("\";");

      JavascriptManager javascriptManager = pcontext.getJavascriptManager();
      javascriptManager.addJavascript(js.toString());

      uiWorkingWorkspace.setRenderedChild(UIPortalApplication.UI_VIEWING_WS_ID);
      pcontext.ignoreAJAXUpdateOnPortlets(!pcontext.useAjax());
    }

    if (pcontext.isResponseComplete()) {
      return;
    } else if (getCurrentSite() == null || getCurrentSite().getSelectedUserNode() == null) {
      // If path to node is invalid, get the default node instead of.
      pcontext.sendRedirect("/portal/" + portalConfigService.getMetaPortal() + "/page-not-found");
    }
    super.processAction(pcontext);
  }

  /**
   * The processrender() method handles the creation of the returned HTML either
   * for a full page render or in the case of an AJAX call The first request,
   * Ajax is not enabled (means no ajaxRequest parameter in the request) and
   * hence the super.processRender() method is called. This will hence call the
   * processrender() of the Lifecycle object as this method is not overidden in
   * UIPortalApplicationLifecycle. There we simply render the bounded template
   * (groovy usually). Note that bounded template are also defined in component
   * annotations, so for the current class it is UIPortalApplication.gtmpl On
   * second calls, request have the "ajaxRequest" parameter set to true in the
   * URL. In that case the algorithm is a bit more complex: a) The list of
   * components that should be updated is extracted using the
   * context.getUIComponentToUpdateByAjax() method. That list was setup during
   * the process action phase b) Portlets and other UI components to update are
   * split in 2 different lists c) Portlets full content are returned and set
   * with the tag {@code <div class="PortalResponse">} d) Block to updates
   * (which are UI components) are set within the
   * {@code <div class="PortalResponseData">} tag e) Extra markup headers are in
   * the {@code <div class="MarkupHeadElements">} tag f) additional scripts are
   * in {@code <div class="ImmediateScripts">}, JS GMD modules will be loaded by
   * generated JS command on AMD js loader, and is put into PortalResponseScript
   * block g) Then the scripts and the skins to reload are set in the
   * {@code <div class="PortalResponseScript">}
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception { // NOSONAR
    PortalRequestContext portalRequestContext = PortalRequestContext.getCurrentInstance();
    portalRequestContext.startServerTime("UIPortalApplication");
    String maximizedPortletId = getMaximizedPortletId();
    if (StringUtils.isNotBlank(maximizedPortletId)) {
      UIPortlet maximizedUiPortlet = getCurrentPortlets().stream()
                                                         .filter(p -> StringUtils.equals(p.getStorageId(), maximizedPortletId)
                                                                      || StringUtils.equals(p.getId(), maximizedPortletId))
                                                         .findFirst()
                                                         .orElseThrow(() -> new IllegalStateException(String.format("Portlet with id %s to maximize wasn't found in page with title '%s'",
                                                                                                                    maximizedPortletId,
                                                                                                                    getCurrentPage().getTitle())));
      UIPage uiPage = getCurrentPage();
      uiPage.normalizePortletWindowStates();
      portalRequestContext.setMaximizedUIPortlet(maximizedUiPortlet);
    }
    try {
      portalRequestContext.setAttribute("requestStartTime", System.currentTimeMillis());

      //
      if (!portalRequestContext.useAjax()) {
        super.processRender(portalRequestContext);
      } else {
        Writer w = portalRequestContext.getWriter();
        JavascriptManager jsManager = portalRequestContext.getJavascriptManager();
        Set<UIComponent> list = portalRequestContext.getUIComponentToUpdateByAjax();
        List<UIPortlet> uiPortlets = new ArrayList<>(3);
        List<UIComponent> uiDataComponents = new ArrayList<>(5);
        if (list != null) {
          for (UIComponent uicomponent : list) {
            if (uicomponent instanceof UIPortlet uiPortlet) {
              uiPortlets.add(uiPortlet);
            } else {
              uiDataComponents.add(uicomponent);
            }
          }
        }
        w.write("<div class=\"PortalResponse\">");
        w.write("<div class=\"PortalResponseData\">");
        for (UIComponent uicomponent : uiDataComponents) {
          renderBlockToUpdate(uicomponent, portalRequestContext, w);
        }
        w.write("</div>"); // NOSONAR

        if (!portalRequestContext.getFullRender()) {
          for (UIPortlet uiPortlet : uiPortlets) {
            w.write("<div class=\"PortletResponse\" style=\"display: none\">");
            w.append("<div class=\"PortletResponsePortletId\">" + uiPortlet.getId() + "</div>");
            w.append("<div class=\"PortletResponseData\">");

            /*
             * If the portlet is using our UI framework or supports it then it
             * will return a set of block to updates. If there is not block to
             * update the javascript client will see that as a full refresh of
             * the content part
             */
            uiPortlet.processRender(portalRequestContext);

            w.append("</div>");
            w.append("<div class=\"PortletResponseScript\"></div>");
            w.write("</div>");
          }
        }
        w.write("<div class=\"MarkupHeadElements\">");
        List<String> headElems = portalRequestContext.getExtraMarkupHeadersAsStrings();
        for (String elem : headElems) {
          w.write(elem);
        }
        w.write("</div>");
        w.write("<div class=\"LoadingScripts\">");
        writeLoadingScripts(portalRequestContext);
        w.write("</div>");
        w.write("<div class=\"PortalResponseScript\">");
        String skin = getAddSkinScript(list);
        if (skin != null) {
          jsManager.require("SHARED/skin", "skin").addScripts(skin);
        }
        w.write(jsManager.getJavaScripts());
        w.write("</div>");
        w.write("</div>");
      }
    } finally {
      if (StringUtils.isNotBlank(maximizedPortletId)) {
        UIPage uiPage = getCurrentPage();
        uiPage.normalizePortletWindowStates();
        portalRequestContext.setMaximizedUIPortlet(null);
      }
      portalRequestContext.endServerTime("UIPortalApplication");
    }
  }

  private void writeLoadingScripts(PortalRequestContext context) throws IOException {
    Writer w = context.getWriter();
    Map<String, Boolean> scriptURLs = getScripts();
    w.write("<div class=\"ImmediateScripts\">");
    w.write(StringUtils.join(scriptURLs.keySet(), ","));
    w.write("</div>");
  }

  private String getAddSkinScript(Set<UIComponent> updateComponents) {
    if (updateComponents == null) {
      return null;
    }
    List<UIPortlet> uiportlets = new ArrayList<>();
    for (UIComponent uicomponent : updateComponents) {
      if (uicomponent instanceof UIContainer uiContainer) {
        uiContainer.findComponentOfType(uiportlets, UIPortlet.class);
      }
    }

    List<SkinConfig> skins = new ArrayList<>();
    for (UIPortlet uiPortlet : uiportlets) {
      String skinId = uiPortlet.getSkinId();
      if (skinId != null) {
        SkinConfig skinConfig = skinService.getSkin(skinId, getSkin());
        if (skinConfig != null) {
          skins.add(skinConfig);
        }
      }
    }
    StringBuilder b = new StringBuilder(1000);
    for (SkinConfig ele : skins) {
      SkinURL url = ele.createURL();
      url.setOrientation(getOrientation());
      b.append("skin.addSkin('").append(ele.getId()).append("','").append(url).append("');\n");
    }

    return b.toString();
  }

  /**
   * @return User Home page preference
   */
  public String getUserHomePage() {
    return portalConfigService.getUserHomePage(getPortalRequestContext().getRemoteUser());
  }

  /**
   * Return the portal url template which will be sent to client ( browser ) and
   * used for JS based portal url generation.
   * <p>
   * The portal url template are calculated base on the current request and site
   * state. Something like :
   * {@code "/portal/g/:platform:administrators/administration/registry?portal:componentId={portal:uicomponentId}&portal:action={portal:action}"
   * ;}
   *
   * @return return portal url template
   */
  public String getPortalURLTemplate() {
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    ComponentURL urlTemplate = pcontext.createURL(ComponentURL.TYPE);
    urlTemplate.setMimeType(MimeType.PLAIN);
    urlTemplate.setPath(pcontext.getNodePath());
    urlTemplate.setResource(EMPTY_COMPONENT);
    urlTemplate.setAction("_portal:action_");
    return urlTemplate.toString();
  }

  public String getBaseURL() {
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    NodeURL nodeURL = pcontext.createURL(NodeURL.TYPE,
                                         new NavigationResource(pcontext.getSiteKey(), pcontext.getNodePath()));
    return nodeURL.toString();
  }

  public void includePortletScripts() {
    PortalRequestContext pcontext = getPortalRequestContext();
    JavascriptManager jsMan = pcontext.getJavascriptManager();
    List<UIPortlet> portlets = new ArrayList<>();
    uiViewWorkingWorkspace.findComponentOfType(portlets, UIPortlet.class);
    for (UIPortlet uiPortlet : portlets) {
      try {
        jsMan.loadScriptResource(ResourceScope.PORTLET, uiPortlet.getApplicationId());
      } catch (Exception e) {
        LOG.warn("Can't load JS resource for portlet {}", uiPortlet.getName(), e);
      }
    }
    // Load static body-end-container applications added on UIPortalApplication
    // Body independently from SharedLayout Display State and from displayed
    // Page/Site
    getBodyEndContainerContentIds().forEach(contentId -> {
      try {
        jsMan.loadScriptResource(ResourceScope.PORTLET, contentId);
      } catch (Exception e) {
        LOG.warn("Can't load JS resource for portlet {}", contentId, e);
      }
    });
  }

  public UIPage getCurrentPage() {
    return getPortalRequestContext().getUiPage();
  }

  private boolean isRefreshPage() {
    return getCurrentSite() == null
           || isDraftPage()
           || isMaximizePortlet();
  }

  private boolean isDraftPage() {
    return getPortalRequestContext().isDraftPage();
  }

  private boolean isDraftSite() {
    return getPortalRequestContext().isDraftSite();
  }

  public boolean isNoCache() {
    return getPortalRequestContext().isNoCache();
  }

  public boolean isMaximizePortlet() {
    return getPortalRequestContext().isMaximizePortlet();
  }

  public String getMaximizedPortletId() {
    return getPortalRequestContext().getMaximizedPortletId();
  }

  private PortalRequestContext getPortalRequestContext() {
    return PortalRequestContext.getCurrentInstance();
  }

  private List<UIPortlet> getCurrentPortlets() {
    PortalRequestContext requestContext = getPortalRequestContext();
    List<UIPortlet> uiPortlets = requestContext.getUiPortlets();
    if (uiPortlets == null) {
      // Determine portlets visible on the page
      uiPortlets = new ArrayList<>();
      UISharedLayout sharedLayout = uiWorkingWorkspace.findFirstComponentOfType(UISharedLayout.class);
      if (sharedLayout == null || sharedLayout.isShowSharedLayout(requestContext)) {
        uiWorkingWorkspace.findComponentOfType(uiPortlets, UIPortlet.class);
      } else {
        UIPage currentPage = getCurrentPage();
        if (currentPage == null) {
          return Collections.emptyList();
        } else if (!currentPage.isShowMaxWindow()) {
          getCurrentSite().findComponentOfType(uiPortlets, UIPortlet.class);
        } else {
          currentPage.findComponentOfType(uiPortlets, UIPortlet.class);
        }
      }
      requestContext.setUiPortlets(uiPortlets);
    }
    return uiPortlets;
  }

  @Asynchronous
  public class RefreshUIPortalListener extends Listener<LayoutService, PortalConfig> {
    @Override
    public void onEvent(org.exoplatform.services.listener.Event<LayoutService, PortalConfig> event) throws Exception {
      PortalConfig site = event.getData();
      if (site == null) {
        return;
      }
      SiteKey siteKey = site.getSiteKey();
      if (siteKey == null) {
        return;
      }
      if (all_UIPortals != null && all_UIPortals.containsKey(siteKey)) {
        all_UIPortals.remove(siteKey);
      }
    }
  }
}
