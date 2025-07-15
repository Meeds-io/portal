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
package org.exoplatform.portal.pc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import jakarta.servlet.ServletContext;

import org.gatein.pc.portlet.container.ContainerPortletInvoker;
import org.gatein.pc.portlet.impl.deployment.DeploymentException;
import org.gatein.pc.portlet.impl.deployment.PortletApplicationDeployer;
import org.gatein.pc.portlet.impl.deployment.PortletApplicationDeployment;
import org.gatein.pc.portlet.impl.metadata.PortletApplication10MetaData;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Extends the {@link org.gatein.pc.portlet.impl.deployment.PortletApplicationDeployer} to inject configuration metadata from
 * global portlet.xml and to configure the resource bundle factory of deployed portlet applications. The resource bundle factory
 * used is {@link org.exoplatform.portal.pc.ExoResourceBundleFactory}.
 *
 */
public class ExoPortletApplicationDeployer extends PortletApplicationDeployer {

    /** . */
    private final Log                        log = ExoLogger.getLogger(ExoPortletApplicationDeployer.class);

    public ExoPortletApplicationDeployer() {
        this(null);
    }

    public ExoPortletApplicationDeployer(ContainerPortletInvoker containerPortletInvoker) {
        super(containerPortletInvoker);
    }

    @Override
    protected PortletApplication10MetaData buildPortletApplicationMetaData(ServletContext webApp) throws DeploymentException {
        PortletApplication10MetaData md = super.buildPortletApplicationMetaData(webApp);
        if (md != null) {
            md.setResourceBundleFactoryName(ExoResourceBundleFactory.class.getName());

            String globalPortletLocation = PropertyManager.getProperty("gatein.portlet.config");
            if (globalPortletLocation != null) {
                try {
                    GlobalPortletMetaData globalPortletMetaData = loadGlobalMetadata(globalPortletLocation);
                    if (globalPortletMetaData != null) {
                      globalPortletMetaData.mergeTo(md);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Complete merging global portlet metadata to portlet application "
                                + webApp.getServletContextName());
                    }
                } catch (Exception ex) {
                    if (log.isErrorEnabled()) {
                        log.error(
                                "Error during merge global portlet metadata to portlet application "
                                        + webApp.getServletContextName(), ex);
                    }
                }
            } else {
                log.warn("The global portlet metadata is not configured");
            }

        }
        return md;
    }

    @Override
    protected PortletApplicationDeployment createPortletApplicationDeployment(ServletContext webApp, PortletApplication10MetaData metaData) {
        return super.createPortletApplicationDeployment(webApp, metaData);
    }

    /**
     * This method is invoked for each portlet application deployment. That is necessary for the moment to ensure independence
     * between portlet applications
     *
     * @return the global meta data
     * @throws Exception any exception
     */
    private GlobalPortletMetaData loadGlobalMetadata(String globalPortletLocation) throws Exception {
        File globalPortletFile = new File(globalPortletLocation);
        if (globalPortletFile.exists()) {
          try (InputStream in = new FileInputStream(globalPortletFile)) {
            return GlobalPortletMetaData.unmarshalling(in);
          }
        } else {
          return null;
        }
    }
}
