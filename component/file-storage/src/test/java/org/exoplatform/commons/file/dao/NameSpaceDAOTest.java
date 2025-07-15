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
package org.exoplatform.commons.file.dao;

import org.exoplatform.commons.file.CommonsJPAIntegrationTest;
import org.exoplatform.commons.file.storage.entity.NameSpaceEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public class NameSpaceDAOTest extends CommonsJPAIntegrationTest {
  @Override
  @Before
  public void setUp() {
    super.setUp();
    nameSpaceDAO.deleteAll();
  }

  @Override
  @After
  public void tearDown() {
    nameSpaceDAO.deleteAll();
  }

  @Test
  public void testFindAll() {
    nameSpaceDAO.create(new NameSpaceEntity("FORUM", "Forum files resources"));
    nameSpaceDAO.create(new NameSpaceEntity("SOC", "Social files resources"));

    List<NameSpaceEntity> data = nameSpaceDAO.findAll();

    assertEquals(data.size(), 2);
  }
}
