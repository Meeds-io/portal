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
package io.meeds.portal.theme.entity;

import java.io.Serializable;
import java.util.Date;

import io.meeds.common.persistence.PortableSequence;
import io.meeds.portal.theme.constant.ThemeOrigin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A theme. Its three maps (variable to value, as JSON) hold only what an
 * administrator set from the UI: an absent text or icon color is Auto, an
 * absent dark value is Automatic, any other absent value resolves to its
 * configured or built-in default. The Default Theme is the row of id 1.
 */
@Entity(name = "PortalTheme")
@Table(name = "PORTAL_THEMES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThemeEntity implements Serializable {

  private static final long serialVersionUID = -6420153702683151347L;

  @Id
  @PortableSequence(name = "SEQ_PORTAL_THEME_ID")
  @Column(name = "THEME_ID")
  private Long              id;

  @Column(name = "NAME", nullable = false)
  private String            name;

  /** True once an administrator renamed a configured theme: the configuration no longer sets its name. */
  @Column(name = "NAME_SET_IN_UI", nullable = false)
  private boolean           nameSetInUI;

  /** The name lower-cased with Locale.ROOT: the database enforces the case-insensitive uniqueness of names. */
  @Column(name = "NAME_KEY", nullable = false, unique = true)
  private String            nameKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "ORIGIN", nullable = false)
  private ThemeOrigin       origin;

  /** The exo.branding.themes.&lt;key&gt; binding the row, null for a theme added from the UI. */
  @Column(name = "CONFIG_KEY")
  private String            configKey;

  /** JSON snapshot {shared, light, dark} of the configured values of a bound row, null for an unbound one. */
  @Column(name = "CONFIG_SETTINGS")
  private String            configSettings;

  /** JSON map of the settings both versions share. */
  @Column(name = "SHARED_SETTINGS")
  private String            sharedSettings;

  /** JSON map of the light values. */
  @Column(name = "LIGHT_SETTINGS")
  private String            lightSettings;

  /** JSON map of the customized dark values only. */
  @Column(name = "DARK_SETTINGS")
  private String            darkSettings;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "UPDATED_DATE")
  private Date              updatedDate;

}
