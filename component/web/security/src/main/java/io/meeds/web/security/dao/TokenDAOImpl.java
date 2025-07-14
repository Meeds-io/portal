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
package io.meeds.web.security.dao;

import java.util.List;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

import io.meeds.web.security.entity.TokenEntity;

import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

public class TokenDAOImpl extends GenericDAOJPAImpl<TokenEntity, Long> implements TokenDAO {

    @Override
    public TokenEntity findByTokenId(String tokenId) {
        TypedQuery<TokenEntity> query = getEntityManager().createNamedQuery("PortalToken.findByTokenId", TokenEntity.class);
        query.setParameter("tokenId", tokenId);
        try {
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public List<TokenEntity> findByUsername(String username) {
        TypedQuery<TokenEntity> query = getEntityManager().createNamedQuery("PortalToken.findByUser", TokenEntity.class);
        query.setParameter("username", username);
        return query.getResultList();
    }

    @Override
    @ExoTransactional
    public void cleanExpired() {
        Query query = getEntityManager().createNamedQuery("PortalToken.deleteExpiredTokens");
        query.setParameter("expireTime", System.currentTimeMillis());
        query.executeUpdate();
    }

    @Override
    @ExoTransactional
    public void deleteTokensByUsernameAndType(String username, String tokenType) {
        Query query = getEntityManager().createNamedQuery("PortalToken.deleteTokensByUserAndType");
        query.setParameter("username", username);
        query.setParameter("tokenType", tokenType);
        query.executeUpdate();
    }
}
