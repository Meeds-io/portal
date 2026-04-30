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
package org.exoplatform.services.organization.idm;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import org.picketlink.idm.api.Attribute;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;

public class EntityMapperUtils {

  private static final Log       LOG                  = ExoLogger.getLogger(EntityMapperUtils.class);

  public static final String     USER_PASSWORD        = "password";

  public static final String     USER_FIRST_NAME      = "firstName";

  public static final String     USER_LAST_NAME       = "lastName";

  public static final String     USER_DISPLAY_NAME    = "displayName";

  public static final String     USER_EMAIL           = "email";

  public static final String     USER_CREATED_DATE    = "createdDate";

  public static final String     USER_LAST_LOGIN_TIME = "lastLoginTime";

  public static final String     USER_ORGANIZATION_ID = "organizationId";

  public static final String     USER_ENABLED         = "enabled";

  public static final String     ORIGINATING_STORE    = "store";

  public static final DateFormat dateFormat           = DateFormat.getInstance();

  public static void populateUser(User user, Map<String, Attribute> attrs) {
    populateUser(user, attrs, false);
  }

  @SuppressWarnings("deprecation")
  public static boolean populateUser(User user, Map<String, Attribute> attrs, boolean checkIfChanged) {
    if (user == null) {
      throw new IllegalArgumentException("User entity is mandatory");
    }
    if (attrs == null) {
      throw new IllegalStateException("Attributes map is mandatory");
    }
    boolean changed = false;
    if (attrs.containsKey(USER_CREATED_DATE) && attrs.get(USER_CREATED_DATE) != null && attrs.get(USER_CREATED_DATE).getValue() != null) {
      Date origCreatedDate = user.getCreatedDate();
      Date createdDate = null;
      try {
        long date = Long.parseLong(attrs.get(USER_CREATED_DATE).getValue().toString());
        createdDate = new Date(date);
        user.setCreatedDate(createdDate);
      } catch (NumberFormatException e) {
        // For backward compatibility with GateIn 3.0 and EPP 5 Beta
        try {
          createdDate = dateFormat.parse(attrs.get(USER_CREATED_DATE).getValue().toString());
          user.setCreatedDate(createdDate);
        } catch (ParseException e2) {
          LOG.error("Cannot parse the creation date for: " + user.getUserName());
        }
      }
      changed |= checkIfChanged && !Objects.equals(createdDate, origCreatedDate);
    }
    if (attrs.containsKey(USER_EMAIL) && attrs.get(USER_EMAIL) != null && attrs.get(USER_EMAIL).getValue() != null) {
      String email = attrs.get(USER_EMAIL).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(email, user.getEmail());
      user.setEmail(email);
    }
    if (attrs.containsKey(USER_FIRST_NAME) && attrs.get(USER_FIRST_NAME) != null && attrs.get(USER_FIRST_NAME).getValue() != null) {
      String firstName = attrs.get(USER_FIRST_NAME).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(firstName, user.getFirstName());
      user.setFirstName(firstName);
    }
    if (attrs.containsKey(USER_LAST_LOGIN_TIME)) {
      Date originalLastLoginDate = user.getLastLoginTime();
      Date lastLoginDate = null;
      try {
        Long lastLoginMillis = null;
        Attribute lastLoginAttr = attrs.get(USER_LAST_LOGIN_TIME);
        if (lastLoginAttr != null) {
          Object lastLoginValue = lastLoginAttr.getValue();
          if (lastLoginValue != null) {
            lastLoginMillis = Long.parseLong(lastLoginValue.toString());
          }
        }
        if (lastLoginMillis != null) {
          lastLoginDate = new Date(lastLoginMillis);
          user.setLastLoginTime(lastLoginDate);
        }
      } catch (NumberFormatException e) {
        // For backward compatibility with GateIn 3.0 and EPP 5 Beta
        try {
          lastLoginDate = dateFormat.parse(attrs.get(USER_LAST_LOGIN_TIME).getValue().toString());
          user.setLastLoginTime(lastLoginDate);
        } catch (ParseException e2) {
          LOG.error("Cannot parse the last login date for: " + user.getUserName());
        }
      }
      changed |= checkIfChanged && !Objects.equals(originalLastLoginDate, lastLoginDate);
    }
    if (attrs.containsKey(USER_LAST_NAME) && attrs.get(USER_LAST_NAME) != null && attrs.get(USER_LAST_NAME).getValue() != null) {
      String lastName = attrs.get(USER_LAST_NAME).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(lastName, user.getLastName());
      user.setLastName(lastName);
    }
    if (attrs.containsKey(USER_DISPLAY_NAME) && attrs.get(USER_DISPLAY_NAME) != null && attrs.get(USER_DISPLAY_NAME).getValue() != null) {
      // TODO: GTNPORTAL-2358 Change once displayName will be available as
      // part of Organization API
      String fullName = attrs.get(USER_DISPLAY_NAME).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(fullName, user.getDisplayName());
      user.setDisplayName(fullName);
    }
    if (attrs.containsKey(USER_ORGANIZATION_ID) && attrs.get(USER_ORGANIZATION_ID) != null && attrs.get(USER_ORGANIZATION_ID).getValue() != null) {
      String organizationId = attrs.get(USER_ORGANIZATION_ID).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(organizationId, user.getOrganizationId());
      user.setOrganizationId(organizationId);
    }

    if (attrs.containsKey(USER_ENABLED) && attrs.get(USER_ENABLED) != null && attrs.get(USER_ENABLED).getValue() != null) {

      // used when populating User from AD ; it returns numbers : 512 = enbaled, 514 = disabled
      String status = attrs.get(USER_ENABLED).getValue().toString();

      // used when populating User from the platform ; it return true or false
      // if it is from AD it always returns false since it is a number .
      // in AD , the (2) value represents a disabled user and  Many UserAccessControl values represent an enabled user like 65536 which represent an enabled user .
      //whose password never expires : this user is detected as disabled if he has 65538 as UserAccessControl value   */
      // UserAccessControl Documentation Link : https://docs.microsoft.com/en-us/troubleshoot/windows-server/identity/useraccountcontrol-manipulate-account-properties
      Boolean enabled = Boolean.parseBoolean(attrs.get(USER_ENABLED).getValue().toString()) ;

      if (isInteger(status))
      {
        enabled = isUserEnabled(Integer.parseInt(status));
      }
      changed |= checkIfChanged && !Objects.equals(enabled, user.isEnabled());

      if(!Objects.equals(enabled, user.isEnabled())) {
        ((UserImpl) user).setEnabled(enabled);
      }
    }
    if (user instanceof UserImpl && attrs.containsKey(ORIGINATING_STORE) && attrs.get(ORIGINATING_STORE).getValue() != null && attrs.get(ORIGINATING_STORE).getValue().toString() != null) {
      UserImpl userImpl = (UserImpl) user;
      userImpl.setOriginatingStore(attrs.get(ORIGINATING_STORE).getValue().toString());
    }
    return changed;
  }

  private static Boolean isUserEnabled (int status) {
    String binaryStatus = Integer.toBinaryString(status);
    if(binaryStatus.charAt(binaryStatus.length()-2) == '1') {
      return false;
    }
    return true;
  }

  private static boolean isInteger( String input ) {
    try {
      Integer.parseInt( input );
      return true;
    }
    catch( Exception e ) {
      return false;
    }
  }
}
