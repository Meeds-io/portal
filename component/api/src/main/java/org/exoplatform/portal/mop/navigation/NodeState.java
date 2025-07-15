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
package org.exoplatform.portal.mop.navigation;

import java.io.Serializable;
import java.util.Date;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.page.PageKey;

/**
 * An immutable node state class.
 *
 */
public final class NodeState implements Serializable {

    /** . */
    public static final NodeState INITIAL = new NodeState.Builder().build();

    /**
     * Builder class.
     */
    public static class Builder {

        /** . */
        private String label;

        /** . */
        private String icon;

        /** . */
        private long startPublicationTime;

        /** . */
        private long endPublicationTime;

        /** . */
        private Visibility visibility;

        /** . */
        private PageKey pageRef;

        private SiteKey siteKey;

        private String  target;

        private long updatedDate;

        public Builder() {
            this.icon = null;
            this.label = null;
            this.startPublicationTime = -1;
            this.endPublicationTime = -1;
            this.visibility = Visibility.DISPLAYED;
            this.pageRef = null;
            this.target = null;
            this.updatedDate = 0;
        }

        /**
         * Creates a builder from a specified state.
         *
         * @param state the state to copy
         * @throws NullPointerException if the stateis null
         */
        public Builder(NodeState state) throws NullPointerException {
            if (state == null) {
                throw new NullPointerException();
            }
            this.label = state.label;
            this.icon = state.icon;
            this.startPublicationTime = state.startPublicationTime;
            this.endPublicationTime = state.endPublicationTime;
            this.visibility = state.visibility;
            this.pageRef = state.pageRef;
            this.siteKey = state.siteKey;
            this.target = state.target;
            this.updatedDate = state.getUpdatedDate();
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder startPublicationTime(long startPublicationTime) {
            this.startPublicationTime = startPublicationTime;
            return this;
        }

        public Builder endPublicationTime(long endPublicationTime) {
            this.endPublicationTime = endPublicationTime;
            return this;
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder pageRef(PageKey pageRef) {
            this.pageRef = pageRef;
            return this;
        }

        public Builder siteKey(SiteKey siteKey) {
          this.siteKey = siteKey;
          return this;
        }
        public Builder target(String target) {
          this.target = target;
          return this;
        }
        public Builder updatedDate(long updatedDate) {
          this.updatedDate = updatedDate;
          return this;
        }

        public NodeState build() {
            return new NodeState(label, icon, startPublicationTime, endPublicationTime, visibility, pageRef, siteKey, target, updatedDate);
        }
    }

    /** . */
    private final String label;

    /** . */
    private final String icon;

    /** . */
    private final long startPublicationTime;

    /** . */
    private final long endPublicationTime;

    /** . */
    private final Visibility visibility;

    /** . */
    private final PageKey pageRef;

    /** . */
    private final SiteKey siteKey;

    private final String  target;

    private long updatedDate;

    public NodeState(String label,
                     String icon,
                     long startPublicationTime,
                     long endPublicationTime,
                     Visibility visibility,
                     PageKey pageRef,
                     SiteKey siteKey,
                     String target,
                     long updatedDate) {
        this.label = label;
        this.icon = icon;
        this.startPublicationTime = startPublicationTime;
        this.endPublicationTime = endPublicationTime;
        this.visibility = visibility;
        this.pageRef = pageRef;
        this.siteKey = siteKey;
        this.target = target;
        this.updatedDate = updatedDate;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public long getStartPublicationTime() {
        return startPublicationTime;
    }

    Date getStartPublicationDate() {
        return startPublicationTime != -1 ? new Date(startPublicationTime) : null;
    }

    public long getEndPublicationTime() {
        return endPublicationTime;
    }

    Date getEndPublicationDate() {
        return endPublicationTime != -1 ? new Date(endPublicationTime) : null;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public PageKey getPageRef() {
        return pageRef;
    }

    public String getTarget() {
        return target;
    }


    public SiteKey getSiteKey() {
      return siteKey;
    }

    public long getUpdatedDate() {
        return updatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof NodeState) {
            NodeState that = (NodeState) o;
            return Safe.equals(label, that.label) && Safe.equals(icon, that.icon)
                    && Safe.equals(startPublicationTime, that.startPublicationTime)
                    && Safe.equals(endPublicationTime, that.endPublicationTime) && Safe.equals(visibility, that.visibility)
                    && Safe.equals(pageRef, that.pageRef) && Safe.equals(target, that.target);
        }
        return false;
    }

    @Override
    public String toString() {
        return "NodeState[label=" + label + ",icon=" + icon + ",startPublicationTime=" + startPublicationTime
                + ",endPublicationTime=" + endPublicationTime + ",visibility=" + visibility + ",pageRef=" + pageRef + "]";
    }

    public Builder builder() {
        return new Builder(this);
    }
}
