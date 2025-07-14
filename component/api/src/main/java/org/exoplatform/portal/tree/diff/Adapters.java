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
package org.exoplatform.portal.tree.diff;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Adapters {

  /** . */
  @SuppressWarnings("rawtypes")
  private static final ArrayAdapter ARRAY_INSTANCE = new ArrayAdapter();

  @SuppressWarnings("unchecked")
  public static <E> ListAdapter<E[], E> list() {
    return ARRAY_INSTANCE;
  }

  private static class ArrayAdapter<E> implements ListAdapter<E[], E> {
    @Override
    public int size(E[] list) {
      return list.length;
    }

    @Override
    public Iterator<E> iterator(final E[] list, final boolean reverse) {
      return new Iterator<E>() {
        /** . */
        int count = 0;

        @Override
        public boolean hasNext() {
          return count < list.length;
        }

        @Override
        public E next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          int index = count++;
          if (reverse) {
            index = list.length - index - 1;
          }
          return list[index];
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }
}
