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

import static org.picketlink.idm.impl.store.hibernate.PatchedHibernateIdentityStoreImpl.ALL_GROUPS_TYPE;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.LogLevel;
import org.picketlink.idm.api.IdentitySearchCriteria;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.Group;

public class IDMGroupListAccess implements ListAccess<Group>, Serializable {

  private static final long            serialVersionUID = 7072169099411659727L;

  private static final Log             LOG              = ExoLogger.getLogger(IDMGroupListAccess.class);

  private final IdentitySearchCriteria identitySearchCriteria;

  private final GroupDAOImpl           groupDAOImpl;

  private final PicketLinkIDMService   idmService;

  private int                          totalSize        = -1;

  private String                       rootGroupName    = null;

  public IDMGroupListAccess(GroupDAOImpl groupDAOImpl,
                            PicketLinkIDMService idmService,
                            IdentitySearchCriteria identitySearchCriteria) {
    this.groupDAOImpl = groupDAOImpl;
    this.idmService = idmService;
    this.identitySearchCriteria = identitySearchCriteria;
    this.rootGroupName = groupDAOImpl.orgService.getConfiguration().getRootGroupName();
  }

  public Group[] load(int index, int length) throws Exception {
    if (LOG.isTraceEnabled()) {
      Tools.logMethodIn(LOG, LogLevel.TRACE, "load", new Object[] { "index", index, "length", length });
    }
    if (length == 0) {
      return new Group[0];
    }
    // As test suppose, we should throw exception when try to load more element
    // than size
    int size = this.getSize();
    if (index + length > size) {
      throw new IllegalArgumentException("Try to get more than groups can retrieve");
    }
    Collection<org.picketlink.idm.api.Group> groups = listQuery(index, length);
    groups = groups.stream().filter(group -> !group.getName().equals(rootGroupName)).collect(Collectors.toList());
    Group[] exoGroups = new Group[groups.size()];
    int i = 0;
    for (org.picketlink.idm.api.Group group : groups) {
      exoGroups[i++] = groupDAOImpl.convertGroup(group);
    }
    if (LOG.isTraceEnabled()) {
      Tools.logMethodOut(LOG, LogLevel.TRACE, "load", exoGroups);
    }
    return exoGroups;
  }

  public int getSize() throws Exception {
    if (LOG.isTraceEnabled()) {
      Tools.logMethodIn(LOG, LogLevel.TRACE, "getSize", null);
    }

    if (totalSize > -1) {
      return totalSize;
    }
    totalSize = idmService.getIdentitySession().getPersistenceManager().findGroup(ALL_GROUPS_TYPE, identitySearchCriteria).size();
    // should check count
    if (LOG.isTraceEnabled()) {
      Tools.logMethodOut(LOG, LogLevel.TRACE, "getSize", totalSize);
    }

    return totalSize;
  }

  private List<org.picketlink.idm.api.Group> listQuery(int index, int length) throws Exception {
    identitySearchCriteria.page(index, length);
    return (List<org.picketlink.idm.api.Group>) idmService.getIdentitySession()
                                                          .getPersistenceManager()
                                                          .findGroup(ALL_GROUPS_TYPE, identitySearchCriteria);
  }
}
