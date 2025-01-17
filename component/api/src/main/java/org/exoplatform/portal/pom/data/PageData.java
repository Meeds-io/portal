/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.portal.pom.data;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(force = true)
public class PageData extends ContainerData {

  public static final PageData NULL_OBJECT      = new PageData();

  private static final long    serialVersionUID = 2741613958131096156L;

  /** . */
  private final PageKey        key;

  /** . */
  private final String         editPermission;

  /** . */
  private final boolean        showMaxWindow;

  /** . */
  private final boolean        hideSharedLayout;

  private final boolean        showSharedLayout;

  /** . */
  private final String         type;

  /** . */
  private final String         link;

  public PageData(String storageId, // NOSONAR
                  String id,
                  String name,
                  String icon,
                  String template,
                  String factoryId,
                  String title,
                  String description,
                  String width,
                  String height,
                  String cssClass,
                  String profiles,
                  List<String> accessPermissions,
                  List<ComponentData> children,
                  String ownerType,
                  String ownerId,
                  String editPermission,
                  boolean showMaxWindow,
                  boolean hideSharedLayout,
                  boolean showSharedLayout,
                  String type,
                  String link) {
    super(storageId,
          id,
          name,
          icon,
          template,
          factoryId,
          title,
          description,
          width,
          height,
          cssClass,
          profiles,
          null,
          null,
          accessPermissions,
          children);

    //
    this.key = new PageKey(ownerType, ownerId, name);
    this.editPermission = editPermission;
    this.showMaxWindow = showMaxWindow;
    this.hideSharedLayout = hideSharedLayout;
    this.showSharedLayout = showSharedLayout;
    this.type = type;
    this.link = link;
  }

  public String getType() {
    return type;
  }

  public String getLink() {
    return link;
  }

  public String getOwnerType() {
    return key.getType();
  }

  public String getOwnerId() {
    return key.getId();
  }

  public boolean isNull() {
    return key == null;
  }
}
