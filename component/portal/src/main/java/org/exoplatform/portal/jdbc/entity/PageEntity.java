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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.SiteType;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "GateInPage")
@Table(name = "PORTAL_PAGES")
@NamedQuery(name = "PageEntity.deleteByOwner", query = "DELETE GateInPage p WHERE p.owner.id = :ownerId")
@NamedQuery(name = "PageEntity.findByKey", query = "SELECT p FROM GateInPage p WHERE p.name = :name AND p.owner.siteType = :ownerType AND p.owner.name = :ownerId")
public class PageEntity extends ComponentEntity implements Serializable {

  private static final long     serialVersionUID = -6195451978995765259L;

  @Id
  @PortableSequence(name = "SEQ_PAGE_ID_GENERATOR")
  @Column(name = "ID")
  protected Long                id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "SITE_ID")
  private SiteEntity            owner;

  @Column(name = "SHOW_MAX_WINDOW")
  private boolean               showMaxWindow;
  
  @Column(name = "HIDE_SHARED_LAYOUT")
  private boolean               hideSharedLayout;

  @Getter
  @Setter
  @Column(name = "SHOW_SHARED_LAYOUT")
  private boolean               showSharedLayout;

  @Column(name = "DISPLAY_NAME", length = 200)
  private String                displayName;

  @Column(name = "NAME", length = 200)
  private String                name;

  @Column(name = "DESCRIPTION", length = 2000)
  private String                description;

  @Column(name = "FACTORY_ID", length = 200)
  private String                factoryId;

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "PAGE_TYPE")
  private PageType              pageType;

  @Column(name = "LINK")
  private String                link;

  @Column(name = "PROFILES")
  private String                profiles;

  @Column(name = "PAGE_BODY", length = 5000)
  private String                pageBody         = getJSONString(new JSONArray());

  @Transient
  private List<ComponentEntity> children         = new LinkedList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public SiteEntity getOwner() {
    return owner;
  }

  public void setOwner(SiteEntity owner) {
    this.owner = owner;
  }

  public boolean isShowMaxWindow() {
    return showMaxWindow;
  }

  public boolean isHideSharedLayout() {
    return hideSharedLayout;
  }

  public SiteType getOwnerType() {
    if (getOwner() != null) {
      return getOwner().getSiteType();
    } else {
      return null;
    }
  }

  public String getOwnerId() {
    if (getOwner() != null) {
      return getOwner().getName();
    } else {
      return null;
    }
  }

  public void setShowMaxWindow(boolean showMaxWindow) {
    this.showMaxWindow = showMaxWindow;
  }

  public void setHideSharedLayout(boolean hideSharedLayout) {
    this.hideSharedLayout = hideSharedLayout;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getPageBody() {
    return pageBody;
  }

  public void setPageBody(String pageBody) {
    this.pageBody = pageBody;
  }

  public String getProfiles() {
    return profiles;
  }

  public void setProfiles(String profiles) {
    this.profiles = profiles;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getFactoryId() {
    return factoryId;
  }

  public void setFactoryId(String factoryId) {
    this.factoryId = factoryId;
  }

  public List<ComponentEntity> getChildren() {
    return children;
  }

  public void setChildren(List<ComponentEntity> children) {
    this.children = children;
  }

  public PageType getPageType() {
    return pageType;
  }

  public void setPageType(PageType pageType) {
    this.pageType = pageType;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
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
    return TYPE.PAGE;
  }

}
