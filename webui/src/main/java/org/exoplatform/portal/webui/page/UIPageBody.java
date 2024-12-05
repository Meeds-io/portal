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

package org.exoplatform.portal.webui.page;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;

/**
 * May 19, 2006
 */
@ComponentConfig(template = "system:/groovy/portal/webui/page/UIPageBody.gtmpl")
public class UIPageBody extends UIComponentDecorator {

  private String storageId;

  public UIPageBody() {
    setId("UIPageBody");
  }

  public String getStorageId() {
    return storageId;
  }

  public void setStorageId(String storageId) {
    this.storageId = storageId;
  }

  public String getPageName() {
    UIPage uiPage = getUiPage();
    return uiPage == null ? null : uiPage.getName();
  }

  @Override
  public UIComponent getUIComponent() {
    return getUiPage();
  }

  @Override
  protected void setChildComponent(UIComponent uicomponent) {
    PortalRequestContext.getCurrentInstance().setUiPage((UIPage) uicomponent);
  }

  private UIPage getUiPage() {
    return PortalRequestContext.getCurrentInstance().getUiPage();
  }

}
