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
package org.exoplatform.services.resources.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.MapResourceBundle;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.resources.ExoResourceBundle;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.Query;
import org.exoplatform.services.resources.ResourceBundleData;

import lombok.SneakyThrows;

public class SimpleResourceBundleService extends BaseResourceBundleService {

  // This depends on Crowdin Default language and not Platform Default
  // Language, thus it's a constant
  public static final Locale                              DEFAULT_I18N_LOCALE = Locale.ENGLISH;

  private static final String                             CACHE_NAME          = "portal.ResourceBundleData";

  private static final boolean                            DEVELOPPING         = PropertyManager.isDevelopping();

  private final ConcurrentMap<String, ResourceBundleData> bundles             = new ConcurrentHashMap<>();

  private final ConcurrentMap<Integer, File>              files               = new ConcurrentHashMap<>();

  public SimpleResourceBundleService(PortalContainer portalContainer,
                                     InitParams params,
                                     CacheService cService,
                                     LocaleConfigService localeService) {
    portalContainer_ = portalContainer;
    localeService_ = localeService;
    cache_ = cService.getCacheInstance(CACHE_NAME);
    initParams(params);
  }

  public ResourceBundleData getResourceBundleData(String id) {
    return bundles.get(id);
  }

  public ResourceBundleData removeResourceBundleData(String id) {
    if (id == null) {
      return null;
    }
    ResourceBundleData data = bundles.remove(id);
    invalidate(id);
    return data;
  }

  public void saveResourceBundle(ResourceBundleData resourceData) {
    String id = resourceData.getId();
    bundles.put(id, resourceData);
    invalidate(id);
  }

  public PageList<ResourceBundleData> findResourceDescriptions(Query q) {
    final ArrayList<ResourceBundleData> list = new ArrayList<>();
    for (ResourceBundleData data : bundles.values()) {
      boolean matches = true;
      if (q.getName() != null) {
        matches &= q.getName().equals(data.getName());
      }
      if (q.getLanguage() != null) {
        matches &= q.getLanguage().equals(data.getLanguage());
      }
      if (matches) {
        list.add(data);
      }
    }
    Collections.sort(list, new Comparator<>() {
      public int compare(ResourceBundleData o1, ResourceBundleData o2) {
        String l1 = o1.getLanguage();
        String l2 = o2.getLanguage();
        if (l1 == null) {
          return l2 == null ? 0 : 1;
        } else {
          return l1.compareTo(l2);
        }
      }
    });
    return new LazyPageList<>(new ListAccess<>() {
      public ResourceBundleData[] load(int index, int length) throws Exception {
        List<ResourceBundleData> sub = list.subList(index, index + length);
        return sub.toArray(new ResourceBundleData[sub.size()]);
      }

      public int getSize() throws Exception {
        return list.size();
      }
    }, 20);
  }

  @Override
  protected ResourceBundle getResourceBundleFromDb(String id, ResourceBundle parent, Locale locale) throws Exception {
    ResourceBundleData data = getResourceBundleData(id);
    if (data == null) {
      return null;
    }
    return new MapResourceBundle(new ExoResourceBundle(data, parent), locale);
  }

  @SneakyThrows
  @Override
  public String getResourceBundleContent(String resourceBundleName, Locale locale) {
    if (DEVELOPPING) {
      return getResourceBundleFileContent(resourceBundleName, locale);
    } else {
      File bundleFile = files.computeIfAbsent((resourceBundleName + locale.toLanguageTag()).hashCode(),
                                              k -> {
                                                String fileContent = getResourceBundleFileContent(resourceBundleName, locale);
                                                if (fileContent != null) {
                                                  return writeResourceBundleFileContent(resourceBundleName, locale, fileContent);
                                                } else {
                                                  return null;
                                                }
                                              });
      if (bundleFile != null) {
        return FileUtils.readFileToString(bundleFile, StandardCharsets.UTF_8);
      } else {
        return null;
      }
    }
  }

  private File writeResourceBundleFileContent(String resourceBundleName, Locale locale, String fileContent) {
    try {
      // Cache result into a temporary file
      File file = File.createTempFile("i18n_cache_", resourceBundleName + locale.toLanguageTag() + ".json");
      FileUtils.write(file, fileContent, StandardCharsets.UTF_8);
      // Ensure to clean cached file on JVM exit
      file.deleteOnExit();
      return file;
    } catch (Exception e) {
      LOG.error("Error while I18N Bundle {} with lang {}", resourceBundleName, locale, e);
      return null;
    }
  }

  private String getResourceBundleFileContent(String resourceBundleName, Locale locale) {
    ResourceBundle resourceBundle = getResourceBundle(resourceBundleName, locale);
    ResourceBundle defaultResourceBundle = getResourceBundle(resourceBundleName, DEFAULT_I18N_LOCALE);
    if (resourceBundle == null) {
      resourceBundle = defaultResourceBundle;
    }
    if (resourceBundle != null) {
      JSONObject resultJSON = new JSONObject();
      Enumeration<String> keys = resourceBundle.getKeys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        resultJSON.put(key, resourceBundle.getString(key));
      }
      keys = defaultResourceBundle.getKeys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        if (resultJSON.isNull(key)) {
          resultJSON.put(key, defaultResourceBundle.getString(key));
        }
      }
      return resultJSON.toString();
    }
    return null;
  }

}
