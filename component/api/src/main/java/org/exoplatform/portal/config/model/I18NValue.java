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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class I18NValue<V, L extends LocalizedValue<V>> extends ArrayList<L> {

  private static final long serialVersionUID = 1876619356367592499L;

  public static I18NValue create() {
    return new I18NValue();
  }

  public I18NValue() {
  }

  public I18NValue(Collection<? extends L> c) {
    super(c);
  }

  public I18NValue(L... values) {
    super(Arrays.asList(values));
  }

  /**
   * Returns true when the collection contains exactly one localized value and
   * this value does not have a locale associated with it.
   *
   * @return true if the value is simple
   */
  public boolean isSimple() {
    return size() == 1 && get(0).getLang() == null;
  }

  /**
   * Returns true if the collection extended which means that it is not empty
   * and it is not simple.
   *
   * @return true if the value is extended
   */
  public boolean isExtended() {
    return !isEmpty() && !isSimple();
  }

  /**
   * Returns the simple value or null if the collection is empty or extended.
   *
   * @return the simple value
   */
  public V getSimple() {
    if (isSimple()) {
      return get(0).getValue();
    } else {
      return null;
    }
  }

  /**
   * Returns a map of the values or null if the collection is empty. The
   * <code>defaultLocale</code> argument is used as a key when an unqualified
   * value is found, otherwise it is not used
   *
   * @param defaultLocale the default locale
   * @return the extended value map
   */
  public Map<Locale, V> getExtended(Locale defaultLocale) {
    Map<Locale, V> map = Collections.emptyMap();
    L unqualifiedLocalizedValue = null;
    for (L localizedValue : this) {
      if (localizedValue.getLang() != null) {
        if (map.isEmpty()) {
          map = new HashMap<>();
        }
        map.put(localizedValue.getLang(), localizedValue.getValue());
      } else {
        unqualifiedLocalizedValue = localizedValue;
      }
    }
    if (map.isEmpty()) {
      return null; // NOSONAR
    } else {
      if (unqualifiedLocalizedValue != null && !map.containsKey(defaultLocale)) { // NOSONAR
        map.put(defaultLocale, unqualifiedLocalizedValue.getValue());
      }
      return map;
    }
  }
}
