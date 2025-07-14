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
package org.exoplatform.web.application.javascript;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.ScriptContent;
import org.gatein.portal.controller.resource.ScriptKey;
import org.gatein.portal.controller.resource.ScriptLoader;
import org.gatein.portal.controller.resource.script.BaseScriptResource;
import org.gatein.portal.controller.resource.script.FetchMode;
import org.gatein.portal.controller.resource.script.Module;
import org.gatein.portal.controller.resource.script.ScriptGraph;
import org.gatein.portal.controller.resource.script.ScriptGroup;
import org.gatein.portal.controller.resource.script.ScriptResource;
import org.gatein.portal.controller.resource.script.ScriptResource.DepInfo;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.WebApp;
import org.gatein.wci.WebAppListener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.picocontainer.Startable;

import org.exoplatform.commons.cache.future.FutureMap;
import org.exoplatform.commons.utils.CompositeReader;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.portal.resource.AbstractResourceService;
import org.exoplatform.portal.resource.compressor.ResourceCompressor;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;

import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.SneakyThrows;

@Managed
@NameTemplate({ @Property(key = "view", value = "portal"), @Property(key = "service", value = "management"),
                @Property(key = "type", value = "javascript") })
@ManagedDescription("Javascript config service")
@RESTEndpoint(path = "javascriptService")
public class JavascriptConfigService extends AbstractResourceService implements Startable {

  public static final Pattern                               JS_ID_PATTERN   = Pattern.compile("^[a-zA-Z_$][0-9a-zA-Z_$]*$");

  public static final List<String>                          RESERVED_MODULE =
                                                                            Collections.unmodifiableList(Arrays.asList("require",
                                                                                                                       "exports",
                                                                                                                       "module"));

  private static final boolean                              DEVELOPPING     = PropertyManager.isDevelopping();

  private static final Log                                  LOG             = ExoLogger.getLogger(JavascriptConfigService.class);

  private static final Pattern                              INDEX_PATTERN   = Pattern.compile("^.+?(_([1-9]+))$");

  private final WebAppListener                              deployer;

  @Getter
  private final ScriptGraph                                 scriptGraph;

  private final Map<String, String>                         scriptURLs      = new ConcurrentHashMap<>();

  private final FutureMap<ScriptKey, ScriptContent, Object> scriptCache     = new FutureMap<>(new ScriptLoader());

  private JSONObject                                        jsConfig;

  public JavascriptConfigService(ExoContainerContext context, ResourceCompressor compressor) {
    super(compressor);

    //
    this.scriptGraph = new ScriptGraph();
    this.deployer = new JavascriptConfigDeployer(context.getPortalContainerName(), this);
  }

  @Override
  public void start() {
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
    LOG.info("Start caching Javascript data");
    ExecutorService executorService = Executors.newSingleThreadExecutor(); // NOSONAR
    executorService.execute(() -> {
      ExoContainerContext.setCurrentContainer(PortalContainer.getInstance());
      try {
        getAllResources().parallelStream().forEach(this::generateUrl);
      } finally {
        LOG.info("End caching Javascript data within {}ms", System.currentTimeMillis() - start);
        ExoContainerContext.setCurrentContainer(null);
        executorService.shutdownNow();
      }
    });
  }

  @Managed
  @ManagedDescription("Retrieve all javascript modules IDs")
  @Impact(ImpactType.READ)
  public Collection<String> getJavascriptKeys() {
    List<String> keys = new ArrayList<>();
    Set<ScriptKey> cacheKeys = scriptCache.getKeys();
    for (ScriptKey scriptKey : cacheKeys) {
      keys.add(scriptKey.getId().toString());
    }
    return keys;
  }

  @Managed
  @ManagedDescription("Reload all javascript modules")
  @Impact(ImpactType.WRITE)
  public void reloadJavascripts() {
    scriptCache.clear();
  }

