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
package org.exoplatform.portal.config;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.*;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A {@link ComponentPlugin} to inject by IOC in order to customize site layout
 * to display in UI switch site properties.
 */
public class DynamicPortalLayoutMatcherPlugin extends BaseComponentPlugin {

  private static final Log           LOG =
                                         ExoLogger.getLogger(DynamicPortalLayoutMatcherPlugin.class);

  /**
   * Whether the plugin is enabled or not
   */
  private boolean                    enabled;

  /**
   * Whether the plugin is initialized or not
   */
  private boolean                    initialized;

  /**
   * Whether to use last displayed site layout of type PORTAL
   */
  private boolean                    useCurrentPortalLayout;

  /**
   * If not null, the site layout to display will be retrieved from this file
   * path
   */
  private String                     layoutTemplatePath;

  /**
   * Retrieved site layout to display from layoutTemplatePath parameter
   */
  private Container                  layoutTemplate;

  /**
   * Matcher to use in order to check if current plugin site layout should be
   * used
   */
  private DynamicPortalLayoutMatcher dynamicLayoutMatcher;

  public DynamicPortalLayoutMatcherPlugin(InitParams params) {
    if (params != null) {
      ValueParam enabledParam = params.getValueParam("enabled");
      this.enabled = enabledParam == null || enabledParam.getValue() == null
          || Boolean.parseBoolean(enabledParam.getValue().toLowerCase());

      ValueParam useCurrentPortalLayoutParam = params.getValueParam("useCurrentPortalLayout");
      this.useCurrentPortalLayout = useCurrentPortalLayoutParam == null || useCurrentPortalLayoutParam.getValue() == null
          || Boolean.parseBoolean(useCurrentPortalLayoutParam.getValue().toLowerCase());

      ValueParam layoutTemplatePathParam = params.getValueParam("layoutTemplatePath");
      this.layoutTemplatePath = layoutTemplatePathParam == null ? null : layoutTemplatePathParam.getValue();

      ObjectParameter matcherParam = params.getObjectParam("matcher");
      if (matcherParam == null) {
        this.enabled = false;
        LOG.warn("No matcher found for plugin {}, it will be disabled.", getName());
      } else {
        this.dynamicLayoutMatcher = (DynamicPortalLayoutMatcher) matcherParam.getObject();
      }
    }
  }

  /**
   * Return {@link PortalConfig} of siteKey parameter. The portal layout of
   * corresponding sitePortalConfig parameter will be changed switch configured
   * matcher plugins.
   * 
   * @param siteKey siteKey of currently displaying site
   * @param sitePortalConfig site PortalConfig of currently displaying site
   * @param currentSitePortalConfig used site PortalConfig of TYPE 'PORTAL' to
   *          use in layout computing
   * @return {@link PortalConfig} with modified portalLayout switch dynamic
   *         matchers. If not modified, return null.
   */
  public PortalConfig getPortalConfigWithDynamicLayout(SiteKey siteKey,
                                                       PortalConfig sitePortalConfig,
                                                       PortalConfig currentSitePortalConfig) {
    if (!isEnabled()) {
      return null;
    }

    if (siteKey == null) {
      throw new IllegalArgumentException("Site key is mandatory");
    }

    if (currentSitePortalConfig == null) {
      LOG.warn("Current site Portalconfig is null, matcher will not be executed");
      return null;
    }

    String lastPortalSiteName = currentSitePortalConfig.getName();
    if (getDynamicLayoutMatcher().matches(siteKey, lastPortalSiteName)) {
      sitePortalConfig = sitePortalConfig.clone();
      if (isUseCurrentPortalLayout()) {
        Container portalLayout = currentSitePortalConfig.getPortalLayout();
        if (portalLayout == null) {
          LOG.warn("Last displayed PORTAL site with name '{}' have a NULL layout. An empty layout will be used instead.",
                   lastPortalSiteName);
          sitePortalConfig.useMetaPortalLayout();
        } else {
          Container portalContainer = portalLayout.clone();
          try {
            portalContainer.resetStorage();
          } catch (ObjectNotFoundException e) {
            throw new IllegalStateException("Error while resetting portal layout container", e);
          }
          sitePortalConfig.setPortalLayout(portalContainer);
        }
      } else if (getLayoutTemplate() != null) {
        sitePortalConfig.setPortalLayout(getLayoutTemplate().clone());
      }
      return sitePortalConfig;
    }

    return null;
  }

