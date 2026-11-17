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
package io.meeds.portal.theme.dao;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import io.meeds.portal.theme.entity.ThemeEntity;

/**
 * The themes. Their number stays small (fifty at most, a stated assumption),
 * so the list is read whole, sorted by id.
 */
public interface ThemeDAO extends JpaRepository<ThemeEntity, Long> {

  Sort BY_ID = Sort.by(Sort.Direction.ASC, "id");

  ThemeEntity findByNameKey(String nameKey);

  ThemeEntity findByConfigKey(String configKey);

  List<ThemeEntity> findByConfigKeyNotNull(Sort sort);

}
