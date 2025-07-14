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

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.security-configuration-local.xml") })
public class TestSimpleGeneratorService extends AbstractKernelTest {
    private SimpleGeneratorCookieTokenService service;
    String type="testtype";

    protected void setUp() throws Exception {
        PortalContainer container = getContainer();
        service = container.getComponentInstanceOfType(SimpleGeneratorCookieTokenService.class);
        Thread.sleep(1000); // for enough time initial database
        begin();
    }
    protected void tearDown() {
        end();
    }

    /**
     * Test that duplicated token is never generated
     */
    public void testDuplicatedTokenGeneration() throws Exception {
        service.resetCounter();
        String token1 = service.createToken("root1");
        assertEquals("rememberme0.random0", token1);
        assertEquals(service.getCounter(), 1);

        String token2 = service.createToken("root2");
        assertEquals("rememberme1.random1", token2);
        assertEquals(service.getCounter(), 2);

        String token3 = service.createToken("-root3");
        assertEquals("rememberme2.random2", token3);
        // Counter should be 4 now due to duplicated token generation
        assertEquals(service.getCounter(), 4);

        assertEquals("root1", service.getToken(token1).getUsername());
        assertEquals("root2", service.getToken(token2).getUsername());
        assertEquals("-root3", service.getToken(token3).getUsername());
        
        service.deleteToken(token1);
        service.deleteToken(token2);
        service.deleteToken(token3);
    }
    
    /**
     * Test that duplicated token is never generated
     */
    public void testDuplicatedTokenWithTypeGeneration() throws Exception {
        service.resetCounter();
        String token1 = service.createToken("root1",type);
        assertEquals("rememberme0.random0", token1);
        assertEquals(service.getCounter(), 1);
        
        String token2 = service.createToken("root2",type);
        assertEquals("rememberme1.random1", token2);
        assertEquals(service.getCounter(), 2);
        
        String token3 = service.createToken("-root3",type);
        assertEquals("rememberme2.random2", token3);
        // Counter should be 4 now due to duplicated token generation
        assertEquals(service.getCounter(), 4);
        
        assertEquals("root1", service.getToken(token1,type).getUsername());
        assertEquals("root2", service.getToken(token2,type).getUsername());
        assertEquals("-root3", service.getToken(token3,type).getUsername());
    
        service.deleteToken(token1,type);
        service.deleteToken(token2,type);
        service.deleteToken(token3,type);
    }
    
    public void testTokenValidationWithDifferentTypes() throws Exception {
        service.resetCounter();
        String token1 = service.createToken("root1",type);
        assertEquals("rememberme0.random0", token1);
        assertEquals(service.getCounter(), 1);
        assertNull(service.getToken(token1,"otherType"));
        service.deleteToken(token1,type);
    
    }
}
