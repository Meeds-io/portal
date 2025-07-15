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
package io.meeds.portal.permlink.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostCreateTask;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;

import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.plugin.PermanentLinkPlugin;

import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.SneakyThrows;

public class PermanentLinkServiceImpl implements PermanentLinkService, Startable {

  private static final Log                 LOG                      =
                                               ExoLogger.getLogger(PermanentLinkServiceImpl.class);

  public static final Context              PERMANEN_LINK_CONTEXT    = Context.GLOBAL.id("PermanentLink");

  public static final Scope                PERMANENT_LINK_SCOPE     = Scope.GLOBAL.id("PermanentLinkIds");

  public static final Scope                PERMANENT_LINK_IDS_SCOPE = Scope.APPLICATION.id("PermanentLinkIds");

  public static final String               PERMANENT_LINK_SALT_NAME = "PermanentLinkSalt";

  public static final Pattern              PERMANENT_LINK_PATTERN   =
                                                                  Pattern.compile("(?<objectType>[^/]*)/(?<objectId>[^/?]*)\\?(?<params>.*)");

  public static final String               PERMANENT_LINK_FOMRAT    = "%s/%s?%s";

  private static final MessageDigest       DIGEST                   = getDigest();

  private PortalContainer                  container;

  private SettingService                   settingService;

  /**
   * {@link Map} of Object Type with associated plugin
   */
  private Map<String, PermanentLinkPlugin> plugins                  = new ConcurrentHashMap<>();

  @Getter
  private String                           salt;

  public PermanentLinkServiceImpl(PortalContainer container, SettingService settingService) {
    this.container = container;
    this.settingService = settingService;
  }

  @Override
  public void start() {
    PortalContainer.addInitTask(container.getPortalContext(), new PortalContainerPostCreateTask() {
      @Override
      public void execute(ServletContext context, PortalContainer portalContainer) {
        initPlugins();
      }
    });

    initSalt();
  }

  @Override
  public String getPermanentLink(PermanentLinkObject object) {
    if (plugins.containsKey(object.getObjectType())) {
      Map<String, String> parameters = object.getParameters();
      List<String> queryParams = parameters == null ? Collections.emptyList() :
                                                    parameters.entrySet()
                                                              .stream()
                                                              .filter(e -> e.getValue() != null)
                                                              .map(e -> e.getKey() + "=" + encodeUrl(e.getValue().getBytes()))
                                                              .toList();
      String permanentLink = String.format(PERMANENT_LINK_FOMRAT,
                                           object.getObjectType(),
                                           object.getObjectId(),
                                           StringUtils.join(queryParams, "&"));
      String permanentLinkId = getAndSetId(permanentLink);
      return PERMANENT_LINK_URL_PREFIX + permanentLinkId;
    } else {
      return null;
    }
  }

  @Override
  public String getLink(PermanentLinkObject object) throws ObjectNotFoundException {
    PermanentLinkPlugin plugin = getPlugin(object.getObjectType());
    if (plugin == null) {
      throw new ObjectNotFoundException("Plugin not found with object of type : " + object.getObjectType());
    }
    return plugin.getDirectAccessUrl(object);
  }

  @Override
  public String getDirectAccessUrl(String permanentLinkId, Identity identity) throws IllegalAccessException,
                                                                              ObjectNotFoundException {
    String permanentLink = getById(permanentLinkId);
    PermanentLinkObject object = permanentLink == null ? null : parseObject(permanentLink);
    if (object == null) {
      throw new ObjectNotFoundException(String.format("Url '%s' can't be parsed", permanentLink));
    } else {
      PermanentLinkPlugin plugin = getPlugin(object.getObjectType());
      if (plugin == null) {
        throw new ObjectNotFoundException("Plugin not found with object of type : " + object.getObjectType());
      } else {
        if (plugin.canAccess(object, identity)) {
          return plugin.getDirectAccessUrl(object);
        } else {
          throw new IllegalAccessException();
        }
      }
    }
  }

  protected void initPlugins() {
    container.getComponentInstancesOfType(PermanentLinkPlugin.class)
             .stream()
             .forEach(p -> plugins.put(p.getObjectType(), p));
  }

  @Override
  public void addPlugin(PermanentLinkPlugin plugin) {
    plugins.put(plugin.getObjectType(), plugin);
  }

  protected void initSalt() {
    SettingValue<?> saltSettingValue = settingService.get(PERMANEN_LINK_CONTEXT, PERMANENT_LINK_SCOPE, PERMANENT_LINK_SALT_NAME);
    if (saltSettingValue == null || saltSettingValue.getValue() == null) {
      salt = UUID.randomUUID().toString();
      settingService.set(PERMANEN_LINK_CONTEXT,
                         PERMANENT_LINK_IDS_SCOPE,
                         PERMANENT_LINK_SALT_NAME,
                         SettingValue.create(salt));
    } else {
      salt = saltSettingValue.getValue().toString();
    }
  }

  private PermanentLinkPlugin getPlugin(String objectType) {
    if (plugins == null) {
      initPlugins();
    }
    return plugins.get(objectType);
  }

  private PermanentLinkObject parseObject(String permanentLink) {
    Matcher matcher = PERMANENT_LINK_PATTERN.matcher(permanentLink);
    if (matcher.find()) {
      String objectType = matcher.group("objectType");
      String objectId = matcher.group("objectId");
      PermanentLinkObject object = new PermanentLinkObject(objectType, objectId);
      String params = matcher.group("params");
      if (StringUtils.isNotBlank(params)) {
        object.setParameters(new HashMap<>());
        String[] paramsArray = params.split("&");
        Stream.of(paramsArray).forEach(param -> {
          if (StringUtils.contains(param, "=")) {
            String[] paramParts = param.split("=");
            object.getParameters().put(paramParts[0], new String(decode(paramParts[1])));
          }
        });
      }
      return object;
    } else {
      return null;
    }
  }

  @SneakyThrows
  private byte[] decode(String base64Encoded) {
    return Base64.decodeBase64(base64Encoded);
  }

  @SneakyThrows
  private String encodeUrl(byte[] bytes) {
    return Base64.encodeBase64URLSafeString(bytes);
  }

  private String getById(String permanentLinkId) {
    SettingValue<?> settingValue = settingService.get(PERMANEN_LINK_CONTEXT, PERMANENT_LINK_IDS_SCOPE, permanentLinkId);
    return settingValue == null || settingValue.getValue() == null ? null : settingValue.getValue().toString();
  }

  private String getAndSetId(String permanentLink) {
    String permanentLinkId = generateHash(permanentLink);
    try {
      settingService.set(PERMANEN_LINK_CONTEXT,
                         PERMANENT_LINK_IDS_SCOPE,
                         permanentLinkId,
                         SettingValue.create(permanentLink));
    } catch (RuntimeException e) {
      if (settingService.get(PERMANEN_LINK_CONTEXT,
                             PERMANENT_LINK_IDS_SCOPE,
                             permanentLinkId)
          == null) {
        throw e;
      } else {
        LOG.debug("Duplicated Permanent Link Id {} set. This may be due to parallel saving", permanentLinkId);
      }
    }
    return permanentLinkId;
  }

  private String generateHash(String permanentLink) {
    final byte[] hashbytes = DIGEST.digest((permanentLink + salt).getBytes(StandardCharsets.UTF_8));
    return encodeUrl(hashbytes);
  }

  @SneakyThrows
  private static MessageDigest getDigest() {
    return MessageDigest.getInstance("SHA3-256");
  }

}
