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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.picketlink.idm.spi.model.IdentityObjectRelationship;
import org.picketlink.idm.spi.model.IdentityObjectRelationshipType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "HibernateIdentityObjectRelationship")
@Table(name = "jbid_io_rel")
@NamedQuery(
    name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipWithoutName",
    query = "SELECT r FROM HibernateIdentityObjectRelationship r" + " WHERE r.type.id = :typeId" +
        " AND r.fromIdentityObject = :fromIdentityObject" + " AND r.toIdentityObject = :toIdentityObject")
@NamedQuery(
          name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByIdentitiesAndTypeAndName",
          query = "SELECT r FROM HibernateIdentityObjectRelationship r" +
          " WHERE r.type.name = :typeName" +
          " AND r.name.name = :name" +
          " AND r.fromIdentityObject = :fromIdentityObject" +
          " AND r.toIdentityObject = :toIdentityObject")
@NamedQuery(
    name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipsByIdentities",
    query = "SELECT r FROM HibernateIdentityObjectRelationship r" + " WHERE" + " (" + "   r.fromIdentityObject = :hio1" +
        "     AND" + "   r.toIdentityObject = :hio2" + " ) OR (" + "   r.fromIdentityObject = :hio2" + "     AND" +
        "   r.toIdentityObject = :hio1" + " ) ")
@NamedQuery(
    name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByIdentityByType",
    query = "SELECT r FROM HibernateIdentityObjectRelationship r" + " WHERE r.type.name = :typeName" +
        " AND r.fromIdentityObject = :fromIdentityObject" + " AND r.toIdentityObject = :toIdentityObject")
@NamedQuery(
    name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByAttributes",
    query = "SELECT r FROM HibernateIdentityObjectRelationship r" + " WHERE r.type.id = :typeId" + " AND r.name.name = :name" +
        " AND r.fromIdentityObject = :fromIdentityObject" + " AND r.toIdentityObject = :toIdentityObject")
@NamedQuery(
    name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByIdentity",
    query = "SELECT r FROM HibernateIdentityObjectRelationship r" + " WHERE r.fromIdentityObject = :fromIdentityObject" +
        " AND r.toIdentityObject = :toIdentityObject")
@NamedQuery(
    name = "HibernateIdentityObjectRelationship.getRelationshipsByName",
    query = "SELECT r FROM HibernateIdentityObjectRelationship r " +
        " WHERE r.name.id = :nameId")
@NamedQuery(
    name = "HibernateIdentityObjectRelationship.removeRelationshipsByName",
    query = "DELETE FROM HibernateIdentityObjectRelationship "
        + " WHERE name.id = :nameId"
)
public class HibernateIdentityObjectRelationship implements IdentityObjectRelationship {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator="JBID_IO_REL_ID_SEQ")
  @SequenceGenerator(name = "JBID_IO_REL_ID_SEQ", sequenceName = "JBID_IO_REL_ID_SEQ", allocationSize = 1)
  @Column(name = "ID")
  private Long                                    id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "NAME")
  @Fetch(FetchMode.JOIN)
  private HibernateIdentityObjectRelationshipName name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REL_TYPE",
      nullable = false)
  @Fetch(FetchMode.SELECT)
  private HibernateIdentityObjectRelationshipType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "FROM_IDENTITY",
      nullable = false)
  @Fetch(FetchMode.SELECT)
  private HibernateIdentityObject                 fromIdentityObject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_IDENTITY",
      nullable = false)
  @Fetch(FetchMode.SELECT)
  private HibernateIdentityObject                 toIdentityObject;

  @ElementCollection(fetch = FetchType.LAZY)
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "jbid_io_rel_props",
      joinColumns = { @JoinColumn(name = "PROP_ID",
          referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  private Map<String, String>                     properties = new HashMap<>();

  public HibernateIdentityObjectRelationship() {
  }

  public HibernateIdentityObjectRelationship(HibernateIdentityObjectRelationshipType type,
                                             HibernateIdentityObject fromIdentityObject,
                                             HibernateIdentityObject toIdentityObject) {
    this.type = type;
    this.fromIdentityObject = fromIdentityObject;
    fromIdentityObject.getFromRelationships().add(this);
    this.toIdentityObject = toIdentityObject;
    toIdentityObject.getToRelationships().add(this);
  }

  public HibernateIdentityObjectRelationship(HibernateIdentityObjectRelationshipType type,
                                             HibernateIdentityObject fromIdentityObject,
                                             HibernateIdentityObject toIdentityObject,
                                             HibernateIdentityObjectRelationshipName name) {
    this.type = type;
    this.fromIdentityObject = fromIdentityObject;
    fromIdentityObject.getFromRelationships().add(this);
    this.toIdentityObject = toIdentityObject;
    toIdentityObject.getToRelationships().add(this);
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IdentityObjectRelationshipType getType() {
    return type;
  }

  public void setType(HibernateIdentityObjectRelationshipType type) {
    this.type = type;
  }

  public HibernateIdentityObject getFromIdentityObject() {
    return fromIdentityObject;
  }

  public void setFromIdentityObject(HibernateIdentityObject fromIdentityObject) {
    this.fromIdentityObject = fromIdentityObject;
  }

  public HibernateIdentityObject getToIdentityObject() {
    return toIdentityObject;
  }

  public void setToIdentityObject(HibernateIdentityObject toIdentityObject) {
    this.toIdentityObject = toIdentityObject;
  }

  public String getName() {
    if (name != null) {
      return name.getName();
    }
    return null;
  }

  public void setName(HibernateIdentityObjectRelationshipName name) {
    this.name = name;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
