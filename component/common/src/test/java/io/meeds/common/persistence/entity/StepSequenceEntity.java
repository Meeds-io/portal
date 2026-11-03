/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.common.persistence.entity;

import io.meeds.common.persistence.PortableSequence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The same table and sequence as {@link BlockSequenceEntity}, mapped at step 1:
 * the mapping a release before the upgrade carries.
 */
@Data
@NoArgsConstructor
@Entity(name = "StepSequenceItem")
@Table(name = "BLOCK_SEQUENCE_ITEM")
public class StepSequenceEntity {

  @Id
  @PortableSequence(name = "SEQ_BLOCK_SEQUENCE_ITEM_ID")
  @Column(name = "ITEM_ID")
  private Long   id;

  @Column(name = "LABEL")
  private String label;

}