  @Managed
  @ManagedDescription("Reload a selected javascript module by its ID")
  @Impact(ImpactType.WRITE)
  public void reloadJavascript(
                               @ManagedDescription("JS Module: SCOPE/NAME")
                               @ManagedName("jsModule")
                               String jsModule) {
    String[] scriptIdParts = jsModule.split("/");
    if (scriptIdParts.length != 2) {
      throw new IllegalArgumentException("js module have to be identified by 'SCOPE/id' like 'SHARED/jquery' ");
    }

    ResourceId resource = new ResourceId(ResourceScope.valueOf(scriptIdParts[0]), scriptIdParts[1]);
    LocaleConfigService localeService = ExoContainerContext.getCurrentContainer()
                                                           .getComponentInstanceOfType(LocaleConfigService.class);
    Collection<LocaleConfig> localConfigs = localeService.getLocalConfigs();
    for (LocaleConfig localeConfig : localConfigs) {
      ScriptKey key = new ScriptKey(resource, true, localeConfig.getLocale());
      scriptCache.remove(key);
    }
    ScriptKey key = new ScriptKey(resource, true, null);
    scriptCache.remove(key);
  }

  @SneakyThrows
  public ScriptContent getScriptContent(ResourceScope scope, String module, boolean compress) {
    ResourceId resource = new ResourceId(scope, module);
    ScriptKey key = new ScriptKey(resource, compress, Locale.ENGLISH);
    if (DEVELOPPING) {
      return scriptCache.getLoader().retrieve(null, key);
    } else {
      return scriptCache.get(null, key);
    }
  }

  public Reader getScript(ResourceId resourceId, Locale locale) throws Exception {
    return getCompositeScript(resourceId, locale);
  }

  public CompositeReader getCompositeScript(ResourceId resourceId, Locale locale) throws Exception { // NOSONAR
    if (ResourceScope.GROUP.equals(resourceId.getScope())) {
      ScriptGroup loadGroup = scriptGraph.getLoadGroup(resourceId.getName());
      if (loadGroup != null) {
        List<ResourceId> dependencies = loadGroup.getDependencies()
                                                 .stream()
                                                 .sorted((d1, d2) -> d1.toString().compareTo(d2.toString()))
                                                 .toList();
        List<Reader> readers = new ArrayList<>(dependencies.size());
        for (ResourceId id : dependencies) {
          Reader rd = getCompositeScript(id, locale);
          if (rd != null) {
            readers.add(new StringReader("\n//Begin " + id + "\n"));
            readers.add(rd);
            readers.add(new StringReader("\n//End " + id + "\n"));
          }
        }
        return new CompositeReader(readers);
      } else {
        return null;
      }
    } else {
      ScriptResource resource = getResource(resourceId);

      if (resource != null) {
        List<Module> modules = resource.getModules()
                                       .stream()
                                       .sorted((o1, o2) -> o1.getPriority() - o2.getPriority())
                                       .toList();

        List<Reader> readers = new ArrayList<>(modules.size() * 2);
        StringBuilder buffer = new StringBuilder();

        boolean isModule = FetchMode.ON_LOAD.equals(resource.getFetchMode());
        if (isModule) {
          JSONArray deps = new JSONArray();
          LinkedList<String> params = new LinkedList<>();
          List<String> argNames = new LinkedList<>();
          List<String> argValues = new LinkedList<>(params);
          for (ResourceId id : resource.getDependencies()) {
            ScriptResource dep = getResource(id);
            if (dep != null) {
              Set<DepInfo> depInfos = resource.getDepInfo(id);
              for (DepInfo info : depInfos) {
                String pluginRS = info.getPluginRS();
                String alias = info.getAlias();
                if (alias == null) {
                  alias = dep.getAlias();
                }

                deps.put(parsePluginRS(dep.getId().toString(), pluginRS));
                params.add(encode(params, alias));
                argNames.add(parsePluginRS(alias, pluginRS));
              }
            } else if (RESERVED_MODULE.contains(id.getName())) {
              String reserved = id.getName();
              deps.put(reserved);
              params.add(reserved);
              argNames.add(reserved);
            }
          }
          argValues.addAll(params);
          int reserveIdx = argValues.indexOf("require");
          if (reserveIdx != -1) {
            argValues.set(reserveIdx, "eXo.require");
          }

          //
          buffer.append("\ndefine('").append(resourceId).append("', ");
          buffer.append(deps);
          buffer.append(", function(");
          buffer.append(StringUtils.join(params, ","));
          buffer.append(") {\nvar require = eXo.require, requirejs = eXo.require,define = eXo.define;");
          buffer.append("\neXo.define.names=").append(new JSONArray(argNames)).append(";");
          buffer.append("\neXo.define.deps=[").append(StringUtils.join(argValues, ",")).append("]").append(";");
          buffer.append("\nreturn ");
        }

        //
        boolean isMinify = true;
        for (Module js : modules) {
          // we always only have 0 or 1 moudle actually
          if (js instanceof Module.Local localModule) {
            isMinify = localModule.isMinify();
          }

          Reader jScript = getJavascript(js, locale);
          if (jScript != null) {
            readers.add(new StringReader(buffer.toString()));
            buffer.setLength(0);
            readers.add(new NormalizeJSReader(jScript));
          }
        }

        if (isModule) {
          buffer.append("\n});");
        } else {
          buffer.append("\nif (typeof define === 'function' && define.amd && !require.specified('")
                .append(resource.getId())
                .append("')) {");
          buffer.append("define('").append(resource.getId()).append("');}");
        }
        readers.add(new StringReader(buffer.toString()));

        return new CompositeReader.MinifiableReader(readers, isMinify);
      } else {
        return null;
      }
    }
  }

