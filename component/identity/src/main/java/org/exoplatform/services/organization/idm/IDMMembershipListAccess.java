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

import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.LogLevel;
import org.picketlink.idm.api.*;
import org.picketlink.idm.impl.api.IdentitySearchCriteriaImpl;
import org.picketlink.idm.impl.api.model.SimpleRole;
import org.picketlink.idm.impl.api.model.SimpleRoleType;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;

public class IDMMembershipListAccess implements ListAccess<Membership>, Serializable {
    private static final long serialVersionUID = 6908892334798859546L;

    private static Log                      log                       = ExoLogger.getLogger(IDMMembershipListAccess.class);

    private final Group group;

    private String groupId;

    private final org.picketlink.idm.api.User user;

    private int size = -1;

    private final boolean usePaginatedQuery;

    // List of all requested roles for given user or role. This field is used only if we skip pagination
    private List<Role> fullResults;

    private String associationMembershipType = null;

    private int rolesCount = 0;

    private List<User> associatedUsers = null;

    public IDMMembershipListAccess(Group group, boolean usePaginatedQuery) {
        this.group = group;
        this.user = null;
        this.usePaginatedQuery = usePaginatedQuery;
        this.associationMembershipType = getOrganizationService().getConfiguration().getAssociationMembershipType();
    }

    public IDMMembershipListAccess(Group group, String groupId, boolean usePaginatedQuery) {
        this.group = group;
        this.user = null;
        this.usePaginatedQuery = usePaginatedQuery;
        this.groupId = groupId;
        this.associationMembershipType = getOrganizationService().getConfiguration().getAssociationMembershipType();
    }

    public IDMMembershipListAccess(org.picketlink.idm.api.User user, boolean usePaginatedQuery) {
        this.group = null;
        this.user = user;
        this.usePaginatedQuery = usePaginatedQuery;
        this.associationMembershipType = getOrganizationService().getConfiguration().getAssociationMembershipType();
    }

