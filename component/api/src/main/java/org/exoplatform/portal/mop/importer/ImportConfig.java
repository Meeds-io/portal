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
package org.exoplatform.portal.mop.importer;

public class ImportConfig {

    /** . */
    final boolean destroyOrphan;

    /** . */
    final boolean updatedSame;

    /** . */
    final boolean createMissing;

    public ImportConfig(boolean destroyOrphan, boolean updatedSame, boolean createMissing) {
        this.destroyOrphan = destroyOrphan;
        this.updatedSame = updatedSame;
        this.createMissing = createMissing;
    }

    /**
     * Returns true when an orphan node should be destroyed.
     *
     * @return the destroy orphan value
     */
    public boolean getDestroyOrphan() {
        return destroyOrphan;
    }

    public boolean getUpdatedSame() {
        return updatedSame;
    }

    public boolean getCreateMissing() {
        return createMissing;
    }
}
