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
package org.exoplatform.commons;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.naming.BindReferencePlugin;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.naming.SimpleContextFactory;

/**
 * This code should be moved in the core, for now it is here as it is needed here. It extends the
 * {@link org.exoplatform.services.naming.InitialContextInitializer} to override the
 * {@link #addPlugin(org.exoplatform.container.component.ComponentPlugin)} method and perform no binding if there is an existing
 * binding before.
 *
 */
public class InitialContextInitializer2 extends InitialContextInitializer implements Startable {

    public InitialContextInitializer2(InitParams params) throws Exception { // NOSONAR
        super(params);
        System.setProperty("java.naming.factory.initial", SimpleContextFactory.class.getName());
    }

    @Override
    public void addPlugin(ComponentPlugin plugin) {
        if (plugin instanceof BindReferencePlugin) {
            BindReferencePlugin brplugin = (BindReferencePlugin) plugin;
            InitialContext initialContext = getInitialContext();
            try {
                initialContext.lookup(brplugin.getBindName());
                // If we reach this step it means that something is already bound
            } catch (NamingException e) {
                super.addPlugin(plugin);
            }
        }
    }

}
