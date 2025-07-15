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

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

public class TestSiteDataImportMerge extends AbstractSiteDataImportTest {

    @Override
    protected ImportMode getMode() {
        return ImportMode.MERGE;
    }

    @Override
    protected void afterSecondBootWithOverride(PortalContainer container) throws Exception {
        restartTransaction();

        LayoutService layoutService = container.getComponentInstanceOfType(LayoutService.class);
        PageStorage pageStorage = container.getComponentInstanceOfType(PageStorage.class);
        PortalConfig portal = layoutService.getPortalConfig("classic");
        assertNotNull(portal);
        Container layout = portal.getPortalLayout();
        assertEquals(1, layout.getChildren().size());
        Application layoutPortlet = (Application) layout.getChildren().get(0);
        assertEquals("site2/layout", layoutService.getId(layoutPortlet.getState()));

        PageContext page1 = pageStorage.loadPage(PageKey.parse("portal::classic::page1"));
        assertNotNull(page1);
        assertEquals("site 2", page1.getState().getDisplayName());

        PageContext page2 = pageStorage.loadPage(PageKey.parse("portal::classic::page2"));
        assertNotNull(page2);
        assertEquals("site 2", page2.getState().getDisplayName());

        //
        PageContext home = pageStorage.loadPage(PageKey.parse("portal::classic::home"));
        assertNotNull(home);
        assertEquals("site 1", home.getState().getDisplayName());
    }
}
