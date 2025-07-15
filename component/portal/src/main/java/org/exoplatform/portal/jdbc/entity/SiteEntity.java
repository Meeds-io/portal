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
package org.exoplatform.portal.jdbc.entity;

import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.exoplatform.portal.mop.SiteType;

@Entity(name = "GateInSite")
@Table(name = "PORTAL_SITES")
@NamedQuery(
  name = "SiteEntity.findByKey",
  query = "SELECT s FROM GateInSite s WHERE"
        + " s.siteType = :siteType AND s.name = :name"
)
@NamedQuery(
  name = "SiteEntity.findByType",
  query = "SELECT s FROM GateInSite s"
        + " WHERE s.siteType = :siteType"
)
@NamedQuery(
  name = "SiteEntity.findSiteKey",
  query = "SELECT s.name FROM GateInSite s"
        + " WHERE s.siteType = :siteType"
        + " ORDER BY s.name ASC"
)
@NamedQuery(
  name = "SiteEntity.findGroupSites",
  query = "SELECT s.name FROM GateInSite s"
        + " WHERE s.siteType = :siteType"
        + " AND NOT(s.name LIKE :excludeName)"
        + " ORDER BY s.name ASC"
)
@NamedQuery(
  name = "SiteEntity.findSpaceSites",
  query = "SELECT s.name FROM GateInSite s"
        + " WHERE s.siteType = :siteType"
        + " AND s.name LIKE :includeName"
        + " ORDER BY s.name ASC"
)
@NamedQuery(
  name = "SiteEntity.findPortalSites",
  query = "SELECT s.name FROM GateInSite s"
        + " WHERE s.siteType = :siteType"
        + " ORDER BY s.name ASC"
)
public class SiteEntity extends ComponentEntity {

  private static final long     serialVersionUID = 3036823700771832314L;

  @Id
  @SequenceGenerator(name = "SEQ_SITE_ID_GENERATOR", sequenceName = "SEQ_SITE_ID_GENERATOR", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_SITE_ID_GENERATOR")
  @Column(name = "ID")
  protected Long                id;

  @Column(name = "TYPE")
  private SiteType              siteType;

  @Column(name = "NAME", length = 200)
  private String                name;

  @Column(name = "LOCALE", length = 20)
  private String                locale;

  @Column(name = "SKIN", length = 200)
  private String                skin;

  @Column(name = "LABEL", length = 200)
  private String                label;

  @Column(name = "DESCRIPTION", length = 2000)
  private String                description;

  @Column(name = "PROPERTIES", length = 2000)
  private String                properties       = getJSONString(new JSONObject());

  @Column(name = "DEFAULT_SITE_BODY")
  private boolean               defaultSiteBody  = false;

  @Column(name = "SITE_BODY", length = 5000)
  private String                siteBody         = getJSONString(new JSONArray());

  @Transient
  private List<ComponentEntity> children         = new LinkedList<>();

  @Column(name = "DISPLAYED")
  private boolean               displayed;

  @Column(name = "DISPLAY_ORDER")
  private int               displayOrder;

  @Column(name = "BANNER_FILE_ID")
  protected long            bannerFileId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getSkin() {
    return skin;
  }

  public void setSkin(String skin) {
    this.skin = skin;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSiteBody() {
    return siteBody;
  }

  public void setSiteBody(String siteBody) {
    this.siteBody = siteBody;
  }

  public SiteType getSiteType() {
    return siteType;
  }

  public void setSiteType(SiteType siteType) {
    this.siteType = siteType;
  }

  public List<ComponentEntity> getChildren() {
    return children;
  }

  public void setChildren(List<ComponentEntity> children) {
    this.children = children;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public void setDefaultSiteBody(boolean defaultSiteBody) {
    this.defaultSiteBody = defaultSiteBody;
  }

  public boolean isDefaultSiteBody() {
    return defaultSiteBody;
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

  public long getBannerFileId() {
    return bannerFileId;
  }

  public void setBannerFileId(long bannerFileId) {
    this.bannerFileId = bannerFileId;
  }

  @Override
  public JSONObject toJSON() {
    JSONObject obj = super.toJSON();

    JSONArray jChildren = new JSONArray();
    for (ComponentEntity child : getChildren()) {
      jChildren.add(child.toJSON());
    }
    obj.put("children", jChildren);
    return obj;
  }

  @Override
  public TYPE getType() {
    return TYPE.SITE;
  }

}