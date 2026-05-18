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
package org.picketlink.idm.impl.model.hibernate;

import org.picketlink.idm.spi.model.IdentityObjectRelationshipType;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity(name = "HibernateIdentityObjectRelationshipType")
@Table(name = "jbid_io_rel_type")
@NamedQuery(name = "HibernateIdentityObjectRelationshipType.findIdentityRelationshipTypeByName", query = "SELECT t FROM HibernateIdentityObjectRelationshipType t" +
    " WHERE t.name = :name")
public class HibernateIdentityObjectRelationshipType implements IdentityObjectRelationshipType {

  @Id
  @PortableSequence(name = "JBID_IO_REL_TYPE_ID_SEQ")
  @Column(name = "ID")
  private Long   id;

  @Column(name = "NAME", nullable = false)
  private String name;

  public HibernateIdentityObjectRelationshipType() {
  }

  public HibernateIdentityObjectRelationshipType(String name) {
    this.name = name;
  }

  public HibernateIdentityObjectRelationshipType(IdentityObjectRelationshipType type) {
    if (type == null) {
      throw new IllegalArgumentException("type is null");
    }
    if (type.getName() != null) {
      this.name = type.getName();
    }
  }

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
}
