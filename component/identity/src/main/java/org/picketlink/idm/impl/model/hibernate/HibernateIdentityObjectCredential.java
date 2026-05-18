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
import org.picketlink.idm.spi.model.IdentityObjectCredential;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.CascadeType;
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

@Entity(name = "HibernateIdentityObjectCredential")
@Table(name = "jbid_io_creden")
@NamedQuery(
    name = "HibernateIdentityObjectCredential.findCredentialByTypeAndIdentity",
    query = "SELECT c FROM HibernateIdentityObjectCredential c"
        + " INNER JOIN c.type type ON type.name = :cTypeName"
        + " WHERE c.identityObject.id = :ioId"
)
public class HibernateIdentityObjectCredential implements IdentityObjectCredential {

  @Id
  @Column(name = "ID")
  @PortableSequence(name = "JBID_IO_CREDEN_ID_SEQ")
  private Long                                         id;

  @ManyToOne(fetch = FetchType.EAGER)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "CREDENTIAL_TYPE", nullable = false)
  private HibernateIdentityObjectCredentialType        type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "IDENTITY_OBJECT_ID", nullable = false)
  @Fetch(FetchMode.SELECT)
  private HibernateIdentityObject                      identityObject;

  @Column(name = "TEXT")
  private String                                       textValue;

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @Fetch(FetchMode.SELECT)
  @JoinColumn(name = "BIN_VALUE_ID")
  private HibernateIdentityObjectCredentialBinaryValue binaryValue;

  @ElementCollection(fetch = FetchType.LAZY)
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "jbid_io_creden_props", joinColumns = { @JoinColumn(name = "PROP_ID", referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  private Map<String, String>                          properties = new HashMap<String, String>();

  public HibernateIdentityObjectCredential() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTextValue() {
    return textValue;
  }

  public void setTextValue(String textValue) {
    this.textValue = textValue;
  }

  public HibernateIdentityObjectCredentialBinaryValue getBinaryValue() {
    return binaryValue;
  }

  public void setBinaryValue(HibernateIdentityObjectCredentialBinaryValue binaryValue) {
    this.binaryValue = binaryValue;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public HibernateIdentityObjectCredentialType getType() {
    return type;
  }

  public void setType(HibernateIdentityObjectCredentialType type) {
    this.type = type;
  }

  public HibernateIdentityObject getIdentityObject() {
    return identityObject;
  }

  public void setIdentityObject(HibernateIdentityObject identityObject) {
    this.identityObject = identityObject;
  }

  public Object getValue() {
    if (textValue != null) {
      return textValue;
    }
    return binaryValue.getValue();
  }

  public Object getEncodedValue() {
    return null;
  }
}
