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

import org.exoplatform.portal.pom.data.BodyData;
import org.exoplatform.portal.pom.data.BodyType;
import org.exoplatform.portal.pom.data.ModelData;

/**
 * Created by The eXo Platform SAS Apr 25, 2007
 */
public class PageBody extends ModelObject {

  public PageBody(BodyData bodyData) {
    super(bodyData.getStorageId());
    this.storageName = bodyData.getStorageName();
    this.width = bodyData.getWidth();
    this.height = bodyData.getHeight();
    this.cssClass = bodyData.getCssClass();
    this.cssStyle = bodyData.getCssStyle();
  }

  public PageBody(String storageId) {
    super(storageId);
  }

  public PageBody() {
  }

  @Override
  public ModelData build() {
    return new BodyData(storageId,
                        storageName,
                        cssStyle,
                        width,
                        height,
                        cssClass,
                        BodyType.PAGE);
  }
}
