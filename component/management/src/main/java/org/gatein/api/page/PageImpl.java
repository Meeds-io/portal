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
package org.gatein.api.page;

import java.io.Serializable;

import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.gatein.api.Util;
import org.gatein.api.internal.Parameters;
import org.gatein.api.security.Permission;
import org.gatein.api.site.SiteId;

@Deprecated(forRemoval = true, since = "7.0")
public class PageImpl implements Page, Serializable {

    private static final long serialVersionUID = 4541337067269269259L;

    private PageKey           key;

    private PageState         state;

    private boolean create;

    public PageImpl(PageContext pageContext) {
        this.key = pageContext.getKey();
        this.state = pageContext.getState();
    }

    @Override
    public PageId getId() {
        return Util.from(key);
    }

    @Override
    public SiteId getSiteId() {
        return Util.from(key.getSite());
    }

    @Override
    public String getName() {
        return key.getName();
    }

    @Override
    public String getDescription() {
        return state.getDescription();
    }

    @Override
    public void setDescription(String description) {
       setState(builder().description(description));
    }

    @Override
    public void setDisplayName(String displayName) {
       setState(builder().displayName(displayName));
    }

    @Override
    public String getDisplayName() {
        return state.getDisplayName();
    }

    @Override
    public Permission getAccessPermission() {
        return Util.from(state.getAccessPermissions());
    }

    @Override
    public void setAccessPermission(Permission permission) {
        Parameters.requireNonNull(permission, "permission", "To allow access to everyone use Permission.everyone()");

        setState(builder().accessPermissions(Util.from(permission)));
    }

    @Override
    public Permission getEditPermission() {
        return Util.from(state.getEditPermission());
    }

    @Override
    public void setEditPermission(Permission permission) {
        Parameters.requireNonNull(permission, "permission", "To allow edit for everyone use Permission.everyone()");

        // Only one edit permission (membership) is allowed at this time.
        String[] permissions = Util.from(permission);
        if (permissions.length != 1)
            throw new IllegalArgumentException("Invalid permission. Only one membership is allowed for an edit permission");

        setState(builder().editPermission(permissions[0]));
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    @Override
    public int compareTo(Page page) {
        return getName().compareTo(page.getName());
    }

    public PageContext getPageContext() {
        return new PageContext(key, state);
    }

    private PageState.Builder builder() {
        return state.builder();
    }

    private void setState(PageState.Builder builder) {
        this.state = builder.build();
    }
}