  public String generateUrl(ScriptResource resource) {
    if (resource.getGroup() != null) {
      ResourceId grpId = resource.getGroup().getId();
      String key = grpId.toString();
      return scriptURLs.computeIfAbsent(key, k -> generateUrl(grpId));
    } else {
      String key = resource.getId().toString();
      return scriptURLs.computeIfAbsent(key, k -> generateUrl(resource.getId()));
    }
  }

  public String generateUrl(ResourceId id) {
    @SuppressWarnings("rawtypes")
    BaseScriptResource resource = null;
    if (ResourceScope.GROUP.equals(id.getScope())) {
      resource = scriptGraph.getLoadGroup(id.getName());
    } else {
      resource = getResource(id);
    }

    //
    if (resource != null) {
      if (resource instanceof ScriptResource rs) {
        List<Module> modules = rs.getModules();
        if (CollectionUtils.isNotEmpty(modules)
            && modules.get(0) instanceof Module.Remote remoteModule) {
          return remoteModule.getURI();
        }
      }
      ScriptContent scriptContent = getScriptContent(id.getScope(), id.getName(), !DEVELOPPING);
      String fileName = id.getName();
      if (fileName.indexOf("/") >= 0) {
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
      }
      return resource.getContextPath() + "/js/" + fileName + ".js?" + ScriptKey.HASH_QUERY_PARAM + "=" +
          scriptContent.getHash() + "&" + ScriptKey.SCOPE_QUERY_PARAM + "=" + id.getScope() + "&" +
          ScriptKey.MINIFY_QUERY_PARAM +
          "=" + (!DEVELOPPING);
    }
    return null;
  }

  public Map<ScriptResource, FetchMode> resolveIds(Map<ResourceId, FetchMode> ids) {
    return scriptGraph.resolve(ids);
  }

  public JSONObject getJSConfig() { // NOSONAR
    if (jsConfig == null) {
      JSONObject paths = new JSONObject();
      JSONObject shim = new JSONObject();

      for (ScriptResource resource : getAllResources()) {
        if (!resource.isEmpty() || ResourceScope.SHARED.equals(resource.getId().getScope())) {
          String name = resource.getId().toString();
          List<Module> modules = resource.getModules();

          if (FetchMode.IMMEDIATE.equals(resource.getFetchMode())
              || (CollectionUtils.isNotEmpty(modules)
                  && modules.get(0) instanceof Module.Remote)) {
            JSONArray deps = new JSONArray();
            for (ResourceId id : resource.getDependencies()) {
              deps.put(getResource(id).getId());
            }
            if (deps.length() > 0) {
              shim.put(name, new JSONObject().put("deps", deps));
            }
          }
          paths.put(name, generateUrl(resource));
        }
      }

      JSONObject config = new JSONObject();
      config.put("paths", paths);
      config.put("shim", shim);
      jsConfig = config;
    }
    return jsConfig;
  }

