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
import java.util.HashMap;
import java.util.Map;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity(name = "GateInDescription")
@Table(name = "PORTAL_DESCRIPTIONS")
@NamedQueries({
  @NamedQuery(name = "DescriptionEntity.getByRefId", query = "SELECT d FROM GateInDescription d WHERE d.referenceId = :refId") })
public class DescriptionEntity implements Serializable {

  private static final long serialVersionUID = 1173817577220348267L;

  @Id
  @PortableSequence(name = "SEQ_PORTAL_DESCRIPTIONS_ID")
  @Column(name = "DESCRIPTION_ID")
  private Long              id;

  @Column(name = "REF_ID", length = 200)
  private String            referenceId;

  @Embedded
  private DescriptionState state;
  
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name="PORTAL_DESCRIPTION_LOCALIZED", joinColumns = @JoinColumn(name = "DESCRIPTION_ID"))
  @MapKeyColumn(name="LOCALE")
  private Map<String, DescriptionState> localized = new HashMap<>();
  
  public DescriptionEntity() {
  }

  public DescriptionEntity(String referenceId) {
    this.referenceId = referenceId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public DescriptionState getState() {
    return state;
  }

  public void setState(DescriptionState state) {
    this.state = state;
  }

  public Map<String, DescriptionState> getLocalized() {
    return localized;
  }

  public void setLocalized(Map<String, DescriptionState> localized) {
    this.localized = localized;
  }
  
}
