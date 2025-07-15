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

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.dao.SiteDAO;

public class InMemorySiteDAO extends AbstractInMemoryDAO<SiteEntity> implements SiteDAO {

  @Override
  public SiteEntity findByKey(SiteKey siteKey) {
    return entities.values()
                   .stream()
                   .filter(site -> site.getSiteType() == siteKey.getType()
                       && StringUtils.equals(site.getName(), siteKey.getName()))
                   .findFirst()
                   .orElse(null);
  }

  @Override
  public List<SiteEntity> findByType(SiteType siteType) {
    return entities.values().stream().filter(site -> site.getSiteType() == siteType).toList();
  }

  @Override
  public List<SiteKey> findSiteKey(SiteType siteType) {
    return entities.values()
                   .stream()
                   .filter(site -> site.getSiteType() == siteType)
                   .map(site -> new SiteKey(siteType, site.getName()))
                   .toList();
  }

  @Override
  public List<String> findPortalSites(int offset, int limit) {
    return findSiteKey(SiteType.PORTAL).stream().skip(offset).limit(limit(limit)).map(SiteKey::getName).toList();
  }

  @Override
  public List<String> findUserSites(int offset, int limit) {
    return findSiteKey(SiteType.USER).stream().skip(offset).limit(limit(limit)).map(SiteKey::getName).toList();
  }

  @Override
  public List<String> findGroupSites(int offset, int limit) {
    return findSiteKey(SiteType.GROUP).stream()
                                      .filter(key -> !key.getName().startsWith("/spaces/"))
                                      .skip(offset)
                                      .limit(limit(limit))
                                      .map(SiteKey::getName)
                                      .toList();
  }

  @Override
  public List<String> findSpaceSites(int offset, int limit) {
    return findSiteKey(SiteType.GROUP).stream()
                                      .filter(key -> key.getName().startsWith("/spaces/"))
                                      .skip(offset)
                                      .limit(limit(limit))
                                      .map(SiteKey::getName)
                                      .toList();
  }

  @Override
  public List<SiteKey> findSitesKeys(SiteFilter filter) {
    List<SiteEntity> res = entities.values().stream().toList();
    if (filter.isFilterByDisplayed()) {
      res = res.stream().filter(siteEntity -> siteEntity.isDisplayed() == filter.isDisplayed()).toList();
    }
    if (filter.getSiteType() != null) {
      res = res.stream().filter(siteEntity -> siteEntity.getSiteType() == filter.getSiteType()).toList();
    }
    if (StringUtils.isNotBlank(filter.getExcludedSiteName())) {
      res = res.stream().filter(siteEntity -> !siteEntity.getName().equals(filter.getExcludedSiteName())).toList();
    }
    if (filter.isSortByDisplayOrder()) {
      res = res.stream().sorted(Comparator.comparingInt(SiteEntity::getDisplayOrder)).toList();
    }
    return res.stream().map(site -> new SiteKey(site.getSiteType(), site.getName())).toList();
  }

  private int limit(int limit) {
    return limit > 0 ? limit : Integer.MAX_VALUE;
  }

}
