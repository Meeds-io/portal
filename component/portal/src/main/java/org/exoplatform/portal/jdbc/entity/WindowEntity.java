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

import org.json.simple.JSONObject;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity(name = "GateInWindow")
@Table(name = "PORTAL_WINDOWS")
@NamedQuery(name = "WindowEntity.findByIds", query = "SELECT w FROM GateInWindow w WHERE w.id IN (:ids)")
@NamedQuery(name = "WindowEntity.findByContentIds", query = "SELECT w.id FROM GateInWindow w WHERE w.contentId IN (:contentIds)")
@NamedQuery(name = "WindowEntity.updateContentId", query = "UPDATE GateInWindow SET contentId = :newContentId WHERE contentId = :oldContentId")
@NamedQuery(name = "WindowEntity.deleteByContentId", query = "DELETE GateInWindow WHERE contentId = :contentId")
public class WindowEntity extends ComponentEntity implements Serializable {

  private static final long serialVersionUID = 6633792468705838255L;

  @Id
  @PortableSequence(name = "SEQ_WINDOW_ID_GENERATOR")
  @Column(name = "ID")
  protected Long            id;

  @Column(name = "TITLE")
  private String            title;

  @Column(name = "ICON")
  private String            icon;

  @Column(name = "DESCRIPTION")
  private String            description;

  @Column(name = "SHOW_INFO_BAR")
  private boolean           showInfoBar;

  @Column(name = "SHOW_APP_STATE")
  private boolean           showApplicationState;

  @Column(name = "SHOW_APP_MODE")
  private boolean           showApplicationMode;

  @Column(name = "THEME")
  private String            theme;

  @Column(name = "WIDTH")
  private String            width;

  @Column(name = "HEIGHT")
  private String            height;

  @Column(name = "PROPERTIES")
  private String            properties       = getJSONString(new JSONObject());

  @Column(name = "APP_TYPE")
  private AppType           appType;

  @Column(name = "CONTENT_ID")
  private String            contentId;

  @Lob
  @Column(name = "CUSTOMIZATION")
  @Basic(fetch = FetchType.LAZY)
  private byte[]            customization;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isShowInfoBar() {
    return showInfoBar;
  }

  public void setShowInfoBar(boolean showInfoBar) {
    this.showInfoBar = showInfoBar;
  }

  public boolean isShowApplicationState() {
    return showApplicationState;
  }

  public void setShowApplicationState(boolean showApplicationState) {
    this.showApplicationState = showApplicationState;
  }

  public boolean isShowApplicationMode() {
    return showApplicationMode;
  }

  public void setShowApplicationMode(boolean showApplicationMode) {
    this.showApplicationMode = showApplicationMode;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
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

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public AppType getAppType() {
    return appType;
  }

  public void setAppType(AppType appType) {
    this.appType = appType;
  }

  public String getContentId() {
    return contentId;
  }

  public void setContentId(String contentId) {
    this.contentId = contentId;
  }

  public byte[] getCustomization() {
    return customization;
  }

  public void setCustomization(byte[] customization) {
    this.customization = customization;
  }

  @Override
  public TYPE getType() {
    return TYPE.WINDOW;
  }

  public enum AppType {
    PORTLET
  }
}
