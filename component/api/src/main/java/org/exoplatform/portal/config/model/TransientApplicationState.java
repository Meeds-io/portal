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
package org.exoplatform.portal.config.model;

import org.exoplatform.portal.pom.spi.portlet.Portlet;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The transient state of an application when it has not yet been stored in the
 * database.
 *
 */
@Data
@NoArgsConstructor
public class TransientApplicationState implements ApplicationState {

  private static final long serialVersionUID = 3687244236805541930L;

  /** The owner type. */
  private String  contentId;

  /** The owner type. */
  private String  ownerType;

  /** The owner id. */
  private String  ownerId;

  /** The content state. */
  private Portlet contentState;

  public TransientApplicationState(String contentId) {
    this.contentId = contentId;
  }

  public TransientApplicationState(String contentId, Portlet contentState) {
    this.contentId = contentId;
    this.contentState = contentState;
  }

  public TransientApplicationState(String contentId, Portlet contentState, String ownerType, String ownerId) {
    this.contentId = contentId;
    this.contentState = contentState;
    this.ownerType = ownerType;
    this.ownerId = ownerId;
  }
}
