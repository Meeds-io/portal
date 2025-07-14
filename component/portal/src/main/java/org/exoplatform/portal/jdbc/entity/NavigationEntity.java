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

import jakarta.persistence.*;

import org.exoplatform.portal.mop.SiteType;

@Entity(name = "GateInNavigation")
@Table(name = "PORTAL_NAVIGATIONS")
@NamedQueries({
  @NamedQuery(name = "NavigationEntity.findByOwner", query = "SELECT nav FROM GateInNavigation nav INNER JOIN nav.owner s WHERE s.siteType = :ownerType AND s.name = :ownerId"),
  @NamedQuery(name = "NavigationEntity.findByRootNode", query = "SELECT nav FROM GateInNavigation nav INNER JOIN nav.rootNode r WHERE r.id = :rootNodeId")
})
public class NavigationEntity implements Serializable {

  private static final long serialVersionUID = 3811683620903785319L;

  @Id
  @SequenceGenerator(name="SEQ_GTN_NAVIGATION_ID", sequenceName="SEQ_GTN_NAVIGATION_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_GTN_NAVIGATION_ID")
  @Column(name = "NAVIGATION_ID")
  private Long             id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "SITE_ID")
  private SiteEntity owner;

  @Column(name = "PRIORITY")
  private int               priority = 1;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
  @JoinColumn(name = "NODE_ID")
  private NodeEntity        rootNode;

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

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public NodeEntity getRootNode() {
    return rootNode;
  }

  public void setRootNode(NodeEntity rootNode) {
    this.rootNode = rootNode;
  }

}
