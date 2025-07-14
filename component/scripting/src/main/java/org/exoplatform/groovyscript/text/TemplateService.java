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
package org.exoplatform.groovyscript.text;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.exoplatform.commons.cache.future.FutureCache;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.groovyscript.GroovyTemplate;
import org.exoplatform.groovyscript.GroovyTemplateEngine;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.resolver.ResourceKey;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import org.apache.commons.lang3.StringUtils;
import org.gatein.common.io.IOTools;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.picocontainer.Startable;

import groovy.lang.Writable;
import groovy.text.Template;

/**
 * Created by The eXo Platform SAS Dec 26, 2005
 */
@Managed
@NameTemplate({ @Property(key = "view", value = "portal"), @Property(key = "service", value = "management"),
        @Property(key = "type", value = "template") })
@ManagedDescription("Template management service")
@RESTEndpoint(path = "templateservice")
public class TemplateService implements Startable {
  
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private final static String CACHE_NAME = "portal.TemplateService";

    private GroovyTemplateEngine engine_;

    private ExoCache<ResourceKey, GroovyTemplate> templatesCache_;

    private TemplateStatisticService statisticService;

    private boolean cacheTemplate_ = true;

    private boolean collectTemplateStatistics_ = true;

    private Vector<TemplateStatisticTime> statisticTimes = new Vector<>();

    private Map<String, Set<String>> templateExtensions = new HashMap<>();

    private final Loader<ResourceKey, GroovyTemplate, ResourceResolver> loader = new Loader<ResourceKey, GroovyTemplate, ResourceResolver>() {
        public GroovyTemplate retrieve(ResourceResolver context, ResourceKey key) throws Exception {
            byte[] bytes;
            InputStream is = context.getInputStream(key.getURL());
            try {
                bytes = IOUtil.getStreamContentAsBytes(is);
                is.close();
            } finally {
                IOTools.safeClose(is);
            }

            // The template class name
            int pos = key.getURL().lastIndexOf('/');
            if (pos == -1) {
                pos = 0;
            }
            String name = key.getURL().substring(pos);

            // Julien: it's a bit dangerious here, with respect to the file encoding...
            String text = new String(bytes);

            // Finally do the expensive template creation
            return engine_.createTemplate(key.getURL(), name, text);
        }
    };

    private FutureCache<ResourceKey, GroovyTemplate, ResourceResolver> futureCache;

    /** . */
    private final Log                                                 log                        =
                                                                          ExoLogger.getLogger(TemplateService.class);

    public TemplateService(TemplateStatisticService statisticService, CacheService cservice, InitParams initParams) throws Exception {
        this.engine_ = new GroovyTemplateEngine();
        this.statisticService = statisticService;
        this.templatesCache_ = cservice.getCacheInstance(CACHE_NAME);
        this.futureCache = new FutureExoCache<ResourceKey, GroovyTemplate, ResourceResolver>(loader, templatesCache_);

        if(initParams != null) {
          ValueParam valueParam = initParams.getValueParam("templates.collect.statistics");
          if(valueParam != null && StringUtils.isNotBlank(valueParam.getValue())) {
            collectTemplateStatistics_ = Boolean.valueOf(valueParam.getValue());
          }
        }
    }

    public void merge(String name, BindingContext context) throws Exception {
        long startTime = System.currentTimeMillis();

        GroovyTemplate template = getTemplate(name, context.getResourceResolver());
        context.put("_ctx", context);
        context.setGroovyTemplateService(this);
        template.render(context.getWriter(), context, (Locale) context.get("locale"));
        long endTime = System.currentTimeMillis();

        if(collectTemplateStatistics_ ) {
          final ResourceResolver resourceResolver = context.getResourceResolver();
          final Long time = endTime - startTime;
          final TemplateStatistic templateStatistic = statisticService.getTemplateStatistic(name);
          templateStatistic.setResolver(resourceResolver);
          CompletableFuture.runAsync(() -> {
            statisticTimes.add(new TemplateStatisticTime(templateStatistic, time));
          });
        }
    }

    @Deprecated
    public void merge(Template template, BindingContext context) throws Exception {
        context.put("_ctx", context);
        context.setGroovyTemplateService(this);
        Writable writable = template.make(context);
        writable.writeTo(context.getWriter());
    }

    public void include(String name, BindingContext context) throws Exception {
        if (context == null)
            throw new Exception("Binding cannot be null");
        include(name, context, context.getResourceResolver());
    }

