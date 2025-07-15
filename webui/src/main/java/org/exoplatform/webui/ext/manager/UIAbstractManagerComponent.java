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
package org.exoplatform.webui.ext.manager;

import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 15 mai 2009  
 */
public abstract class UIAbstractManagerComponent extends UIComponent {

  /**
   * The name of the related UIExtension
   */
  private String uiExtensionName;

  /**
   * The category of the related UIExtension
   */
  private String uiExtensionCategory;    
  
  public String getUIExtensionName() {
    return uiExtensionName;
  }

  public void setUIExtensionName(String uiExtensionName) {
    this.uiExtensionName = uiExtensionName;
  }

  public String getUIExtensionCategory() {
    return uiExtensionCategory;
  }

  public void setUIExtensionCategory(String uiExtensionCategory) {
    this.uiExtensionCategory = uiExtensionCategory;
  }

  /**
   * Gives the class related to manager
   */
  public abstract Class<? extends UIAbstractManager> getUIAbstractManagerClass();
}
