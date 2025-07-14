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
package org.exoplatform.webui.config;

import java.io.InputStream;
import java.security.PrivilegedAction;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.xml.object.XMLObject;
import org.gatein.common.classloader.DelegatingClassLoader;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class Param {

    /** . */
    private static final Log log = ExoLogger.getLogger(Param.class);

    private String name;

    private String value;

    private transient Object object;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Object getMapXMLObject(WebuiRequestContext context) throws Exception {
        if (object == null) {
            synchronized (this) {
                if (object == null) {
                    ResourceResolver resolver = context.getResourceResolver(value);
                    InputStream is = resolver.getInputStream(value);
                    object = XMLObject.getObject(is);
                    is.close();
                }
            }
        }
        return object;
    }

    public Object getMapGroovyObject(WebuiRequestContext context) throws Exception {
        if (object == null) {
            synchronized (this) {
                if (object == null) {
                    ResourceResolver resolver = context.getResourceResolver(value);
                    InputStream is = resolver.getInputStream(value);
                    Binding binding = new Binding();
                    GroovyShell shell = new GroovyShell(prepareClassLoader(), binding);
                    object = shell.evaluate(IOUtil.getStreamContentAsString(is));
                }
            }
        }
        return object;
    }

    public Object getFreshObject(WebuiRequestContext context) throws Exception {
        try {
            ResourceResolver resolver = context.getResourceResolver(value);
            InputStream is = resolver.getInputStream(value);
            Binding binding = new Binding();
            GroovyShell shell = new GroovyShell(prepareClassLoader(), binding);
            object = shell.evaluate(IOUtil.getStreamContentAsString(is));
            return object;
        } catch (Exception e) {
            log.error("A  problem in the groovy script : " + value, e);
            throw e;
        }
    }

    private ClassLoader prepareClassLoader() {
        final ClassLoader tccl =  Thread.currentThread().getContextClassLoader();

        return new DelegatingClassLoader(tccl, GroovyShell.class.getClassLoader());
    }
}
