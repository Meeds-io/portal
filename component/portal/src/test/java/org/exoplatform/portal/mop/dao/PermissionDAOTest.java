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
package org.exoplatform.portal.mop.dao;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE;
import org.exoplatform.portal.mop.dao.PermissionDAO;

public class PermissionDAOTest extends AbstractDAOTest {
  private PermissionDAO     permissionDAO;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.permissionDAO = getContainer().getComponentInstanceOfType(PermissionDAO.class);
  }

  @Override
  protected void tearDown() throws Exception {
    permissionDAO.deleteAll();
    super.tearDown();
    end();
  }

  public void testCreate() {
    PermissionEntity per1 = new PermissionEntity("type1", 1l, "per1", TYPE.ACCESS);
    permissionDAO.create(per1);
    restartTransaction();
    
    PermissionEntity expected = permissionDAO.find(per1.getId());
    assertNotNull(expected);
    assertPermission(expected, per1);
  }
  
  public void testGet() {
    PermissionEntity per1 = new PermissionEntity("type1", 1l, "per1", TYPE.ACCESS);
    permissionDAO.create(per1);
    PermissionEntity per2 = new PermissionEntity("type2", 2L, "per2", TYPE.EDIT);
    permissionDAO.create(per2);
    restartTransaction();
    
    List<PermissionEntity> result1 = permissionDAO.getPermissions("type1", 1L, TYPE.ACCESS);
    assertEquals(1, result1.size());
    assertPermission(per1, result1.get(0));
    
    List<PermissionEntity> result2 = permissionDAO.getPermissions("type2", 2L, TYPE.EDIT);
    assertEquals(1, result2.size());
    assertPermission(per2, result2.get(0));
  }
  
  public void testDelete() {
    PermissionEntity per1 = new PermissionEntity("type1", 1L, "per1", TYPE.ACCESS);
    permissionDAO.create(per1);
    PermissionEntity per2 = new PermissionEntity("type1", 1L, "per2", TYPE.EDIT);
    permissionDAO.create(per2);
    PermissionEntity per3 = new PermissionEntity("type3", 3L, "per3", TYPE.EDIT);
    permissionDAO.create(per3);
    restartTransaction();
    
    int deleted = permissionDAO.deletePermissions("type1", 1L);
    assertEquals(2, deleted);
    assertEquals(1, permissionDAO.findAll().size());    
  }
  
  public void testSave() {
    PermissionEntity per1 = new PermissionEntity("type1", 1L, "per1", TYPE.ACCESS);
    permissionDAO.create(per1);
    PermissionEntity per2 = new PermissionEntity("type1", 1L, "per2", TYPE.ACCESS);
    permissionDAO.create(per2);
    PermissionEntity per3 = new PermissionEntity("type1", 1L, "per3", TYPE.ACCESS);
    permissionDAO.create(per3);
    restartTransaction();
    
    List<PermissionEntity> results = permissionDAO.savePermissions("type1", 1L, TYPE.ACCESS, Arrays.asList("per1", "per4"));
    assertEquals(2, results.size());
    assertTrue(results.remove(per1));
    assertEquals("per4", results.get(0).getPermission());
    
    assertNull(permissionDAO.find(per2.getId()));
    assertNull(permissionDAO.find(per3.getId()));
  }

  private void assertPermission(PermissionEntity expected, PermissionEntity entity) {
    assertEquals(expected.getPermission(), entity.getPermission());
    assertEquals(expected.getReferenceId(), entity.getReferenceId());
    assertEquals(expected.getId(), entity.getId());
    assertEquals(expected.getType(), entity.getType());
  }
}
