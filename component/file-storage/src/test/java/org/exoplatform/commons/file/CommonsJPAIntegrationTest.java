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
package org.exoplatform.commons.file;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.file.storage.dao.FileBinaryDAO;
import org.exoplatform.commons.file.storage.dao.FileInfoDAO;
import org.exoplatform.commons.file.storage.dao.NameSpaceDAO;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;


@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.file-storage-local-configuration.xml"),
})
public class CommonsJPAIntegrationTest extends BaseTest {
  protected FileInfoDAO   fileInfoDAO;

  protected NameSpaceDAO  nameSpaceDAO;

  protected FileBinaryDAO fileBinaryDAO;

  public void setUp() {
    super.setUp();

    // make sure data are well initialized for each test

    DataInitializer dataInitializer = getService(DataInitializer.class);
    dataInitializer.initData();

    // Init DAO
    fileInfoDAO = getService(FileInfoDAO.class);
    nameSpaceDAO = getService(NameSpaceDAO.class);
    fileBinaryDAO = getService(FileBinaryDAO.class);

    // Clean Data
    cleanDB();
  }

  public void testInit() {
    assertNotNull(fileInfoDAO);
    assertNotNull(nameSpaceDAO);
    assertNotNull(fileBinaryDAO);
  }

  public void tearDown() {
    // Clean Data
    cleanDB();
    super.tearDown();
  }

  private void cleanDB() {
    fileInfoDAO.deleteAll();
    nameSpaceDAO.deleteAll();
    fileBinaryDAO.deleteAll();
  }
}