    public Membership[] load(int index, int length) throws Exception {
        if (log.isTraceEnabled()) {
            Tools.logMethodIn(log, LogLevel.TRACE, "load", new Object[] { "index", index, "length", length });
        }

        if(size < -1) {
          // Load lists size
          getSize();
        }

        List<Role> roles = null;

        if (fullResults != null) {
            // If we already have fullResults (all pages) we can simply sublist them
            int toIndex = (index + length > fullResults.size()) ? fullResults.size() : index + length;
            roles = fullResults.subList(index, toIndex);
        } else {
          if (group != null) {
            if (isMembershipTypeNotUsed() && associatedUsers != null && !associatedUsers.isEmpty()) {
              int startAssociatedUsersIndex = 0;
              int associatedUsersLength = length;
              if (rolesCount > 0) {
                int startRolesIndex = index;
                int rolesLength = length;
    
                if ((index + length) <= rolesCount) {
                  associatedUsersLength = 0;
                } else if (index < rolesCount && (index + length) > rolesCount) {
                  rolesLength = rolesCount - index;
                  startAssociatedUsersIndex = 0;
                  associatedUsersLength = length - rolesLength;
                } else if (index >= rolesCount) {
                  rolesLength = 0;
                  startAssociatedUsersIndex = index - rolesCount;
                  associatedUsersLength = length;
                }
    
                if (rolesLength > 0) {
                  // Decide if use paginated query or skip pagination and obtain
                  // full results
                  IdentitySearchCriteria crit =
                                              usePaginatedQuery ? new IdentitySearchCriteriaImpl().page(startRolesIndex, rolesLength)
                                                                : new IdentitySearchCriteriaImpl().page(0, size);
                  crit.sort(SortOrder.ASCENDING);
                  roles = new LinkedList<Role>(getIDMService().getIdentitySession().getRoleManager().findRoles(group, null, crit));
                }
              }
              if (roles == null) {
                roles = new LinkedList<>();
              }
              if (associatedUsersLength > 0) {
                List<User> subList = associatedUsers.subList(startAssociatedUsersIndex,
                                                             startAssociatedUsersIndex + associatedUsersLength);
                for (User user : subList) {
                  roles.add(new SimpleRole(new SimpleRoleType("JBOSS_IDENTITY_MEMBERSHIP"), user, group));
                }
              }
            } else {
              IdentitySearchCriteria crit = usePaginatedQuery ? new IdentitySearchCriteriaImpl().page(index, length)
                                                              : new IdentitySearchCriteriaImpl().page(0, rolesCount);
              crit.sort(SortOrder.ASCENDING);
              roles = new LinkedList<Role>(getIDMService().getIdentitySession().getRoleManager().findRoles(group, null, crit));
            }
          } else if (user != null) {
            // Decide if use paginated query or skip pagination and obtain full
            // results
            IdentitySearchCriteria crit = usePaginatedQuery ? new IdentitySearchCriteriaImpl().page(index, length)
                                                            : new IdentitySearchCriteriaImpl().page(0, size);
            crit.sort(SortOrder.ASCENDING);
            roles = new LinkedList<Role>(getIDMService().getIdentitySession().getRoleManager().findRoles(user, null, crit));
          }
    
          // If pagination wasn't used, we have all roles and we can save them for
          // future
          if (!usePaginatedQuery) {
            fullResults = roles;
            int toIndex = (index + length > fullResults.size()) ? fullResults.size() : index + length;
            roles = fullResults.subList(index, toIndex);
          }
        }

        Set<Membership> memberships = new HashSet<>();


        for (int i = 0; i <roles.size(); i++) {

            Role role = roles.get(i);

            org.exoplatform.services.organization.Group exoGroup = ((GroupDAOImpl) getOrganizationService().getGroupHandler())
                    .convertGroup(role.getGroup());

            MembershipImpl memb = new MembershipImpl();
            memb.setGroupId(exoGroup.getId());
            memb.setUserName(role.getUser().getId());

            // LDAP store may return raw membership type as role type
            if (role.getRoleType().getName().equals("JBOSS_IDENTITY_MEMBERSHIP")) {
                memb.setMembershipType(this.associationMembershipType);
            } else {
                memb.setMembershipType(role.getRoleType().getName());
            }

            memberships.add(memb);
        }

        if (log.isTraceEnabled()) {
            Tools.logMethodOut(log, LogLevel.TRACE, "load", memberships);
        }

        return memberships.toArray(new Membership[0]);
    }

    public int getSize() throws Exception {
        if (log.isTraceEnabled()) {
            Tools.logMethodIn(log, LogLevel.TRACE, "getSize", null);
        }

        int result = 0;

        if (size < 0) {
            if (group != null && user == null) {
                result = rolesCount = getIDMService().getIdentitySession().getRoleManager().getRolesCount(group, null, null);
                if (isMembershipTypeNotUsed()) {
                  Collection<User> associatedUsersCollection = getIDMService().getIdentitySession()
                                                                    .getRelationshipManager()
                                                                    .findAssociatedUsers(group, false);
                  associatedUsers = associatedUsersCollection == null ? Collections.emptyList() : new LinkedList<>(associatedUsersCollection);
                  result += associatedUsers == null ? 0 : associatedUsers.size();
                }
            } else if (group == null && user != null) {
                result = rolesCount = getIDMService().getIdentitySession().getRoleManager().getRolesCount(user, null, null);
            }

            size = result;
        } else {
            result = size;
        }

        if (log.isTraceEnabled()) {
            Tools.logMethodOut(log, LogLevel.TRACE, "getSize", result);
        }

        return result;

    }

    PicketLinkIDMService getIDMService() {
        return ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PicketLinkIDMService.class);
    }

    PicketLinkIDMOrganizationServiceImpl getOrganizationService() {
        return (PicketLinkIDMOrganizationServiceImpl) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(
                OrganizationService.class);
    }
  
    private boolean isMembershipTypeNotUsed() {
      return StringUtils.isNotBlank(groupId) && StringUtils.isNotBlank(associationMembershipType)
          && getOrganizationService().getConfiguration().isIgnoreMappedMembershipTypeForGroup(groupId);
    }

}
