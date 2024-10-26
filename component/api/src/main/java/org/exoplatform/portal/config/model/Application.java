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

package org.exoplatform.portal.config.model;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * May 13, 2004
 *
 * @author Tuan Nguyen
 * @version $Id: Portlet.java,v 1.7 2004/09/30 01:00:05 tuan08 Exp $
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Application extends ModelObject implements Cloneable {

  private ApplicationState state;

  private String           id;

  private String           title;

  private String           icon;

  private String           description;

  private boolean          showInfoBar;

  private boolean          showApplicationState = true;

  private boolean          showApplicationMode  = true;

  private String           theme;

  private Properties       properties;

  private String[]         accessPermissions;

  public Application(ApplicationData data) {
    super(data.getStorageId());

    // For now here, need to make a real NAME and
    // remove disturbing storage name
    this.storageName = data.getStorageName();

    //
    this.state = data.getState();
    this.id = data.getId();
    this.title = data.getTitle();
    this.icon = data.getIcon();
    this.description = data.getDescription();
    this.showInfoBar = data.isShowInfoBar();
    this.showApplicationState = data.isShowApplicationState();
    this.showApplicationMode = data.isShowApplicationMode();
    this.theme = data.getTheme();
    this.width = data.getWidth();
    this.height = data.getHeight();
    this.cssClass = data.getCssClass();
    this.cssStyle = data.getCssStyle();
    this.properties = new Properties(data.getProperties());
    this.accessPermissions = data.getAccessPermissions().toArray(new String[data.getAccessPermissions().size()]);
  }

  public Application(String storageId) {
    super(storageId);
  }

  public String getProfiles() {
    if (properties != null && properties.containsKey(MappedAttributes.PROFILES.getName())) {
      return properties.get(MappedAttributes.PROFILES.getName());
    }
    return null;
  }

  public boolean getShowInfoBar() {
    return showInfoBar;
  }

  public void setShowInfoBar(boolean b) {
    showInfoBar = b;
  }

  public boolean getShowApplicationState() {
    return showApplicationState;
  }

  public void setShowApplicationState(boolean b) {
    showApplicationState = b;
  }

  public boolean getShowApplicationMode() {
    return showApplicationMode;
  }

  public void setShowApplicationMode(boolean b) {
    showApplicationMode = b;
  }

  public Properties getProperties() {
    if (properties == null) {
      properties = new Properties();
    }
    return properties;
  }

  @Override
  public String getCssClass() {
    if (cssClass == null && cssStyle == null) {
      return null;
    } else if (cssStyle == null) {
      return cssClass;
    } else if (cssClass == null) {
      return cssStyle.getCssClass();
    } else {
      StringBuilder cssClasses = new StringBuilder();
      cssClasses.append(cssStyle.getCssClass(cssClass));
      cssClasses.append(" ");
      cssClasses.append(cssClass);
      return cssClasses.toString();
    }
  }

  @Override
  public ApplicationData build() {
    return new ApplicationData(getStorageId(),
                               getStorageName(),
                               getState(),
                               getId(),
                               getTitle(),
                               getIcon(),
                               getDescription(),
                               getShowInfoBar(),
                               getShowApplicationState(),
                               getShowApplicationMode(),
                               getTheme(),
                               getWidth(),
                               getHeight(),
                               getCssClass(),
                               getCssStyle(),
                               Utils.safeImmutableMap(properties),
                               Utils.safeImmutableList(accessPermissions));
  }

  public static Application createPortletApplication(ApplicationData data) {
    return new Application(data);
  }

  public static Application createPortletApplication(String storageId) {
    return new Application(storageId);
  }

  public static Application createPortletApplication() {
    return new Application();
  }

  @Override
  public void checkStorage() throws ObjectNotFoundException {
    if (!(this.state instanceof TransientApplicationState)) {
      LayoutService layoutService = ExoContainerContext.getService(LayoutService.class);
      String contentId = layoutService.getId(this.state);
      if (contentId == null) {
        throw new ObjectNotFoundException(String.format("Application not found with state %s",
                                                        this.state));
      }
    }
  }

  @Override
  public void resetStorage() throws ObjectNotFoundException {
    if (!(this.state instanceof TransientApplicationState)) {
      LayoutService layoutService = ExoContainerContext.getService(LayoutService.class);
      Portlet preferences = layoutService.load(this.state);
      String contentId = layoutService.getId(this.state);
      if (contentId == null) {
        throw new ObjectNotFoundException(String.format("Application wasn't found with state %s", this.state));
      } else {
        this.state = new TransientApplicationState(contentId, preferences);
        // No other application type is supported
      }
    }
    super.resetStorage();
  }

  @Override
  public Application clone() { // NOSONAR
    return new Application(build());
  }

}
