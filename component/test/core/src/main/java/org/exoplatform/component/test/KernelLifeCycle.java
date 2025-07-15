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

import org.exoplatform.container.PortalContainer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * <p>
 * The kernel life cycle is a JUnit rule for simplifying the life cycle of eXo kernel in unit tests. The kernel configuration
 * uses annotation for declaring the kernel configuration files to load. The rule can either be scoped at the class level or at
 * the test level depending on the life cycle of the kernel required for the tested class.
 * </p>
 *
 * <p>
 * The following example runs a kernel for the whole test case:
 * </p>
 *
 * <pre>
 * {@code
 * &#064;ConfiguredBy(&#064;ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/my-configuration.xml"))
 * public class ClassScopedTestCase {
 *
 *   &#064;ClassRule
 *   public static final KernelLifeCycle kernel = new KernelLifeCycle();
 *   private PortalContainer container;
 *
 *   &#064;Test
 *   public void testFoo() {
 *     container = kernel.getContainer();
 *   }
 *
 *   &#064;Test
 *   public void testBar() {
 *     assertSame(container, kernel.getContainer());
 *   }
 * }
 * }
 * </pre>
 *
 * <p>
 * The kernel life cycle can also follow the test methods of a test case:
 * </p>
 *
 * <pre>
 * {@code
 * public class MethodScopedTestCase {
 *
 *   &#064;Rule
 *   public final KernelLifeCycle kernel = new KernelLifeCycle();
 *   private PortalContainer container;
 *
 *   &#064;Test
 *   &#064;ConfiguredBy(&#064;ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/my-configuration.xml"))
 *   public void testFoo() {
 *     container = kernel.getContainer();
 *   }
 *
 *   &#064;Test
 *   &#064;ConfiguredBy(&#064;ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/my-configuration.xml"))
 *   public void testBar() {
 *     assertNotSame(container, kernel.getContainer());
 *   }
 * }
 * }
 * </pre>
 *
 */
public class KernelLifeCycle implements TestRule {

    /** . */
    private KernelBootstrap bootstrap;

    public PortalContainer getContainer() {
        return bootstrap != null ? bootstrap.getContainer() : null;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                bootstrap = new KernelBootstrap(Thread.currentThread().getContextClassLoader());

                // Add configuration if any
                ConfiguredBy config = description.getAnnotation(ConfiguredBy.class);
                if (config != null) {
                    bootstrap.addConfiguration(config);
                }

                //
                bootstrap.boot();

                //
                try {
                    base.evaluate();
                } finally {
                    bootstrap.dispose();
                    bootstrap = null;
                }
            }
        };
    }
}
