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
package org.exoplatform.portal.config;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

/**
 * A metadata class to describe security configuration.
 *
 */
public class UserACLMetaData {

    /** . */
    private String superUser;

    /** . */
    private String guestsGroups;

    /** . */
    private String navigationCreatorMembershipType;

    /** . */
    private String portalCreateGroups;

    public UserACLMetaData() {
    }

    /**
     * Initialize the metadata for UserACL service base on the {@link InitParams} params passed to the constructor
     *
     * @param params
     */
    public UserACLMetaData(InitParams params) {
        ValueParam superUserParam = params.getValueParam("super.user");
        if (superUserParam != null) {
            setSuperUser(superUserParam.getValue());
        }
        ValueParam guestGroupParam = params.getValueParam("guests.group");
        if (guestGroupParam != null) {
            setGuestsGroups(guestGroupParam.getValue());
        }
        ValueParam navCretorParam = params.getValueParam("navigation.creator.membership.type");
        if (navCretorParam != null) {
            setNavigationCreatorMembershipType(navCretorParam.getValue());
        }
        ValueParam portalCretorGroupsParam = params.getValueParam("portal.creator.groups");
        if (portalCretorGroupsParam != null) {
            setPortalCreateGroups(portalCretorGroupsParam.getValue());
        }
    }

    public String getSuperUser() {
        return superUser;
    }

    public void setSuperUser(String superUser) {
        this.superUser = superUser;
    }

    public String getGuestsGroups() {
        return guestsGroups;
    }

    public void setGuestsGroups(String guestsGroups) {
        this.guestsGroups = guestsGroups;
    }

    public String getNavigationCreatorMembershipType() {
        return navigationCreatorMembershipType;
    }

    public void setNavigationCreatorMembershipType(String navigationCreatorMembershipType) {
        this.navigationCreatorMembershipType = navigationCreatorMembershipType;
    }

    public String getPortalCreateGroups() {
        return portalCreateGroups;
    }

    public void setPortalCreateGroups(String portalCreateGroups) {
        this.portalCreateGroups = portalCreateGroups;
    }
}
