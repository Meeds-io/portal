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
package org.exoplatform.services.organization.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeEventListener;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;

public class InMemoryMembershipTypeHandler implements MembershipTypeHandler {

  private static final String                ERROR_BROADCASTING_EVENT_MESSAGE = "Error broadcasting event : {}";

  private OrganizationService                organizationService;

  private List<MembershipTypeEventListener>  membershipTypeListeners          = new ArrayList<>();

  private static Map<String, MembershipType> membershipTypesById              = new HashMap<>();

  public InMemoryMembershipTypeHandler(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @Override
  public void addMembershipTypeEventListener(MembershipTypeEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    membershipTypeListeners.add(listener);
  }

  @Override
  public void removeMembershipTypeEventListener(MembershipTypeEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    membershipTypeListeners.remove(listener);
  }

  @Override
  public final MembershipType createMembershipTypeInstance() {
    return new MembershipTypeImpl();
  }

  @Override
  public MembershipType createMembershipType(MembershipType membershipType, boolean broadcast) {
    if (membershipTypesById.containsKey(membershipType.getName())) {
      return findMembershipType(membershipType.getName());
    }
    return saveMembershipType(membershipType, broadcast);
  }

  @Override
  public MembershipType saveMembershipType(MembershipType membershipType, boolean broadcast) {
    if (broadcast) {
      preSave(membershipType, true);
    }
    Date now = new Date();
    membershipType.setCreatedDate(now);
    membershipType.setModifiedDate(now);
    membershipTypesById.put(membershipType.getName(), ObjectUtils.clone(membershipType));
    if (broadcast) {
      postSave(membershipType, true);
    }
    return ObjectUtils.clone(membershipType);
  }

  @Override
  public MembershipType findMembershipType(String name) {
    return ObjectUtils.clone(membershipTypesById.get(name));
  }

  @Override
  public MembershipType removeMembershipType(String name, boolean broadcast) {
    if (!membershipTypesById.containsKey(name)) {
      return null;
    }
    MembershipType membershipType = findMembershipType(name);
    if (broadcast) {
      preDelete(membershipType);
    }
    Date now = new Date();
    membershipType.setCreatedDate(now);
    membershipType.setModifiedDate(now);
    membershipTypesById.remove(membershipType.getName());
    getMembershipHandler().removeMembershipByMembershipType(name, broadcast);

    if (broadcast) {
      postDelete(membershipType);
    }
    return membershipType;
  }

  @Override
  public Collection<MembershipType> findMembershipTypes() {
    return membershipTypesById.values();
  }

  private void preSave(MembershipType membershipType, boolean isNew) {
    for (MembershipTypeEventListener listener : membershipTypeListeners) {
      try {
        listener.preSave(membershipType, isNew);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void postSave(MembershipType membershipType, boolean isNew) {
    for (MembershipTypeEventListener listener : membershipTypeListeners) {
      try {
        listener.postSave(membershipType, isNew);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void preDelete(MembershipType membershipType) {
    for (MembershipTypeEventListener listener : membershipTypeListeners) {
      try {
        listener.preDelete(membershipType);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private void postDelete(MembershipType membershipType) {
    for (MembershipTypeEventListener listener : membershipTypeListeners) {
      try {
        listener.postDelete(membershipType);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(ERROR_BROADCASTING_EVENT_MESSAGE.replace("{}", listener.getClass().getName()), e);
      }
    }
  }

  private InMemoryMembershipHandler getMembershipHandler() {
    return (InMemoryMembershipHandler) organizationService.getMembershipHandler();
  }

}
