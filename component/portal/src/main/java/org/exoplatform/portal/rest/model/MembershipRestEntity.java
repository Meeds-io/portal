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
package org.exoplatform.portal.rest.model;

import org.exoplatform.services.organization.*;

public class MembershipRestEntity {
  private String id;

  private String membershipType;

  private String groupId;

  private String groupLabel;

  private String userName;

  private String fullName;

  private String firstName;

  private String lastName;

  private String email;

  public MembershipRestEntity() {
  }

  public MembershipRestEntity(Membership membership, Group group, User user) {
    this.id = membership.getId();
    this.membershipType = membership.getMembershipType();
    this.groupId = group.getId();
    this.groupLabel = group.getLabel();
    this.userName = user.getUserName();
    this.fullName = user.getDisplayName();
    this.firstName = user.getFirstName();
    this.lastName = user.getLastName();
    this.email = user.getEmail();
  }

  public MembershipRestEntity(String membershipType, Group group, User user) {
    this.id = String.format("%s:%s:%s", membershipType, user.getUserName(), group.getId());
    this.membershipType = membershipType;
    this.groupId = group.getId();
    this.groupLabel = group.getLabel();
    this.userName = user.getUserName();
    this.fullName = user.getDisplayName();
    this.firstName = user.getFirstName();
    this.lastName = user.getLastName();
    this.email = user.getEmail();
  }

  public String getMembershipType() {
    return membershipType;
  }

  public void setMembershipType(String membershipType) {
    this.membershipType = membershipType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getGroupLabel() {
    return groupLabel;
  }

  public void setGroupLabel(String groupLabel) {
    this.groupLabel = groupLabel;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

}
