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
package org.exoplatform.commons.file.storage.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "NameSpaceEntity")
@Table(name = "FILES_NAMESPACES")
@NamedQuery(
    name = "nameSpace.getNameSpaceByName",
    query = "SELECT t FROM NameSpaceEntity t WHERE t.name = :name"
)
public class NameSpaceEntity {
  @Id
  @Column(name = "NAMESPACE_ID")
  @SequenceGenerator(name = "SEQ_FILES_NS_NAMESPACE_ID", sequenceName = "SEQ_FILES_NS_NAMESPACE_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_FILES_NS_NAMESPACE_ID")
  private Long                 id;

  @Column(name = "NAME")
  private String               name;

  @Column(name = "DESCRIPTION")
  private String               description;

  @OneToMany(mappedBy = "nameSpaceEntity", fetch = FetchType.LAZY)
  private List<FileInfoEntity> filesInfo;

  public NameSpaceEntity() {
  }

  public NameSpaceEntity(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public NameSpaceEntity(long id, String name, String description) {
    this(name, description);
    this.id = id;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<FileInfoEntity> getFilesInfo() {
    return filesInfo;
  }

  public void setFilesInfo(List<FileInfoEntity> filesInfo) {
    this.filesInfo = filesInfo;
  }
}
