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
package io.meeds.spring.module.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "Test")
@Table(name = "TEST")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestJpaEntity implements Serializable {

  private static final long serialVersionUID = -1657319966260461019L;

  @Id
  @SequenceGenerator(name = "TEST_ID_SEQ",
      sequenceName = "TEST_ID_SEQ",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO,
      generator = "TEST_ID_SEQ")
  @Column(name = "TEST_ID")
  private Long              id;

  @Column(name = "TEXT")
  private String            text;

}
