/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.web.security;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.web.security.security.CookieTokenService;

/**
 *
 * @author <a href="mailto:haithanh0809@gmail.com">Hai Thanh Nguyen</a>
 *
 */

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.security-configuration-local.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration-local.xml")})
public abstract class AbstractCookieTokenServiceTest extends AbstractTokenServiceTest<CookieTokenService> {
    public String type="testType";

    @Override
    public void testGetToken() throws Exception {
        String tokenId = service.createToken("root");
        assertEquals(service.getValidityTime(), 2);

        GateInToken token = service.getToken(tokenId);
        assertEquals(token.getUsername(), "root");
        service.deleteToken(tokenId);
    }

    @Override
    public void testGetAllToken() throws Exception {
        /* Do nothing there is no CookieTokenService.getAllTokens(); */
    }

    @Override
    public void testSize() throws Exception {
        String token = service.createToken("root");
        assertEquals(service.size(), 1);
        service.deleteToken(token);
    }

    @Override
    public void testDeleteToken() throws Exception {
        String tokenId = service.createToken("root");
        GateInToken deletedToken = service.deleteToken(tokenId);
        assertNotNull(deletedToken);
        assertNotSame(service.getToken(tokenId), deletedToken);
        assertNull(service.getToken(tokenId));
        assertEquals(0, service.size());
        service.deleteToken(tokenId);
    }

    @Override
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
        String tokenId = service.createToken("root",type);
        assertEquals(service.getValidityTime(), 2);
        
        GateInToken token = service.getToken(tokenId,type);
        assertEquals(token.getUsername(), "root");
        service.deleteToken(tokenId,type);
    }

    @Override
    public void testGetTokenWithWrongType() throws Exception {
        String tokenId = service.createToken("root",type);
        assertEquals(service.getValidityTime(), 2);

        GateInToken token = service.getToken(tokenId,"otherType");
        assertNull(token);
        service.deleteToken(tokenId,type);
    }
    
    @Override
    public void testGetAllTokenWithType() throws Exception {
        /* Do nothing there is no CookieTokenService.getAllTokens(); */
    }
    
    @Override
    public void testSizeWithType() throws Exception {
        String token = service.createToken("root",type);
        assertEquals(service.size(), 1);
        service.deleteToken(token,type);
    }
    
    @Override
    public void testDeleteTokenWithType() throws Exception {
        String tokenId = service.createToken("root",type);
        GateInToken deletedToken = service.deleteToken(tokenId,type);
        assertNotNull(deletedToken);
        assertNotSame(service.getToken(tokenId,type), deletedToken);
        assertNull(service.getToken(tokenId,type));
        assertEquals(0, service.size());
        service.deleteToken(tokenId,type);
    }
    
    @Override
    public void testCleanExpiredTokensWithType() throws Exception {
        assertEquals(2, service.getValidityTime());
        String tokenId1 = service.createToken("user1",type);
        assertEquals(1, service.size());
        
        Thread.sleep(2100);
        service.cleanExpiredTokens();
        assertEquals(0, service.size());
        
        service.deleteToken(tokenId1,type);
    }

}
