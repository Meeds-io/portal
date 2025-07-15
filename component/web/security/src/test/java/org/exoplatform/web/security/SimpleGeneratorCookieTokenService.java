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

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.web.security.hash.NoSaltedHashService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

import io.meeds.web.security.storage.PortalTokenStorage;

/**
 * CookieTokenService with changed mechanism for token generation (testing purposes)
 *
 */
public class SimpleGeneratorCookieTokenService extends CookieTokenService {

    private int counter = 0;
    private int noRandom = 0;

    public SimpleGeneratorCookieTokenService(InitParams initParams, PortalTokenStorage tokenStore)
            throws TokenServiceInitializationException {
        super(replaceHashService(initParams), tokenStore);
    }

    
    public void resetCounter() {
        counter=0;
        noRandom=0;
    }
    /**
     * @param initParams
     * @return
     */
    private static InitParams replaceHashService(InitParams initParams) {
        ObjectParameter hashParam = new ObjectParameter();
        hashParam.setName(CookieTokenService.HASH_SERVICE_INIT_PARAM);
        hashParam.setObject(new NoSaltedHashService());
        initParams.addParameter(hashParam );
        return initParams;
    }

    @Override
    protected String nextTokenId() {
        counter++;
        return "rememberme" + counter / 2;
    }



    /* (non-Javadoc)
     * @see org.exoplatform.web.security.security.AbstractTokenService#nextRandom()
     */
    @Override
    protected String nextRandom() {
        noRandom++;
        return "random"+ String.valueOf(noRandom/2);
    }

    int getCounter() {
        return counter;
    }
}
