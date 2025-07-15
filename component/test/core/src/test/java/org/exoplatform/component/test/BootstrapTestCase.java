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

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.exoplatform.commons.InitialContextInitializer2;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.naming.InitialContextInitializer;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-jdbc-configuration.xml") })
public class BootstrapTestCase extends AbstractKernelTest {

    /** . */
    private String previousTmpDirPath = null;

    public void testRequestLifeCycle() {
        PortalContainer container = PortalContainer.getInstance();
        CustomService testService = (CustomService) container.getComponentInstanceOfType(CustomService.class);
        assertNull(testService.currentContainer);
        RequestLifeCycle.begin(container);
        assertNotNull(testService);
        assertSame(container, testService.currentContainer);
        RequestLifeCycle.end();
        assertNull(testService.currentContainer);
    }

    public void testDataSource() throws Exception {
        InitialContextInitializer initialContextInitializer = PortalContainer.getInstance().getComponentInstanceOfType(InitialContextInitializer.class);
        if (initialContextInitializer instanceof InitialContextInitializer2 startable) {
          startable.start();
        }
        DataSource ds = (DataSource) new InitialContext().lookup("jdbcexo");
        assertNotNull(ds);
        Connection conn = ds.getConnection();
        DatabaseMetaData databaseMD = conn.getMetaData();
        String db = databaseMD.getDatabaseProductName() + " " + databaseMD.getDatabaseProductVersion() + "."
                + databaseMD.getDatabaseMajorVersion() + "." + databaseMD.getDatabaseMinorVersion();
        String driver = databaseMD.getDriverName() + " " + databaseMD.getDriverVersion() + "."
                + databaseMD.getDriverMajorVersion() + "." + databaseMD.getDriverMinorVersion();
        log.info("Using database " + db + " with driver " + driver);
        conn.close();
    }

    public void testTmpDir1() throws Exception {
        ensureTmpDir();
    }

    public void testTmpDir2() throws Exception {
        ensureTmpDir();
    }

    private void ensureTmpDir() {
        String tmpDirPath = System.getProperty("gatein.test.tmp.dir");
        assertNotNull(tmpDirPath);

        //
        if (previousTmpDirPath != null) {
            assertEquals(previousTmpDirPath, tmpDirPath);
        } else {
            previousTmpDirPath = tmpDirPath;
        }

        //
        File f = new File(tmpDirPath);
        assertTrue(f.exists());
        assertTrue(f.isDirectory());
        assertTrue(f.canWrite());
    }
}
