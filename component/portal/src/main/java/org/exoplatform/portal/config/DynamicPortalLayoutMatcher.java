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
package org.exoplatform.portal.config;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.mop.SiteKey;

/**
 * <p>
 * A class that will be used to check if a site portal should have dynamic site
 * layout retrieved into it or not.
 * </p>
 * <p>
 * This check is made swicth multiple conditions and regex.
 * </p>
 */
public class DynamicPortalLayoutMatcher {

  private String  currentSiteNameRegex   = null;

  private String  siteNameRegex          = null;

  private String  siteTypeRegex          = null;

  private Pattern currentSiteNamePattern = null;

  private Pattern siteNamePattern        = null;

  private Pattern siteTypePattern        = null;

  public void setCurrentSiteNameRegex(String currentSiteNameRegex) {
    this.currentSiteNameRegex = currentSiteNameRegex;
  }

  public void setSiteNameRegex(String siteNameRegex) {
    this.siteNameRegex = siteNameRegex;
  }

  public void setSiteTypeRegex(String siteTypeRegex) {
    this.siteTypeRegex = siteTypeRegex;
  }

  public Pattern getSiteNamePattern() {
    if (siteNamePattern == null && StringUtils.isNotBlank(siteNameRegex)) {
      this.siteNamePattern = Pattern.compile(siteNameRegex);
    }
    return siteNamePattern;
  }

  public Pattern getSiteTypePattern() {
    if (siteTypePattern == null && StringUtils.isNotBlank(siteTypeRegex)) {
      this.siteTypePattern = Pattern.compile(siteTypeRegex);
    }
    return siteTypePattern;
  }

  public Pattern getCurrentSiteNamePattern() {
    if (currentSiteNamePattern == null && StringUtils.isNotBlank(currentSiteNameRegex)) {
      this.currentSiteNamePattern = Pattern.compile(currentSiteNameRegex);
    }
    return currentSiteNamePattern;
  }

  /**
   * Checks whether currently displaying site (designated by siteKey) should be
   * retrieved with dynamic site layout or not.
   * 
   * @param siteKey currently displaying site
   * @param currentPortalSiteName last displayed site name of type PORTAL
   * @return true if condifitons of current Matcher matches, else false.
   */
  public boolean matches(SiteKey siteKey, String currentPortalSiteName) {
    if (siteKey == null || StringUtils.isBlank(currentPortalSiteName)) {
      return false;
    }

    if (getCurrentSiteNamePattern() != null && !getCurrentSiteNamePattern().matcher(currentPortalSiteName).matches()) {
      return false;
    }
    if (getSiteNamePattern() != null && !getSiteNamePattern().matcher(siteKey.getName()).matches()) {
      return false;
    }
    if (getSiteTypePattern() != null && !getSiteTypePattern().matcher(siteKey.getTypeName()).matches()) { // NOSONAR
      return false;
    }
    return true;
  }
}
