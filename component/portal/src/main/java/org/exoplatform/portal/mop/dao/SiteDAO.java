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

import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

public interface SiteDAO extends GenericDAO<SiteEntity, Long> {

  SiteEntity findByKey(SiteKey siteKey);

  List<SiteEntity> findByType(SiteType siteType);

  List<SiteKey> findSiteKey(SiteType siteType);

  List<String> findPortalSites(int offset, int limit);

  List<String> findGroupSites(int offset, int limit);

  List<String> findSpaceSites(int offset, int limit);

  List<String> findUserSites(int offset, int limit);

  List<SiteKey> findSitesKeys(SiteFilter siteFilter);
}
