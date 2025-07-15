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

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.web.security.security.AbstractTokenService;

public abstract class AbstractTokenServiceTest<S extends AbstractTokenService<?, ?>> extends AbstractKernelTest {
    protected S service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        begin();
    }

    @Override
    public void tearDown() throws Exception {
        end();
        super.tearDown();
    }

    public abstract void testGetToken() throws Exception;

    public abstract void testGetAllToken() throws Exception;

    public abstract void testSize() throws Exception;

    public abstract void testDeleteToken() throws Exception;

    public abstract void testCleanExpiredTokens() throws Exception;
    
    public abstract void testGetTokenWithType() throws Exception;
    public abstract void testGetTokenWithWrongType() throws Exception;

    public abstract void testGetAllTokenWithType() throws Exception;
    
    public abstract void testSizeWithType() throws Exception;
    
    public abstract void testDeleteTokenWithType() throws Exception;
    
    public abstract void testCleanExpiredTokensWithType() throws Exception;
}
