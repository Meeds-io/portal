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
package org.gatein.version;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
/**
 * Common GateIn version.
 *
 */
public class Version {
    public static final String productName;
    public static final String productVersion;
    public static final String implementationVersion;
    public static final String prettyVersion;

    private static final Log log = ExoLogger.getLogger(Version.class);

    static {
        URL url = Version.class.getProtectionDomain().getCodeSource().getLocation();

        Manifest manifest = null;

        try {
            InputStream stream = new URL(url.toString() + "META-INF/MANIFEST.MF").openStream();
            if (stream != null) {
                manifest = new Manifest(stream);
            }
        } catch (IOException e) {
            log.debug("Unable to get the MANIFEST.MF from the gatein portal common component jar.");
        }

        if (manifest != null) {
            productName = manifest.getMainAttributes().getValue("JBoss-Product-Release-Name");
            productVersion = manifest.getMainAttributes().getValue("JBoss-Product-Release-Version");
            implementationVersion = manifest.getMainAttributes().getValue("Implementation-Version");
        } else {
            productName = null;
            productVersion = "Unknown";
            implementationVersion = "Unknown";
        }
        String iVersion = implementationVersion == null ? "Unknown" : implementationVersion;
        String version = productVersion == null ? iVersion : productVersion;
        if (productName == null) {
            prettyVersion = String.format("GateIn Portal %s", iVersion);
        } else {
            prettyVersion = String.format("%s %s (GateIn Portal %s)", productName, version, iVersion);
        }
    }
}
