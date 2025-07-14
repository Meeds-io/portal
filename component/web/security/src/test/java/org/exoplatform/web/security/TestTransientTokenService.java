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
package org.exoplatform.web.security;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.web.security.security.TransientTokenService;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.security-configuration-local.xml") })
public class TestTransientTokenService extends AbstractTokenServiceTest<TransientTokenService> {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        PortalContainer container = getContainer();
        service = container.getComponentInstanceOfType(TransientTokenService.class);
    }

    @Override
    public void testGetToken() throws Exception {
        String tokenId = service.createToken("root");
        assertEquals(service.getValidityTime(), 2);

        PortalToken token = service.getToken(tokenId);
        assertEquals(token.getUsername(), "root");
        service.deleteToken(tokenId);
    }

    @Override
    public void testGetAllToken() throws Exception {
        String tokenId1 = service.createToken("root1");
        String tokenId2 = service.createToken("root2");
        String[] tokens = service.getAllTokens();
        assertEquals(tokens.length, 2);

        PortalToken token1 = service.getToken(tokenId1);
        assertEquals(token1.getUsername(), "root1");

        PortalToken token2 = service.getToken(tokenId2);
        assertEquals(token2.getUsername(), "root2");
        service.deleteToken(tokenId1);
        service.deleteToken(tokenId2);
    }

    @Override
    public void testSize() throws Exception {
        String tokenId1 = service.createToken("root1");
        assertEquals(service.size(), 1);
        service.deleteToken(tokenId1);
    }

    @Override
    public void testDeleteToken() throws Exception {
        String tokenId = service.createToken("root");
        service.deleteToken(tokenId);
        assertNull(service.getToken(tokenId));
    }

    public void testCleanExpiredTokens() throws Exception {
        assertEquals(2, service.getValidityTime());
        String tokenId1 = service.createToken("user1");
        assertEquals(1, service.size());
        
        Thread.sleep(2100);
        service.cleanExpiredTokens();
        assertEquals(0, service.size());

        service.deleteToken(tokenId1);
    }
    
    @Override
    public void testGetTokenWithType() throws Exception {
        //TransientTokenService have no type for token
    }
    @Override
    public void testGetTokenWithWrongType() throws Exception {
        //TransientTokenService have no type for token
    }
    
    @Override
    public void testGetAllTokenWithType() throws Exception {
        //TransientTokenService have no type for token
    }
    
    @Override
    public void testSizeWithType() throws Exception {
        //TransientTokenService have no type for token
    
    }
    
    @Override
    public void testDeleteTokenWithType() throws Exception {
        //TransientTokenService have no type for token
    
    }
    
    @Override
    public void testCleanExpiredTokensWithType() throws Exception {
        //TransientTokenService have no type for token
    
    }
}
