/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

public class UserACL {

  public static final String                       EVERYONE               = "Everyone";

  @Getter
  private String                                   superUser;

  @Getter
  private String                                   guestsGroup;

  @Getter
  private List<String>                             portalCreatorGroups;

  @Getter
  private String                                   makableMT;

  @Getter
  private List<String>                             mandatoryGroups;

  @Getter
  private List<String>                             mandatoryMSTypes;

  @Getter
  @Setter
  private String                                   adminGroups;

  @Getter
  @Setter
  private String                                   adminMSType;

  private Map<String, GroupVisibilityPlugin>       groupVisibilityPlugins = new HashMap<>();

  private Authenticator                            authenticator;

  private IdentityRegistry                         identityRegistry;

  public UserACL(InitParams params) {
    ValuesParam mandatoryGroupsParam = params.getValuesParam("mandatory.groups");
    if (mandatoryGroupsParam != null) {
      mandatoryGroups = mandatoryGroupsParam.getValues();
    } else {
      mandatoryGroups = new ArrayList<>();
    }

    ValuesParam mandatoryMSTypesParam = params.getValuesParam("mandatory.mstypes");
    if (mandatoryMSTypesParam != null)
      mandatoryMSTypes = mandatoryMSTypesParam.getValues();
    else
      mandatoryMSTypes = new ArrayList<>();

    // tam.nguyen get admin group value
    ValueParam adminGroupsParam = params.getValueParam("portal.administrator.groups");
    if (adminGroupsParam != null) {
      setAdminGroups(adminGroupsParam.getValue());
    }

    // tam.nguyen get administrator member type
    ValueParam adminMSTypeParam = params.getValueParam("portal.administrator.mstype");
    if (adminMSTypeParam != null) {
      setAdminMSType(adminMSTypeParam.getValue());
    }

    UserACLMetaData md = new UserACLMetaData(params);
    if (md.getSuperUser() != null) {
      superUser = md.getSuperUser();
    }
    if (superUser == null || superUser.trim().length() == 0) {
      superUser = "root";
    }

    if (md.getGuestsGroups() != null) {
      guestsGroup = md.getGuestsGroups();
    }
    if (guestsGroup == null || guestsGroup.trim().length() < 1) {
      guestsGroup = "/platform/guests";
    }

    if (md.getNavigationCreatorMembershipType() != null) {
      makableMT = md.getNavigationCreatorMembershipType();
    }
    if (makableMT == null || makableMT.trim().length() == 0) {
      makableMT = "owner";
    }

    String allGroups = "";
    if (md.getPortalCreateGroups() != null) {
      allGroups = md.getPortalCreateGroups();
    }
    portalCreatorGroups = defragmentPermission(allGroups);
  }

  public void addGroupVisibilityPlugin(GroupVisibilityPlugin plugin) {
    this.groupVisibilityPlugins.put(plugin.getName(), plugin);
  }

  public boolean hasPermission(Identity identity, Group group, String pluginId) {
    GroupVisibilityPlugin plugin = groupVisibilityPlugins.get(pluginId);
    return plugin == null || plugin.hasPermission(identity, group);
  }

  /**
   * Retrieves the User ACL {@link Identity} from Registry, else build it from
   * {@link OrganizationService} using
   * {@link Authenticator#createIdentity(String)}
   * 
   * @param username
   * @return
   */
  @SneakyThrows
  public Identity getUserIdentity(String username) {
    if (StringUtils.isBlank(username) || IdentityConstants.ANONIM.equals(username)) {
      return null;
    }
    Identity identity = getIdentityRegistry().getIdentity(username);
    if (identity == null) {
      identity = getAuthenticator().createIdentity(username);
      identityRegistry.register(identity);
    }
    return identity;
  }

  /**
   * Checks whether a designated {@link Identity} is the super user of platform
   * or not
   * 
   * @return true if super user, else false
   */
  public boolean isSuperUser(Identity identity) {
    return isSameUser(identity, getSuperUser());
  }

  /**
   * Checks whether a designated {@link Identity} is a super administrator or a
   * member of <strong>manager:/platform/administrators</strong>
   * 
   * @param identity {@link Identity} to check
   * @return true if is an administrator, else false
   */
  public boolean isAdministrator(Identity identity) {
    return isSuperUser(identity) || isMemberOf(identity, getAdminMSType(), getAdminGroups());
  }

