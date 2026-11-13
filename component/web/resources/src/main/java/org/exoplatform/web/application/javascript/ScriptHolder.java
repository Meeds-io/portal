/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.gatein.portal.controller.resource.ScriptContent;
import org.gatein.portal.controller.resource.ScriptKey;
import org.gatein.portal.controller.resource.ScriptLoader;
import org.json.JSONObject;

import org.exoplatform.commons.cache.future.FutureMap;

/**
 * One generation of the served scripts: their contents, their URLs and the
 * loader configuration naming them, built together and replaced together.
 */
final class ScriptHolder {

  final FutureMap<ScriptKey, ScriptContent, Object> scriptCache;

  final Map<String, String>                         scriptURLs = new ConcurrentHashMap<>();

  volatile JSONObject                               jsConfig;

  ScriptHolder(ScriptLoader scriptLoader) {
    this.scriptCache = new FutureMap<>(scriptLoader);
  }

}
