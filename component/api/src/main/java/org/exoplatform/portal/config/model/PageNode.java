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
package org.exoplatform.portal.config.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageKey;

import lombok.Getter;
import lombok.Setter;

public class PageNode extends PageNodeContainer {

    /** . */
    private I18NString labels;

    /** . */
    private String icon;

    /** . */
    private String name;

    /** . */
    private Date startPublicationDate;

    /** . */
    private Date endPublicationDate;

    /** . */
    private Visibility visibility = Visibility.DISPLAYED;

    /** . */
    private String pageReference;

    /** . */
    private String   target;

    /** . */
    private long updatedDate;

    @Getter
    @Setter
    private int      order      = -1;

    public I18NString getLabels() {
        return labels;
    }

    public void setLabels(I18NString labels) {
        this.labels = labels;
    }

    public String getLabel() {
        if (labels != null) {
            for (LocalizedString label : labels) {
                if (label.getLang() == null) {
                    return label.getValue();
                }
            }
        }
        return null;
    }

    public void setLabel(String s) {
        if (labels == null) {
            labels = new I18NString();
        } else {
            labels.clear();
        }
        labels.add(new LocalizedString(s));
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String s) {
        icon = s;
    }

    public String getPageReference() {
        return pageReference;
    }

    public void setPageReference(String s) {
        pageReference = s;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PageNode> getChildren() {
        return getNodes();
    }

    public void setChildren(ArrayList<PageNode> children) {
        setNodes(children);
    }

    public Date getStartPublicationDate() {
        return startPublicationDate;
    }

    public void setStartPublicationDate(Date startDate) {
        startPublicationDate = startDate;
    }

    public Date getEndPublicationDate() {
        return endPublicationDate;
    }

    public void setEndPublicationDate(Date endDate) {
        endPublicationDate = endDate;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Visibility getVisibility() {
        return this.visibility;
    }

    public PageNode getChild(String name) {
        return getNode(name);
    }


    public long getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(long updatedDate) {
        this.updatedDate = updatedDate;
    }

    public NodeState getState() {
        PageKey pageKey = pageReference != null ? PageKey.parse(pageReference) : null;
        return new NodeState(labels.getSimple(), icon, startPublicationDate == null ? -1 : startPublicationDate.getTime(),
                endPublicationDate == null ? -1 : endPublicationDate.getTime(), visibility,
                pageKey, pageKey == null ? null : pageKey.getSite(), target, updatedDate);
    }

    @Override
    public String toString() {
        return "PageNode[" + name + "]";
    }
}
