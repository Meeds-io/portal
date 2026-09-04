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
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.portal.mop.NodeTarget;
import org.exoplatform.portal.mop.Visibility;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity(name = "GateInNavigationNode")
@Table(name = "PORTAL_NAVIGATION_NODES")
@NamedQuery(name = "NodeEntity.findByPage", query = "SELECT n FROM GateInNavigationNode n INNER JOIN n.page p WHERE p.id = :pageId")
public class NodeEntity implements Serializable {

  private static final long serialVersionUID = 8630708630711337929L;

  @Id
  @PortableSequence(name = "SEQ_GTN_NAVIGATION_NODE_ID")
  @Column(name = "NODE_ID")
  private Long              id;

  @Column(name = "NAME", length = 200)
  private String            name;

  @Column(name = "LABEL", length = 200)
  private String            label;

  @Column(name = "ICON", length = 200)
  private String            icon;

  @Column(name = "START_TIME")
  private long              startTime;

  @Column(name = "END_TIME")
  private long              endTime;

  @Column(name = "VISIBILITY")
  private Visibility        visibility       = Visibility.DISPLAYED;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "PAGE_ID", nullable = true)
  private PageEntity        page;

  @Column(name = "NODE_INDEX")
  private int               index;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "parent")
  @OrderBy("index ASC")
  private List<NodeEntity>  children         = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_ID")
  private NodeEntity        parent;

  @OneToOne(fetch = FetchType.EAGER, mappedBy = "rootNode")
  private NavigationEntity  navigationEntity;

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "TARGET")
  private NodeTarget        target;

  @Column(name = "UPDATED_DATE")
  private long updatedDate;

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

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public void setVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  public PageEntity getPage() {
    return page;
  }

  public void setPage(PageEntity page) {
    this.page = page;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public List<NodeEntity> getChildren() {
    return children;
  }

  public void setChildren(List<NodeEntity> children) {
    if (children != null) {
      for (int i = 0; i < children.size(); i++) {
        children.get(i).setIndex(i);
      }
    }
    this.children = children;
  }

  public NodeTarget getTarget() {
    return target;
  }

  public void setTarget(NodeTarget target) {
    this.target = target;
  }

  public NodeEntity getParent() {
    return parent;
  }

  public long getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(long updatedDate) {
    this.updatedDate = updatedDate;
  }

  public void setParent(NodeEntity parent) {
    this.parent = parent;
  }

  public NavigationEntity getNavigationEntity() {
    return navigationEntity;
  }

  public void setNavigationEntity(NavigationEntity navigationEntity) {
    this.navigationEntity = navigationEntity;
  }

}
