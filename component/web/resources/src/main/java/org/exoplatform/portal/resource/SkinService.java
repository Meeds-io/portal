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
package org.exoplatform.portal.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.controller.resource.ResourceRequestHandler;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.WebApp;
import org.gatein.wci.WebAppListener;
import org.picocontainer.Startable;

import org.exoplatform.commons.cache.future.FutureMap;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.portal.resource.compressor.ResourceCompressor;
import org.exoplatform.portal.resource.compressor.ResourceCompressorException;
import org.exoplatform.portal.resource.compressor.ResourceType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.ControllerContext;

import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Managed
@NameTemplate({ @Property(key = "view", value = "portal"), @Property(key = "service", value = "management"),
                @Property(key = "type", value = "skin") })
@ManagedDescription("Skin service")
@RESTEndpoint(path = "skinservice")
public class SkinService extends AbstractResourceService implements Startable {

  protected static final Log                                     LOG                     =
                                                                     ExoLogger.getLogger("portal.SkinService");

  private static final String                                    LEFT_P                  = "\\(";

  private static final String                                    RIGHT_P                 = "\\)";

  private static final Pattern                                   BACKGROUND_PATTERN      =
                                                                                    Pattern.compile("(background[^;])+([^;]*;)");

  private static final Pattern                                   FONT_FACE_PATTERN       = Pattern.compile("(src[^;])+([^;]*;)");

  private static final Pattern                                   URL_PATTERN             = Pattern.compile("(url" + LEFT_P +
      "['\"]?)([^'\";" + RIGHT_P + "]+)(['\"]?\\))");

  /** Immutable and therefore thread safe. */
  private static final Pattern                                   LT                      =
                                                                    Pattern.compile(".*/\\*.*orientation=lt.*\\*/.*");

  /** Immutable and therefore thread safe. */
  private static final Pattern                                   RT                      =
                                                                    Pattern.compile(".*/\\*.*orientation=rt.*\\*/.*");

  public static final String                                     DEFAULT_SKIN_PARAM_NAME = "skin.default";

  private static final String                                    DEFAULT_SKIN            = "Enterprise";

  public static final String                                     CUSTOM_MODULE_ID        = "customModule";

  public static final long                                       MAX_AGE;

  /** The deployer. */
  private final WebAppListener                                   deployer;

  private final Map<SkinKey, SkinConfig>                         portalSkins;

  private final Map<SkinKey, SkinConfig>                         customPortalSkins;

  private final Map<SkinKey, SkinConfig>                         skinConfigs;

  private final HashSet<String>                                  availableSkins;

  /**
   * @deprecated is replaced by a stored cache using files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  private final FutureMap<String, CachedStylesheet, SkinContext> ltCache;

  /**
   * @deprecated is replaced by a stored cache using files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  private final FutureMap<String, CachedStylesheet, SkinContext> rtCache;

  private final Map<String, Set<String>>                         portletThemes;

  @Getter
  private Map<Integer, File>                                     files                   = new ConcurrentHashMap<>();

  private String                                                 defaultSkin;

  /**
   * The name of the portal container
   */
  final String                                                   portalContainerName;

  /**
   * An id used for caching request. The id life cycle is the same than the
   * class instance because we consider css will change until server is
   * restarted. Of course this only applies for the developing mode set to
   * false.
   */
  final String                                                   id                      =
                                                                    Long.toString(System.currentTimeMillis());

  @Setter
  private boolean                                                developing;

  static {
    long seconds = 31536000L;
    String propValue = PropertyManager.getProperty("gatein.assets.css.max-age");
    if (propValue != null) {
      try {
        seconds = Long.valueOf(propValue);
      } catch (NumberFormatException e) {
        LOG.warn("The gatein.assets.css.max-age property is not set properly.");
      }
    }

    MAX_AGE = seconds;
  }

  static class SkinContext {
    final ControllerContext controller;

    final Orientation       orientation;

