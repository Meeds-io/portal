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
package io.meeds.portal.theme;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whether the theme services have finished their startup on this node. The
 * Kernel components of the branding look the theme service up only once it
 * is set: before, they answer as the single-theme platform did. Set by the
 * configuration synchroniser, also when one of its steps failed.
 */
public final class ThemeReadiness {

  private static final AtomicBoolean READY = new AtomicBoolean();

  private ThemeReadiness() {
    // static flag
  }

  public static boolean isReady() {
    return READY.get();
  }

  public static void setReady() {
    READY.set(true);
  }

  /** For tests only: back to the startup state. */
  public static void reset() {
    READY.set(false);
  }

}
