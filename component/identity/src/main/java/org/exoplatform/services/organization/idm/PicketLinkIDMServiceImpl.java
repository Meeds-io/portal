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
package org.exoplatform.services.organization.idm;

import java.io.InputStream;
import java.net.URL;

import javax.naming.InitialContext;

import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.idm.impl.store.attribute.ExtendedAttributeManager;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.IdentitySessionFactory;
import org.picketlink.idm.api.SecureRandomProvider;
import org.picketlink.idm.api.cfg.IdentityConfiguration;
import org.picketlink.idm.common.exception.IdentityConfigurationException;
import org.picketlink.idm.impl.api.session.IdentitySessionImpl;
import org.picketlink.idm.impl.configuration.IdentityConfigurationImpl;
import org.picketlink.idm.impl.configuration.jaxb2.JAXB2IdentityConfiguration;
import org.picketlink.idm.impl.credential.DatabaseReadingSaltEncoder;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;
import org.picocontainer.Startable;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class PicketLinkIDMServiceImpl implements PicketLinkIDMService, Startable {

    private static Log log = ExoLogger.getLogger(PicketLinkIDMServiceImpl.class);

    public static final String PARAM_CONFIG_OPTION = "config";

    public static final String PARAM_JNDI_NAME_OPTION = "jndiName";

    public static final String PARAM_USE_SECURE_RANDOM_SERVICE = "useSecureRandomService";

    public static final int DEFAULT_STALE_CACHE_NODES_LINKS_CLEANER_DELAY = 120000;

    public static final String REALM_NAME_OPTION = "portalRealm";

    public static final String CACHE_CONFIG_API_OPTION = "apiCacheConfig";

    public static final String CACHE_CONFIG_STORE_OPTION = "storeCacheConfig";

    private IdentitySessionFactory      identitySessionFactory;

    private String                      config;

    private String                      realmName                                     = "idm_realm";

    private IdentityConfiguration       identityConfiguration;

    private IdentityConfigurationMetaData configMD;

    private IdmHibernateService         idmHibernateService;

    private ExtendedAttributeManager    extendedAttributeManager;

    public PicketLinkIDMServiceImpl(IdmHibernateService hibernateService,
                                    ConfigurationManager confManager,
                                    InitParams initParams)
        throws Exception {
      ValueParam configValueParam = null;

      ValueParam directoryTypeValueParam = initParams.getValueParam("ldap.type");
      String directoryType = null;
      if (directoryTypeValueParam != null) {
        directoryType = directoryTypeValueParam.getValue();
      }
      if (StringUtils.isNotBlank(directoryType)) {
        configValueParam = initParams.getValueParam(PARAM_CONFIG_OPTION + "." + directoryType);
      }

      if (configValueParam == null) {
        configValueParam = initParams.getValueParam(PARAM_CONFIG_OPTION);
      }

      ValueParam jndiName = initParams.getValueParam(PARAM_JNDI_NAME_OPTION);
      ValueParam realmNameValueParam = initParams.getValueParam(REALM_NAME_OPTION);
      ValueParam apiCacheConfig = initParams.getValueParam(CACHE_CONFIG_API_OPTION);
      ValueParam storeCacheConfig = initParams.getValueParam(CACHE_CONFIG_STORE_OPTION);
      ValueParam useSecureRandomService = initParams.getValueParam(PARAM_USE_SECURE_RANDOM_SERVICE);

      this.idmHibernateService = hibernateService;

      if (configValueParam == null && jndiName == null) {
        throw new IllegalStateException("Either '" + PARAM_CONFIG_OPTION + "' or '" + PARAM_JNDI_NAME_OPTION +
            "' parameter must " + "be specified");
      }
      if (realmNameValueParam != null) {
        this.realmName = realmNameValueParam.getValue();
      }

      if (configValueParam != null) {
        this.config = configValueParam.getValue();
        URL configURL = confManager.getURL(this.config);

        if (configURL == null) {
          throw new IllegalStateException("Cannot fine resource: " + this.config);
        }

        this.configMD = JAXB2IdentityConfiguration.createConfigurationMetaData(confManager.getInputStream(this.config));
        identityConfiguration = new IdentityConfigurationImpl().configure(this.configMD);
        identityConfiguration.getIdentityConfigurationRegistry()
                             .register(hibernateService.getSessionFactory(),
                                       "hibernateSessionFactory");

        if (apiCacheConfig != null) {
          log.warn("The parameter 'apiCacheProvider' has been deprecated. It has been replaced by caches in Organization Service top layer. Thus, the parameter should be removed.");

          InputStream configStream = confManager.getInputStream(apiCacheConfig.getValue());

          if (configStream == null) {
            throw new IllegalArgumentException("Infinispan configuration InputStream is null");
          }
          configStream.close();
        }

        if (storeCacheConfig != null) {
          log.warn("The parameter 'storeCacheProvider' has been deprecated. It has been replaced by caches in Organization Service top layer. Thus, the parameter should be removed.");

          InputStream configStream = confManager.getInputStream(storeCacheConfig.getValue());

          if (configStream == null) {
            throw new IllegalArgumentException("Infinispan configuration InputStream is null");
          }
        }

        if (useSecureRandomService != null && "true".equals(useSecureRandomService.getValue())) {
          SecureRandomProvider secureRandomProvider = ExoContainerContext.getService(SecureRandomProvider.class);
          if (secureRandomProvider != null) {
            identityConfiguration.getIdentityConfigurationRegistry()
                                 .register(secureRandomProvider,
                                           DatabaseReadingSaltEncoder.DEFAULT_SECURE_RANDOM_PROVIDER_REGISTRY_NAME);
          }
        }
      } else {
        identitySessionFactory = (IdentitySessionFactory) new InitialContext().lookup(jndiName.getValue());
      }
    }

    @Override
    public void start() {
      if (identitySessionFactory == null) {
        try {
          identitySessionFactory = identityConfiguration.buildIdentitySessionFactory();
        } catch (IdentityConfigurationException e) {
          throw new IllegalStateException(String.format("Error building configuration: %s", this.config), e);
        }
      }
    }

    @Override
    public IdentitySessionFactory getIdentitySessionFactory() {
        return identitySessionFactory;
    }

    @Override
    public IdentitySession getIdentitySession() throws Exception {
        if(getIdentitySessionFactory() != null) {
            return getIdentitySessionFactory().getCurrentIdentitySession(realmName);
        } else {
            return null;
        }
    }

    @Override
    public IdentitySession getIdentitySession(String realm) throws Exception {
        if (realm == null) {
            throw new IllegalArgumentException("Realm name cannot be null");
        }
        return getIdentitySessionFactory().getCurrentIdentitySession(realm);
    }

    @Override
    public IdentityConfiguration getIdentityConfiguration() {
        return identityConfiguration;
    }

    @Override
    public ExtendedAttributeManager getExtendedAttributeManager() throws Exception {
      if (this.extendedAttributeManager == null) {
          this.extendedAttributeManager = new ExtendedAttributeManager((IdentitySessionImpl) getIdentitySession());
      }
      return this.extendedAttributeManager;
    }

    @Override
    public IdentityConfigurationMetaData getConfigMD() {
        return this.configMD;
    }

    public String getRealmName() {
        return realmName;
    }

    public IdmHibernateService getHibernateService() {
        return idmHibernateService;
    }

    public void setConfigMD(IdentityConfigurationMetaData configMD) {
        this.configMD = configMD;
    }
}
