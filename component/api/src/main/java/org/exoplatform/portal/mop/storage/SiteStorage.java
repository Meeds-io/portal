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
package org.exoplatform.portal.mop.storage;

import java.util.List;

import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;

public interface SiteStorage {

  void create(PortalConfig config);

  void create(PortalData config);

  void save(PortalConfig config);

  void save(PortalData config);

  void remove(PortalConfig config);

  void remove(PortalData config);

  void remove(SiteKey siteKey);

  PortalData getPortalConfig(SiteKey siteKey);

  PortalData getPortalConfig(long siteId);

  PortalData getPortalConfig(PortalKey key);

  PortalConfig getPortalConfig(String ownerType, String portalName);

  /**
   * Retrieves the list of site names of a designated type
   * 
   * @param  siteType {@link SiteType}
   * @param  offset   offset of the query
   * @param  limit    limit to fetch
   * @return          {@link List} of site names
   */
  List<String> getSiteNames(SiteType siteType, int offset, int limit);

  Status getImportStatus();

  void saveImportStatus(Status status);

  Container getSharedLayout(String siteName);

  List<SiteKey> getSitesKeys(SiteFilter siteFilter);
}
