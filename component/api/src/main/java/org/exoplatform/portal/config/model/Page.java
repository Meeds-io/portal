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
package org.exoplatform.portal.config.model;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.PageData;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * May 13, 2004
 **/
@EqualsAndHashCode(callSuper = true)
public class Page extends Container {

  public static final String DEFAULT_PAGE     = "Default";

  private PageKey            pageKey;

  private String             ownerType;

  private String             ownerId;

  private String             editPermission;

  private boolean            showMaxWindow    = false;

  /**
   * Whether to 'hide' shared layout or not even when the site 'displays' the
   * shared layout
   */
  @Getter
  @Setter
  private boolean            hideSharedLayout = false;

  /**
   * Whether to 'show' shared layout or not even when the site 'hides' the
   * shared layout layout
   */
  @Getter
  @Setter
  private boolean            showSharedLayout = false;

  private String             type;

  private String             link;

  public Page() {
  }

  public Page(String ownerType, String ownerId, String name) {
    this.ownerType = ownerType;
    this.ownerId = ownerId;
    this.name = name;
  }

  public Page(PageData data) {
    super(data);

    //
    this.ownerType = data.getOwnerType();
    this.ownerId = data.getOwnerId();
    this.editPermission = data.getEditPermission();
    this.showMaxWindow = data.isShowMaxWindow();
    this.hideSharedLayout = data.isHideSharedLayout();
    this.showSharedLayout = data.isShowSharedLayout();
    this.type = data.getType();
    this.link = data.getLink();
  }

  public Page(String storageId) {
    super(storageId);
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(String ownerType) {
    this.ownerType = ownerType;
  }

  public String getEditPermission() {
    return editPermission;
  }

  public void setEditPermission(String editPermission) {
    this.editPermission = editPermission;
  }

  public boolean isShowMaxWindow() {
    return showMaxWindow;
  }

  public void setShowMaxWindow(Boolean showMaxWindow) {
    this.showMaxWindow = showMaxWindow.booleanValue();
  }

  public PageKey getPageKey() {
    if (pageKey == null && getPageId() != null) {
      pageKey = PageKey.parse(getPageId());
    }
    return pageKey;
  }

  public String getPageId() {
    if (ownerType == null || ownerId == null || name == null) {
      return null;
    } else {
      return String.format("%s::%s::%s", ownerType, ownerId, name);
    }
  }

  public void setPageId(String pageId) {
    if (pageId == null) {
      ownerType = null;
      ownerId = null;
      name = null;
    } else {
      String[] pageIdParts = pageId.split("::");
      this.ownerType = pageIdParts[0];
      this.ownerId = pageIdParts[1];
      this.name = pageIdParts[2];
    }
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  @Override
  public PageData build() {
    List<ComponentData> children = buildChildren();
    return new PageData(storageId,
                        id,
                        name,
                        icon,
                        template,
                        factoryId,
                        title,
                        description,
                        width,
                        height,
                        cssClass,
                        profiles,
                        Utils.safeImmutableList(accessPermissions),
                        children,
                        ownerType,
                        ownerId,
                        editPermission,
                        showMaxWindow,
                        hideSharedLayout,
                        showSharedLayout,
                        type,
                        link);
  }

  public static class PageSet {
    private ArrayList<Page> pages;

    public PageSet() {
      pages = new ArrayList<>();
    }

    public ArrayList<Page> getPages() {
      return pages;
    }

    public void setPages(ArrayList<Page> list) {
      pages = list;
    }
  }

  @Override
  public String toString() {
    return "Page[ownerType=" + ownerType + ",ownerId=" + ownerId + ",name=" + name + "]";
  }
}
