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

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity(name = "HibernateIdentityObjectRelationshipName")
@Table(name = "jbid_io_rel_name")
@NamedQuery(
    name = "HibernateIdentityObjectRelationshipName.findIdentityObjectRelationshipNameByName",
    query = "select rn from HibernateIdentityObjectRelationshipName rn where rn.name like :name and rn.realm.name = :realmName")
public class HibernateIdentityObjectRelationshipName {

  @Id
  @PortableSequence(name = "JBID_IO_REL_NAME_ID_SEQ")
  @Column(name = "ID")
  private Long                id;

  @Column(name = "NAME",
      nullable = false)
  private String              name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REALM",
      nullable = false)
  @Fetch(FetchMode.SELECT)
  private HibernateRealm      realm;

  @ElementCollection(fetch = FetchType.LAZY)
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "jbid_io_rel_name_props",
      joinColumns = { @JoinColumn(name = "PROP_ID",
          referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  private Map<String, String> properties                               = new HashMap<>();

  public HibernateIdentityObjectRelationshipName() {
  }

  public HibernateIdentityObjectRelationshipName(String name, HibernateRealm realm) {
    this.name = name;
    this.realm = realm;
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

  public HibernateRealm getRealm() {
    return realm;
  }

  public void setRealm(HibernateRealm realm) {
    this.realm = realm;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
