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
package org.exoplatform.portal.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.json.JSONObject;

import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;
import org.exoplatform.services.rest.impl.ContainerResponse;

public class MembershipTypeRestResourcesTest extends BaseRestServicesTestCase {

  private static final String MANDATORY_MS_TYPE = "manager";

  private static final String MEMBERSHIP_TYPE_1 = "mt1";

  private static final String MEMBERSHIP_TYPE_2 = "mt2";

  private static final String MEMBERSHIP_TYPE_3 = "mt3";

  private static final String MEMBERSHIP_TYPE_4 = "mt";

  private static final String MEMBERSHIP_TYPE_5 = "aaaaaa aaaaaaaaaaaa aaaaaaaaaaa";

  private static final String MEMBERSHIP_TYPE_6 = "mt6";

  private static final String MEMBERSHIP_TYPE_7 = "mt7";

  protected Class<?> getComponentClass() {
    return MembershipTypeRestResourcesV1.class;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    OrganizationService organizationService = mock(OrganizationService.class);
    MembershipTypeHandler membershipTypeHandler = mock(MembershipTypeHandler.class);

    when(organizationService.getMembershipTypeHandler()).thenReturn(membershipTypeHandler);
    MembershipTypeImpl membershipType1 = new MembershipTypeImpl(MEMBERSHIP_TYPE_1, null, MEMBERSHIP_TYPE_1);
    when(membershipTypeHandler.findMembershipType(MEMBERSHIP_TYPE_1)).thenReturn(membershipType1);

    MembershipTypeImpl membershipType2 = new MembershipTypeImpl(MEMBERSHIP_TYPE_2, null, MEMBERSHIP_TYPE_2);
    when(membershipTypeHandler.findMembershipType(MEMBERSHIP_TYPE_2)).thenReturn(membershipType2);

    when(membershipTypeHandler.findMembershipTypes()).thenReturn(Arrays.asList(membershipType1, membershipType2));

    getContainer().unregisterComponent(OrganizationService.class);
    getContainer().registerComponentInstance("org.exoplatform.services.organization.OrganizationService", organizationService);

    startUserSession("testuser");
  }

  @Override
  public void tearDown() throws Exception {
    getContainer().unregisterComponent("org.exoplatform.services.organization.OrganizationService");
    super.tearDown();
  }

  public void testCreateMembershipType() throws Exception {
    JSONObject data = new JSONObject();

    ContainerResponse response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());

    data.put("name", MEMBERSHIP_TYPE_1);
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());

    data.put("name", "NEW_MEMBERSHIP_TYPE");
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());

    data.put("name", MEMBERSHIP_TYPE_3);
    data.put("description","desc");
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(204, response.getStatus());

    // if the role name < 3
    data.put("name", MEMBERSHIP_TYPE_4);
    data.put("description","desc");
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());

    // if the role name > 30
    data.put("name", MEMBERSHIP_TYPE_5);
    data.put("description","desc");
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());

    // if the role description < 3
    data.put("name", MEMBERSHIP_TYPE_6);
    data.put("description","de");
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());

    // if the role description > 255
    String desc = "aaaaaa aaaaaaaaaaaa aaaaaaaaaa aaaaaaaaaaaaaaa aaaaaaaaaaaaaaa aaaaaaaaaaaa aaaaaaaaaaaaa aaaaaaaaaaaaaaaaa aaaaaaaaaaa aaaaaaaa aaa aa aaa aaa aaaaa aaa aaaa aaaaa aaaaaaaaaa aaaaaaaaaaaa aaaaaaaaaaaaa aaaa aaaaaa aaaa aaaaa aaaaaa aaaaaaaaaaa aaaaaaaaaa a";
    data.put("name", MEMBERSHIP_TYPE_7);
    data.put("description",desc);
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
  }

  public void testUpdateMembershipType() throws Exception {
    JSONObject data = new JSONObject();

    ContainerResponse response = getResponse("PUT", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());

    data.put("name", "NOT_EXISTING");
    response = getResponse("PUT", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    data.put("name", MEMBERSHIP_TYPE_1);
    response = getResponse("PUT", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(204, response.getStatus());
  }

  public void testDeleteMembershipType() throws Exception {
    ContainerResponse response = launcher.service("DELETE", "/v1/membershipTypes/NOT_EXISTING", "", null, null, null);
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    response = launcher.service("DELETE", "/v1/membershipTypes/" + MANDATORY_MS_TYPE, "", null, null, null);
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals("MandatoryMembershipType", response.getEntity());

    response = launcher.service("DELETE", "/v1/membershipTypes/" + MEMBERSHIP_TYPE_1, "", null, null, null);
    assertNotNull(response);
    assertEquals(204, response.getStatus());
  }

  public void testGetMembershipTypes() throws Exception {
    ContainerResponse response = launcher.service("GET", "/v1/membershipTypes", "", null, null, null);
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof Collection);

    @SuppressWarnings("unchecked")
    Collection<MembershipType> membershipTypes = (Collection<MembershipType>) response.getEntity();
    assertEquals(2, membershipTypes.size());
  }

}
