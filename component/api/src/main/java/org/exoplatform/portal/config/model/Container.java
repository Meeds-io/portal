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

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.Tools;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.pom.data.*;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Container extends ModelObject implements Cloneable {

  public static final String           EVERYONE                              = "Everyone";

  public static final List<String>     DEFAULT_ACCESS_PERMISSIONS            = Collections.singletonList(EVERYONE);

  public static final List<String>     DEFAULT_MOVE_APPLICATIONS_PERMISSIONS = Collections.singletonList(EVERYONE);

  public static final List<String>     DEFAULT_MOVE_CONTAINERS_PERMISSIONS   = Collections.singletonList(EVERYONE);

  protected String                     id;

  protected String                     name;

  protected String                     icon;

  protected String                     template;

  protected String                     factoryId;

  protected String                     title;

  protected String                     description;

  protected String                     profiles;

  protected String[]                   accessPermissions;

  protected ArrayList<ModelObject>     children;

  @Getter
  @Setter
  protected ApplicationBackgroundStyle appBackgroundStyle;

  public Container() {
    children = new ArrayList<>();
  }

  public Container(String storageId) {
    super(storageId);
    //
    this.children = new ArrayList<>();
  }

  public Container(ContainerData data) {
    super(data.getStorageId());

    //
    this.id = data.getId();
    this.name = data.getName();
    this.icon = data.getIcon();
    this.template = data.getTemplate();
    this.factoryId = data.getFactoryId();
    this.title = data.getTitle();
    this.description = data.getDescription();
    this.width = data.getWidth();
    this.height = data.getHeight();
    this.cssClass = data.getCssClass();
    this.cssStyle = data.getCssStyle();
    this.appBackgroundStyle = data.getAppBackgroundStyle();
    this.profiles = data.getProfiles();
    this.accessPermissions = data.getAccessPermissions().toArray(new String[data.getAccessPermissions().size()]);
    this.children = new ArrayList<>();
    for (ComponentData child : data.getChildren()) {
      ModelObject m = ModelObject.build(child);
      if (m != null) {
        children.add(ModelObject.build(child));
      }
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String s) {
    id = s;
  }

  public String getName() {
    return name;
  }

  public void setName(String s) {
    name = s;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public ArrayList<ModelObject> getChildren() {
    return children;
  }

  public void setChildren(ArrayList<ModelObject> children) {
    this.children = children;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String des) {
    description = des;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getFactoryId() {
    return factoryId;
  }

  public void setFactoryId(String factoryId) {
    this.factoryId = factoryId;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String[] getAccessPermissions() {
    return accessPermissions;
  }

  public void setAccessPermissions(String[] accessPermissions) {
    this.accessPermissions = accessPermissions;
  }

  public String getProfiles() {
    return profiles;
  }

  public void setProfiles(String profiles) {
    this.profiles = profiles;
  }

  @Override
  public ContainerData build() {
    return new ContainerData(getStorageId(),
                             getId(),
                             getName(),
                             getIcon(),
                             getTemplate(),
                             getFactoryId(),
                             getTitle(),
                             getDescription(),
                             getWidth(),
                             getHeight(),
                             getCssClass(),
                             getProfiles(),
                             getCssStyle(),
                             getAppBackgroundStyle(),
                             Utils.safeImmutableList(accessPermissions),
                             buildChildren());
  }

  @Override
  public void checkStorage() throws ObjectNotFoundException {
    if (getChildren() != null && !getChildren().isEmpty()) {
      for (ModelObject child : getChildren()) {
        child.checkStorage();
      }
    }
  }

  @Override
  public void resetStorage() throws ObjectNotFoundException {
    super.resetStorage();
    if (getChildren() != null && !getChildren().isEmpty()) {
      for (ModelObject child : getChildren()) {
        child.resetStorage();
      }
    }
  }

  @Override
  public Container clone() { // NOSONAR
    return new Container(build());
  }

  protected List<ComponentData> buildChildren() { // NOSONAR
    if (StringUtils.isNotBlank(getProfiles())) {
      Set<String> activeProfiles = Tools.parseCommaList(getProfiles());
      if (ExoContainer.getProfiles()
                      .stream()
                      .noneMatch(activeProfiles::contains)) {
        return Collections.emptyList();
      }
    }
    if (getChildren() != null && !getChildren().isEmpty()) {
      ArrayList<ComponentData> dataChildren = new ArrayList<>();
      for (int i = 0; i < children.size(); i++) { // NOSONAR
        ModelObject node = children.get(i);
        if (node instanceof Container container) {
          String nodeProfiles = container.getProfiles();
          if (StringUtils.isNotBlank(nodeProfiles)) {
            Set<String> activeProfiles = Tools.parseCommaList(nodeProfiles);
            if (ExoContainer.getProfiles()
                            .stream()
                            .noneMatch(activeProfiles::contains)) {
              continue;
            }
          }
        } else if (node instanceof Application application) { // NOSONAR
          String nodeProfiles = application.getProfiles();
          if (StringUtils.isNotBlank(nodeProfiles)) {
            Set<String> activeProfiles = Tools.parseCommaList(nodeProfiles);
            if (ExoContainer.getProfiles()
                            .stream()
                            .noneMatch(activeProfiles::contains)) {
              continue;
            }
          }
        }
        ModelData data = node.build();
        ComponentData componentData = (ComponentData) data;
        dataChildren.add(componentData);
      }
      return Collections.unmodifiableList(dataChildren);
    } else {
      return Collections.emptyList();
    }
  }
}
