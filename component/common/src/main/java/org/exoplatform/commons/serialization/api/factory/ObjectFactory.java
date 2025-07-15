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
package org.exoplatform.commons.serialization.api.factory;

import java.util.Map;

import org.exoplatform.commons.serialization.model.FieldModel;

/**
 * A factory that creates instance of a type.
 *
 */
public abstract class ObjectFactory<B> {

    /**
     * Instantiate an object based on the provided type. The implementor should take care of configuring the state of the
     * returned objet with the provided state map argument.
     *
     * @param type the type
     * @param state the state
     * @param <S> the parameter type of the sub type of the base type
     * @return the S instance
     * @throws CreateException anything wrong that happened during instance creation
     */
    public abstract <S extends B> S create(Class<S> type, Map<FieldModel<? super S, ?>, ?> state) throws CreateException;

}
