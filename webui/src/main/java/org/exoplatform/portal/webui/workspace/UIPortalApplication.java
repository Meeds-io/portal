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

package org.exoplatform.portal.webui.workspace;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.RequestNavigationData;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.resource.*;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageActionListener.ChangeNodeActionListener;
import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.portal.webui.portal.PageNodeEvent;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.portal.UISharedLayout;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.javascript.JavascriptConfigParser;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.url.MimeType;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.url.ComponentURL;

import org.gatein.pc.portlet.impl.info.ContainerPortletInfo;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.script.FetchMap;
import org.gatein.portal.controller.resource.script.FetchMode;
import org.gatein.portal.controller.resource.script.Module;
import org.gatein.portal.controller.resource.script.ScriptResource;
import org.json.JSONObject;

import jakarta.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This extends the UIApplication and hence is a sibling of UIPortletApplication
 * (used by any eXo Portlets as the Parent class to build the portlet component
 * tree). The UIPortalApplication is responsible to build its subtree according
 * to some configuration parameters. If all components are displayed it is
 * composed of 2 UI components: -UIWorkingWorkSpace: the right part that can
 * display the normal or webos portal layouts - UIPopupWindow: a popup window
 * that display or not
 */
@ComponentConfig(lifecycle = UIPortalApplicationLifecycle.class,
                 template = "system:/groovy/portal/webui/workspace/UIPortalApplication.gtmpl",
                 events = { @EventConfig(listeners = ChangeNodeActionListener.class, csrfCheck = false) })
@SuppressWarnings("rawtypes")
public class UIPortalApplication extends UIApplication {

