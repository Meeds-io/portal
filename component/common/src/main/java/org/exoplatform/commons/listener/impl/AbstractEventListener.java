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
package org.exoplatform.commons.listener.impl;

import org.exoplatform.commons.api.event.EventListener;
import org.exoplatform.services.listener.Listener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 21, 2012
 * 1:45:40 PM  
 */

/**
 * An abstract listener to be extended by the dedicated listeners.
 * @param <S> This is a generic object of source, it can be a File/Folder/Content or something else 
 * which should be extended from <code>BaseObject</code>.
 * @param <D> This is a generic object of data. It can be an event type such as NODE_ADDED/PROPERTY_CHANGED/NODE_REMOVED
 */
public abstract class AbstractEventListener<S, D> extends Listener<S, D> implements EventListener<S, D> {
}
