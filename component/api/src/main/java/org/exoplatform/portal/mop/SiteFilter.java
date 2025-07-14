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
package org.exoplatform.portal.mop;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteFilter implements Serializable {

  private SiteType siteType;

  private SiteType excludedSiteType;

  private String   excludedSiteName;

  private boolean  excludeSpaceSites;
  
  private boolean  sortByDisplayOrder;

  private boolean  filterByDisplayed;

  private boolean  displayed;

  private int      limit;

  private int      offset;

  @Override
  public SiteFilter clone() { // NOSONAR
    return new SiteFilter(siteType,
                          excludedSiteType,
                          excludedSiteName,
                          excludeSpaceSites,
                          sortByDisplayOrder,
                          filterByDisplayed,
                          displayed,
                          limit,
                          offset);
  }

}