  /**
   * Property settable in the portal'S configuration.properties file. See
   * {@link EditMode} for possible values. See also
   * {@link #getDefaultEditMode()}.
   */
  public static final String      DEFAULT_MODE_PROPERTY     = "gatein.portal.pageEditor.defaultEditMode";

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
                                                              public String getId() {
                                                                return "_portal:componentId_";
                                                              };
                                                            };

  public static final String      UI_WORKING_WS_ID          = "UIWorkingWorkspace";

  public static final String      UI_VIEWING_WS_ID          = "UIViewWS";

  public static final String      UI_EDITTING_WS_ID         = "UIEditInlineWS";

  public static final String      UI_MASK_WS_ID             = "UIMaskWorkspace";

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

  protected UIWorkingWorkspace   uiWorkingWorkspace;

  private static EditMode        defaultEditMode = null;

  private int                    modeState       = NORMAL_MODE;

  private EditLevel              editLevel       = EditLevel.NO_EDIT;

  private Orientation            orientation_    = Orientation.LT;

  private SkinService            skinService;

  private SkinVisitor            skinVisitor;

  private LayoutService          layoutService;

  private boolean                isSessionOpen   = false;

  private Map<SiteKey, UIPortal> all_UIPortals;

  private boolean                isAjaxInLastRequest;

  private RequestNavigationData  lastNonAjaxRequestNavData;

  private RequestNavigationData  lastRequestNavData;

  private UIComponentDecorator   uiViewWorkingWorkspace;

  private String                 lastPortal;

  private String                 lastPortalOwner;

  /**
   * Returns a locally cached value of {@value #DEFAULT_MODE_PROPERTY} property
   * from configuration.properties.
   *
   * @return
   */
  public static EditMode getDefaultEditMode() {
    if (defaultEditMode == null) {
      /*
       * Initialization: For performance reasons, we have chosen to prefer to
       * ignore the potential concurrent updates on app startup to some kind of
       * locking. The concurrent updates should be harmless as they all produce
       * the same result.
       */
      String val = PropertyManager.getProperty(DEFAULT_MODE_PROPERTY);
      if (val == null || val.length() == 0) {
        /* hard coded default */
        defaultEditMode = EditMode.BLOCK;
      } else {
        try {
          defaultEditMode = EditMode.valueOf(val.toUpperCase());
        } catch (IllegalArgumentException e) {
          StringBuilder msg = new StringBuilder().append("Ignoring illegal value '")
                                                 .append(val)
                                                 .append("' of ")
                                                 .append(DEFAULT_MODE_PROPERTY)
                                                 .append(" property in configuration.properties. One of [");
          for (EditMode mode : EditMode.values()) {
            if (msg.charAt(msg.length() - 1) != '[') {
              msg.append(", ");
            }
            msg.append(mode.name());
          }
          msg.append("] is expected. Using default value '").append(EditMode.BLOCK.name()).append("'.");
          log.warn(msg.toString());
          defaultEditMode = EditMode.BLOCK;
        }
      }
    }
    return defaultEditMode;
  }

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
    log = ExoLogger.getLogger("portal:UIPortalApplication");
    PortalRequestContext context = PortalRequestContext.getCurrentInstance();
    skinService = getApplicationComponent(SkinService.class);
    skinVisitor = getApplicationComponent(SkinVisitor.class);
    layoutService = getApplicationComponent(LayoutService.class);

    LocaleConfigService localeConfigService = getApplicationComponent(LocaleConfigService.class);

    Locale locale = context.getLocale();
    if (locale == null) {
      if (log.isWarnEnabled())
        log.warn("No locale set on PortalRequestContext! Falling back to 'en'.");
      locale = Locale.ENGLISH;
    }

    String localeName = LocaleContextInfo.getLocaleAsString(locale);
    LocaleConfig localeConfig = localeConfigService.getLocaleConfig(localeName);
    if (localeConfig == null) {
      if (log.isWarnEnabled())
        log.warn("Unsupported locale set on PortalRequestContext: " + localeName + "! Falling back to 'en'.");
      localeConfig = localeConfigService.getLocaleConfig(Locale.ENGLISH.getLanguage());
    }
    setOrientation(localeConfig.getOrientation());
    context.setUIApplication(this);

    this.all_UIPortals = new HashMap<>();
    this.lastPortalOwner = context.getPortalOwner();
    initWorkspaces();
  }

  /**
   * Sets the specified portal to be showed in the normal mode currently
   *
   * @param uiPortal
   */
  public void setCurrentSite(UIPortal uiPortal) {
    PortalRequestContext.getCurrentInstance().setUiPortal(uiPortal);
    UISiteBody siteBody = this.findFirstComponentOfType(UISiteBody.class);
    if (siteBody != null) {
      // TODO: Check this part carefully
      siteBody.setUIComponent(uiPortal);
    }
  }

  /**
   * Returns current UIPortal which being showed in normal mode
   *
   * @return
   */
  public UIPortal getCurrentSite() {
    return PortalRequestContext.getCurrentInstance().getUiPortal();
  }

  /**
   * Returns a cached UIPortal matching to OwnerType and OwnerId if any
   *
   * @param ownerType
   * @param ownerId
   * @return
   */
  public UIPortal getCachedUIPortal(String ownerType, String ownerId) {
    if (ownerType == null || ownerId == null) {
      return null;
    }
    return this.all_UIPortals.get(new SiteKey(ownerType, ownerId));
  }

  public UIPortal getCachedUIPortal(SiteKey key) {
    if (key == null) {
      return null;
    }
    return this.all_UIPortals.get(key);
  }

  /**
   * Associates the specified UIPortal to a cache map with specified key which
   * bases on OwnerType and OwnerId
   *
   * @param uiPortal
   */
  public void putCachedUIPortal(UIPortal uiPortal) {
    SiteKey siteKey = uiPortal.getSiteKey();

    if (siteKey != null) {
      this.all_UIPortals.put(siteKey, uiPortal);
    }
  }

  /**
   * Remove the UIPortal from the cache map
   *
   * @param ownerType
   * @param ownerId
   */
  public void removeCachedUIPortal(String ownerType, String ownerId) {
    if (ownerType == null || ownerId == null) {
      return;
    }
    this.all_UIPortals.remove(new SiteKey(ownerType, ownerId));
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

  public void refreshCachedUI() throws Exception {
    all_UIPortals.clear();

    UIPortal uiPortal = getCurrentSite();
    if (uiPortal != null) {
      SiteKey siteKey = uiPortal.getSiteKey();

      UIPortal tmp = null;
      PortalConfig portalConfig = Util.getPortalRequestContext().getDynamicPortalConfig();
      if (portalConfig != null) {
        tmp = this.createUIComponent(UIPortal.class, null, null);
        PortalDataMapper.toUIPortalWithMetaLayout(tmp, portalConfig);
        this.putCachedUIPortal(tmp);
        tmp.setNavPath(uiPortal.getNavPath());
        tmp.refreshUIPage();

        setCurrentSite(tmp);
        if (SiteType.PORTAL.equals(siteKey.getType())) {
          PortalRequestContext pcontext = Util.getPortalRequestContext();
          if (pcontext != null) {
            UserPortalConfig userPortalConfig = pcontext.getUserPortalConfig();
            userPortalConfig.setPortalConfig(portalConfig);
          }
        }
      }
    }
  }

  public boolean isSessionOpen() {
    return isSessionOpen;
  }

  public void setSessionOpen(boolean isSessionOpen) {
    this.isSessionOpen = isSessionOpen;
  }

  public Orientation getOrientation() {
    return orientation_;
  }

  public void setOrientation(Orientation orientation) {
    this.orientation_ = orientation;
  }

  public Locale getLocale() {
    return Util.getPortalRequestContext().getLocale();
  }

  public void setModeState(int mode) {
    this.modeState = mode;
    if (modeState == NORMAL_MODE) {
      editLevel = EditLevel.NO_EDIT;
    }
  }

  public void setDefaultEditMode(ComponentTab componentTab, EditLevel editLevel) {
    this.editLevel = editLevel;
    EditMode editMode = getDefaultEditMode();
    switch (componentTab) {
    case APPLICATIONS:
      switch (editMode) {
      case BLOCK:
        this.modeState = APP_BLOCK_EDIT_MODE;
        break;
      case PREVIEW:
        this.modeState = APP_VIEW_EDIT_MODE;
        break;
      default:
        log.warn("Ignoring unexpected " + EditMode.class.getName() + " value '" + editMode.name() + "' and using '" +
            EditMode.BLOCK.name() + "'.");
      }
      break;
    case CONTAINERS:
      switch (editMode) {
      case BLOCK:
        this.modeState = CONTAINER_BLOCK_EDIT_MODE;
        break;
      case PREVIEW:
        this.modeState = CONTAINER_VIEW_EDIT_MODE;
        break;
      default:
        log.warn("Ignoring unexpected " + EditMode.class.getName() + " value '" + editMode.name() + "' and using '" +
            EditMode.BLOCK.name() + "'.");
      }
      break;
    default:
      log.warn("Ignoring unexpected " + ComponentTab.class.getName() + " value '" + componentTab.name() + "' and using '" +
          ComponentTab.APPLICATIONS.name() + "'.");
      switch (editMode) {
      case BLOCK:
        this.modeState = APP_BLOCK_EDIT_MODE;
        break;
      case PREVIEW:
        this.modeState = APP_VIEW_EDIT_MODE;
        break;
      default:
        log.warn("Ignoring unexpected " + EditMode.class.getName() + " value '" + editMode.name() + "' and using '" +
            EditMode.BLOCK.name() + "'.");
      }
    }
  }

  public int getModeState() {
    return modeState;
  }

  public void setLastRequestNavData(RequestNavigationData navData) {
    this.lastRequestNavData = navData;
  }

  /**
   * @deprecated use the Mode State instead
   * @return True if the Portal is not in the normal mode
   */
  public boolean isEditing() {
    return (modeState != NORMAL_MODE);
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
    PortalRequestContext prc = PortalRequestContext.getCurrentInstance();
    JavascriptManager jsMan = prc.getJavascriptManager();

    //
    FetchMap<ResourceId> requiredResources = jsMan.getScriptResources();
    log.debug("Resource ids to resolve: {}", requiredResources);

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

    //
    log.debug("Resolved resources for page: " + ret);

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
    return PortalRequestContext.getCurrentInstance().getSkin();
  }

  /**
   * Returns a set of portlets skin that have to be added in the HTML head tag.
   * Those portlets doesn't belongs to portal
   *
   * @return the portlet skins
   */
  public Set<Skin> getPortletSkins() {
    String skin = getSkin();
    List<SkinConfig> portletSkins = getCurrentPortlets().stream().map(this::getPortletSkinConfig).filter(Objects::nonNull).toList();
    List<SkinConfig> additionalSkins = portletSkins.stream()
                                                   .filter(portletSkin -> portletSkin instanceof SkinConfig skinConfig
                                                                          && CollectionUtils.isNotEmpty(skinConfig.getAdditionalModules()))
                                                   .map(SkinConfig::getAdditionalModules)
                                                   .flatMap(List::stream)
                                                   .distinct()
                                                   .map(module -> skinService.getPortalSkin(module, skin))
                                                   .filter(Objects::nonNull)
                                                   .toList();
    return Stream.concat(portletSkins.stream(), additionalSkins.stream())
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
                               .filter(pinfo -> pinfo instanceof ContainerPortletInfo)
                               .map(p -> (ContainerPortletInfo) p)
                               .toList();
  }

  private SkinConfig getPortletSkinConfig(UIPortlet portlet) {
    String portletId = portlet.getSkinId();
    if (portletId != null) {
      return skinService.getSkin(portletId, getSkin());
    } else {
      return null;
    }
  }

  /**
   * The central area is called the WorkingWorkspace. It is composed of: 1) A
   * UIPortal child which is filled with portal data using the PortalDataMapper
   * helper tool 2) A UIPortalToolPanel which is not rendered by default A
   * UIMaskWorkspace is also added to provide powerfull focus only popups
   *
   * @throws Exception
   */
  private void initWorkspaces() throws Exception {
    if (this.getChildById(UIPortalApplication.UI_WORKING_WS_ID) != null) {
      this.removeChildById(UIPortalApplication.UI_WORKING_WS_ID);
    }
    this.uiWorkingWorkspace = this.addChild(UIWorkingWorkspace.class, UIPortalApplication.UI_WORKING_WS_ID, null);
    this.uiViewWorkingWorkspace = this.uiWorkingWorkspace.addChild(UIComponentDecorator.class, null, UI_VIEWING_WS_ID);

    if (this.getChildById(UIPortalApplication.UI_MASK_WS_ID) == null) {
      this.addChild(UIMaskWorkspace.class, UIPortalApplication.UI_MASK_WS_ID, null);
    }
    initSharedLayout();
  }

  private void initSharedLayout() throws Exception {
    Container container = layoutService.getSharedLayout(this.lastPortalOwner);
    if (container != null) {
      UISharedLayout uiContainer = createUIComponent(UISharedLayout.class, null, null);
      uiContainer.setStorageId(container.getStorageId());
      PortalDataMapper.toUIContainer(uiContainer, container);
      uiContainer.setRendered(true);
      this.uiViewWorkingWorkspace.setUIComponent(uiContainer);
    }
    refreshCachedUI();
  }

  /**
   * Check current portal name, if it's changing, reload portal properties (for
   * now, skin setting)
   */
  @Override
  public void processDecode(WebuiRequestContext context) throws Exception {
    PortalRequestContext prc = (PortalRequestContext) context;
    String portalName = prc.getUserPortalConfig().getPortalName();
    if (!Safe.equals(portalName, lastPortal)) {
      reloadPortalProperties();
      lastPortal = portalName;
    }

    UIPortal uiPortal = getCachedUIPortal(prc.getSiteKey());
    if (uiPortal != null) {
      setCurrentSite(uiPortal);
      uiPortal.refreshUIPage();
    }
    super.processDecode(context);
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
    PortalRequestContext pcontext = (PortalRequestContext) context;
    RequestNavigationData requestNavData = pcontext.getNavigationData();

    boolean isAjax = pcontext.useAjax();

    if (!isAjax) {
      if (isAjaxInLastRequest) {
        isAjaxInLastRequest = false;
        if (requestNavData.equals(lastNonAjaxRequestNavData) && isRefreshPage(requestNavData)
            && pcontext.getPortletParameters().isEmpty()) {
          NodeURL nodeURL = pcontext.createURL(NodeURL.TYPE).setNode(getCurrentSite().getSelectedUserNode());
          pcontext.sendRedirect(nodeURL.toString());
          return;
        }
      }
      lastNonAjaxRequestNavData = requestNavData;
    }

    isAjaxInLastRequest = isAjax;

    if (isRefreshPage(requestNavData)) {
      if (!isDraftPage() && !isMaximizePortlet()) {
        lastRequestNavData = requestNavData;
      }

      StringBuilder js = new StringBuilder("eXo.env.server.portalBaseURL=\"");
      js.append(getBaseURL()).append("\";\n");

      String url = getPortalURLTemplate();
      js.append("eXo.env.server.portalURLTemplate=\"");
      js.append(url).append("\";");

      JavascriptManager javascriptManager = pcontext.getJavascriptManager();
      if (USE_WEBUI_RESOURCES) {
        javascriptManager.require("SHARED/base").addScripts(js.toString());
      } else {
        javascriptManager.addJavascript(js.toString());
      }

      SiteKey siteKey = pcontext.getSiteKey();
      PageNodeEvent<UIPortalApplication> pnevent = new PageNodeEvent<UIPortalApplication>(this,
                                                                                          PageNodeEvent.CHANGE_NODE,
                                                                                          siteKey,
                                                                                          pcontext.getNodePath());
      broadcast(pnevent, Event.Phase.PROCESS);
    }

    if (!isAjax) {
      lastNonAjaxRequestNavData = requestNavData;
    }

    if (pcontext.isResponseComplete()) {
      return;
    }

    if (getCurrentSite() == null || getCurrentSite().getSelectedUserNode() == null) {
      pcontext.sendError(HttpServletResponse.SC_NOT_FOUND);
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
  public void processRender(WebuiRequestContext context) throws Exception {
    String maximizedPortletId = getMaximizedPortletId();
    if (StringUtils.isNotBlank(maximizedPortletId)) {
      UIPortlet maximizedUiPortlet = getCurrentPortlets().stream()
                                                         .filter(p -> StringUtils.equals(p.getStorageId(), maximizedPortletId)
                                                                      || StringUtils.equals(p.getId(), maximizedPortletId))
                                                         .findFirst()
                                                         .orElseThrow(() -> new IllegalStateException(String.format("Portlet with id %s to maximize wasn't found in page with title '%s'",
                                                                                                                    maximizedPortletId,
                                                                                                                    getCurrentPage().getTitle())));
      UIPage uiPage = findFirstComponentOfType(UIPage.class);
      uiPage.normalizePortletWindowStates();
      uiPage.setMaximizedUIPortlet(maximizedUiPortlet);
    }
    try {
      PortalRequestContext pcontext = (PortalRequestContext) context;
      pcontext.setAttribute("requestStartTime", System.currentTimeMillis());

      // Reload shared layout if site has changed
      if (!StringUtils.equals(this.lastPortalOwner, pcontext.getPortalOwner())) {
        this.lastPortalOwner = pcontext.getPortalOwner();
        initWorkspaces();
      }

      JavascriptManager jsMan = context.getJavascriptManager();
      // Add JS resource of current portal
      String portalOwner = pcontext.getPortalOwner();
      jsMan.loadScriptResource(ResourceScope.PORTAL, portalOwner);

      //
      Writer w = context.getWriter();
      if (!context.useAjax()) {
        // Support for legacy resource declaration
        jsMan.loadScriptResource(ResourceScope.SHARED, JavascriptConfigParser.LEGACY_JAVA_SCRIPT);
        // Need to add bootstrap as immediate since it contains the loader
        jsMan.loadScriptResource(ResourceScope.SHARED, "bootstrap");

        super.processRender(context);
      } else {
        UIMaskWorkspace uiMaskWS = getChildById(UIPortalApplication.UI_MASK_WS_ID);
        if (uiMaskWS.isUpdated())
          pcontext.addUIComponentToUpdateByAjax(uiMaskWS);
        if (USE_WEBUI_RESOURCES && getUIPopupMessages().hasMessage()) {
          pcontext.addUIComponentToUpdateByAjax(getUIPopupMessages());
        }

        Set<UIComponent> list = context.getUIComponentToUpdateByAjax();
        List<UIPortlet> uiPortlets = new ArrayList<UIPortlet>(3);
        List<UIComponent> uiDataComponents = new ArrayList<UIComponent>(5);

        if (list != null) {
          for (UIComponent uicomponent : list) {
            if (uicomponent instanceof UIPortlet)
              uiPortlets.add((UIPortlet) uicomponent);
            else
              uiDataComponents.add(uicomponent);
          }
        }
        w.write("<div class=\"PortalResponse\">");
        w.write("<div class=\"PortalResponseData\">");
        for (UIComponent uicomponent : uiDataComponents) {
          if (log.isDebugEnabled())
            log.debug("AJAX call: Need to refresh the UI component " + uicomponent.getName());
          renderBlockToUpdate(uicomponent, context, w);
        }
        w.write("</div>");

        if (!context.getFullRender()) {
          for (UIPortlet uiPortlet : uiPortlets) {
            if (log.isDebugEnabled())
              log.debug("AJAX call: Need to refresh the Portlet " + uiPortlet.getId());

            w.write("<div class=\"PortletResponse\" style=\"display: none\">");
            w.append("<div class=\"PortletResponsePortletId\">" + uiPortlet.getId() + "</div>");
            w.append("<div class=\"PortletResponseData\">");

            /*
             * If the portlet is using our UI framework or supports it then it
             * will return a set of block to updates. If there is not block to
             * update the javascript client will see that as a full refresh of
             * the content part
             */
            uiPortlet.processRender(context);

            w.append("</div>");
            w.append("<div class=\"PortletResponseScript\"></div>");
            w.write("</div>");
          }
        }
        w.write("<div class=\"MarkupHeadElements\">");
        List<String> headElems = ((PortalRequestContext) context).getExtraMarkupHeadersAsStrings();
        for (String elem : headElems) {
          w.write(elem);
        }
        w.write("</div>");
        w.write("<div class=\"LoadingScripts\">");
        writeLoadingScripts(pcontext);
        w.write("</div>");
        w.write("<div class=\"PortalResponseScript\">");
        JavascriptManager jsManager = pcontext.getJavascriptManager();
        String skin = getAddSkinScript(pcontext.getControllerContext(), list);
        if (skin != null) {
          jsManager.require("SHARED/skin", "skin").addScripts(skin);
        }
        w.write(jsManager.getJavaScripts());
        w.write("</div>");
        w.write("</div>");
      }
    } finally {
      if (StringUtils.isNotBlank(maximizedPortletId)) {
        UIPage uiPage = findFirstComponentOfType(UIPage.class);
        uiPage.setMaximizedUIPortlet(null);
        uiPage.normalizePortletWindowStates();
      }
    }
  }

  private void writeLoadingScripts(PortalRequestContext context) throws Exception {
    Writer w = context.getWriter();
    Map<String, Boolean> scriptURLs = getScripts();
    w.write("<div class=\"ImmediateScripts\">");
    w.write(StringUtils.join(scriptURLs.keySet(), ","));
    w.write("</div>");
  }

  private String getAddSkinScript(ControllerContext context, Set<UIComponent> updateComponents) {
    if (updateComponents == null) {
      return null;
    }
    List<UIPortlet> uiportlets = new ArrayList<>();
    for (UIComponent uicomponent : updateComponents) {
      if (uicomponent instanceof UIContainer) {
        UIContainer uiContainer = (UIContainer) uicomponent;
        uiContainer.findComponentOfType(uiportlets, UIPortlet.class);
      }
      if (uicomponent instanceof UIComponentDecorator) {
        UIComponentDecorator uiDecorator = (UIComponentDecorator) uicomponent;
        if (uiDecorator.getUIComponent() instanceof UIContainer) {
          UIContainer uiContainer = (UIContainer) uiDecorator.getUIComponent();
          uiContainer.findComponentOfType(uiportlets, UIPortlet.class);
        }
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
      SkinURL url = ele.createURL(context);
      url.setOrientation(orientation_);
      b.append("skin.addSkin('").append(ele.getId()).append("','").append(url).append("');\n");
    }

    return b.toString();
  }

  /**
   * Use {@link PortalRequestContext#getUserPortalConfig()} instead
   *
   * @return
   */
  @Deprecated
  public UserPortalConfig getUserPortalConfig() {
    return Util.getPortalRequestContext().getUserPortalConfig();
  }

  /**
   * Reload portal properties. This is needed to be called when it is changing
   * Portal site<br>
   * If user has been authenticated, get the skin name setting from user
   * profile.<br>
   * anonymous user or no skin setting in user profile, use the skin setting in
   * portal config
   *
   * @throws Exception
   */
  public void reloadPortalProperties() throws Exception {
    PortalRequestContext context = Util.getPortalRequestContext();
    context.refreshPortalConfig();
  }

  /**
   * @return User Home page preference
   */
  public String getUserHomePage() {
    PortalRequestContext context = Util.getPortalRequestContext();
    return getApplicationComponent(UserPortalConfigService.class).getUserHomePage(context.getRemoteUser());
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

  /**
   * @return the editLevel
   */
  public EditLevel getEditLevel() {
    return editLevel;
  }

  /**
   * @param editLevel the editLevel to set
   */
  public void setEditLevel(EditLevel editLevel) {
    this.editLevel = editLevel;
  }

  public EditMode getEditMode() {
    switch (modeState) {
    case NORMAL_MODE:
      return EditMode.NO_EDIT;
    case APP_BLOCK_EDIT_MODE:
    case CONTAINER_BLOCK_EDIT_MODE:
      return EditMode.BLOCK;
    case APP_VIEW_EDIT_MODE:
    case CONTAINER_VIEW_EDIT_MODE:
      return EditMode.PREVIEW;
    default:
      throw new IllegalStateException("Unexpected " + UIPortalApplication.class.getName() + ".modeState value " + modeState +
          ".");
    }
  }

  public ComponentTab getComponentTab() {
    switch (modeState) {
    case NORMAL_MODE:
      return ComponentTab.NO_EDIT;
    case APP_VIEW_EDIT_MODE:
    case APP_BLOCK_EDIT_MODE:
      return ComponentTab.APPLICATIONS;
    case CONTAINER_BLOCK_EDIT_MODE:
    case CONTAINER_VIEW_EDIT_MODE:
      return ComponentTab.CONTAINERS;
    default:
      throw new IllegalStateException("Unexpected " + UIPortalApplication.class.getName() + ".modeState value " + modeState +
          ".");
    }
  }

  @SuppressWarnings("rawtypes")
  public void includePortletScripts() {
    PortalRequestContext pcontext = PortalRequestContext.getCurrentInstance();
    JavascriptManager jsMan = pcontext.getJavascriptManager();
    List<UIPortlet> portlets = new ArrayList<>();
    uiViewWorkingWorkspace.findComponentOfType(portlets, UIPortlet.class);
    for (UIPortlet uiPortlet : portlets) {
      if (!uiPortlet.isLazyResourcesLoading()) {
        try {
          jsMan.loadScriptResource(ResourceScope.PORTLET, uiPortlet.getApplicationId());
        } catch (Exception e) {
          log.warn("Can't load JS resource for portlet {}", uiPortlet.getName(), e);
        }
      }
    }
  }

  public String getLastPortal() {
    return lastPortal;
  }

  public UIPage getCurrentPage() {
    return PortalRequestContext.getCurrentInstance().getUiPage();
  }

  public void setCurrentPage(UIPage currentPage) {
    PortalRequestContext.getCurrentInstance().setUiPage(currentPage);
  }

  private boolean isRefreshPage(RequestNavigationData requestNavData) {
    return !requestNavData.equals(lastRequestNavData)
           || getCurrentSite() == null
           || isDraftPage()
           || isMaximizePortlet();
  }

  private boolean isDraftPage() {
    return PortalRequestContext.getCurrentInstance().isDraftPage();
  }

  public boolean isMaximizePortlet() {
    return PortalRequestContext.getCurrentInstance().isMaximizePortlet();
  }

  public String getMaximizedPortletId() {
    return PortalRequestContext.getCurrentInstance().getMaximizedPortletId();
  }

  private List<UIPortlet> getCurrentPortlets() {
    PortalRequestContext requestContext = PortalRequestContext.getCurrentInstance();
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
        } else if (!requestContext.isMaximizePortlet() && !currentPage.isShowMaxWindow()) {
          getCurrentSite().findComponentOfType(uiPortlets, UIPortlet.class);
        } else {
          currentPage.findComponentOfType(uiPortlets, UIPortlet.class);
        }
      }
      requestContext.setUiPortlets(uiPortlets);
    }
    return uiPortlets;
  }

}
