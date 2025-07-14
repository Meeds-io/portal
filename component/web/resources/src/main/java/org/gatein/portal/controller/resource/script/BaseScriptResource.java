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
package org.gatein.portal.controller.resource.script;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;

import lombok.Getter;
import lombok.Setter;

import org.gatein.portal.controller.resource.Resource;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceRequestHandler;

public class BaseScriptResource<R extends Resource<R>> extends Resource<R> {

    /** . */
    ScriptGraph graph;

    /** . */
    private final Map<QualifiedName, String> parameters;

    /** . */
    private final Map<Locale, Map<QualifiedName, String>> parametersMap;

    /** . */
    private final Map<QualifiedName, String> minParameters;

    /** . */
    private final Map<Locale, Map<QualifiedName, String>> minParametersMap;

    /** . */
    @Getter
    @Setter
    protected String contextPath;

    BaseScriptResource(ScriptGraph graph, ResourceId id) {
        super(id);

        //
        Map<QualifiedName, String> parameters = new HashMap<>();
        parameters.put(WebAppController.HANDLER_PARAM, "script");
        parameters.put(ResourceRequestHandler.RESOURCE_QN, id.getName());
        parameters.put(ResourceRequestHandler.SCOPE_QN, id.getScope().name());
        parameters.put(ResourceRequestHandler.COMPRESS_QN, "");
        parameters.put(ResourceRequestHandler.VERSION_QN, ResourceRequestHandler.VERSION);
        parameters.put(ResourceRequestHandler.LANG_QN, "");

        //
        Map<QualifiedName, String> minifiedParameters = new HashMap<>(parameters);
        minifiedParameters.put(ResourceRequestHandler.COMPRESS_QN, "min");

        //
        this.parameters = parameters;
        this.minParameters = minifiedParameters;
        this.graph = graph;
        this.parametersMap = new ConcurrentHashMap<>();
        this.minParametersMap = new ConcurrentHashMap<>();
    }

    public Map<QualifiedName, String> getParameters(boolean minified, Locale locale) {
        Map<Locale, Map<QualifiedName, String>> map = minified ? minParametersMap : parametersMap;
        for (Locale current = locale; current != null; current = I18N.getParent(current)) {
            Map<QualifiedName, String> ret = map.get(locale);
            if (ret != null) {
                return ret;
            }
        }
        return minified ? minParameters : parameters;
    }

    public void addSupportedLocale(Locale locale) {
        if (!parametersMap.containsKey(locale)) {
            Map<QualifiedName, String> localizedParameters = new HashMap<QualifiedName, String>(parameters);
            localizedParameters.put(ResourceRequestHandler.LANG_QN, I18N.toTagIdentifier(locale));
            parametersMap.put(locale, localizedParameters);
            Map<QualifiedName, String> localizedMinParameters = new HashMap<QualifiedName, String>(minParameters);
            localizedMinParameters.put(ResourceRequestHandler.LANG_QN, I18N.toTagIdentifier(locale));
            minParametersMap.put(locale, localizedMinParameters);
        }
    }
}
