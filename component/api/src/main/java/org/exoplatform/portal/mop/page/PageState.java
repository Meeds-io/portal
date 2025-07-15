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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.portal.mop.PageType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageState implements Serializable {

  private static final long serialVersionUID = 7874166775312871923L;

  private String            storageId;

  private boolean           showMaxWindow;

  private boolean           hideSharedLayout;

  private boolean           showSharedLayout;

  private String            profiles;

  private String            factoryId;

  private String            displayName;

  private String            description;

  private String            type;

  private String            link;

  private String            editPermission;

  private List<String>      accessPermissions;

  public PageState(PageState pageState) {
    this(pageState.storageId,
         pageState.showMaxWindow,
         pageState.hideSharedLayout,
         pageState.showSharedLayout,
         pageState.profiles,
         pageState.factoryId,
         pageState.displayName,
         pageState.description,
         pageState.type,
         pageState.link,
         pageState.editPermission,
         pageState.accessPermissions);
  }

  public PageState(String displayName, // NOSONAR
                   String description,
                   boolean showMaxWindow,
                   String factoryId,
                   List<String> accessPermissions,
                   String editPermission) {
    this(displayName, description, showMaxWindow, false, false, factoryId, accessPermissions, editPermission);
  }

  public PageState(String displayName, // NOSONAR
                   String description,
                   boolean showMaxWindow,
                   boolean hideSharedLayout,
                   boolean showSharedLayout,
                   String factoryId,
                   String profiles,
                   List<String> accessPermissions,
                   String editPermission,
                   String type,
                   String link) {
    this.editPermission = editPermission;
    this.showMaxWindow = showMaxWindow;
    this.hideSharedLayout = hideSharedLayout;
    this.showSharedLayout = showSharedLayout;
    this.factoryId = factoryId;
    this.profiles = profiles;
    this.displayName = displayName;
    this.description = description;
    this.accessPermissions = accessPermissions;
    this.type = type;
    this.link = link;
  }

  public PageState(String displayName, // NOSONAR
                   String description,
                   boolean showMaxWindow,
                   boolean hideSharedLayout,
                   boolean showSharedLayout,
                   String factoryId,
                   List<String> accessPermissions,
                   String editPermission) {
    this.editPermission = editPermission;
    this.showMaxWindow = showMaxWindow;
    this.hideSharedLayout = hideSharedLayout;
    this.showSharedLayout = showSharedLayout;
    this.factoryId = factoryId;
    this.profiles = null;
    this.displayName = displayName;
    this.description = description;
    this.accessPermissions = accessPermissions;
    this.type = PageType.PAGE.name();
    this.link = null;
  }

  public Builder builder() {
    return new Builder(editPermission,
                       showMaxWindow,
                       hideSharedLayout,
                       showSharedLayout,
                       factoryId,
                       profiles,
                       displayName,
                       description,
                       accessPermissions,
                       type,
                       link);
  }

  public static class Builder {

    /** . */
    private String       editPermission;

    /** . */
    private boolean      showMaxWindow;

    /** . */
    private boolean      hideSharedLayout;

    /** . */
    private boolean      showSharedLayout;

    /** . */
    private String       factoryId;

    private String       profiles;

    /** . */
    private String       displayName;

    /** . */
    private String       description;

    /** . */
    private String       type;

    /** . */
    private String       link;

    /** . */
    private List<String> accessPermissions;

    private Builder(String editPermission, // NOSONAR
                    boolean showMaxWindow,
                    boolean hideSharedLayout,
                    boolean showSharedLayout,
                    String factoryId,
                    String profiles,
                    String displayName,
                    String description,
                    List<String> accessPermissions,
                    String type,
                    String link) {
      this.editPermission = editPermission;
      this.showMaxWindow = showMaxWindow;
      this.hideSharedLayout = hideSharedLayout;
      this.showSharedLayout = showSharedLayout;
      this.factoryId = factoryId;
      this.profiles = profiles;
      this.displayName = displayName;
      this.description = description;
      this.accessPermissions = accessPermissions;
      this.type = type;
      this.link = link;
    }

    public Builder editPermission(String editPermission) {
      this.editPermission = editPermission;
      return this;
    }

    public Builder accessPermissions(List<String> accessPermissions) {
      this.accessPermissions = accessPermissions;
      return this;
    }

    public Builder accessPermissions(String... accessPermissions) {
      this.accessPermissions = new ArrayList<>(Arrays.asList(accessPermissions));
      return this;
    }

    public Builder showMaxWindow(boolean showMaxWindow) {
      this.showMaxWindow = showMaxWindow;
      return this;
    }
    
    public Builder hideSharedLayout(boolean hideSharedLayout) {
      this.hideSharedLayout = hideSharedLayout;
      return this;
    }

    public Builder showSharedLayout(boolean showSharedLayout) {
      this.showSharedLayout = showSharedLayout;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder profiles(String profiles) {
      this.profiles = profiles;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder factoryId(String factoryId) {
      this.factoryId = factoryId;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder link(String link) {
      this.link = link;
      return this;
    }

    public PageState build() {
      return new PageState(displayName,
                           description,
                           showMaxWindow,
                           hideSharedLayout,
                           showSharedLayout,
                           factoryId,
                           profiles,
                           accessPermissions,
                           editPermission,
                           type,
                           link);
    }
  }
}