    SkinContext(ControllerContext controller, Orientation orientation) {
      this.controller = controller;
      this.orientation = orientation;
    }
  }

  public SkinService(InitParams initParams, ExoContainerContext context, ResourceCompressor compressor) {
    super(compressor);
    Loader<String, CachedStylesheet, SkinContext> loader = new Loader<String, CachedStylesheet, SkinContext>() {
      public CachedStylesheet retrieve(SkinContext context, String key) throws Exception {
        Resource skin = getCSSResource(key, key);
        if (skin == null) {
          return null;
        }

        StringBuffer sb = new StringBuffer();
        try (Reader sourceReader = skin.read()) {
          processCSSRecursively(skin.getContextPath() + skin.getParentPath(),
                                sourceReader,
                                sb,
                                context.orientation);
        }
        String css = sb.toString();
        if (SkinService.this.compressor.isSupported(ResourceType.STYLESHEET)) {
          css = SkinService.this.compressor.compress(css, ResourceType.STYLESHEET);
        }
        return new CachedStylesheet(css);
      }
    };

    //
    portalSkins = new LinkedHashMap<>();
    customPortalSkins = new LinkedHashMap<>();
    skinConfigs = new LinkedHashMap<>();
    availableSkins = new HashSet<>();
    ltCache = new FutureMap<>(loader);
    rtCache = new FutureMap<>(loader);
    portletThemes = new HashMap<>();
    portalContainerName = context.getPortalContainerName();
    deployer = new GateInSkinConfigDeployer(portalContainerName, this);

    defaultSkin = DEFAULT_SKIN;
    if (initParams != null) {
      ValueParam defaultSkinValueParam = initParams.getValueParam(DEFAULT_SKIN_PARAM_NAME);
      if (defaultSkinValueParam != null && StringUtils.isNotEmpty(defaultSkinValueParam.getValue())) {
        defaultSkin = defaultSkinValueParam.getValue();
      }
    }
  }

  @Override
  public void start() {
    developing = PropertyManager.isDevelopping();
    ServletContainerFactory.getServletContainer().addWebAppListener(deployer);
  }

  @Override
  public void stop() {
    ServletContainerFactory.getServletContainer().removeWebAppListener(deployer);
  }

  /**
   * Cache data after startup
   */
  public void initData() {
    long start = System.currentTimeMillis();
    LOG.info("Start caching CSS data");
    ExecutorService executorService = Executors.newSingleThreadExecutor(); // NOSONAR
    executorService.execute(() -> {
      ExoContainerContext.setCurrentContainer(PortalContainer.getInstance());
      try {
        skinConfigs.values().parallelStream().forEach(this::initSkinModuleCache);
      } finally {
        LOG.info("End caching CSS data within {}ms", System.currentTimeMillis() - start);
        ExoContainerContext.setCurrentContainer(null);
        executorService.shutdownNow();
      }
    });
  }

  /**
   * Get all available skin
   *
   * @return all available skin
   */
  @Managed
  @ManagedDescription("The list of registered skins identifiers")
  public String[] getSkinList() {
    // get all available skin
    List<String> availableSkin = new ArrayList<>();
    for (String skin : availableSkins) {
      availableSkin.add(skin);
    }
    // sort skin name asc
    Collections.sort(availableSkin);

    return availableSkin.toArray(new String[availableSkin.size()]);
  }

  /**
   * Clean cache, reload all Skins
   */
  @Managed
  @ManagedDescription("Reload all skins")
  @Impact(ImpactType.WRITE)
  public void reloadSkins() {
    // remove all ltCache, rtCache
    ltCache.clear();
    rtCache.clear();
    files.clear();
  }

  /**
   * Invalidate skin from the cache
   *
   * @param path the key
   */
  public void invalidateCachedSkin(String path) {
    ltCache.remove(path);
    rtCache.remove(path);
  }

