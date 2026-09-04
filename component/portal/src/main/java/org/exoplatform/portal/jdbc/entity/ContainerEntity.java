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
package org.exoplatform.portal.jdbc.entity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity(name = "GateInContainer")
@Table(name = "PORTAL_CONTAINERS")
@NamedQueries({ @NamedQuery(name = "ContainerEntity.findByIds", query = "SELECT c FROM GateInContainer c WHERE c.id in (:ids)") })
public class ContainerEntity extends ComponentEntity implements Serializable {

  private static final long     serialVersionUID = -8045606258160322858L;

  @Id
  @PortableSequence(name = "SEQ_CONTAINER_ID_GENERATOR")
  @Column(name = "ID")
  protected Long                id;

  @Column(name = "WEBUI_ID", length = 200)
  private String                webuiId;

  @Column(name = "NAME", length = 200)
  private String                name;

  @Column(name = "ICON", length = 200)
  private String                icon;

  @Column(name = "TEMPLATE", length = 500)
  private String                template;

  @Column(name = "FACTORY_ID", length = 200)
  private String                factoryId;

  @Column(name = "TITLE", length = 200)
  private String                title;

  @Column(name = "DESCRIPTION", length = 2000)
  private String                description;

  @Column(name = "WIDTH", length = 20)
  private String                width;

  @Column(name = "HEIGHT", length = 20)
  private String                height;

  @Column(name = "PROPERTIES", length = 2000)
  private String                properties       = getJSONString(new JSONObject());

  @Transient
  private List<ComponentEntity> children         = new LinkedList<ComponentEntity>();

  @Column(name = "CONTAINER_BODY", length = 5000)
  private String                containerBody    = getJSONString(new JSONArray());

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getWebuiId() {
    return webuiId;
  }

  public void setWebuiId(String webuiId) {
    this.webuiId = webuiId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String getFactoryId() {
    return factoryId;
  }

  public void setFactoryId(String factoryId) {
    this.factoryId = factoryId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getWidth() {
    return width;
  }

  public void setWidth(String width) {
    this.width = width;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public List<ComponentEntity> getChildren() {
    return children;
  }

  public void setChildren(List<ComponentEntity> children) {
    this.children = children;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public String getContainerBody() {
    return containerBody;
  }

  public void setContainerBody(String containerBody) {
    this.containerBody = containerBody;
  }

  @Override
  public JSONObject toJSON() {
    JSONObject obj = super.toJSON();

    JSONArray jChildren = new JSONArray();
    for (ComponentEntity child : getChildren()) {
      jChildren.add(child.toJSON());
    }
    obj.put("children", jChildren);
    return obj;
  }

  @Override
  public TYPE getType() {
    return TYPE.CONTAINER;
  }

}
