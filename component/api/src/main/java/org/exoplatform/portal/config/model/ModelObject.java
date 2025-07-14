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
package org.exoplatform.portal.config.model;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.BodyData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.ModelData;
import org.exoplatform.portal.pom.data.PageData;

import lombok.Data;

@Data
public abstract class ModelObject {

  /** Storage id. */
  protected String     storageId;

  /** The storage name that is unique among a container context. */
  protected String     storageName;

  protected String     width;

  protected String     height;

  protected String     cssClass;

  protected ModelStyle cssStyle;

  protected ModelObject(String storageId) {
    this.storageId = storageId;
  }

  protected ModelObject() {
    this.storageId = null;
  }

  public void checkStorage() throws ObjectNotFoundException {
    // A method to use to check consistency of storage information
  }

  public void resetStorage() throws ObjectNotFoundException {
    this.checkStorage();
    this.storageId = null;
    this.storageName = null;
  }

  public abstract ModelData build();

  public static ModelObject build(ModelData data) {
    if (data instanceof ContainerData containerData) {
      return new Container(containerData);
    } else if (data instanceof PageData pageData) {
      return new Page(pageData);
    } else if (data instanceof BodyData bodyData) {
      switch (bodyData.getType()) {
      case PAGE:
        return new PageBody(bodyData);
      case SITE:
        return new SiteBody(data.getStorageId());
      default:
        throw new AssertionError();
      }
    } else if (data instanceof ApplicationData applicationData) {
      return Application.createPortletApplication(applicationData);
    }
    return null;
  }
}
