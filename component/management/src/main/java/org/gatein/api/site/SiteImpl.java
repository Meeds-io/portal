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
package org.gatein.api.site;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.gatein.api.ApiException;
import org.gatein.api.EntityAlreadyExistsException;
import org.gatein.api.Util;
import org.gatein.api.common.Attributes;
import org.gatein.api.internal.ObjectToStringBuilder;
import org.gatein.api.internal.Parameters;
import org.gatein.api.security.Permission;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.data.PortalData;

@Deprecated(forRemoval = true, since = "7.0")
public class SiteImpl implements Site {
    private static final long serialVersionUID = -5352376442491205465L;

    private final SiteId id;
    private transient PortalConfig portalConfig;

    private Attributes attributes;

    private boolean create;
    private String templateName; // only used when creating a site
    private Set<String> changed = new HashSet<String>(); // only used to know what fields were set after a create so we can replay properly

    public SiteImpl(SiteId id, String templateName) {
        this.id = id;
        SiteKey siteKey = Util.from(id);
        this.portalConfig = new PortalConfig(siteKey.getTypeName(), siteKey.getName());
        this.attributes = new Attributes();
        this.create = true;
        this.templateName = templateName;
    }

    public SiteImpl(PortalConfig portalConfig) {
        this.portalConfig = portalConfig;
        SiteKey siteKey = new SiteKey(portalConfig.getType(), portalConfig.getName());
        this.id = Util.from(siteKey);
        this.attributes = Util.from(portalConfig.getProperties());
    }

    @Override
    public SiteId getId() {
        return id;
    }

    @Override
    public SiteType getType() {
        return id.getType();
    }

    @Override
    public String getName() {
        return id.getName();
    }

    @Override
    public String getDescription() {
        return portalConfig.getDescription();
    }

    @Override
    public void setDescription(String description) {
        if (create) changed.add("description");

        portalConfig.setDescription(description);
    }


    @Override
    public void setDisplayName(String displayName) {
        if (create) changed.add("displayName");

        portalConfig.setLabel(displayName);
    }

    @Override
    public String getDisplayName() {
        // TODO: For sites of type SiteType.SPACE this should return the label of the group
        return portalConfig.getLabel();
    }

    @Override
    public Locale getLocale() {
        return Util.toLocale(portalConfig.getLocale());
    }

    @Override
    public void setLocale(Locale locale) {
        if (create) changed.add("locale");

        Parameters.requireNonNull(locale, "locale");
        portalConfig.setLocale(Util.fromLocale(locale));
    }

    @Override
    public String getSkin() {
        return portalConfig.getSkin();
    }

    @Override
    public void setSkin(String skin) {
        if (create) changed.add("skin");

        portalConfig.setSkin(skin);
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public Permission getAccessPermission() {
        return Util.from(portalConfig.getAccessPermissions());
    }

    @Override
    public void setAccessPermission(Permission permission) {
        if (create) changed.add("access");

        portalConfig.setAccessPermissions(Util.from(permission));
    }

    @Override
    public Permission getEditPermission() {
        return Util.from(portalConfig.getEditPermission());
    }

    @Override
    public void setEditPermission(Permission permission) {
        Parameters.requireNonNull(permission, "permission", "To allow edit for everyone use Permission.everyone()");

        // Only one edit permission (membership) is allowed at this time.
        String[] permissions = Util.from(permission);
        if (permissions.length != 1)
            throw new IllegalArgumentException("Invalid permission. Only one membership is allowed for an edit permission");

        if (create) changed.add("edit");

        portalConfig.setEditPermission(permissions[0]);
    }

    public void save(LayoutService storage, UserPortalConfigService service) {
        if (create) {
            try {
                PortalConfig existing = storage.getPortalConfig(portalConfig.getType(), portalConfig.getName());
                if (existing != null) throw new EntityAlreadyExistsException("Cannot create site. Site " + id + " already exists.");
            } catch (Exception e) {
                throw new ApiException("Exception occurred checking if site already existed before creating site " + id, e);
            }

            // In order to properly create a site (which includes creating it from a template) it seemed much harder
            // to get it working properly (NewPortalConfigListener)
            try {
              switch (id.getType()) {
              case SITE:
                service.createUserPortalConfig(portalConfig.getType(), portalConfig.getName(), templateName);
                break;
              }
            } catch (Exception e) {
              throw new ApiException("Could not create site " + id, e);
            }

            // Retrieve the site that was created above and replay any changes done via the Site api object.
            PortalConfig created;
            try {
                created = storage.getPortalConfig(portalConfig.getType(), portalConfig.getName());
                if (created == null) throw new ApiException("Could not find site after we successfully created it.");

            } catch (Exception e) {
                throw new ApiException("Exception occurred retrieving previously created site " + id);
            }
            SiteImpl createdSite = new SiteImpl(created);

            // Now replay/set the data that may have changed since the call from Portal.create to Portal.save
            if (changed.contains("access")) {
                createdSite.setAccessPermission(getAccessPermission());
            }
            if (changed.contains("edit")) {
                createdSite.setEditPermission(getEditPermission());
            }
            if (changed.contains("skin")) {
                createdSite.setSkin(getSkin());
            }
            if (changed.contains("locale")) {
                createdSite.setLocale(getLocale());
            }
            if (changed.contains("description")) {
                createdSite.setDescription(getDescription());
            }
            if (changed.contains("displayName")) {
                createdSite.setDisplayName(getDisplayName());
            }
            createdSite.getAttributes().putAll(getAttributes());
            this.attributes = createdSite.getAttributes();
            // Now we can set the internal PortalConfig object which should now properly reflect the site that was created
            // above and the changes done via the API
            this.portalConfig = createdSite.portalConfig;
        }

        // Attributes is the only object that can be modified outside the context of this object
        portalConfig.setProperties(Util.from(attributes));

        try {
            storage.save(portalConfig);
            changed = null;
            create = false;
            templateName = null;
        } catch (Exception e) {
            throw new ApiException("Exception occurred trying to save site " + id, e);
        }
    }

    @Override
    public int compareTo(Site other) {
        return getName().compareTo(other.getName());
    }

    @Override
    public String toString() {
        return ObjectToStringBuilder.toStringBuilder(getClass()).add("type", getType().getName()).add("name", getName())
                .add("displayName", getDisplayName()).add("description", getDescription()).add("locale", getLocale())
                .add("skin", getSkin()).add("attributes", getAttributes()).add("editPermission", getEditPermission())
                .add("accessPermission", getAccessPermission()).toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        PortalData data = (PortalData) in.readObject();
        portalConfig = new PortalConfig(data);
    }

    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException
    {
        out.defaultWriteObject();
        out.writeObject(portalConfig.build());
    }

}
