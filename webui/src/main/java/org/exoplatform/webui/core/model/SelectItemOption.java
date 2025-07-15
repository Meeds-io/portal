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
package org.exoplatform.webui.core.model;

import org.exoplatform.commons.serialization.api.annotations.Serialized;

/**
 * Created by The eXo Platform SARL Author : Tuan Nguyen tuan08@users.sourceforge.net Jun 26, 2006
 *
 * An item in a UIFormInputItemSelector Each item is actually held in a SelectItemCategory, which is held by the
 * UIFormInputItemSelector
 *
 * @see SelectItemCategory
 * @see org.exoplatform.webui.form.UIFormInputItemSelector
 */
@Serialized
public class SelectItemOption<T> {
    /**
     * The label of the item
     */
    protected String label;

    /**
     * THe value of the item
     */
    protected T value;

    /**
     * The icon url of the item
     */
    protected String icon;

    /**
     * Whether this item is selected
     */
    protected boolean selected = false;

    /**
     * A description of the item
     */
    protected String description;

    public SelectItemOption() {
    }

    public SelectItemOption(String label, T value, String icon) {
        this(label, value, "", icon);
    }

    public SelectItemOption(String label, T value, String desc, String icon) {
        this.label = label;
        this.value = value;
        this.description = desc;
        this.icon = icon;
    }

    public SelectItemOption(String label, T value, String desc, String icon, boolean selected) {
        this(label, value, desc, icon);
        this.selected = selected;
    }

    public SelectItemOption(String label, T value) {
        this(label, value, "", null);
    }

    public SelectItemOption(T value) {
        this(value.toString(), value, "", null);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String s) {
      this.label = s;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T s) {
      this.value = s;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String s) {
      this.description = s;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean b) {
      this.selected = b;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

}
