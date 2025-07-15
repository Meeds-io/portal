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
package org.exoplatform.portal.mop.dao.mock;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE;
import org.exoplatform.portal.mop.dao.PermissionDAO;

public class InMemoryPermissionDAO extends AbstractInMemoryDAO<PermissionEntity> implements PermissionDAO {

  @Override
  public List<PermissionEntity> getPermissions(String refType, Long refId, TYPE type) {
    return entities.values()
                   .stream()
                   .filter(permission -> StringUtils.equals(refType, permission.getReferenceType())
                       && refId.longValue() == permission.getReferenceId().longValue()
                       && type == permission.getType())
                   .toList();
  }

  @Override
  public int deletePermissions(String refType, Long refId) {
    List<PermissionEntity> permissionList = entities.values()
                                                    .stream()
                                                    .filter(permission -> StringUtils.equals(refType,
                                                                                             permission.getReferenceType())
                                                        && refId.longValue() == permission.getReferenceId().longValue())
                                                    .toList();
    deleteAll(permissionList);
    return permissionList.size();
  }

  @Override
  public List<PermissionEntity> savePermissions(String refType, Long refId, TYPE type, List<String> permissions) {
    if (refId == null || type == null || refType == null) {
      throw new IllegalArgumentException("refType, refId , type must not be null");
    }

    List<PermissionEntity> oldPers = new ArrayList<>(getPermissions(refType, refId, type));
    List<PermissionEntity> results = new LinkedList<>();
    if (permissions != null) {
      for (String permission : permissions) {
        PermissionEntity entity = new PermissionEntity(refType, refId, permission, type);

        int idx = oldPers.indexOf(entity);
        if (idx != -1) {
          results.add(oldPers.get(idx));
          oldPers.remove(entity);
        } else {
          create(entity);
          results.add(entity);
        }
      }
    }
    deleteAll(oldPers);
    return results;
  }

}
