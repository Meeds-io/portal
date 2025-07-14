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
package org.exoplatform.component.test;

import static junit.framework.Assert.*;

import org.exoplatform.container.PortalContainer;
import org.junit.Rule;
import org.junit.Test;

public class KernelRuleTestScopedTestCase {

    @Rule
    public KernelLifeCycle kernel = new KernelLifeCycle();

    /** . */
    private PortalContainer container;

    @ConfiguredBy({})
    @Test
    public void testA() {
        test();
        assertNull(kernel.getContainer().getComponentInstance(CustomService.class));
    }

    @ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-jdbc-configuration.xml") })
    @Test
    public void testB() throws Exception {
        test();
        assertNotNull(kernel.getContainer().getComponentInstance(CustomService.class));
    }

    private void test() {
        if (container == null) {
            container = kernel.getContainer();
        } else {
            assertNotSame(container, kernel.getContainer());
        }
    }
}