  /**
   * @param fileWebAppPath File Path including webapp context name
   * @param fileContentHash File Content Hash
   * @param orientation {@link Orientation} of UI requesting for the CSS
   * @param compress whether to compress the CSS or not
   * @return {@link File} representing the CSS File content
   * @throws IOException when an error happens while reading file
   */
  public String getSkinModuleContent(String fileWebAppPath,
                                     int fileContentHash,
                                     Orientation orientation,
                                     boolean compress) throws IOException {
    if (developing) {
      return getSkinModuleFileContent(fileWebAppPath, orientation, compress);
    } else {
      File cssFile = files.computeIfAbsent(Objects.hash(fileContentHash, orientation, compress),
                                           k -> {
                                             try {
                                               return getSkinModuleFileNoCache(fileWebAppPath, orientation, compress);
                                             } catch (IOException e) {
                                               throw new IllegalStateException(String.format("Error while reading file %s content",
                                                                                             fileWebAppPath),
                                                                               e);
                                             }
                                           });
      if (cssFile == null) {
        return null;
      } else {
        return FileUtils.readFileToString(cssFile, StandardCharsets.UTF_8);
      }
    }
  }

  /**
   * Returns CSS file content switch file request path
   * 
   * @param fileWebAppPath file request path including context path
   * @return file content
   * @throws IOException if file not found or an error occurred while reading it
   */
  public String getSkinModuleFileContent(String fileWebAppPath) throws IOException {
    ServletContext servletContext = Collections.unmodifiableCollection(contexts.values())
                                               .stream()
                                               .filter(c -> fileWebAppPath.startsWith(c.getContextPath()+"/"))
                                               .map(WebApp::getServletContext)
                                               .findFirst()
                                               .orElse(null);
    if (servletContext == null) {
      throw new IllegalStateException("Can't retrieve ServletContext of path " + fileWebAppPath);
    }
    try (InputStream inputStream = servletContext.getResourceAsStream(fileWebAppPath.replaceFirst(servletContext.getContextPath(),
                                                                                                  ""))) {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
  }

  public String getDefaultSkin() {
    if (!availableSkins.contains(defaultSkin)) {
      LOG.warn("Skin \"{}\" does not exist, switching to skin \"Default\" as the default skin", defaultSkin);
      defaultSkin = DEFAULT_SKIN;
    }
    return defaultSkin;
  }

  /**
   * Add a portal skin with the <code>priority</code> is Integer.MAX_VALUE and
   * the <code>overwrite</code> is false by default
   *
   * @param module
   * @param skinName
   * @param cssPath
   */
  public void addPortalSkin(String module, String skinName, String cssPath) {
    addPortalSkin(module, skinName, cssPath, Integer.MAX_VALUE, false);
  }

  /**
   * Add a portal skin with the <code>priority</code> is Integer.MAX_VALUE by
   * default
   *
   * @param module
   * @param skinName
   * @param cssPath
   * @param overwrite
   */
  public void addPortalSkin(String module, String skinName, String cssPath, boolean overwrite) {
    addPortalSkin(module, skinName, cssPath, Integer.MAX_VALUE, overwrite);
  }

  /**
   * Register a portal skin
   *
   * @param module skin module identifier
   * @param skinName skin name
   * @param cssPath path uri to the css file. This is relative to the root
   *          context, use leading '/'
   * @param priority priority to support sorting in skin list
   * @param overwrite if any previous skin should be replaced by that one
   */
  public void addPortalSkin(String module, String skinName, String cssPath, int priority, boolean overwrite) {
    availableSkins.add(skinName);
    SkinKey key = new SkinKey(module, skinName);
    SkinConfig skinConfig = portalSkins.get(key);
    if (skinConfig == null || overwrite) {
      if (priority < 0) {
        priority = Integer.MAX_VALUE;
      }

      skinConfig = new SimpleSkin(module, skinName, cssPath, priority);
      if (module.startsWith(CUSTOM_MODULE_ID)) {
        skinConfig.setType("custom-skin");
        customPortalSkins.put(key, skinConfig);
      } else {
        skinConfig.setType("portal-skin");
        portalSkins.put(key, skinConfig);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding Portal skin : Bind " + key + " to " + skinConfig);
      }
    }
  }

  /**
   * Register a portal skin
   *
   * @param module skin module identifier
   * @param skinName skin name
   * @param cssPath path uri to the css file. This is relative to the root
   *          context, use leading '/'
   * @param priority priority to support sorting in skin list
   * @param overwrite if any previous skin should be replaced by that one
   * @param filtered if true, then the portal skin will be loaded only when
   *          required by a portlet
   * @return
   */
  public SkinConfig addPortalSkin(String module,
                                  String skinName,
                                  String cssPath,
                                  int priority,
                                  boolean overwrite,
                                  boolean filtered) {
    availableSkins.add(skinName);
    SkinKey key = new SkinKey(module, skinName);
    SkinConfig skinConfig = portalSkins.get(key);
    if (skinConfig == null || overwrite) {
      if (priority < 0) {
        priority = Integer.MAX_VALUE;
      }

      SimpleSkin skin = new SimpleSkin(module, skinName, cssPath, priority, filtered);
      if (module.startsWith(CUSTOM_MODULE_ID)) {
        skin.setType("custom-skin");
        customPortalSkins.put(key, skin);
      } else {
        skin.setType("portal-skin");
        portalSkins.put(key, skin);
      }
    }
    return skinConfig;
  }

  /**
   * Add a skin with the <code>priority</code> is Integer.MAX_VALUE and the
   * <code>overwrite</code> is false by default
   *
   * @param module
   * @param skinName
   * @param cssPath
   */
  public void addSkin(String module, String skinName, String cssPath) {
    addSkin(module, skinName, cssPath, Integer.MAX_VALUE, false);
  }

  /**
   * Add a portal skin with the <code>priority</code> is Integer.MAX_VALUE
   *
   * @param module
   * @param skinName
   * @param cssPath
   * @param overwrite
   */
  public void addSkin(String module, String skinName, String cssPath, boolean overwrite) {
    addSkin(module, skinName, cssPath, Integer.MAX_VALUE, overwrite);
  }

  /**
   * Register the Skin for available portal Skins. Support priority
   *
   * @param module skin module identifier
   * @param skinName skin name
   * @param cssPath path uri to the css file. This is relative to the root
   *          context, use leading '/'
   * @param overwrite if any previous skin should be replaced by that one
   * @param priority priority to support sorting in skin list
   */
  public void addSkin(String module, String skinName, String cssPath, int priority, boolean overwrite) {
    addSkin(module, skinName, cssPath, priority, overwrite, null);
  }

  public SkinConfig addSkin(String module,
                            String skinName,
                            String cssPath,
                            int priority,
                            boolean overwrite,
                            List<String> additionalModules) {
    availableSkins.add(skinName);
    SkinKey key = new SkinKey(module, skinName);
    SkinConfig skinConfig = skinConfigs.get(key);
    if (skinConfig == null || overwrite) {
      if (priority < 0) {
        priority = Integer.MAX_VALUE;
      }
      skinConfig = new SimpleSkin(module, skinName, cssPath, priority, additionalModules);
      skinConfig.setType("portlet-skin");
      skinConfigs.put(key, skinConfig);
    } else {
      if (CollectionUtils.isNotEmpty(additionalModules)) {
        skinConfig.getAdditionalModules().addAll(additionalModules);
      }
      if (skinConfig.getCSSPath() == null && cssPath != null) {
        skinConfig.setType("portlet-skin");
        skinConfig.setCSSPath(cssPath);
        skinConfig.setPriority(priority);
      }
    }
    return skinConfig;
  }

  /**
   * Registry theme category with its themes for portlet Theme
   *
   * @param categoryName category name that will be registried
   * @param themesName list theme name of categoryName
   */
  public void addTheme(String categoryName, List<String> themesName) {
    portletThemes.computeIfAbsent(categoryName, k -> new HashSet<>())
                 .addAll(themesName);
  }

  /**
   * Get names of all the currently registered skins.
   *
   * @return an unmodifiable Set of the currently registered skins
   */
  public Set<String> getAvailableSkinNames() {
    return availableSkins;
  }

  /**
   * Render css content of the file specified by the given URI
   *
   * @param context
   * @param renderer the webapp's
   *          {@link org.exoplatform.portal.resource.ResourceRenderer}
   * @param compress
   * @throws IOException
   * @return <code>true</code> if the <code>CSS resource </code>is found and
   *         rendered; <code>false</code> otherwise.
   * @deprecated since not used for CSS content retrieval anymore. Kept for
   *             retro compatibility with UIs making static reference to Portal
   *             Skin CSS paths
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public boolean renderCSS(ControllerContext context, ResourceRenderer renderer, boolean compress) throws IOException {
    Orientation orientation = getOrientation(context);
    // Check if it is running under developing mode
    String resource = "/" + context.getParameter(ResourceRequestHandler.RESOURCE_QN) + ".css";
    if (!compress) {
      StringBuffer sb = new StringBuffer();
      Resource skin = getCSSResource(resource, resource);
      if (skin != null) {
        try (Reader sourceReader = skin.read()) {
          processCSSRecursively(skin.getContextPath() + skin.getParentPath(),
                                sourceReader,
                                sb,
                                orientation);
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        renderer.setExpiration(MAX_AGE);
        if (context.getResponse() != null) {
          context.getResponse().setContentLength(bytes.length);
        }
        renderer.getOutput().write(bytes);
        return true;
      }
    } else {
      CachedStylesheet cachedCss = getCache(orientation).get(new SkinContext(context, orientation), resource);
      if (cachedCss != null) {
        renderer.setExpiration(MAX_AGE);
        if (context.getResponse() != null) {
          context.getResponse().setContentLength(cachedCss.getLength());
        }
        cachedCss.writeTo(renderer.getOutput());
        return true;
      }
    }

    //
    return false;
  }

  /**
   * Return a collection of Portal Skins that its elements are ordered by CSS
   * priority
   *
   * @param skinName name of Portal Skin
   * @return all org.exoplatform.portal.resource.SkinConfig of Portal Skin
   */
  public Collection<SkinConfig> getPortalSkins(String skinName) {
    if (StringUtils.isEmpty(skinName)) {
      skinName = getDefaultSkin();
    }
    Set<SkinKey> keys = portalSkins.keySet();
    List<SkinConfig> skins = new ArrayList<>();
    for (SkinKey key : keys) {
      if (key.getName().equals(skinName)) {
        SkinConfig skinConfig = this.portalSkins.get(key);
        if (!skinConfig.isFiltered()) {
          skins.add(skinConfig);
        }
      }
    }
    Collections.sort(skins, (o1, o2) -> o1.getCSSPriority() - o2.getCSSPriority());
    return skins;
  }

  /**
   * Return a collection of SkinConfig based on SkinVisitor provided as the
   * argument
   *
   * @param visitor
   * @return
   */
  public Collection<SkinConfig> findSkins(SkinVisitor visitor) {
    return visitor.getSkins(portalSkins.entrySet(), skinConfigs.entrySet());
  }

  /**
   * Return the collection of custom portal skins
   *
   * @param skinName
   * @return the map of custom portal skins
   */
  public Collection<SkinConfig> getCustomPortalSkins(String skinName) {
    if (StringUtils.isEmpty(skinName)) {
      skinName = getDefaultSkin();
    }
    Set<SkinKey> keys = customPortalSkins.keySet();
    List<SkinConfig> skins = new ArrayList<>();
    for (SkinKey key : keys) {
      if (key.getName().equals(skinName)) {
        SkinConfig skinConfig = this.customPortalSkins.get(key);
        if (!skinConfig.isFiltered()) {
          skins.add(skinConfig);
        }
      }
    }
    Collections.sort(skins, (o1, o2) -> o1.getCSSPriority() - o2.getCSSPriority());
    return skins;
  }

  /**
   * Return the map of portlet themes
   *
   * @return the map of portlet themes
   */
  public Map<String, Set<String>> getPortletThemes() {
    return portletThemes;
  }

  /**
   * Return a SkinConfig mapping by the module and skin name
   *
   * @param module
   * @param skinName
   * @return SkinConfig by SkinKey(module, skinName), or SkinConfig by
   *         SkinKey(module, defaultSkin)
   */
  public SkinConfig getSkin(String module, String skinName) {
    if (StringUtils.isBlank(skinName)) {
      skinName = getDefaultSkin();
    }

    SkinConfig config = skinConfigs.get(new SkinKey(module, skinName));
    if (config == null) {
      config = skinConfigs.get(new SkinKey(module, getDefaultSkin()));
    }
    return config;
  }

  /**
   * Return a Portal SkinConfig mapping by the module and skin name
   *
   * @param module
   * @param skinName
   * @return SkinConfig by SkinKey(module, skinName), or SkinConfig by
   *         SkinKey(module, SkinService.DEFAULT_SKIN)
   */
  public SkinConfig getPortalSkin(String module, String skinName) {
    SkinConfig portalSkin = getPortalSkin(module, skinName, portalSkins);
    if (portalSkin == null) {
      portalSkin = getPortalSkin(module, skinName, customPortalSkins);
    }
    return portalSkin;
  }

  /**
   * Returns last modified date of cached css.
   * <p>
   * In development mode, it always returns {@link Long#MAX_VALUE}. Return null
   * if cached css can not be found
   *
   * @param context
   * @deprecated CSS Caching is using files mechanism instead of InMemory cache
   */
  @Deprecated
  public long getLastModified(ControllerContext context) {
    String resource = "/" + context.getParameter(ResourceRequestHandler.RESOURCE_QN) + ".css";

    Orientation orientation = getOrientation(context);
    CachedStylesheet cachedCSS = getCache(orientation).get(new SkinContext(context, orientation), resource);
    if (cachedCSS == null) {
      return System.currentTimeMillis();
    } else {
      return cachedCSS.getLastModified();
    }
  }

  /**
   * Remove a Skin mapped to the <code>key</code>
   *
   * @param key key whose mapping skin is to be removed from the service
   */
  public void removeSkin(SkinKey key) {
    if (key == null) {
      return;
    }

    SkinConfig remove = skinConfigs.remove(key);

    if (remove != null && StringUtils.isNotBlank(remove.getCSSPath())) {
      invalidateCachedSkin(remove.getCSSPath());
    }
  }

  /**
   * Remove a Skin from the service as well as its cache
   *
   * @param module
   * @param skinName
   */
  public void removePortalSkin(String module, String skinName) {
    SkinKey key;
    if (StringUtils.isBlank(skinName)) {
      key = new SkinKey(module, getDefaultSkin());
    } else {
      key = new SkinKey(module, skinName);
    }

    removePortalSkin(key);
  }

  /**
   * Remove a Skin mapped to the <code>key</code>
   *
   * @param key key whose mapping skin is to be removed from the service
   */
  public void removePortalSkin(SkinKey key) {
    if (key == null) {
      return;
    }

    SkinConfig remove = null;
    if (key.getModule().startsWith(CUSTOM_MODULE_ID)) {
      remove = customPortalSkins.remove(key);
    } else {
      remove = portalSkins.remove(key);
    }

    if (remove != null && StringUtils.isNotBlank(remove.getCSSPath())) {
      invalidateCachedSkin(remove.getCSSPath());
    }
  }

  /**
   * Remove SkinConfig from Portal Skin Config by SkinKey
   *
   * @param keys SkinKey list these will be removed
   */
  public void removeSkins(List<SkinKey> keys) {
    if (keys == null) {
      return;
    }

    for (SkinKey key : keys) {
      removeSkin(key);
    }
  }

  /**
   * Remove Skin from Portal available Skin by skin name
   *
   * @param skinName name of skin that will be removed
   */
  public void removeSupportedSkin(String skinName) {
    availableSkins.remove(skinName);
  }

  /**
   * Return the number of skin config maintaining in this SkinService
   *
   * @return the number of skin config maintaining in this SkinService
   */
  public int size() {
    return skinConfigs.size();
  }

  /**
   * Apply CSS for Skin <br>
   * If skin is null, do nothing
   * 
   * @param basePath
   * @param sourceReader
   * @param appendable
   * @param orientation
   * @throws IOException
   */
  public void processCSSRecursively(String basePath,
                                    Reader sourceReader,
                                    Appendable appendable,
                                    Orientation orientation) throws IOException {
    try (BufferedReader reader = new SkipCommentReader(sourceReader, new CommentBlockHandler.OrientationCommentBlockHandler())) {
      String line = reader.readLine();
      while (line != null) {
        line = proccessOrientation(line, orientation);
        line = processURL(BACKGROUND_PATTERN, line, basePath);
        line = processURL(FONT_FACE_PATTERN, line, basePath);
        if (StringUtils.isNotBlank(line)) {
          appendable.append(line);
        }
        if ((line = reader.readLine()) != null) {
          appendable.append("\n");
        }
      }
    }
  }

  /**
   * This method delegates the resource resolving to MainResourceResolver and
   * prints out appropriated log messages Consider the two cases the method is
   * invoked Case 1: Resolve nested .css file In Stylesheet.css we have the
   * statement
   *
   * @import url(xyzt.css); To resolve the resource from xyzt.css,
   *         getCSSResource("xyzt.css", "Stylesheet.css") is called Case 2:
   *         Resolve top root .css file To resolve a top root Stylesheet.css
   *         file, getCSSResource("Stylesheet.css", "Stylesheet.css") is called
   * @param cssPath
   * @param outerCssFile
   * @return
   */
  private Resource getCSSResource(String cssPath, String outerCssFile) {
    Resource resource = mainResolver.resolve(cssPath);
    if (resource == null && LOG.isErrorEnabled()) {
      String logMessage;
      if (!cssPath.equals(outerCssFile)) {
        int lastIndexOfSlash = cssPath.lastIndexOf('/');
        String loadedCssFile = (lastIndexOfSlash >= 0) ? (cssPath.substring(lastIndexOfSlash + 1)) : cssPath;
        logMessage = "Invalid <CSS FILE> configuration, please check the @import url(" + loadedCssFile + ") in " + outerCssFile +
            " , SkinService could not load the skin " + cssPath;
      } else {
        logMessage = "Not found <CSS FILE>, the path " + cssPath + " is invalid, SkinService could not load the skin " + cssPath;
      }
      LOG.error(logMessage);
    }
    return resource;
  }

  private String processURL(Pattern pattern, String line, String basePath) {
    Matcher patternMatcher = pattern.matcher(line);
    StringBuffer tmpBuilder = new StringBuffer();
    while (patternMatcher.find()) {
      Matcher urlMatcher = URL_PATTERN.matcher(patternMatcher.group());
      StringBuffer tmpURL = new StringBuffer();
      while (urlMatcher.find()) {
        if (!urlMatcher.group(2).startsWith("\"/") && !urlMatcher.group(2).startsWith("'/")
            && !urlMatcher.group(2).startsWith("/")) {
          StringBuilder urlBuilder = new StringBuilder();
          urlBuilder.append(urlMatcher.group(1));
          urlBuilder.append(basePath);
          urlBuilder.append(urlMatcher.group(2));
          urlBuilder.append(urlMatcher.group(3));
          urlMatcher.appendReplacement(tmpURL, urlBuilder.toString());
        }
      }
      urlMatcher.appendTail(tmpURL);
      patternMatcher.appendReplacement(tmpBuilder, tmpURL.toString());
    }

    patternMatcher.appendTail(tmpBuilder);
    return tmpBuilder.toString();
  }

  private String proccessOrientation(String line, Orientation orientation) {
    Pattern orientationPattern = orientation == Orientation.LT ? RT : LT;
    Matcher matcher = orientationPattern.matcher(line);
    StringBuffer tmpBuilder = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(tmpBuilder, "");
    }
    matcher.appendTail(tmpBuilder);
    return tmpBuilder.toString();
  }

