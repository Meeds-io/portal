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

/*
 * JBoss, a division of Red Hat
 * Copyright 2010, Red Hat Middleware, LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Config {

    private Map<String, String> groupTypeMappings = new HashMap<String, String>();

    private boolean useParentIdAsGroupType = false;

    private boolean passwordAsAttribute = false;

    private String defaultGroupType = "GTN_GROUP_TYPE";

    private String rootGroupName = "GTN_ROOT_GROUP";

    private String pathSeparator = ".";

    private String slashReplacement = "@_@_@";

    private boolean forceMembershipOfMappedTypes = false;

    private String associationMembershipType;

    private List<String> ignoreMappedMembershipTypeGroupList = new ArrayList<String>();

    private boolean useCache = true;

    private boolean sortGroups = true;

    private boolean sortMemberships = true;

    /* For some LDAP configurations where part of users can duplicate in both DB and LDAP
    it is not possible to count user efficiently for paginated query. Only way is to download
    whole content of LDAP server and exclude duplicates manually to return accurate user count.
            When this option is set to true GateIn will rely on user count information returned from PLIDM
    which can return greater number of users then in real non duplicated count for perf reasons..
    Those users will be filtered before returning search page however to not return nulls last entry
    can be duplicated in returned user list.
            If this value is set to false GateIn will perform whole non paginated query and filter it after.
    It will result in more accurate results and paginated list size info however can affect performance
    If you have DB only setup, it's possible to switch this option to true. This may help to have better performance.
    If you have DB+LDAP setup, it's necessary to keep this option as false, otherwise you can have inaccurate results*/
    private boolean countPaginatedUsers = true;

    private boolean filterDisabledUsersInQueries = true;
    
    private boolean disableUserActived = true;

    /*For DB+LDAP it is not possible to efficiently perform paginated membership query. Only way is to download
    all memberships from LDAP server and all memberships from DB and merge them together.
    When this option is set to false GateIn will rely on membership count information returned from PLIDM
    and it will use paginated membership queries based on this. This is better for performance but for DB+LDAP the
    memberships pagination may not behave correctly.
    If this value is set to true GateIn will perform whole non paginated query to obtain all memberships and filter it after.
    It will result in more accurate results however can affect performance.
    If you have DB only setup, it's recommended to switch this option to false. This will help to have better performance.
    If you have DB+LDAP setup, it's recommended to switch this option to true, otherwise you can have inaccurate results*/
    private boolean skipPaginationInMembershipQuery = false;

    private boolean updateLastLoginTimeAfterAuthentication = true;

    private int               maxAuthenticationAttempts;
    private int blockingTime;

    public Config() {
    }

    public String getGroupType(String parentId) {

        if (parentId == null || parentId.length() == 0) {
            parentId = "/";
        }

        if (!useParentIdAsGroupType) {
            String type = _getGroupType(parentId, true, true, true);
            if (type != null) {
                return type;
            }
            return getDefaultGroupType();
        }

        // Search for exact match in mappings
        String type = _getGroupType(parentId, false, true, true);

        // If not then check for inherited type
        if (type == null) {
            type = _getGroupType(parentId, true, false, true);
        }

        // If not then prepare type from this id
        if (type == null) {
            type = convertType(parentId);
        }

        return type;

    }

    private String _getGroupType(String parentId, boolean checkParents, boolean matchExact, boolean matchInherited) {

        if (matchExact && getGroupTypeMappings().keySet().contains(parentId)) {
            return getGroupTypeMappings().get(parentId);
        }

        String id = !parentId.equals("/") ? parentId + "/*" : "/*";

        if (matchInherited && getGroupTypeMappings().keySet().contains(id)) {
            return getGroupTypeMappings().get(id);
        }

        if (checkParents && !parentId.equals("/") && parentId.contains("/")) {
            // Check if any mapping that contains '/*' match this id
            for (String key : groupTypeMappings.keySet()) {
                id = key;
                if (id.endsWith("/*")) {
                    id = id.substring(0, id.length() - 2);
                } else {
                    continue;
                }

                if (parentId.startsWith(id)) {
                    return groupTypeMappings.get(key);
                }
            }
        }

        return null;
    }

    public String getParentId(String type) {
        for (Map.Entry<String, String> entry : groupTypeMappings.entrySet()) {
            if (entry.getValue().equals(type)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public boolean isIgnoreMappedMembershipTypeForGroup(String groupId) {
        if ("/".equals(groupId)) {
            return false;
        }

        String parentId = groupId.substring(0, groupId.lastIndexOf("/"));

        for (String id : ignoreMappedMembershipTypeGroupList) {
            // Check if any mapping that contains '/*' match this id
            if (id.endsWith("/*")) {
                id = id.substring(0, id.length() - 2);
                // Check exact equality case
            } else if (id.equals(groupId)) {
                return true;
            } else {
                continue;
            }

            if (parentId.startsWith(id)) {
                return true;
            }
        }

        return false;
    }

    public String getPLIDMGroupName(String gtnGroupName) {
        return gtnGroupName.replaceAll(getSlashReplacement(), "/");
    }

    public String getGtnGroupName(String plidmGroupName) {
        return plidmGroupName.replaceAll("/", getSlashReplacement());
    }

    Set<String> getTypes(String id) {
        HashSet<String> types = new HashSet<String>();

        for (String key : groupTypeMappings.keySet()) {
            if (key.equals(id) || key.equals(id + "/*")) {
                types.add(groupTypeMappings.get(key));
            }
        }

        return types;
    }

    Set<String> getAllTypes() {
        HashSet<String> types = new HashSet<String>(groupTypeMappings.values());

        return types;

    }

    private String convertType(String type) {

        return type.replaceAll("/", pathSeparator);
    }

    public boolean isUseParentIdAsGroupType() {
        return useParentIdAsGroupType;
    }

    public void setUseParentIdAsGroupType(boolean useParentIdAsGroupType) {
        this.useParentIdAsGroupType = useParentIdAsGroupType;
    }

    public String getDefaultGroupType() {
        return defaultGroupType;
    }

    public void setDefaultGroupType(String defaultGroupType) {
        this.defaultGroupType = defaultGroupType;
    }

    public String getRootGroupName() {
        return rootGroupName;
    }

    public void setRootGroupName(String rootGroupName) {
        this.rootGroupName = rootGroupName;
    }

    public void setGroupTypeMappings(Map<String, String> groupTypeMappings) {
        this.groupTypeMappings = groupTypeMappings;
    }

    public Map<String, String> getGroupTypeMappings() {
        return groupTypeMappings;
    }

    public boolean isPasswordAsAttribute() {
        return passwordAsAttribute;
    }

    public void setPasswordAsAttribute(boolean passwordAsAttribute) {
        this.passwordAsAttribute = passwordAsAttribute;
    }

    public String getPathSeparator() {
        return pathSeparator;
    }

    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = pathSeparator;
    }

    public boolean isForceMembershipOfMappedTypes() {
        return forceMembershipOfMappedTypes;
    }

    public void setForceMembershipOfMappedTypes(boolean forceMembershipOfMappedTypes) {
        this.forceMembershipOfMappedTypes = forceMembershipOfMappedTypes;
    }

    public String getAssociationMembershipType() {
        return associationMembershipType;
    }

    public void setAssociationMembershipType(String associationMembershipType) {
        this.associationMembershipType = associationMembershipType;
    }

    public List<String> getIgnoreMappedMembershipTypeGroupList() {
        return ignoreMappedMembershipTypeGroupList;
    }

    public void setIgnoreMappedMembershipTypeGroupList(List<String> ignoreMappedMembershipTypeGroupList) {
        this.ignoreMappedMembershipTypeGroupList = ignoreMappedMembershipTypeGroupList;
    }

    public String getSlashReplacement() {
        return slashReplacement;
    }

    public void setSlashReplacement(String slashReplacement) {
        this.slashReplacement = slashReplacement;
    }

    public boolean isSortGroups() {
        return sortGroups;
    }

    public void setSortGroups(boolean sortGroups) {
        this.sortGroups = sortGroups;
    }

    public boolean isSortMemberships() {
        return sortMemberships;
    }

    public void setSortMemberships(boolean sortMemberships) {
        this.sortMemberships = sortMemberships;
    }

    public boolean isCountPaginatedUsers() {
        return countPaginatedUsers;
    }

    public void setCountPaginatedUsers(boolean countPaginatedUsers) {
        this.countPaginatedUsers = countPaginatedUsers;
    }

    public boolean isFilterDisabledUsersInQueries() {
        return filterDisabledUsersInQueries;
    }

    public void setFilterDisabledUsersInQueries(boolean filterDisabledUsersInQueries) {
        this.filterDisabledUsersInQueries = filterDisabledUsersInQueries;
    }

    public boolean isDisableUserActived() {
        return disableUserActived;
    }

    public void setDisableUserActived(boolean disableUserActived) {
        this.disableUserActived = disableUserActived;
    }

    public boolean isSkipPaginationInMembershipQuery() {
        return skipPaginationInMembershipQuery;
    }

    public void setSkipPaginationInMembershipQuery(boolean skipPaginationInMembershipQuery) {
        this.skipPaginationInMembershipQuery = skipPaginationInMembershipQuery;
    }

    /**
     * @param updateLastLoginTimeAfterAuthentication
     * @deprecated kept for backward compatibility with existing configurations
     *             The update of login time is mandatory to be able to associate
     *             default spaces/groups to users after login
     */
    @Deprecated(forRemoval = true, since = "1.5.0")
    public void setUpdateLastLoginTimeAfterAuthentication(boolean updateLastLoginTimeAfterAuthentication) {
      this.updateLastLoginTimeAfterAuthentication = updateLastLoginTimeAfterAuthentication;
    }

    /**
     * @return updateLastLoginTimeAfterAuthentication
     * @deprecated kept for backward compatibility with existing configurations
     *             The update of login time is mandatory to be able to associate
     *             default spaces/groups to users after login
     */
    @Deprecated(forRemoval = true, since = "1.5.0")
    public boolean isUpdateLastLoginTimeAfterAuthentication() {
        return updateLastLoginTimeAfterAuthentication;
    }

    public boolean isUseCache() {
      return useCache;
    }

    public void setUseCache(boolean useCache) {
      this.useCache = useCache;
    }

    public int getMaxAuthenticationAttempts() {
      return maxAuthenticationAttempts;
    }

    public void setMaxAuthenticationAttempts(int maxAuthenticationAttempts) {
      this.maxAuthenticationAttempts = maxAuthenticationAttempts;
    }

    public int getBlockingTime() {
        return blockingTime;
    }

    public void setBlockingTime(int blockingTime) {
        this.blockingTime = blockingTime;
    }
}
