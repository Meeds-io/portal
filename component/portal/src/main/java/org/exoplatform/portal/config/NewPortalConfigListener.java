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
package org.exoplatform.portal.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelUnmarshaller;
import org.exoplatform.portal.config.model.NavigationFragment;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.Page.PageSet;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.UnmarshalledObject;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.importer.NavigationImporter;
import org.exoplatform.portal.mop.importer.PageImporter;
import org.exoplatform.portal.mop.importer.PortalConfigImporter;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;

/**
 * Created by The eXo Platform SARL Author : Tuan Nguyen
 * tuan08@users.sourceforge.net May 22, 2006
 */

public class NewPortalConfigListener extends BaseComponentPlugin {

  private static final Log               LOG                 = ExoLogger.getLogger(NewPortalConfigListener.class);

  private static final Pattern           OWNER_PATTERN       = Pattern.compile("@owner@");

  private static final Pattern           OWNER_TYPE_PATTERN  = Pattern.compile("@owner_type@");

  /** . */
  private final UserPortalConfigService  owner_;

  /** . */
  private ConfigurationManager           cmanager_;

  /** . */
  private LayoutService                  layoutService;

  /** . */
  private volatile List<NewPortalConfig> configs;

  /**
   * @deprecated Site Templates has been changed to be stored in database to
   *             make it dynamically managed by UI rather than statis pages and
   *             navigation from source files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  private List<SiteConfigTemplates>      templateConfigs;

  /** . */
  private String                         metaPortal;

  /**
   * If true the meta portal name has been explicitly set. If false the name has
   * not been set and we are using the default.
   */
  private boolean                        metaPortalSpecified = false;

  /**
   * @deprecated Site Templates has been changed to be stored in database to
   *             make it dynamically managed by UI rather than statis pages and
   *             navigation from source files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  private String                         defaultPortalTemplate;

  /**
   * If true the portal clear portal metadata from data storage and replace it
   * with new data created from .xml files.
   */
  private boolean                        overrideExistingData;

  /** . */
  private Log                            log                 = ExoLogger.getLogger(getClass());

  /** . */
  private NavigationService              navigationService;

  /** . */
  private DescriptionStorage             descriptionStorage;

  /** . */
  private LocaleConfigService            localeConfigService;

  /** . */
  private UserACL                        userAcl;

  final Set<String>                      createdOwners       = new HashSet<>();

  private boolean                        isFirstStartup      = false;

  public NewPortalConfigListener(UserPortalConfigService owner, // NOSONAR
                                 LayoutService layoutService,
                                 ConfigurationManager cmanager,
                                 InitParams params,
                                 NavigationService navigationService,
                                 DescriptionStorage descriptionStorage,
                                 UserACL userACL,
                                 LocaleConfigService localeConfigService) {

    this.owner_ = owner;
    this.cmanager_ = cmanager;
    this.layoutService = layoutService;
    this.navigationService = navigationService;
    this.descriptionStorage = descriptionStorage;
    this.userAcl = userACL;
    this.localeConfigService = localeConfigService;

    ValueParam valueParam = params.getValueParam("meta.portal");
    if (valueParam != null) {
      metaPortal = valueParam.getValue();
    } else {
      valueParam = params.getValueParam("default.portal");
      if (valueParam != null) {
        metaPortal = valueParam.getValue();
      }
    }

    if (StringUtils.isNotBlank(metaPortal)) {
      metaPortalSpecified = true;
    }

    // I guess we'll use the term 'portal' to mean site as to be consistent with
    // defaultPortal
    valueParam = params.getValueParam("default.portal.template");
    if (valueParam != null) {
      defaultPortalTemplate = valueParam.getValue().trim();
    }

    configs = params.getObjectParamValues(NewPortalConfig.class);

    templateConfigs = params.getObjectParamValues(SiteConfigTemplates.class);

    valueParam = params.getValueParam("override");
    if (valueParam != null) {
      overrideExistingData = "true".equals(valueParam.getValue());
    }
    for (NewPortalConfig ele : configs) {
      if (ele.getOverrideMode() == null) {
        ele.setOverrideMode(overrideExistingData);
      }
    }
  }

