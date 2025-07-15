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
package org.exoplatform.jpa;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.settings.jpa.SettingContextDAO;
import org.exoplatform.settings.jpa.SettingScopeDAO;
import org.exoplatform.settings.jpa.SettingsDAO;

import io.meeds.kernel.test.KernelExtension;
import io.meeds.spring.AvailableIntegration;
import io.meeds.spring.kernel.KernelCacheConfiguration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({ SpringExtension.class, KernelExtension.class })
@SpringBootApplication(scanBasePackages = {
  CommonsDAOJPAImplTest.MODULE_NAME,
  AvailableIntegration.KERNEL_TEST_MODULE,
  AvailableIntegration.JPA_MODULE,
  AvailableIntegration.LIQUIBASE_MODULE,
})
@EnableJpaRepositories(basePackages = CommonsDAOJPAImplTest.MODULE_NAME)
@ContextConfiguration(classes = { KernelCacheConfiguration.class })
@TestPropertySource(properties = {
  "spring.liquibase.change-log=" + CommonsDAOJPAImplTest.CHANGELOG_PATH,
})
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.settings-configuration-local.xml")
})
public class CommonsDAOJPAImplTest extends BaseTest { // NOSONAR

  protected static final String MODULE_NAME    = "io.meeds.spring.module";

  protected static final String CHANGELOG_PATH = "classpath:db/changelog/test-rdbms.db.changelog.xml";

  protected SettingContextDAO settingContextDAO;

  protected SettingScopeDAO   settingScopeDAO;

  protected SettingsDAO       settingsDAO;

  @Override
  public void setUp() {
    super.setUp();

    // make sure data are well initialized for each test

    // Init DAO
    settingContextDAO = getService(SettingContextDAO.class);
    settingScopeDAO = getService(SettingScopeDAO.class);
    settingsDAO = getService(SettingsDAO.class);

    // Clean Data
    cleanDB();
  }

  @Override
  public void tearDown() {
    // Clean Data
    cleanDB();
    super.tearDown();
  }

  @BeforeClass
  @Override
  protected void beforeRunBare() {
    if (System.getProperty("gatein.test.output.path") == null) {
      System.setProperty("gatein.test.output.path", System.getProperty("java.io.tmpdir"));
    }
    super.beforeRunBare();
  }

  @AfterClass
  @Override
  protected void afterRunBare() {
    super.afterRunBare();
  }

  public void testInit() {
    assertNotNull(settingContextDAO);
    assertNotNull(settingScopeDAO);
    assertNotNull(settingsDAO);
  }

  private void cleanDB() {
    settingsDAO.deleteAll();
    settingScopeDAO.deleteAll();
    settingContextDAO.deleteAll();
  }
}
