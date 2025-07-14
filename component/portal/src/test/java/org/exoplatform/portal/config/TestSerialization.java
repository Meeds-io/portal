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

import java.util.*;

import org.exoplatform.portal.config.model.ApplicationBackgroundStyle;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.mop.PageType;
import org.gatein.common.io.IOTools;

import org.exoplatform.component.test.AbstractGateInTest;
import org.exoplatform.portal.pom.data.*;

public class TestSerialization extends AbstractGateInTest {
    /** . */
    private final BodyData body = new BodyData("foo", BodyType.PAGE, null);

    /** . */
    private final ContainerData container = new ContainerData("foo01",
                                                            "foo02",
                                                            "foo03",
                                                            "foo04",
                                                            "foo05",
                                                            "foo06",
                                                            "foo07",
                                                            "foo08",
                                                            "foo09",
                                                            "foo10",
                                                            "foo11",
                                                            "foo12",
                                                            new ModelStyle(),
                                                            new ApplicationBackgroundStyle(),
                                                            Collections.singletonList("foo11"),
                                                            Collections.<ComponentData> singletonList(body));

    public void testPortalKey() throws Exception {
        PortalKey key = new PortalKey("foo", "bar");
        assertEquals(key.hashCode(), IOTools.clone(key).hashCode());
        assertEquals(key, IOTools.clone(key));
    }

    public void testPageKey() throws Exception {
        PageKey key = new PageKey("foo", "bar", "juu");
        assertEquals(key.hashCode(), IOTools.clone(key).hashCode());
        assertEquals(key, IOTools.clone(key));
    }

    public void testBody() throws Exception {
        BodyData clone = IOTools.clone(body);
        assertEquals(body.getStorageId(), clone.getStorageId());
        assertEquals(body.getStorageName(), clone.getStorageName());
        assertEquals(body.getType(), clone.getType());
    }

    public void testContainer() throws Exception {
        ContainerData clone = IOTools.clone(container);
        assertEquals(container.getStorageId(), clone.getStorageId());
        assertEquals(container.getStorageName(), clone.getStorageName());
        assertEquals(container.getId(), clone.getId());
        assertEquals(container.getName(), clone.getName());
        assertEquals(container.getIcon(), clone.getIcon());
        assertEquals(container.getTemplate(), clone.getTemplate());
        assertEquals(container.getFactoryId(), clone.getFactoryId());
        assertEquals(container.getTitle(), clone.getTitle());
        assertEquals(container.getDescription(), clone.getDescription());
        assertEquals(container.getWidth(), clone.getWidth());
        assertEquals(container.getHeight(), clone.getHeight());
        assertEquals(container.getAccessPermissions(), clone.getAccessPermissions());
        List<ComponentData> clonedChildren = container.getChildren();
        assertEquals(1, clonedChildren.size());
        assertEquals("foo", clonedChildren.get(0).getStorageId());
        assertEquals(BodyType.PAGE, ((BodyData) clonedChildren.get(0)).getType());
    }

    public void testPage() throws Exception {
    PageData obj = new PageData("foo01",
                                "foo02",
                                "foo03",
                                "foo04",
                                "foo05",
                                "foo06",
                                "foo07",
                                "foo08",
                                "foo09",
                                "foo10",
                                "foo11",
                                "foo12",
                                Collections.singletonList("foo13"),
                                Arrays.<ComponentData> asList(body),
                                "foo14",
                                "foo15",
                                "foo16",
                                true,
                                true,
                                true,
                                PageType.LINK.name(),
                                "wwww.exo.com");
        PageData clone = IOTools.clone(obj);
        assertEquals(obj.getStorageId(), clone.getStorageId());
        assertEquals(obj.getStorageName(), clone.getStorageName());
        assertEquals(obj.getId(), clone.getId());
        assertEquals(obj.getName(), clone.getName());
        assertEquals(obj.getIcon(), clone.getIcon());
        assertEquals(obj.getTemplate(), clone.getTemplate());
        assertEquals(obj.getFactoryId(), clone.getFactoryId());
        assertEquals(obj.getTitle(), clone.getTitle());
        assertEquals(obj.getDescription(), clone.getDescription());
        assertEquals(obj.getWidth(), clone.getWidth());
        assertEquals(obj.getHeight(), clone.getHeight());
        assertEquals(obj.getAccessPermissions(), clone.getAccessPermissions());
        List<ComponentData> clonedChildren = obj.getChildren();
        assertEquals(1, clonedChildren.size());
        assertEquals("foo", clonedChildren.get(0).getStorageId());
        assertEquals(BodyType.PAGE, ((BodyData) clonedChildren.get(0)).getType());
        assertEquals(obj.getOwnerType(), clone.getOwnerType());
        assertEquals(obj.getOwnerId(), clone.getOwnerId());
        assertEquals(obj.getEditPermission(), clone.getEditPermission());
        assertEquals(obj.isShowMaxWindow(), clone.isShowMaxWindow());
        assertEquals(obj.isHideSharedLayout(), clone.isHideSharedLayout());
        assertEquals(obj.isShowSharedLayout(), clone.isShowSharedLayout());
        assertEquals(obj.getType(), clone.getType());
        assertEquals(obj.getLink(), clone.getLink());
    }

    public void testPortal() throws Exception {
      PortalData obj = new PortalData("foo01",
                                      "foo02",
                                      "foo03",
                                      "foo04",
                                      "foo10",
                                      "foo11",
                                      Arrays.asList("foo05"),
                                      "foo06",
                                      Collections.singletonMap("foo07", "foo08"),
                                      "foo09",
                                      container,
                                      true,
                                      8,
                                      0);
        PortalData clone = IOTools.clone(obj);
        assertEquals(obj.getStorageId(), clone.getStorageId());
        assertEquals(obj.getStorageName(), clone.getStorageName());
        assertEquals(obj.getName(), clone.getName());
        assertEquals(obj.getType(), clone.getType());
        assertEquals(obj.getLocale(), clone.getLocale());
        assertEquals(obj.getAccessPermissions(), clone.getAccessPermissions());
        assertEquals(obj.getEditPermission(), clone.getEditPermission());
        assertEquals(obj.getProperties(), clone.getProperties());
        assertEquals(obj.getSkin(), clone.getSkin());
    }
}
