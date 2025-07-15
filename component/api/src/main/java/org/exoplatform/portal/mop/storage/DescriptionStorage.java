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
package org.exoplatform.portal.mop.storage;

import java.util.Locale;
import java.util.Map;

public interface DescriptionStorage {

  /**
   * <p>
   * Resolve a description with the <code>locale</code> argument.
   * </p>
   *
   * @param  id                   the object id
   * @param  locale               the locale to resolve
   * @return                      the description
   */
  org.exoplatform.portal.mop.State resolveDescription(String id, Locale locale);

  /**
   * <p>
   * Resolve a description, the <code>locale1</code> argument specifies which
   * locale is relevant for retrieval, the <code>locale2</code> specifies which
   * locale should be defaulted to when the <code>locale1</code> cannot provide
   * any relevant match. The <code>locale2</code> argument is optional.
   * </p>
   * The resolution follows those rules:
   * <ul>
   * <li>The resolution is performed against the locale1.</li>
   * <li>When the locale1 does not resolve and a locale2 is provided, a
   * resolution is performed with locale2.</li>
   * <li>Otherwise null is returned.
   * <li>
   * </ul>
   *
   * @param  id                   the object id
   * @param  locale2              the first locale
   * @param  locale1              the second locale
   * @return                      the description
   */
  org.exoplatform.portal.mop.State resolveDescription(String id, Locale locale2, Locale locale1);

  /**
   * Returns the default description or null if it does not exist.
   *
   * @param  id                   the object id
   * @return                      the description
   */
  org.exoplatform.portal.mop.State getDescription(String id);

  /**
   * Update the default description to the new description or remove it if the
   * description argument is null.
   *
   * @param  id                   the object id
   * @param  description          the new description
   */
  void setDescription(String id, org.exoplatform.portal.mop.State description);

  /**
   * Returns a description for the specified locale argument or null if it does
   * not exist.
   *
   * @param  id                   the object id
   * @param  locale               the locale
   * @return                      the description
   */
  org.exoplatform.portal.mop.State getDescription(String id, Locale locale);

  /**
   * Update the description for the specified locale to the new description or
   * remove it if the description argument is null.
   *
   * @param  id                       the object id
   * @param  locale                   the locale
   * @param  description              the new description
   */
  void setDescription(String id, Locale locale, org.exoplatform.portal.mop.State description);

  /**
   * Returns a map containing all the descriptions of an object or null if the
   * object is not internationalized.
   *
   * @param  id                   the object id
   * @return                      the map the description map
   */
  Map<Locale, org.exoplatform.portal.mop.State> getDescriptions(String id);

  /**
   * Updates the description of the specified object or remove the
   * internationalized characteristic of the object if the description map is
   * null.
   *
   * @param  id                       the object id
   * @param  descriptions             the new descriptions
   */
  void setDescriptions(String id, Map<Locale, org.exoplatform.portal.mop.State> descriptions);

}