  /**
   * Retrieves layout to use for displaying site layout from configured file
   * path using configured parameter layoutTemplatePath. If no file path is
   * configured, no specific layout will be used. If the file wasn't found or an
   * error occurs while parsing or retrieving file content, the plugin will be
   * disabled to avoid incoherent behavior.
   * 
   * @param configurationManager used to retrieve files with Kernel path pattern
   *          (war:/..., jar:/...)
   */
  public void init(ConfigurationManager configurationManager) {
    try {
      String layoutTemplateFilePath = getLayoutTemplatePath();
      if (StringUtils.isBlank(layoutTemplateFilePath)) {
        return;
      }

      String dynamicLayoutPluginName = getName();
      try (InputStream inputStream = configurationManager.getInputStream(layoutTemplateFilePath)) {
        if (inputStream == null) {
          LOG.warn("Can't find portal layout using path '{}'. Matcher '{}' will be ignored.",
                   layoutTemplateFilePath,
                   dynamicLayoutPluginName);
          setEnabled(false);
        } else {
          UnmarshalledObject<PortalConfig> obj = ModelUnmarshaller.unmarshall(PortalConfig.class, inputStream);
          if (obj == null || obj.getObject() == null) {
            LOG.warn("Can't find portal layout content in file '{}'. Matcher '{}' will be ignored.",
                     layoutTemplateFilePath,
                     dynamicLayoutPluginName);
            setEnabled(false);
          } else {
            PortalConfig portalConfig = obj.getObject();
            setLayoutTemplate(portalConfig.getPortalLayout());
          }
        }
      } catch (Exception e) {
        LOG.warn("An error occurred while parsing portal layout from '{}'. Matcher '{}' will be ignored.",
                 layoutTemplateFilePath,
                 dynamicLayoutPluginName,
                 e);
        setEnabled(false);
      }
    } finally {
      this.initialized = true;
    }
  }

  /**
   * @return true if init method has been called at least once, else false
   */
  public boolean isInitialized() {
    return initialized;
  }

  /**
   * @return true if plugin is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * enables/disables plugin
   * 
   * @param enabled true to enabled, else false
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * @return true if last displayed site of type PORTAL should be used to
   *         retrieve its layout
   */
  public boolean isUseCurrentPortalLayout() {
    return useCurrentPortalLayout;
  }

  /**
   * @param useCurrentPortalLayout true to enabled using last displayed site
   *          layout of type PORTAL
   */
  public void setUseCurrentPortalLayout(boolean useCurrentPortalLayout) {
    this.useCurrentPortalLayout = useCurrentPortalLayout;
  }

  /**
   * @return template
   */
  public String getLayoutTemplatePath() {
    return layoutTemplatePath;
  }

  public void setLayoutTemplate(Container layoutTemplatePortalConfig) {
    this.layoutTemplate = layoutTemplatePortalConfig;
  }

  public DynamicPortalLayoutMatcher getDynamicLayoutMatcher() {
    return dynamicLayoutMatcher;
  }

  public void setDynamicLayoutMatcher(DynamicPortalLayoutMatcher dynamicLayoutMatcher) {
    this.dynamicLayoutMatcher = dynamicLayoutMatcher;
  }

  public Container getLayoutTemplate() {
    return layoutTemplate == null ? null : layoutTemplate.clone();
  }

}
