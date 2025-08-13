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
package io.meeds.web.security.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "ApiKey")
@Table(name = "PORTAL_API_KEYS")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyEntity implements Serializable {

  private static final long serialVersionUID = -84331457457586854L;

  @Id
  @SequenceGenerator(name = "SEQ_API_KEYS_ID_GENERATOR", sequenceName = "SEQ_API_KEYS_ID_GENERATOR", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_API_KEYS_ID_GENERATOR")
  @Column(name = "ID")
  private Long              id;

  @Column(name = "USERNAME")
  private String            userName;

  @Column(name = "PASSWORD")
  private String            encryptedPassword;

  @Column(name = "CREATION_DATE")
  @Temporal(TemporalType.TIMESTAMP)
  private Date              creationDate;

}
