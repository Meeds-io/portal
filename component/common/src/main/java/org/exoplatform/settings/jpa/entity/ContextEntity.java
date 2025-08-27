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
package org.exoplatform.settings.jpa.entity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "SettingsContextEntity")
@Table(name = "STG_CONTEXTS")
@NamedQueries({
    @NamedQuery(name = "SettingsContextEntity.getContextByTypeAndName", query = "SELECT c FROM SettingsContextEntity c " +
        "WHERE c.name = :contextName " +
        "AND c.type= :contextType "),
    @NamedQuery(name = "SettingsContextEntity.getContextByTypeWithNullName", query = "SELECT c FROM SettingsContextEntity c " +
        "WHERE c.name IS NULL " +
        "AND c.type= :contextType "),
    @NamedQuery(name = "SettingsContextEntity.getEmptyContextsByScopeAndContextType", query = "SELECT distinct(c) FROM SettingsContextEntity c " +
        "WHERE  c.type = :contextType " +
        "AND NOT EXISTS( " +
        " SELECT s FROM SettingsEntity s " +
        " JOIN s.context c2 " +
        " JOIN s.scope sc " +
        " WHERE c2.id = c.id " +
        " AND s.name = :settingName " +
        " AND sc.type = :scopeType " +
        " AND sc.name = :scopeName " +
        ")"),
    @NamedQuery(name = "SettingsContextEntity.getEmptyContextsByScopeWithNullNameAndContextType", query = "SELECT distinct(c) FROM SettingsContextEntity c " +
        "WHERE c.type = :contextType " +
        "AND NOT EXISTS( " +
        " SELECT s FROM SettingsEntity s " +
        " JOIN s.context c2 " +
        " JOIN s.scope sc " +
        " WHERE c2.id = c.id " +
        " AND s.name = :settingName " +
        " AND sc.type = :scopeType " +
        " AND sc.name IS NULL " +
        ")"),
    @NamedQuery(name = "SettingsContextEntity.getContextsByTypeAndScopeAndSettingName", query = "SELECT distinct(s.context) FROM SettingsEntity s " +
        "JOIN s.context c " +
        "JOIN s.scope sc " +
        "WHERE sc.name = :scopeName " +
        "AND sc.type = :scopeType " +
        "AND c.type = :contextType " +
        "AND s.name = :settingName "),
    @NamedQuery(name = "SettingsContextEntity.getContextsByTypeAndScopeWithNullNameAndSettingName", query = "SELECT distinct(s.context) FROM SettingsEntity s " +
        "JOIN s.context c " +
        "JOIN s.scope sc " +
        "WHERE sc.name = :scopeName " +
        "AND sc.type = :scopeType " +
        "AND c.type = :contextType " +
        "AND s.name = :settingName "),
    @NamedQuery(name = "SettingsContextEntity.countContextsByType", query = "SELECT count(c) FROM SettingsContextEntity c " +
        "WHERE c.type = :contextType "),
    @NamedQuery(name = "SettingsContextEntity.getContextNamesByType", query = "SELECT c.name FROM SettingsContextEntity c " +
        "WHERE c.type = :contextType ")
})
public class ContextEntity {
  @Id
  @Column(name = "CONTEXT_ID")
  @SequenceGenerator(name="SEQ_STG_CONTEXT_COMMON_ID", sequenceName="SEQ_STG_CONTEXT_COMMON_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_STG_CONTEXT_COMMON_ID")
  private Long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "TYPE")
  private String type;

  @OneToMany(fetch=FetchType.LAZY, mappedBy = "context")
  private Set<SettingsEntity> settings;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public ContextEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getType() {
    return type;
  }

  public ContextEntity setType(String type) {
    this.type = type;
    return this;
  }

  public Set<SettingsEntity> getSettings() {
    return settings;
  }

  public void setSettings(Set<SettingsEntity> settings) {
    this.settings = settings;
  }
}

