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
package org.exoplatform.portal.mop.page;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

import lombok.Data;

/**
 * The immutable key for a page.
 *
 */
@Data
public class PageKey implements Serializable {

  private static final long serialVersionUID = -5005776237959733435L;

  /**
   * Parse the string representation of a page key.
   *
   * @param  pageKey the string representation
   * @return         the corresponding page key
   */
  public static PageKey parse(String pageKey) {
    if (StringUtils.isBlank(pageKey)) {
      throw new IllegalArgumentException("No null string argument allowed");
    }
    String[] pageKeyParts = StringUtils.split(pageKey, "::");
    if (pageKeyParts.length != 3) {
      throw new IllegalArgumentException("Format should be SITE_TYPE::SITE_NAME::PAGE_NAME");
    }
    return SiteType.valueOf(pageKeyParts[0].toUpperCase()).key(pageKeyParts[1]).page(pageKeyParts[2]);
  }

  final SiteKey  site;

  final String   name;

  private String ref;

  public PageKey(String siteType, String siteName, String name) {
    this(SiteType.valueOf(siteType.toUpperCase()).key(siteName), name);
  }

  public PageKey(SiteType siteType, String siteName, String name) {
    this(siteType.key(siteName), name);
  }

  public PageKey(SiteKey site, String name) {
    this.site = site;
    this.name = name;
  }

  public PageKey sibling(String name) {
    return new PageKey(site, name);
  }

  public String format() {
    if (ref == null) {
      ref = String.format("%s::%s::%s",
                          site == null ? "" : site.getType().getName(),
                          site == null ? "" : site.getName(),
                          name);
    }
    return ref;
  }

  public org.exoplatform.portal.pom.data.PageKey toPomPageKey() {
    return new org.exoplatform.portal.pom.data.PageKey(site.getType().getName(),
                                                       site.getName(),
                                                       name);
  }

  @Override
  public String toString() {
    return format();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PageKey other = (PageKey) obj;
    return Objects.equals(format(), other.format());
  }

  @Override
  public int hashCode() {
    return Objects.hash(format());
  }
}
