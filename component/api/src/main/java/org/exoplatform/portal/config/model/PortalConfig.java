/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.config.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.pom.data.PortalData;

import lombok.EqualsAndHashCode;

/**
 * May 13, 2004
 *
 * @author Tuan Nguyen
 * @version $Id: PortalConfig.java,v 1.7 2004/08/06 03:02:29 tuan08 Exp $
 **/
@EqualsAndHashCode(callSuper = true)
public class PortalConfig extends ModelObject implements Cloneable {

  public static final String    USER_TYPE      = SiteType.USER.getName();

  public static final String    GROUP_TYPE     = SiteType.GROUP.getName();

  public static final String    PORTAL_TYPE    = SiteType.PORTAL.getName();

  public static final String    SPACE_TYPE     = SiteType.SPACE.getName();

  public static final String    GROUP_TEMPLATE = SiteType.GROUP_TEMPLATE.getName();

  public static final Container DEFAULT_LAYOUT = initDefaultLayout();

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

  private Container             portalLayout;

  private boolean               defaultLayout;

  private boolean               displayed      = true;

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
    this.portalLayout = new Container();
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
    this.portalLayout = new Container(data.getPortalLayout());
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String s) {
    name = s;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String s) {
    locale = s;
  }

  public String[] getAccessPermissions() {
    return accessPermissions;
  }

  public void setAccessPermissions(String[] s) {
    accessPermissions = s;
  }

  public String getEditPermission() {
    return editPermission;
  }

  public void setEditPermission(String editPermission) {
    this.editPermission = editPermission;
  }

  public String getSkin() {
    return skin;
  }

  public void setSkin(String s) {
    skin = s;
  }

  public Container getPortalLayout() {
    return portalLayout;
  }

  public void setPortalLayout(Container container) {
    portalLayout = container;
  }

  public boolean isDefaultLayout() {
    return defaultLayout;
  }

  public void setDefaultLayout(boolean defaultLayout) {
    this.defaultLayout = defaultLayout;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties props) {
    properties = props;
  }

  public String getProperty(String name) {
    if (name == null)
      throw new NullPointerException();
    if (properties == null || !properties.containsKey(name))
      return null;
    return properties.get(name);
  }

  public String getProperty(String name, String defaultValue) {
    String value = getProperty(name);
    if (value != null)
      return value;
    return defaultValue;
  }

  public void setProperty(String name, String value) {
    if (name == null || properties == null)
      throw new NullPointerException();
    properties.setProperty(name, value);
  }

  public void removeProperty(String name) {
    if (name == null || properties == null)
      throw new NullPointerException();
    properties.remove(name);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public boolean isDisplayed() {
    return displayed;
  }

  public void setDisplayed(boolean displayed) {
    this.displayed = displayed;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public String getBannerUploadId() {
    return bannerUploadId;
  }

  public void setBannerUploadId(String bannerUploadId) {
    this.bannerUploadId = bannerUploadId;
  }

  public long getBannerFileId() {
    return bannerFileId;
  }

  public void setBannerFileId(long bannerFileId) {
    this.bannerFileId = bannerFileId;
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

}
