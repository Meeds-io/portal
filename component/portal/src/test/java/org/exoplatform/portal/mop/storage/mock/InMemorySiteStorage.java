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
package org.exoplatform.portal.mop.storage.mock;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.storage.LayoutStorage;
import org.exoplatform.portal.mop.storage.NavigationStorage;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.mop.storage.SiteStorageImpl;
import org.exoplatform.upload.UploadService;

public class InMemorySiteStorage extends SiteStorageImpl {

  private static Status status;

  public InMemorySiteStorage(SettingService settingService,
                             ConfigurationManager confManager,
                             NavigationStorage navigationStorage,
                             PageStorage pageStorage,
                             LayoutStorage layoutStorage,
                             SiteDAO siteDAO,
                             UploadService uploadService,
                             FileService fileService) {
    super(settingService, confManager, navigationStorage, pageStorage, layoutStorage, siteDAO, uploadService, fileService);
  }

  @Override
  public void saveImportStatus(Status status) {
    InMemorySiteStorage.status = status; // NOSONAR
  }

  @Override
  public Status getImportStatus() {
    return status;
  }

}