  public ScriptResource getResource(ResourceId resource) {
    return scriptGraph.getResource(resource);
  }

  private Reader getJavascript(Module module, Locale locale) {
    if (module instanceof Module.Local localModule) {
      final WebApp webApp = contexts.get(localModule.getContextPath());
      if (webApp != null) {
        ServletContext sc = webApp.getServletContext();
        return localModule.read(locale, sc, webApp.getClassLoader());
      }
    }
    return null;
  }

  private List<ScriptResource> getAllResources() {
    List<ScriptResource> resources = new LinkedList<>();
    for (ResourceScope scope : ResourceScope.values()) {
      resources.addAll(scriptGraph.getResources(scope));
    }
    return resources;
  }

  private String encode(LinkedList<String> params, String alias) {
    alias = alias.replace("/", "_");
    Matcher validMatcher = JS_ID_PATTERN.matcher(alias);
    if (!validMatcher.matches()) {
      LOG.error("alias {} is not valid, changing to default 'alias' name", alias);
      alias = "alias";
    }

    //
    int idx = -1;
    Iterator<String> iterator = params.descendingIterator();
    while (iterator.hasNext()) { // NOSONAR
      String param = iterator.next();
      Matcher matcher = INDEX_PATTERN.matcher(param);
      if (matcher.matches()) {
        if (param.replace(matcher.group(1), "").equals(alias)) {
          idx = Integer.parseInt(matcher.group(2));
          break;
        }
      } else if (alias.equals(param)) {
        idx = 0;
        break;
      }
    }
    if (idx != -1) {
      StringBuilder tmp = new StringBuilder(alias);
      tmp.append("_").append(idx + 1);
      String a = tmp.toString();
      LOG.warn("alias {} is duplicated, adding index: {}", alias, a);
      return a;
    } else {
      return alias;
    }
  }

  private String parsePluginRS(String name, String pluginRS) {
    StringBuilder depBuild = new StringBuilder(name);
    if (pluginRS != null) {
      depBuild.append("!").append(pluginRS);
    }
    return depBuild.toString();
  }

  private class NormalizeJSReader extends Reader {
    private boolean finished      = false;

    private boolean multiComments = false;

    private boolean singleComment = false;

    private Reader  sub;

    public NormalizeJSReader(Reader sub) {
      this.sub = sub;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException { // NOSONAR
      if (finished) {
        return sub.read(cbuf, off, len);
      } else {
        char[] buffer = new char[len];
        int relLen = sub.read(buffer, 0, len);
        if (relLen == -1) {
          finished = true;
          return -1;
        } else {
          int r = off;

          for (int i = 0; i < relLen; i++) {
            char c = buffer[i];

            char next = 0;
            boolean skip = false;
            boolean overflow = (i + 1 == relLen);
            if (!finished) {
              skip = true;
              if (!singleComment && c == '/' && (next = readNext(buffer, i, overflow)) == '*') {
                multiComments = true;
                i++; // NOSONAR
              } else if (!singleComment && c == '*' && (next = readNext(buffer, i, overflow)) == '/') {
                multiComments = false;
                i++; // NOSONAR
              } else if (!multiComments && c == '/' && next == '/') {
                singleComment = true;
                i++; // NOSONAR
              } else if (c == '\n') {
                singleComment = false;
              } else if (!Character.isWhitespace(c) && !Character.isSpaceChar(c) && !Character.isISOControl(c)) {
                skip = false;
              }

              if (!skip && !multiComments && !singleComment) {
                if (next != 0 && overflow) {
                  sub = new CompositeReader(new StringReader(String.valueOf(c)), sub);
                }
                cbuf[r++] = c;
                finished = true;
              }
            } else {
              cbuf[r++] = c;
            }
          }
          return r - off;
        }
      }
    }

    private char readNext(char[] buffer, int i, boolean overflow) throws IOException {
      char c = 0;
      if (overflow) {
        int tmp = sub.read();
        if (tmp != -1) {
          c = (char) tmp;
        }
      } else {
        c = buffer[i + 1];
      }
      return c;
    }

    @Override
    public void close() throws IOException {
      sub.close();
    }
  }

}
