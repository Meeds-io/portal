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
package org.exoplatform.commons.api.settings.data;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;

import java.io.Serializable;

/**
 * Saves some settings linked to a Context.
 */
public class Context implements Cloneable, Serializable {

  private static final long serialVersionUID = -3753327207819586801L;

  /**
   * GLOBAL: Settings should impact all users in the underlying scope.
   */
  public static final Context GLOBAL = new Context("GLOBAL", "GLOBAL");

  /**
   * USER: Each user should be able to save his own settings.
   */
  public static final Context USER   = new Context("USER", null);

  private String              id;

  private String              name;

  public Context(String name, String id) {
    this.id = id;
    this.name = name;
  }

  /**
   * Creates a context with a specified Id.
   * 
   * @param id The Id that is displayed as username.
   */
  public Context id(String id) {
    Context result = null;
    result = this.clone();
    result.id = id;
    return result;
  }

  /**
   * Gets a context Id.
   * 
   * @return Returns "null" if the context is GLOBAL or user Id if the context
   *         is USER.
   */
  public String getId() {
    if (id == null && USER.getName().equals(getName())) {
      ConversationState state = ConversationState.getCurrent();
      String currentId = (state == null) ? null : state.getIdentity().getUserId();
      if (currentId != null
          && (IdentityConstants.SYSTEM.contentEquals(currentId) || IdentityConstants.ANONIM.contentEquals(currentId))) {
        currentId = null;
      }
      return currentId;
    }
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Context that = (Context) o;
    return that.hashCode() == this.hashCode();
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  @Override
  public Context clone() { // NOSONAR
    return new Context(name, id);
  }
}
