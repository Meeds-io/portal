/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

public class UserACLTest {

  private static final String ADMINISTRATORS_GROUP     = "/platform/administrators";

  private static final String SITE_MANAGERS            = "manager:/platform/web-contributors";

  private static final String DRAFT_SITE_NAME          = "portal_x_draft_doe_john";

  /**
   * john's draft of the site {@code team_draft}, which is also the name of
   * draft_john's draft of the site {@code team}
   */
  private static final String AMBIGUOUS_DRAFT_SITE_NAME = "portal_team_draft_draft_john";

  private OrganizationService organizationService;

  private UserHandler         userHandler;

  private UserACL             userAcl;

  @Before
  public void setUp() throws Exception {
    InitParams params = new InitParams();
    params.addParameter(valueParam("portal.administrator.groups", ADMINISTRATORS_GROUP));
    params.addParameter(valueParam("portal.administrator.mstype", "manager"));
    userAcl = spy(new UserACL(params));
    organizationService = mock(OrganizationService.class);
    userHandler = mock(UserHandler.class);
    doReturn(organizationService).when(userAcl).getOrganizationService();
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    existingUsers("doe_john", "john");
  }

  @Test
  public void shouldGrantDraftSiteToItsOwner() {
    Identity owner = identity("doe_john");

    assertTrue(userAcl.hasAccessPermission(owner, PortalConfig.DRAFT, DRAFT_SITE_NAME, Collections.emptyList()));
    assertTrue(userAcl.hasEditPermission(owner, PortalConfig.DRAFT, DRAFT_SITE_NAME, null));
    assertTrue(userAcl.hasAccessPermission(draftSite(DRAFT_SITE_NAME), owner));
    assertTrue(userAcl.hasEditPermission(draftSite(DRAFT_SITE_NAME), owner));
    verifyNoInteractions(organizationService);
  }

  @Test
  public void shouldRefuseDraftSiteToUserWhoseNameEndsTheOwnerName() {
    Identity john = identity("john");

    assertFalse(userAcl.hasAccessPermission(john, PortalConfig.DRAFT, DRAFT_SITE_NAME, Collections.emptyList()));
    assertFalse(userAcl.hasEditPermission(john, PortalConfig.DRAFT, DRAFT_SITE_NAME, null));
    assertFalse(userAcl.hasAccessPermission(draftSite(DRAFT_SITE_NAME), john));
    assertFalse(userAcl.hasEditPermission(draftSite(DRAFT_SITE_NAME), john));
  }

  /**
   * With its owner deleted, the draft has no other existing candidate: the
   * identity must still be a candidate itself.
   */
  @Test
  public void shouldRefuseDraftSiteOfDeletedOwnerToUserWhoseNameEndsTheOwnerName() throws Exception {
    when(userHandler.findUserByName("doe_john", UserStatus.ANY)).thenReturn(null);
    Identity john = identity("john");

    assertFalse(userAcl.hasAccessPermission(john, PortalConfig.DRAFT, DRAFT_SITE_NAME, Collections.emptyList()));
    assertFalse(userAcl.hasEditPermission(john, PortalConfig.DRAFT, DRAFT_SITE_NAME, null));
  }

  /**
   * The draft keeps the access and edit permissions of the site it was cloned
   * from: a viewer or a manager of that site who is not the draft owner is
   * refused all the same.
   */
  @Test
  public void shouldRefuseDraftSiteToSiteManagerWhoIsNotItsOwner() {
    Identity manager = new Identity("doe", List.of(new MembershipEntry("/platform/web-contributors", "manager")));

    assertFalse(userAcl.hasAccessPermission(draftSite(DRAFT_SITE_NAME), manager));
    assertFalse(userAcl.hasEditPermission(draftSite(DRAFT_SITE_NAME), manager));
  }

  @Test
  public void shouldGrantAmbiguousDraftSiteToItsOnlyExistingCandidate() {
    Identity john = identity("john");

    assertTrue(userAcl.hasAccessPermission(john, PortalConfig.DRAFT, AMBIGUOUS_DRAFT_SITE_NAME, Collections.emptyList()));
    assertTrue(userAcl.hasEditPermission(john, PortalConfig.DRAFT, AMBIGUOUS_DRAFT_SITE_NAME, null));
  }

  @Test
  public void shouldRefuseAmbiguousDraftSiteToEveryCandidateWhenAnotherOneExists() throws Exception {
    existingUsers("john", "draft_john");
    Identity john = identity("john");
    Identity draftJohn = identity("draft_john");

    assertFalse(userAcl.hasAccessPermission(draftJohn, PortalConfig.DRAFT, AMBIGUOUS_DRAFT_SITE_NAME, Collections.emptyList()));
    assertFalse(userAcl.hasEditPermission(draftJohn, PortalConfig.DRAFT, AMBIGUOUS_DRAFT_SITE_NAME, null));
    assertFalse(userAcl.hasAccessPermission(john, PortalConfig.DRAFT, AMBIGUOUS_DRAFT_SITE_NAME, Collections.emptyList()));
    assertFalse(userAcl.hasEditPermission(john, PortalConfig.DRAFT, AMBIGUOUS_DRAFT_SITE_NAME, null));
  }

  @Test
  public void shouldRefuseAmbiguousDraftSiteWhenAnotherCandidateCannotBeChecked() throws Exception {
    when(userHandler.findUserByName("draft_john", UserStatus.ANY)).thenThrow(new IllegalStateException("store down"));
    Identity john = identity("john");

    assertFalse(userAcl.hasAccessPermission(john, PortalConfig.DRAFT, AMBIGUOUS_DRAFT_SITE_NAME, Collections.emptyList()));
    assertFalse(userAcl.hasEditPermission(john, PortalConfig.DRAFT, AMBIGUOUS_DRAFT_SITE_NAME, null));
  }

  @Test
  public void shouldGrantDraftSiteToAdministrator() {
    Identity administrator = new Identity("admin", List.of(new MembershipEntry(ADMINISTRATORS_GROUP, "manager")));

    assertTrue(userAcl.hasAccessPermission(draftSite(DRAFT_SITE_NAME), administrator));
    assertTrue(userAcl.hasEditPermission(draftSite(DRAFT_SITE_NAME), administrator));
    assertTrue(userAcl.hasEditPermission(draftSite(AMBIGUOUS_DRAFT_SITE_NAME), administrator));
  }

  @Test
  public void shouldRefuseDraftSiteToAnonymous() {
    assertFalse(userAcl.hasAccessPermission(null, PortalConfig.DRAFT, DRAFT_SITE_NAME, Collections.emptyList()));
    assertFalse(userAcl.hasEditPermission(null, PortalConfig.DRAFT, DRAFT_SITE_NAME, null));
  }

  private void existingUsers(String... usernames) throws Exception {
    for (String username : usernames) {
      when(userHandler.findUserByName(username, UserStatus.ANY)).thenReturn(mock(User.class));
    }
  }

  private PortalConfig draftSite(String name) {
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, name);
    draftSite.setAccessPermissions(new String[] { UserACL.EVERYONE });
    draftSite.setEditPermission(SITE_MANAGERS);
    return draftSite;
  }

  private Identity identity(String username) {
    return new Identity(username, Collections.emptyList());
  }

  private ValueParam valueParam(String name, String value) {
    ValueParam param = new ValueParam();
    param.setName(name);
    param.setValue(value);
    return param;
  }

}
