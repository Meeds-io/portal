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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.portal.jdbc.entity.NavigationEntity;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.dao.NavigationDAO;

public class InMemoryNavigationDAO extends AbstractInMemoryDAO<NavigationEntity> implements NavigationDAO {

  @Override
  public NavigationEntity findByOwner(SiteType type, String name) {
    return entities.values()
                   .stream()
                   .filter(navigation -> navigation.getOwnerType() == type
                       && StringUtils.equals(name, navigation.getOwnerId()))
                   .findFirst()
                   .orElse(null);
  }

  @Override
  public NavigationEntity findByRootNode(Long nodeId) {
    return entities.values()
                   .stream()
                   .filter(navigation -> navigation.getRootNode() != null
                       && navigation.getRootNode().getId().longValue() == nodeId.longValue())
                   .findFirst()
                   .orElse(null);
  }

  @Override
  public void deleteByOwner(SiteType siteType, String name) {
    NavigationEntity entity = findByOwner(siteType, name);
    delete(entity);
  }

}