  /**
   * Check whether the designated {@link Identity} has permissions to create a
   * new Site of type PORTAL
   * 
   * @param identity {@link Identity}
   * @return true if has site creation permission else false
   */
  public boolean hasCreatePortalPermission(Identity identity) {
    if (isAdministrator(identity)) {
      return true;
    }
    return CollectionUtils.isNotEmpty(getPortalCreatorGroups())
           && getPortalCreatorGroups().stream().anyMatch(expression -> isMemberOf(identity, expression));
  }

  /**
   * Checks whether a designated {@link Identity} has edit permission on
   * designated {@link PortalConfig} or not
   * 
   * @param portalConfig
   * @param identity
   * @return true if have edit permission else false
   */
  public boolean hasEditPermission(PortalConfig portalConfig, Identity identity) {
    return hasEditPermission(identity,
                             portalConfig.getType(),
                             portalConfig.getName(),
                             portalConfig.getEditPermission());
  }

  /**
   * Checks whether a designated {@link Identity} has edit permission on
   * designated {@link Page} or not
   * 
   * @param page
   * @param identity
   * @return true if have edit permission else false
   */
  public boolean hasEditPermission(Page page, Identity identity) {
    return hasEditPermission(identity,
                             page.getOwnerType(),
                             page.getOwnerId(),
                             page.getEditPermission());
  }

  /**
   * Checks whether a designated {@link Identity} has edit permission on
   * designated {@link PageContext} or not
   * 
   * @param pageContext
   * @param identity
   * @return true if have edit permission else false
   */
  public boolean hasEditPermission(PageContext pageContext, Identity identity) {
    return hasEditPermission(identity,
                             pageContext.getKey().getSite().getTypeName(),
                             pageContext.getKey().getSite().getName(),
                             pageContext.getState().getEditPermission());
  }

  /**
   * Checks whether a designated {@link Identity} has access permission on
   * designated Site or not
   * 
   * @param portalConfig
   * @param identity
   * @return true if have access permission else false
   */
  public boolean hasAccessPermission(PortalConfig portalConfig, Identity identity) {
    return hasAccessPermission(identity,
                               portalConfig.getType(),
                               portalConfig.getName(),
                               portalConfig.getAccessPermissions())
           || hasEditPermission(portalConfig, identity);
  }

  /**
   * Checks whether a designated {@link Identity} has access permission on
   * designated Page or not
   * 
   * @param page
   * @param identity
   * @return true if have access permission else false
   */
  public boolean hasAccessPermission(Page page, Identity identity) {
    return hasAccessPermission(identity,
                               page.getOwnerType(),
                               page.getOwnerId(),
                               page.getAccessPermissions())
           || hasEditPermission(page, identity);
  }

  /**
   * Checks whether a designated {@link Identity} has access permission on
   * designated Page or not
   * 
   * @param pageContext
   * @param identity
   * @return true if have access permission else false
   */
  public boolean hasAccessPermission(PageContext pageContext, Identity identity) {
    return hasAccessPermission(identity,
                               pageContext.getKey().getSite().getTypeName(),
                               pageContext.getKey().getSite().getName(),
                               pageContext.getState().getAccessPermissions())
           || hasEditPermission(pageContext, identity);
  }

  /**
   * Checks whether the designated {@link Identity} belongs to a designated
   * group or not
   * 
   * @param identity {@link Identity}
   * @param group groupId
   * @return true if has an associated memberdshipType with this group else
   *         false
   */
  public boolean isUserInGroup(Identity identity, String group) {
    if (identity == null
        || group == null
        || CollectionUtils.isEmpty(identity.getGroups())) {
      return false;
    } else {
      return identity.getGroups().stream().anyMatch(g -> g.equals(group));
    }
  }

  public boolean hasPermission(Identity identity, String[] expressions) {
    return isAdministrator(identity) || Arrays.stream(expressions).anyMatch(expression -> isMemberOf(identity, expression));
  }

  public boolean hasPermission(Identity identity, String expression) {
    return isAdministrator(identity) || isMemberOf(identity, expression);
  }

