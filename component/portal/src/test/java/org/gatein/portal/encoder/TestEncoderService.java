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
package org.gatein.portal.encoder;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.AbstractConfigTest;

/**
 * Test for {@link EncoderService}
 *
 */
@ConfiguredBy({
   @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.encoder-configuration.xml")})
public class TestEncoderService extends AbstractConfigTest {

    private EncoderService encoderService;

    @Override
    protected void setUp() throws Exception {
        PortalContainer portalContainer = PortalContainer.getInstance();
        this.encoderService = (EncoderService) portalContainer.getComponentInstanceOfType(EncoderService.class);
        super.setUp();
    }

    public void testEncoder() throws Exception {
        encodeDecodeTest("gtn", "6MSyXIj3kkQ=");
        encodeDecodeTest("blabla", "tstM3KRJOU4=");
        encodeDecodeTest("gogog", "zlGKEql9zxE=");
    }

    private void encodeDecodeTest(String plainText, String expectedEncoded) throws Exception {
        String encoded = encoderService.encode64(plainText);
        assertEquals(encoded, expectedEncoded);

        String decoded = encoderService.decode64(encoded);
        assertEquals(decoded, plainText);
    }

}
