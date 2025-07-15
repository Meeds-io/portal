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

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageData;
import org.exoplatform.portal.mop.page.PageKey;

public interface PageStorage {

  /**
   * Find and returns a page, if no such page exist, null is returned.
   *
   * @param key the page key
   * @return the matching page
   */
  PageContext loadPage(PageKey key);

  List<PageContext> loadPages(SiteKey siteKey);

  /**
   * Create, update a page. When the page state is not null, the page will be
   * created or updated depending on whether or not the page already exists.
   *
   * @param page the page
   * @return true if the page is not already existed, otherwise return false.
   */
  boolean savePage(PageContext page);

  /**
   * Destroy a page.
   *
   * @param key the page key
   * @return true when the page was destroyed
   */
  boolean destroyPage(PageKey key);

  /**
   * Deletes Pages of a given site
   * 
   * @param siteKey {@link SiteKey}
   * @return true if deleted, else false
   */
  boolean destroyPages(SiteKey siteKey);

  /**
   * Clone a page.
   *
   * @param src the source key
   * @param dst the destination key
   * @return the cloned page
   */
  PageContext clone(PageKey src, PageKey dst);

  /**
   * Query the page service to find pages that match the <code>siteType</code>,
   * <code>siteName</code>, <code>pageName</code> and <code>title</code>
   * criterions.
   *
   * @param offset the query offset
   * @param limit the query limit
   * @param siteType the site type
   * @param siteName the site name
   * @param pageName the page name
   * @param pageTitle the page title
   * @return the query result
   */
  QueryResult<PageContext> findPages(int offset,
                                     int limit,
                                     SiteType siteType,
                                     String siteName,
                                     String pageName,
                                     String pageTitle);

  /**
   * Saves a page. If a page with the same id already exists then a merge
   * operation will occur, otherwise it throws {@link IllegalStateException}
   * From PLF 5.3.x (RDBMS implementation) we drop support return the change
   * list as it's not used any where. So the method always return the empty
   * list.
   *
   * @param page the page to save
   */
  void save(org.exoplatform.portal.pom.data.PageData page);

  /**
   * Retrieves page data switch given key
   * 
   * @param key {@link org.exoplatform.portal.pom.data.PageKey}
   * @return {@link PageData}
   */
  org.exoplatform.portal.pom.data.PageData getPage(org.exoplatform.portal.pom.data.PageKey key);

  Page getPage(String pageKey);

  Page getPage(PageKey pageKey);

  Page getPage(long id);

}
