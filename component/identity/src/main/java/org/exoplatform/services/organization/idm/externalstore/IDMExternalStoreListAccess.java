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
package org.exoplatform.services.organization.idm.externalstore;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.RoleType;
import org.picketlink.idm.api.query.*;
import org.picketlink.idm.impl.api.IdentitySearchCriteriaImpl;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.externalstore.model.IDMEntityType;
import org.exoplatform.services.organization.idm.GroupDAOImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;

/**
 * A {@link ListAccess} that retrieves a list of User/Group/Role coming from
 * external store only
 */
public class IDMExternalStoreListAccess implements ListAccess<String> {
  private static final Log                  LOG                  = ExoLogger.getLogger(IDMExternalStoreListAccess.class);

  private Class<?>                          entityTypeClass;

  private QueryBuilder                      idmQueryBuilder      = null;

  private PicketLinkIDMService              picketLinkIDMService = null;

  private OrganizationService               organizationService  = null;

  private PicketLinkIDMExternalStoreService externalStoreService = null;

  private IdentitySearchCriteriaImpl        searchCriteria       = null;

  private Set<String>                       externalMappedGroups;

  public IDMExternalStoreListAccess(PicketLinkIDMExternalStoreService externalStoreService,
                                    OrganizationService organizationService,
                                    PicketLinkIDMService picketLinkIDMService,
                                    Class<?> entityType,
                                    QueryBuilder idmQueryBuilder,
                                    IdentitySearchCriteriaImpl searchCriteria,
                                    Set<String> externalMappedGroups) {
    this.organizationService = organizationService;
    this.picketLinkIDMService = picketLinkIDMService;
    this.externalStoreService = externalStoreService;
    this.idmQueryBuilder = idmQueryBuilder;
    this.searchCriteria = searchCriteria;
    this.entityTypeClass = entityType;
    this.externalMappedGroups = externalMappedGroups;
  }

  @Override
  public String[] load(int index, int length) throws Exception {
    String[] result = (String[]) externalStoreService.executeOnExternalStore(() -> {
      IdentitySession identitySession = picketLinkIDMService.getIdentitySession();
      if (IDMEntityType.USER.getClassType().equals(entityTypeClass)) {
        UserQueryBuilder userQueryBuilder = (UserQueryBuilder) idmQueryBuilder;
        UserQuery query = userQueryBuilder.createQuery();
        List<org.picketlink.idm.api.User> idmUsers = identitySession.list(query);
        List<String> users = new ArrayList<>();
        for (org.picketlink.idm.api.User idmUser : idmUsers) {
          users.add(idmUser.getId());
        }
        return users.toArray(new String[0]);
      } else if (IDMEntityType.GROUP.getClassType().equals(entityTypeClass)) {

        List<org.picketlink.idm.api.Group> idmGroups = new ArrayList<>();
        for (String groupType : externalMappedGroups) {
          Collection<org.picketlink.idm.api.Group> idmModifiedGroups = identitySession.getPersistenceManager()
                                                                                      .findGroup(groupType, searchCriteria);
          if (idmModifiedGroups != null && !idmModifiedGroups.isEmpty()) {
            idmGroups.addAll(idmModifiedGroups);
          }
        }
        List<String> groups = new ArrayList<>();
        for (org.picketlink.idm.api.Group idmGroup : idmGroups) {
          GroupHandler groupHandler = organizationService.getGroupHandler();
          if (!(groupHandler instanceof GroupDAOImpl)) {
            throw new IllegalStateException("groupHandler class is not recognized :" + groupHandler.getClass());
          }
          try {
            String groupId = ((GroupDAOImpl) groupHandler).getGroupId(idmGroup, null);
            if (StringUtils.isNotBlank(groupId)) {
              groups.add(groupId);
            }
          } catch (Exception e) {
            LOG.error("An error occurred while converting PLIDM group " + idmGroup + " to Exo Group", e);
          }
        }
        return groups.toArray(new String[0]);
      } else if (IDMEntityType.ROLE.getClassType().equals(entityTypeClass)) {
        Collection<RoleType> roleTypes = identitySession.getRoleManager().findRoleTypes();
        Set<String> roles = new HashSet<>();
        for (RoleType rt : roleTypes) {
          roles.add(rt.getName());
        }
        return roles.toArray(new String[0]);
      } else {
        return null;
      }
    });
    return result;
  }

  @Override
  public int getSize() throws Exception {
    throw new UnsupportedOperationException("No LDAP implementation is provided to get size of query");
  }

}