  private void touchImport() {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      layoutService.saveImportStatus(Status.DONE);
    } finally {
      RequestLifeCycle.end();
    }
  }

  private boolean performImport() {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      boolean perform = true;

      Status st = layoutService.getImportStatus();
      if (st != null) {
        perform = (Status.WANT_REIMPORT == st);
      } else {
        if (layoutService.getPortalConfig(metaPortal) != null) {
          perform = false;
          layoutService.saveImportStatus(Status.DONE);
        } else {
          isFirstStartup = true;
        }
      }
      return perform;
    } finally {
      RequestLifeCycle.end();
    }
  }

  public void run() throws Exception {
    boolean prepareImport = performImport();
    for (NewPortalConfig ele : configs) {
      initPortalConfigDB(ele, prepareImport);
    }
    for (NewPortalConfig ele : configs) {
      initPageDB(ele, prepareImport);
    }
    for (NewPortalConfig ele : configs) {
      initPageNavigationDB(ele, prepareImport);
    }

    //
    touchImport();
  }

  public NewPortalConfig getPortalConfig(String type, String name) {
    return CollectionUtils.isNotEmpty(configs) ? configs.stream()
                                                        .filter(c -> StringUtils.equalsIgnoreCase(c.getOwnerType(), type)
                                                                     && CollectionUtils.isNotEmpty(c.getPredefinedOwner())
                                                                     && c.getPredefinedOwner().contains(name))
                                                        .findFirst()
                                                        .orElse(null) :
                                               null;
  }

  /**
   * @deprecated Site Templates has been changed to be stored in database to
   *             make it dynamically managed by UI rather than statis pages and
   *             navigation from source files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  String getDefaultPortalTemplate() {
    return defaultPortalTemplate;
  }

  String getMetaPortal() {
    return metaPortal;
  }

  /**
   * This is used to merge an other NewPortalConfigListener to this one
   *
   * @param other
   */
  public void mergePlugin(NewPortalConfigListener other) {
    // if other didn't actually set anything for the default portal name
    // then we should continue to use the current value. This way if an
    // extension
    // doesn't set it, it wont override the parent's set value.
    if (other.metaPortalSpecified) {
      this.metaPortal = other.metaPortal;
    }

    if (other.defaultPortalTemplate != null && other.defaultPortalTemplate.length() > 0) {
      this.defaultPortalTemplate = other.defaultPortalTemplate;
    }

    if (configs == null) {
      this.configs = other.configs;
    } else if (other.configs != null && !other.configs.isEmpty()) {
      List<NewPortalConfig> result = new ArrayList<>(configs);
      result.addAll(other.configs);
      this.configs = Collections.unmodifiableList(result);
    }

    if (templateConfigs == null) {
      this.templateConfigs = other.templateConfigs;
    } else if (other.templateConfigs != null && !other.templateConfigs.isEmpty()) {
      List<SiteConfigTemplates> result = new ArrayList<>(templateConfigs);
      result.addAll(other.templateConfigs);
      this.templateConfigs = Collections.unmodifiableList(result);
    }
  }

  public void initPortalConfigDB(NewPortalConfig ele, boolean prepareImport) {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      if (ele.getOverrideMode() || prepareImport) {
        initPortalConfigDB(ele);
      }
    } catch (Exception e) {
      log.error("NewPortalConfig error: " + e.getMessage(), e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  public void initPortalConfigDB(NewPortalConfig config) {
    for (String owner : config.getPredefinedOwner()) {
      if (createPortalConfig(config, owner)) {
        LOG.info("Importing Site of type '{}' with name '{}' from location '{}' wth mode '{}'",
                 config.getOwnerType(),
                 owner,
                 config.getLocation(),
                 config.getImportMode());
        this.createdOwners.add(owner);
      }
    }
  }

  public void initPageDB(NewPortalConfig ele, boolean prepareImport) {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      if (ele.getOverrideMode() || prepareImport) {
        initPageDB(ele);
      }
    } catch (Exception e) {
      log.error("NewPortalConfig error: " + e.getMessage(), e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  public void initPageDB(NewPortalConfig config) {
    for (String owner : config.getPredefinedOwner()) {
      if (this.createdOwners.contains(owner)) {
        createPage(config, owner);
      }
    }
  }

  public void initPageNavigationDB(NewPortalConfig ele, boolean prepareImport) {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      if (ele.getOverrideMode() || prepareImport) {
        initPageNavigationDB(ele);
      }
    } catch (Exception e) {
      log.error("NewPortalConfig error: " + e.getMessage(), e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  public void initPageNavigationDB(NewPortalConfig config) {
    for (String owner : config.getPredefinedOwner()) {
      createPageNavigation(config, owner);
    }
  }

  public boolean createPortalConfig(NewPortalConfig config, String owner) {
    String type = config.getOwnerType();
    String fixedOwnerName = fixOwnerName(type, owner);

    PortalConfig pConfig = null;

    if (config.isUseMetaPortalLayout()) {
      // If PortalConfig already exists, no need to erase the data with empty
      // and predefined data
      PortalConfig persistedPortalConfig = layoutService.getPortalConfig(type, fixedOwnerName);
      if (persistedPortalConfig != null) {
        return true;
      }

      // PortalConfig doesn't exists in storage => force to use default layout
      pConfig = buildEmptyPortalConfig(type, fixedOwnerName);
    } else {
      // If nor template location neither template name, we will use default
      // PortalLayout created by constructor
      if (StringUtils.isNotBlank(config.getTemplateName()) || StringUtils.isNotBlank(config.getTemplateLocation())) {
        UnmarshalledObject<PortalConfig> obj = getConfig(config, owner, type, PortalConfig.class);
        if (obj == null) {
          obj = getConfig(config, owner, "portal", PortalConfig.class);
        }
        if (obj != null) {
          pConfig = obj.getObject();
        }
      }

      // If no XML configuration
      if (pConfig == null) {
        PortalConfig persistedPortalConfig = layoutService.getPortalConfig(type, fixedOwnerName);
        // PortalConfig exists in storage => Do not reimport
        if (persistedPortalConfig != null) {
          return true;
        } else {
          // PortalConfig doesn't exists in storage => use default layout
          pConfig = buildEmptyPortalConfig(type, fixedOwnerName);
        }
      }
    }

    // If XML configuration of PortalConfig exists or PortalConfig doesn't exist
    // in storage, import PortalConfig switch default ImportMode
    ImportMode importMode = getRightMode(config.getImportMode());
    PortalConfigImporter portalImporter = new PortalConfigImporter(importMode, pConfig, layoutService);
    try {
      portalImporter.perform();
      return true;
    } catch (Exception ex) {
      log.error("An Exception occured when creating the Portal Configuration. Exception message: " + ex.getMessage(), ex);
      return false;
    }
  }

  public void createPage(NewPortalConfig config, String owner) {
    UnmarshalledObject<PageSet> pageSet = getConfig(config, owner, "pages", PageSet.class);
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      ImportMode importMode = getRightMode(config.getImportMode());
      ArrayList<Page> list = pageSet != null ? pageSet.getObject().getPages() : new ArrayList<>();
      PageImporter importer = new PageImporter(importMode,
                                               new SiteKey(config.getOwnerType(), owner),
                                               list,
                                               layoutService);
      importer.perform();
    } finally {
      RequestLifeCycle.end();
    }
  }

  public void createPageNavigation(NewPortalConfig config, String owner) {
    UnmarshalledObject<PageNavigation> obj = getConfig(config, owner, "navigation", PageNavigation.class);
    if (obj == null) {
      return;
    }

    //
    PageNavigation navigation = obj.getObject();

    //
    ImportMode importMode = getRightMode(config.getImportMode());

    //
    Locale locale;
    PortalConfig portalConfig = layoutService.getPortalConfig(config.getOwnerType(), owner);
    if (portalConfig != null && portalConfig.getLocale() != null) {
      locale = new Locale(portalConfig.getLocale());
    } else {
      locale = Locale.ENGLISH;
    }

    //
    NavigationImporter merge = new NavigationImporter(locale,
                                                      importMode,
                                                      navigation,
                                                      navigationService,
                                                      descriptionStorage);

    //
    merge.perform();
  }

  /**
   * Best effort to load and unmarshall a configuration.
   *
   * @param config the config object
   * @param portalName the owner
   * @param fileName the file name
   * @param objectType the type to unmarshall to
   * @return the xml of the config or null
   * @param <T> the generic type to unmarshall to
   */
  public <T> UnmarshalledObject<T> getConfig(NewPortalConfig config,
                                             String portalName,
                                             String fileName,
                                             Class<T> objectType) {
    String templateName = StringUtils.isBlank(config.getTemplateName()) ? fileName : config.getTemplateName();

    String portalType = config.getOwnerType();
    String location = config.getTemplateLocation();
    return getConfig(portalType, portalName, objectType, fileName, location, templateName);
  }

  public <T> T getConfig(String portalType,
                         String portalName,
                         Class<T> objectType,
                         String parentLocation) {
    String fileName;
    if (objectType.isAssignableFrom(PortalConfig.class)) {
      fileName = switch (portalType.toLowerCase()) {
      case "portal": {
        yield "portal";
      }
      case "group": {
        yield "group";
      }
      case "user": {
        yield "user";
      }
      case "portal_template": {
        yield "portal_template";
      }
      case "group_template": {
        yield "group_template";
      }
      default:
        throw new IllegalArgumentException("Unexpected value: " + portalType);
      };
    } else if (objectType.isAssignableFrom(PageSet.class)) {
      fileName = "pages";
    } else if (objectType.isAssignableFrom(PageNavigation.class)) {
      fileName = "navigation";
    } else {
      throw new IllegalArgumentException("Unexpected value: " + objectType);
    }
    UnmarshalledObject<T> config = getConfig(portalType, portalName, objectType, fileName, parentLocation, null);
    return config == null ? null : config.getObject();
  }

  public <T> UnmarshalledObject<T> getConfig(String portalType,
                                             String portalName,
                                             Class<T> objectType,
                                             String fileName,
                                             String parentLocation) {
    return getConfig(portalType, portalName, objectType, fileName, parentLocation, null);
  }

  public <T> UnmarshalledObject<T> getConfig(String portalType,
                                             String portalName,
                                             Class<T> objectType,
                                             String fileName,
                                             String parentLocation,
                                             String templateName) {
    String filePath = "/" + portalType + "/" + portalName + "/" + fileName + ".xml";
    String templateFilePath = StringUtils.isBlank(templateName) ? null :
                                                                "/" + portalType + "/template/" + templateName + "/" + fileName +
                                                                    ".xml";
    List<String> relativePaths = templateFilePath == null ? Collections.singletonList(filePath) :
                                                          Arrays.asList(filePath, templateFilePath);
    return getConfig(portalName, portalType, objectType, parentLocation, relativePaths);
  }

  public <T> UnmarshalledObject<T> getConfig(String portalName,
                                             String portalType,
                                             Class<T> objectType,
                                             String parentLocation,
                                             List<String> relativePaths) {
    String xml = relativePaths.stream()
                              .sequential()
                              .map(filePath -> getConfig(parentLocation, filePath))
                              .filter(Objects::nonNull)
                              .findFirst()
                              .orElse(null);
    if (xml != null) {
      xml = OWNER_PATTERN.matcher(xml).replaceAll(StringEscapeUtils.escapeXml11(portalName));
      xml = OWNER_TYPE_PATTERN.matcher(xml).replaceAll(StringEscapeUtils.escapeXml11(portalType));
      try {
        return fromXML(portalType, portalName, xml, objectType);
      } catch (Exception e) {
        throw new IllegalStateException(String.format("Error parsing configuration from location %s for portal with type %s and name %s (object type = %s)",
                                                      parentLocation,
                                                      portalType,
                                                      portalName,
                                                      objectType.getSimpleName()),
                                        e);
      }
    }
    return null;
  }

  public String getConfig(String location, String path) {
    String s = location + path;
    String content = null;
    try {
      log.debug("Attempt to load file " + s);
      s = fixPath(s);
      content = IOUtil.getStreamContentAsString(cmanager_.getInputStream(s));
      log.debug("Loaded file from path " + s + " with content " + content);
    } catch (Exception ignore) {
      log.debug("Could not get file " + s + " will return null instead");
    }
    return content;
  }

  /**
   * @param type
   * @param name
   * @return
   * @deprecated Site Templates has been changed to be stored in database to
   *             make it dynamically managed by UI rather than statis pages and
   *             navigation from source files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public String getTemplateConfig(String type, String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    for (SiteConfigTemplates tempConfig : templateConfigs) {
      Set<String> templates = tempConfig.getTemplates(type);
      if (templates != null && templates.contains(name))
        return tempConfig.getLocation();
    }
    return null;
  }

  /**
   * Get all template configurations
   *
   * @param siteType (portal, group, user)
   * @return set of template name
   * @deprecated Site Templates has been changed to be stored in database to
   *             make it dynamically managed by UI rather than statis pages and
   *             navigation from source files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public Set<String> getTemplateConfigs(String siteType) {
    Set<String> result = new HashSet<>();
    for (SiteConfigTemplates tempConfig : templateConfigs) {
      Set<String> templates = tempConfig.getTemplates(siteType);
      if (templates != null && templates.size() > 0) {
        result.addAll(templates);
      }
    }
    return result;
  }

  private String fixPath(String path) {
    while (path.contains("//")) {
      path = path.replaceAll("//", "/");
    }
    return path;
  }

  // Deserializing code
  private <T> UnmarshalledObject<T> fromXML(String ownerType, String owner, String xml, Class<T> clazz) throws Exception {
    UnmarshalledObject<T> obj = ModelUnmarshaller.unmarshall(clazz, xml.getBytes(StandardCharsets.UTF_8));
    T o = obj.getObject();
    if (o instanceof PageNavigation nav) {
      nav.setOwnerType(ownerType);
      nav.setOwnerId(owner);
      if (nav.getPriority() < 1) {
        nav.setPriority(PageNavigation.UNDEFINED_PRIORITY);
      }
      fixOwnerName((PageNavigation) o);
    } else if (o instanceof PortalConfig portalConfig) {
      portalConfig.setType(ownerType);
      portalConfig.setName(owner);
      fixOwnerName(portalConfig);
    } else if (o instanceof PageSet pageSet) {
      for (Page page : pageSet.getPages()) {
        page.setOwnerType(ownerType);
        page.setOwnerId(owner);
        fixOwnerName(page);
      }
    }
    return obj;
  }

  private PortalConfig buildEmptyPortalConfig(String type, String ownerName) {
    PortalConfig pConfig = new PortalConfig(type, ownerName);
    pConfig.useMetaPortalLayout();
    checkPortalConfigGroupProperties(pConfig);
    return pConfig;
  }

  private void checkPortalConfigGroupProperties(PortalConfig portalConfig) {
    if (portalConfig.getAccessPermissions() == null || portalConfig.getAccessPermissions().length == 0) {
      if (StringUtils.equals(portalConfig.getType(), SiteType.GROUP.getName())) {
        portalConfig.setAccessPermissions(new String[] { "*:" + portalConfig.getName() });
      } else if (StringUtils.equals(portalConfig.getType(), SiteType.USER.getName())) {
        portalConfig.setAccessPermissions(new String[] { portalConfig.getName() });
      } else {
        portalConfig.setAccessPermissions(new String[] { UserACL.EVERYONE });
      }
    }

    if (StringUtils.isBlank(portalConfig.getEditPermission())) {
      if (StringUtils.equals(portalConfig.getType(), SiteType.GROUP.getName())) {
        portalConfig.setEditPermission(userAcl.getAdminMSType() + ":" + portalConfig.getName());
      } else if (StringUtils.equals(portalConfig.getType(), SiteType.USER.getName())) {
        portalConfig.setEditPermission(portalConfig.getName());
      } else {
        portalConfig.setEditPermission(userAcl.getSuperUser());
      }
    }
    if (StringUtils.isBlank(portalConfig.getLocale())) {
      portalConfig.setLocale(localeConfigService.getDefaultLocaleConfig().getLocaleName());
    }
  }

  private static String fixOwnerName(String type, String owner) {
    if (type.equals(PortalConfig.GROUP_TYPE) && !owner.startsWith("/")) {
      return "/" + owner;
    } else {
      return owner;
    }
  }

  private static void fixOwnerName(PortalConfig config) {
    config.setName(fixOwnerName(config.getType(), config.getName()));
    fixOwnerName(config.getPortalLayout());
  }

  private static void fixOwnerName(Container container) {
    for (Object o : container.getChildren()) {
      if (o instanceof Container cont) {
        fixOwnerName(cont);
      }
    }
  }

  private static void fixOwnerName(PageNavigation pageNav) {
    pageNav.setOwnerId(fixOwnerName(pageNav.getOwnerType(), pageNav.getOwnerId()));
    ArrayList<NavigationFragment> fragments = pageNav.getFragments();
    if (fragments != null) {
      for (NavigationFragment fragment : fragments) {
        fixOwnerName(fragment);
      }
    }
  }

  private static void fixOwnerName(NavigationFragment fragment) {
    ArrayList<PageNode> nodes = fragment.getNodes();
    if (nodes != null) {
      for (PageNode pageNode : nodes) {
        fixOwnerName(pageNode);
      }
    }
  }

  private static void fixOwnerName(PageNode pageNode) {
    if (pageNode.getPageReference() != null) {
      String pageRef = pageNode.getPageReference();
      int pos1 = pageRef.indexOf("::");
      int pos2 = pageRef.indexOf("::", pos1 + 2);
      String type = pageRef.substring(0, pos1);
      String owner = pageRef.substring(pos1 + 2, pos2);
      String name = pageRef.substring(pos2 + 2);
      owner = fixOwnerName(type, owner);
      pageRef = type + "::" + owner + "::" + name;
      pageNode.setPageReference(pageRef);
    }
    if (pageNode.getNodes() != null) {
      for (PageNode childPageNode : pageNode.getNodes()) {
        fixOwnerName(childPageNode);
      }
    }
  }

  private static void fixOwnerName(Page page) {
    page.setOwnerId(fixOwnerName(page.getOwnerType(), page.getOwnerId()));
    fixOwnerName((Container) page);
  }

  private ImportMode getRightMode(String mode) {
    ImportMode importMode;
    if (mode != null) {
      importMode = ImportMode.valueOf(mode.trim().toUpperCase());
    } else {
      importMode = owner_.getDefaultImportMode();
    }

    if (isFirstStartup && (importMode == ImportMode.CONSERVE || importMode == ImportMode.INSERT)) {
      return ImportMode.MERGE;
    }

    return importMode;
  }

}
