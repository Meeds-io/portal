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

import java.util.List;

import org.exoplatform.portal.config.model.Page;

import lombok.Data;

@Data
public class PageContext {

  /** . */
  final PageKey key;

  /** The new state if any. */
  PageState     state;

  /** A data snapshot. */
  PageData      data;

  PageContext(PageData data) {
    this.key = data.key;
    this.state = null;
    this.data = data;
  }

  public PageContext(PageKey key, PageState state) {
    this.key = key;
    this.state = state;
    this.data = null;
  }

  /**
   * Returns the navigation key.
   *
   * @return the navigation key
   */
  public PageKey getKey() {
    return key;
  }

  /**
   * Returns the navigation state.
   *
   * @return the navigation state
   */
  public PageState getState() {
    if (state != null) {
      return state;
    } else if (data != null) {
      return data.state;
    } else {
      return null;
    }
  }

  /**
   * Updates the page state the behavior is not the same wether or not the page
   * is persistent: When the page is persistent, any state is allowed:
   * <ul>
   * <li>A non null state overrides the current persistent state.</li>
   * <li>The null state means to reset the state to the persistent state.</li>
   * <li>When the page is transient, only a non null state is allowed as it will
   * be used for creation purpose.</li>
   * </ul>
   *
   * @param state the new state
   * @throws IllegalStateException when the state is cleared and the navigation
   *           is not persistent
   */
  public void setState(PageState state) throws IllegalStateException {
    if (data == null && state == null) {
      throw new IllegalStateException("Cannot clear state on a transient page");
    }
    this.state = state;
  }

  public void update(Page page) throws NullPointerException {
    if (page == null) {
      throw new NullPointerException();
    }
    PageState s = getState();
    page.setTitle(s.getDisplayName());
    page.setDescription(s.getDescription());
    page.setFactoryId(s.getFactoryId());
    page.setShowMaxWindow(s.isShowMaxWindow());
    page.setHideSharedLayout(s.isHideSharedLayout());
    page.setShowSharedLayout(s.isShowSharedLayout());
    List<String> permisssions = s.getAccessPermissions();
    page.setAccessPermissions(permisssions == null ? null : permisssions.toArray(new String[permisssions.size()]));
    page.setEditPermission(s.getEditPermission());
  }
}
