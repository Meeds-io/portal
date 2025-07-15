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

import java.util.Map;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

/**
 * A base class for tests that provides base function test.
 *
 */
public abstract class AbstractGateInTest extends BaseGateInTest {

    /** . */
    protected final Log log = ExoLogger.getLogger(getClass());

    protected AbstractGateInTest() {
    }

    protected AbstractGateInTest(String name) {
        super(name);
    }

    protected void beforeRunBare() {
        //
    }

    /**
     * After the run base, it should not throw anything as it is executed in a finally clause.
     */
    protected void afterRunBare() {
        //
    }

    @Override
    public final void runBare() throws Throwable {
        // Patch a bug with maven that does not pass properly the system property
        // with an empty value
        if ("org.hsqldb.jdbcDriver".equals(System.getProperty("gatein.test.datasource.driver"))) {
            System.setProperty("gatein.test.datasource.password", "");
        }

        //
        log.info("Running unit test:" + getName());
        for (Map.Entry<?, ?> entry : System.getProperties().entrySet()) {
            if (entry.getKey() instanceof String) {
                String key = (String) entry.getKey();
                log.debug(key + "=" + entry.getValue());
            }
        }

        //
        beforeRunBare();

        //
        try {
          super.runBare();
        } catch (Throwable throwable) {
          // Log before running 'finally' block which may throw another exception
          log.error("Unit test " + getName() + " did not complete", throwable);
          throw throwable;
        } finally {
          afterRunBare();
        }
      }
}
