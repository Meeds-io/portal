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
package org.exoplatform.services.organization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.exoplatform.services.cache.ObjectCacheInfo;
import org.exoplatform.services.organization.cache.MembershipCacheKey;
import org.exoplatform.services.organization.idm.cache.ClearGroupCacheByGroupIdSelector;
import org.exoplatform.services.organization.idm.cache.ClearMembershipCacheByGroupIdSelector;
import org.exoplatform.services.organization.idm.cache.ClearMembershipCacheByMembershipTypeSelector;
import org.exoplatform.services.organization.impl.MembershipImpl;

/**
 * The selections of the organization caches, which travel to the other nodes
 * of a cluster and select there the entries they name.
 */
public class TestIdentityCacheSelectors {

  private static final MembershipCacheKey KEY = new MembershipCacheKey("john", "/platform/users", "member");

  @Test
  public void testGroupSelectionSelectsTheGroupItsSubgroupsAndTheChildrenLists() throws Exception {
    ClearGroupCacheByGroupIdSelector selector = travel(new ClearGroupCacheByGroupIdSelector("/platform/users",
                                                                                            "/platform",
                                                                                            true));

    assertTrue(selector.select("/platform/users", null));
    assertTrue(selector.select("/platform/users/sub", null));
    assertTrue(selector.select("children_/platform/users", null));
    assertTrue(selector.select("children_/platform", null));
    assertFalse(selector.select("/platform/users-old", null));
    assertFalse(selector.select("/platform/administrators", null));
    assertFalse(selector.select("children_/platform/administrators", null));
  }

  @Test
  public void testGroupSelectionWithoutTheListsKeepsTheChildrenLists() throws Exception {
    ClearGroupCacheByGroupIdSelector selector = travel(new ClearGroupCacheByGroupIdSelector("/platform/users",
                                                                                            "/platform",
                                                                                            false));

    assertTrue(selector.select("/platform/users", null));
    assertFalse(selector.select("children_/platform/users", null));
    assertFalse(selector.select("children_/platform", null));
  }

  @Test
  public void testMembershipSelectionsSelectTheirMembershipsAndTheLists() throws Exception {
    ClearMembershipCacheByGroupIdSelector byGroup = travel(new ClearMembershipCacheByGroupIdSelector("/platform/users"));
    ClearMembershipCacheByMembershipTypeSelector byType = travel(new ClearMembershipCacheByMembershipTypeSelector("member"));

    assertTrue(byGroup.select(KEY, info(membership("/platform/users", "manager"))));
    assertFalse(byGroup.select(KEY, info(membership("/platform/administrators", "manager"))));
    assertTrue(byGroup.select(KEY, info(new ArrayList<>(List.of(membership("/platform/administrators", "manager"))))));
    assertTrue(byType.select(KEY, info(membership("/platform/administrators", "member"))));
    assertFalse(byType.select(KEY, info(membership("/platform/administrators", "manager"))));
    assertTrue(byType.select(KEY, info(new ArrayList<>(List.of(membership("/platform/users", "manager"))))));
  }

  @Test
  public void testASelectionTravelsAsTheSameSelection() throws Exception {
    List<Object> selections = List.of(new ClearGroupCacheByGroupIdSelector("/platform/users", "/platform", true),
                                      new ClearMembershipCacheByGroupIdSelector("/platform/users"),
                                      new ClearMembershipCacheByMembershipTypeSelector("member"));
    for (Object selection : selections) {
      Object received = travel(selection);
      assertEquals(selection, received);
      assertEquals(selection.hashCode(), received.hashCode());
    }
    assertNotEquals(new ClearGroupCacheByGroupIdSelector("/platform/users", "/platform", true),
                    new ClearGroupCacheByGroupIdSelector("/platform/users", "/platform", false));
    assertNotEquals(new ClearMembershipCacheByGroupIdSelector("member"),
                    new ClearMembershipCacheByMembershipTypeSelector("member"));
  }

  private static MembershipImpl membership(String groupId, String type) {
    MembershipImpl membership = new MembershipImpl();
    membership.setUserName("john");
    membership.setGroupId(groupId);
    membership.setMembershipType(type);
    return membership;
  }

  private static ObjectCacheInfo<Object> info(Object value) {
    return new ObjectCacheInfo<>() {
      @Override
      public long getExpireTime() {
        return -1;
      }

      @Override
      public Object get() {
        return value;
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static <T> T travel(T value) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(value);
    }
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      return (T) in.readObject();
    }
  }
}