  public boolean hasPermission(Identity identity, String membershipType, String groupId) {
    return isAdministrator(identity) || isMemberOf(identity, membershipType, groupId);
  }

  /**
   * Checks whether user is member of a groupId or membershipType:groupId
   * 
   * @param identity {@link Identity} to check
   * @param expression permission expression of type groupId or
   *          membershipType:groupId
   * @return true if is member, else false
   */
  public boolean isMemberOf(Identity identity, String expression) {
    if (expression == null) {
      return false;
    }
    String[] temp = expression.split(":");
    String membershipType = temp.length == 2 ? temp[0].trim() : "*";
    String groupId = temp.length == 2 ? temp[1].trim() : expression;
    return isMemberOf(identity, membershipType, groupId);
  }

  public boolean isMemberOf(Identity identity, String membershipType, String groupId) {
    return EVERYONE.equals(groupId)
           || (isGuestsGroup(groupId) && isAnonymousUser(identity))
           || (identity != null && identity.isMemberOf(groupId, membershipType));
  }

  public boolean hasEditPermission(Identity identity, String ownerType, String ownerId, String expression) {
    if (isAdministrator(identity)) {
      return true;
    } else if (PortalConfig.USER_TYPE.equals(ownerType)) {
      return isSameUser(identity, ownerId);
    }
    return isMemberOf(identity, expression);
  }

  public boolean hasAccessPermission(Identity identity,
                                     String ownerType,
                                     String ownerId,
                                     String[] expressions) {
    return hasAccessPermission(identity,
                               ownerType,
                               ownerId,
                               expressions == null ? Stream.empty() : Arrays.stream(expressions));
  }

  public boolean hasAccessPermission(Identity identity,
                                     String ownerType,
                                     String ownerId,
                                     List<String> expressions) {
    return hasAccessPermission(identity,
                               ownerType,
                               ownerId,
                               expressions == null ? Stream.empty() : expressions.stream());
  }

  public boolean hasAccessPermission(Identity identity, String ownerType, String ownerId, Stream<String> expressionsStream) {
    if (isAdministrator(identity)) {
      return true;
    } else if (PortalConfig.USER_TYPE.equals(ownerType)) {
      return isSameUser(identity, ownerId);
    }
    return expressionsStream.anyMatch(expression -> isMemberOf(identity, expression));
  }

  public boolean isGuestsGroup(String groupId) {
    return getGuestsGroup().equals(groupId);
  }

  public boolean isAnonymousUser(Identity identity) {
    return identity == null || isAnonymousUser(identity.getUserId());
  }

  public boolean isAnonymousUser(String username) {
    return StringUtils.isBlank(username) || IdentityConstants.ANONIM.equals(username) || IdentityConstants.SYSTEM.equals(username);
  }

  public Authenticator getAuthenticator() {
    if (authenticator == null) {
      authenticator = ExoContainerContext.getService(Authenticator.class);
    }
    return authenticator;
  }

  public IdentityRegistry getIdentityRegistry() {
    if (identityRegistry == null) {
      identityRegistry = ExoContainerContext.getService(IdentityRegistry.class);
    }
    return identityRegistry;
  }

  private List<String> defragmentPermission(String permission) {
    List<String> result = new ArrayList<>();
    if (permission != null) {
      if (permission.contains(",")) {
        String[] groups = permission.split(",");
        for (String group : groups) {
          result.add(group.trim());
        }
      } else {
        result.add(permission);
      }
    }
    return result;
  }

  private boolean isSameUser(Identity identity, String ownerId) {
    return identity != null && identity.getUserId().equals(ownerId);
  }

  @Data
  public static class Permission implements Serializable {

    private static final long serialVersionUID = -2642107810551203332L;

    private String            name;

    private String            groupId          = "";

    private String            membership       = "";

    private String            expression;

    private boolean           selected         = false;

    public void setPermissionExpression(String exp) {
      if (exp == null || exp.length() == 0) {
        return;
      }
      String[] temp = exp.split(":");
      if (temp.length < 2) {
        return;
      }
      expression = exp;
      membership = temp[0].trim();
      groupId = temp[1].trim();
    }

    public String getValue() {
      if (membership.length() == 0 || groupId.length() == 0) {
        return null;
      }
      return membership + ":" + groupId;
    }
  }
}
