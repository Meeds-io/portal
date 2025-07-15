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
package org.exoplatform.portal.pom.spi.portlet;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Portlet implements Iterable<Preference>, Serializable {

  private static final long         serialVersionUID = -4571832137663336152L;

  protected Map<String, Preference> state;

  private Map<String, Preference>   entries;

  public Portlet() {
    this.state = new HashMap<>();
    this.entries = Collections.unmodifiableMap(this.state);
  }

  public Portlet(Map<String, Preference> preferences) {
    this.state = new HashMap<>(preferences);
    this.entries = Collections.unmodifiableMap(this.state);
  }

  public List<String> getValues(String name) {
    Preference entry = entries.get(name);
    return entry != null ? entry.getValues() : null;
  }

  public Portlet setValues(String name, List<String> values) {
    Preference entry = entries.get(name);
    if (entry == null) {
      entry = new Preference(name, values, false);
    } else {
      entry = new Preference(entry.getName(), values, entry.isReadOnly());
    }
    entries.put(name, entry);
    return this;
  }

  public String getValue(String name) {
    Preference entry = entries.get(name);
    return entry != null ? entry.getValue() : null;
  }

  public Portlet setValue(String name, String value) {
    Preference entry = entries.get(name);
    if (entry == null) {
      entry = new Preference(name, value, false);
    } else {
      entry = new Preference(entry.getName(), value, entry.isReadOnly());
    }
    state.put(name, entry);
    return this;
  }

  public Boolean isReadOnly(String name) {
    Preference entry = entries.get(name);
    return entry != null ? entry.isReadOnly() : null;
  }

  public Portlet setReadOnly(String name, boolean readOnly) {
    Preference entry = entries.get(name);
    if (entry == null) {
      throw new IllegalStateException();
    }
    entry = new Preference(entry.getName(), entry.getValues(), readOnly);
    state.put(name, entry);
    return this;
  }

  public Preference getPreference(String name) {
    return entries.get(name);
  }

  public Portlet putPreference(Preference preference) {
    state.put(preference.getName(), preference);
    return this;
  }

  public Iterator<Preference> iterator() {
    return entries.values().iterator();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Portlet that) {
      return state.equals(that.state);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return state.hashCode();
  }

  @Override
  public String toString() {
    return "Preferences[state=" + state.toString() + "]";
  }
}
