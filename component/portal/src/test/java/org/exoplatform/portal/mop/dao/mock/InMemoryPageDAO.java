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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.dao.PageDAO;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.services.organization.mock.InMemoryListAccess;

public class InMemoryPageDAO extends AbstractInMemoryDAO<PageEntity> implements PageDAO {

  @Override
  public PageEntity findByKey(PageKey pageKey) {
    return entities.values()
                   .stream()
                   .filter(page -> StringUtils.equals(pageKey.getName(), page.getName())
                                   && StringUtils.equals(pageKey.getSite().getName(), page.getOwnerId())
                                   && pageKey.getSite().getType().equals(page.getOwnerType()))
                   .findFirst()
                   .orElse(null);
  }

  @Override
  public InMemoryListAccess<PageKey> findByQuery(SiteType siteType,
                                                 String siteName,
                                                 String name,
                                                 int offset,
                                                 int limit) { // NOSONAR
    Stream<PageKey> pagesStream = entities.values()
                                          .stream()
                                          .filter(page -> {
                                            if (StringUtils.isNotBlank(siteName)
                                                && !StringUtils.equals(siteName, page.getOwnerId())) { // NOSONAR
                                              return false;
                                            }
                                            if (siteType != null && siteType != page.getOwnerType()) {
                                              return false;
                                            }
                                            if (name != null // NOSONAR
                                                && (page.getDisplayName() == null
                                                    || (!StringUtils.contains(page.getDisplayName().toLowerCase(),
                                                                              name.toLowerCase())
                                                        && !StringUtils.contains(page.getName().toLowerCase(),
                                                                                 name.toLowerCase())))) {
                                              return false;
                                            }
                                            return true;
                                          })
                                          .map(entity -> new PageKey(entity.getOwnerType(),
                                                                     entity.getOwnerId(),
                                                                     entity.getName()));
    if (limit > 0) {
      pagesStream = pagesStream.limit((long) offset + limit);
      List<PageKey> result = pagesStream.toList();
      if (offset > 0) {
        result = result.size() > offset ? result.subList(
                                                         offset,
                                                         result.size()) :
                                        Collections.emptyList();
      }
      return new InMemoryListAccess<>(result, new PageKey[0]);
    } else {
      List<PageKey> result = pagesStream.toList();
      return new InMemoryListAccess<>(result, new PageKey[result.size()]);
    }
  }

  @Override
  public void deleteByOwner(long ownerId) {
    List<PageEntity> ownerPages = entities.values()
                                          .stream()
                                          .filter(page -> ownerId == page.getOwner().getId())
                                          .toList();
    deleteAll(ownerPages);
  }

}
