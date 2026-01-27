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

import org.exoplatform.services.organization.Membership;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MembershipImpl extends org.exoplatform.services.organization.impl.MembershipImpl implements Comparable {

  private static final long serialVersionUID = -8572523969777642576L;

  public MembershipImpl(String membershipType,
                        String userName,
                        String groupId,
                        boolean isInherited) {
    super(membershipType, userName, groupId, isInherited);
  }

  public MembershipImpl(String id) {
    String[] fields = id.split(":");

    // Id can be pure "//" in some cases
    if (fields[0] != null) {
      setMembershipType(fields[0]);
    }
    if (fields[1] != null) {
      setUserName(fields[1]);
    }
    if (fields[2] != null) {
      setGroupId(fields[2]);
    }
  }

  public int compareTo(Object o) {
    if (!(o instanceof Membership) || getUserName() == null) {
      return 0;
    }
    Membership m = (Membership) o;
    return getUserName().compareTo(m.getUserName());
  }

}
