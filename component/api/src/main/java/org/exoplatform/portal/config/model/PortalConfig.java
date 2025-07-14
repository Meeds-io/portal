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
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.serialize.model.SiteLayout;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.pom.data.PortalData;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PortalConfig extends ModelObject implements Cloneable {

  public static final String    REMOVABLE_PROP  = "removable";

  public static final String    ICON_PROP       = "icon";

  public static final String    USER_TYPE       = SiteType.USER.getName();

  public static final String    GROUP_TYPE      = SiteType.GROUP.getName();

  public static final String    PORTAL_TYPE     = SiteType.PORTAL.getName();

  public static final String    SPACE_TYPE      = SiteType.SPACE.getName();

  public static final String    GROUP_TEMPLATE  = SiteType.GROUP_TEMPLATE.getName();

  public static final String    PORTAL_TEMPLATE = SiteType.PORTAL_TEMPLATE.getName();

  public static final String    DRAFT           = SiteType.DRAFT.getName();

  public static final Container DEFAULT_LAYOUT  = initDefaultLayout();

  private String                name;

  /** Added for new POM . */
  private String                type;

  private String                locale;

  private String                label;

  private String                description;

  private String[]              accessPermissions;

  private String                editPermission;

  private Properties            properties;

  private String                skin;

  private SiteLayout            portalLayout;

  private boolean               defaultLayout;

  private boolean               displayed;

  private int                   displayOrder;

  private String                bannerUploadId;

  private long                  bannerFileId;

  public PortalConfig() {
    this(PORTAL_TYPE);
  }

  public PortalConfig(String type) {
    this(type, null);
  }

  public PortalConfig(String type, String ownerId) {
    this(type, ownerId, null);
  }

  public PortalConfig(String type, String ownerId, String storageId) {
    super(storageId);

    //
    this.type = type;
    this.name = ownerId;
    this.portalLayout = new SiteLayout();
  }

  public PortalConfig(PortalData data) {
    super(data.getStorageId());

    //
    this.name = data.getName();
    this.type = data.getType();
    this.locale = data.getLocale();
    this.label = data.getLabel();
    this.description = data.getDescription();
    this.accessPermissions = data.getAccessPermissions().toArray(new String[data.getAccessPermissions().size()]);
    this.editPermission = data.getEditPermission();
    this.properties = data.getProperties() == null ? new Properties() : new Properties(data.getProperties());
    this.skin = data.getSkin();
    this.portalLayout = data.getPortalLayout() == null ? new SiteLayout() : new SiteLayout(data.getPortalLayout());
    this.defaultLayout = data.isDefaultLayout();
    this.displayed = data.isDisplayed();
    this.displayOrder = data.getDisplayOrder();
    this.bannerFileId = data.getBannerFileId();
  }

  public long getId() {
    if (StringUtils.contains(storageId, "_")) {
      return Long.parseLong(storageId.split("_")[1]);
    } else {
      return 0;
    }
  }

  public void setPortalLayout(Container container) {
    portalLayout = container == null ? new SiteLayout() : new SiteLayout(container);
  }

  public String getProperty(String name) {
    if (properties == null || !properties.containsKey(name))
      return null;
    return properties.get(name);
  }

  public boolean isRemovable() {
    return properties == null
           || StringUtils.equalsIgnoreCase(getType(), PortalConfig.DRAFT)
           || !StringUtils.equals(properties.get(PortalConfig.REMOVABLE_PROP), "false");
  }

  public void setRemovable(boolean removable) {
    if (removable) {
      removeProperty(REMOVABLE_PROP);
    } else {
      setProperty(REMOVABLE_PROP, "false");
    }
  }

  public String getIcon() {
    return getProperty(ICON_PROP);
  }

  public void setIcon(String icon) {
    if (icon == null) {
      removeProperty(ICON_PROP);
    } else {
      setProperty(ICON_PROP, icon);
    }
  }

  public String getProperty(String name, String defaultValue) {
    String value = getProperty(name);
    return value == null ? defaultValue : value;
  }

  public void setProperty(String name, String value) {
    if (properties == null) {
      properties = new Properties();
    }
    properties.setProperty(name, value);
  }

  public void removeProperty(String name) {
    if (properties != null) {
      properties.remove(name);
    }
  }

  @Override
  public String toString() {
    return "PortalConfig[name=" + name + ",type=" + type + "]";
  }

  @Override
  public PortalConfig clone() { // NOSONAR
    return new PortalConfig(build());
  }

  /**
   * Retuns Container that contains only PageBody to be able to display, at
   * least, the page content
   *
   * @return
   */
  private static Container initDefaultLayout() {
    Container container = new Container();
    ArrayList<ModelObject> children = new ArrayList<>();
    children.add(new PageBody());
    container.setChildren(children);
    return container;
  }

  public PortalData build() {
    return new PortalData(storageId,
                          name,
                          type,
                          locale,
                          label,
                          description,
                          accessPermissions == null ? Collections.emptyList() : Arrays.asList(accessPermissions),
                          editPermission,
                          properties == null ? Collections.emptyMap() : new Properties(properties),
                          skin,
                          portalLayout.build(),
                          defaultLayout,
                          displayed,
                          displayOrder,
                          bannerFileId);
  }

  @Override
  public void resetStorage() throws ObjectNotFoundException {
    super.resetStorage();
    getPortalLayout().resetStorage();
  }

  public void useMetaPortalLayout() {
    this.setPortalLayout(initDefaultLayout());
    this.setDefaultLayout(true);
  }

  /**
   * @return true if the site should be used as default site to redirected to
   *         when no other site is available, else false
   */
  public boolean isDefaultSite() {
    return !StringUtils.equals(getProperty("NO_DEFAULT_PATH"), "true");
  }

  /**
   * @param defaultSite true the site should be used as default site to
   *          redirected to when no other site is available, else false
   */
  public void setDefaultSite(boolean defaultSite) {
    setProperty("NO_DEFAULT_PATH", String.valueOf(!defaultSite));
  }

  /**
   * @return the associated {@link SiteKey}
   */
  public SiteKey getSiteKey() {
    return new SiteKey(type, name);
  }

}
