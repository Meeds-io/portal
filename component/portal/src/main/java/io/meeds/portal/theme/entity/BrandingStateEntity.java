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

import io.meeds.portal.theme.constant.ModesOffered;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Everything the Branding and Theme page publishes besides the themes, in one
 * row of id {@link #STATE_ID}, so that one transaction holds a whole
 * publication. {@link #version} is compared by hand under a row lock and
 * stamps the stylesheet: it is deliberately not a JPA {@code @Version}.
 */
@Entity(name = "PortalBrandingState")
@Table(name = "PORTAL_BRANDING_STATE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandingStateEntity implements Serializable {

  public static final long  STATE_ID         = 1L;

  private static final long serialVersionUID = 2286377330212741062L;

  @Id
  @Column(name = "ID")
  private Long              id;

  @Column(name = "VERSION", nullable = false)
  private long              version;

  @Column(name = "LEGACY_IMPORTED", nullable = false)
  private boolean           legacyImported;

  /** Null: the configured exo.branding.company.name applies. */
  @Column(name = "COMPANY_NAME")
  private String            companyName;

  @Column(name = "LOGO_FILE_ID")
  private Long              logoFileId;

  /** Null: the light logo is used in dark mode. */
  @Column(name = "DARK_LOGO_FILE_ID")
  private Long              darkLogoFileId;

  @Column(name = "FAVICON_FILE_ID")
  private Long              faviconFileId;

  /** Null: the light favicon is used in dark mode. */
  @Column(name = "DARK_FAVICON_FILE_ID")
  private Long              darkFaviconFileId;

  /** JSON map file column to the configured path last tried for it. */
  @Column(name = "DEFAULT_FILE_SOURCES")
  private String            defaultFileSources;

  /** Null: the configured exo.branding.themeInUse applies, else the Default Theme. */
  @Column(name = "THEME_IN_USE_ID")
  private Long              themeInUseId;

  /** Null: the configured exo.branding.modesOffered applies. */
  @Enumerated(EnumType.STRING)
  @Column(name = "MODES_OFFERED")
  private ModesOffered      modesOffered;

  /** The theme in use resolved at the last start; no foreign key, a publication may delete it. */
  @Column(name = "LAST_EFFECTIVE_THEME_ID")
  private Long              lastEffectiveThemeId;

  /** The effective modes at the last write or start, to detect a change of the configured value. */
  @Enumerated(EnumType.STRING)
  @Column(name = "LAST_EFFECTIVE_MODES")
  private ModesOffered      lastEffectiveModes;

  /** Incremented when the modes go from a single one back to both: an older stored user choice reads as Automatic. */
  @Column(name = "MODES_GENERATION", nullable = false)
  private int               modesGeneration;

}