  /**
   * @param orientation {@link Orientation}
   * @return Cache entry according to orientation
   * @deprecated is replaced by a stored cache using files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  private FutureMap<String, CachedStylesheet, SkinContext> getCache(Orientation orientation) {
    return orientation == Orientation.RT ? rtCache : ltCache;
  }

  private Orientation getOrientation(ControllerContext context) {
    if ("rt".equals(context.getParameter(ResourceRequestHandler.ORIENTATION_QN))) {
      return Orientation.RT;
    } else {
      return Orientation.LT;
    }
  }

  private SkinConfig getPortalSkin(String module, String skinName, Map<SkinKey, SkinConfig> portalSkins) {
    if (StringUtils.isEmpty(skinName)) {
      skinName = getDefaultSkin();
    }

    SkinConfig config = portalSkins.get(new SkinKey(module, skinName));
    if (config == null) {
      config = portalSkins.get(new SkinKey(module, getDefaultSkin()));
    }
    return config;
  }

  private File getSkinModuleFileNoCache(String fileWebAppPath, Orientation orientation, boolean compress) throws IOException {
    String fileContent = getSkinModuleFileContent(fileWebAppPath, orientation, compress);
    if (fileContent != null) {
      try {
        // Cache result into a temporary file
        File file = File.createTempFile("stylesheet_cache_", fileWebAppPath.substring(fileWebAppPath.lastIndexOf("/") + 1));
        FileUtils.write(file, fileContent, StandardCharsets.UTF_8);
        // Ensure to clean cached file on JVM exit
        file.deleteOnExit();
        return file;
      } catch (Exception e) {
        LOG.error("Error while processing CSS file {}", fileWebAppPath, e);
        return null;
      }
    } else {
      return null;
    }
  }

  private String getSkinModuleFileContent(String fileWebAppPath, Orientation orientation, boolean compress) throws IOException {
    String fileContent = getSkinModuleFileContent(fileWebAppPath);
    // Process CSS
    try (Reader sourceReader = new StringReader(fileContent)) {
      StringBuilder sb = new StringBuilder();
      processCSSRecursively(fileWebAppPath,
                            sourceReader,
                            sb,
                            orientation);
      fileContent = sb.toString().replaceAll("(\n\n+)", "\n");
    }
    if (compress && compressor.isSupported(ResourceType.STYLESHEET)) {
      try {
        fileContent = compressor.compress(fileContent, ResourceType.STYLESHEET);
      } catch (ResourceCompressorException e) {
        LOG.warn("Error while compressing CSS file {}. Retrieve it as is", fileWebAppPath, e);
      }
    }
    return fileContent;
  }

  @SneakyThrows
  public void initSkinModuleCache(SkinConfig skin) {
    if (StringUtils.isBlank(skin.getCSSPath())) {
      LOG.debug("Ignore caching empty CSS file for module {}", skin.getModule());
      return;
    }
    ExoContainerContext.setCurrentContainer(PortalContainer.getInstance());
    String fullCSSPath = null;
    try {
      fullCSSPath = skin.createURL().toString();
      fullCSSPath = fullCSSPath.substring(0, fullCSSPath.indexOf("?"));
      getSkinModuleContent(fullCSSPath, skin.getFileContentHash(), Orientation.LT, !developing);
    } catch (Exception e) {
      LOG.warn("Error while initializing cache of CSS with path {}", fullCSSPath, e);
    } finally {
      ExoContainerContext.setCurrentContainer(null);
    }
  }

}
