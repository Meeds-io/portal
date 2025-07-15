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
package io.meeds.portal.permlink.plugin;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;

import io.meeds.portal.permlink.model.PermanentLinkObject;

/**
 * A plugin to generate and parse permanent link for one or multiple given
 * object type(s)
 */
public interface PermanentLinkPlugin {

  /**
   * return supported object type. This can be by example of type: space,
   * application, profile, program, action, realization, activity, news, notes,
   * kudos...
   */
  String getObjectType();

  /**
   * @param object {@link PermanentLinkObject}
   * @param identity {@link Identity} accessing the object
   * @return true if can access else false
   * @throws ObjectNotFoundException when the designated object doesn't exists
   */
  boolean canAccess(PermanentLinkObject object, Identity identity) throws ObjectNotFoundException;

  /**
   * @param object {@link PermanentLinkObject} containing the object Type and Id
   *          with additional parameters when relevant to build a full URL
   * @return the direct access URL
   * @throws ObjectNotFoundException when the designated object doesn't exists
   */
  String getDirectAccessUrl(PermanentLinkObject object) throws ObjectNotFoundException;

}