    public void include(String name, BindingContext context, ResourceResolver resourceResolver) throws Exception {
        if (context == null)
            throw new IllegalArgumentException("Binding cannot be null");
        if (resourceResolver == null)
            throw new IllegalArgumentException("Resource resolver cannot be null");
        context.put("_ctx", context);
        GroovyTemplate template = getTemplate(name, resourceResolver);
        template.render(context.getWriter(), context, (Locale) context.get("locale"));
    }

    public final GroovyTemplate getTemplate(String name, ResourceResolver resolver) throws Exception {
        return getTemplate(name, resolver, cacheTemplate_);
    }

    public final GroovyTemplate getTemplate(String url, ResourceResolver resolver, boolean cacheable) throws Exception {
        GroovyTemplate template;
        ResourceKey resourceId = resolver.createResourceKey(url);
        if (cacheable) {
            template = futureCache.get(resolver, resourceId);
        } else {
            template = loader.retrieve(resolver, resourceId);
        }

        //
        return template;
    }

    public final void invalidateTemplate(String name, ResourceResolver resolver) {
        ResourceKey resourceKey = resolver.createResourceKey(name);
        getTemplatesCache().remove(resourceKey);
    }

    public ExoCache<ResourceKey, GroovyTemplate> getTemplatesCache() {
        return templatesCache_;
    }

    /*
     * Clear the templates cache
     */
    @Managed
    @ManagedDescription("Enable collecting templates statistics")
    public void enableStatistics(boolean enable) {
      collectTemplateStatistics_ = enable;
    }

    /*
     * Clear the templates cache
     */
    @Managed
    @ManagedDescription("Clear the template cache")
    public void reloadTemplates() {
        try {
            templatesCache_.clearCache();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /*
     * Clear the template cache by name
     */
    @Managed
    @ManagedDescription("Clear the template cache for a specified template identifier")
    @Impact(ImpactType.IDEMPOTENT_WRITE)
    public void reloadTemplate(@ManagedDescription("The template id") @ManagedName("templateId") String name) {
        TemplateStatistic app = statisticService.findTemplateStatistic(name);
        if (app != null) {
            ResourceResolver resolver = app.getResolver();
            templatesCache_.remove(resolver.createResourceKey(name));
        }
    }

    @Managed
    @ManagedDescription("List the identifiers of the cached templates")
    @Impact(ImpactType.READ)
    public String[] listCachedTemplates() {
        try {
            ArrayList<String> list = new ArrayList<String>();
            for (GroovyTemplate template : templatesCache_.getCachedObjects()) {
                list.add(template.getId());
            }
            return list.toArray(new String[list.size()]);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void start() {
      executorService.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
          List<TemplateStatisticTime> templateStatisticTimes = new ArrayList<>(statisticTimes);
          statisticTimes.clear();
          for (TemplateStatisticTime templateStatisticTime : templateStatisticTimes) {
            templateStatisticTime.computeStatistic();
          }
        }
      }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
      executorService.shutdown();
    }

    public static class TemplateStatisticTime {
      private TemplateStatistic templateStatistic;
      private long time;

      public TemplateStatisticTime(TemplateStatistic templateStatistic, long time) {
        this.templateStatistic = templateStatistic;
        this.time = time;
      }

      public void computeStatistic() {
        templateStatistic.setTime(time);
      }
    }

    /**
     * Add template extension by compoennt plugin injected from kernel
     * configuration
     * 
     * @param templateExtensionPlugin list of gtmpl templates added in component
     *          plugin
     */
    public void addTemplateExtension(TemplateExtensionPlugin templateExtensionPlugin) {
      String parentTemplateName = templateExtensionPlugin.getName();
      if (StringUtils.isBlank(parentTemplateName)) {
        log.warn("Can't register empty plugin name");
        return;
      }
      if (!templateExtensions.containsKey(parentTemplateName)) {
        templateExtensions.put(parentTemplateName, new HashSet<>());
      }
      templateExtensions.get(parentTemplateName).addAll(templateExtensionPlugin.getTemplates());
    }

    public Set<String> getTemplateExtensions(String templateName) {
      Set<String> list = templateExtensions.get(templateName);
      if (list == null) {
        return Collections.emptySet();
      } else {
        return Collections.unmodifiableSet(list);
      }
    }
}
