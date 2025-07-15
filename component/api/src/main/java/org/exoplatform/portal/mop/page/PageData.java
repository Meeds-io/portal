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
package org.exoplatform.portal.mop.page;

import java.io.Serializable;

import lombok.Data;

/**
 * An immutable page data class.
 *
 */
@Data
public class PageData implements Serializable {

  private static final long serialVersionUID = -2859289738034643799L;

  /** Useful. */
  static final PageData     EMPTY            = new PageData();

  /** . */
  final PageKey             key;

  /** . */
  final String              id;

  /** . */
  final PageState           state;

  public PageData(PageKey key, String id, PageState state) {
    this.key = key;
    this.id = id;
    this.state = state;
  }

  private PageData() {
    this.key = null;
    this.id = null;
    this.state = null;
  }

  protected Object readResolve() {
    if (key == null && state == null && id == null) {
      return EMPTY;
    } else {
      return this;
    }
  }

}
