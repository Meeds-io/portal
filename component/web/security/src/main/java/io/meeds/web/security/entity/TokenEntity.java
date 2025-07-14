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
package io.meeds.web.security.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "PortalToken")
@Table(name = "PORTAL_TOKENS")
@NamedQuery(name = "PortalToken.findByTokenId", query = "SELECT t FROM PortalToken t WHERE t.tokenId = :tokenId")
@NamedQuery(name = "PortalToken.findByUser", query = "SELECT t FROM PortalToken t WHERE t.username = :username")
@NamedQuery(name = "PortalToken.deleteExpiredTokens", query = "DELETE FROM PortalToken t WHERE t.expirationTime < :expireTime")
@NamedQuery(name = "PortalToken.deleteTokensByUserAndType", query="DELETE FROM PortalToken t WHERE t.username = :username AND t.tokenType = :tokenType")
public class TokenEntity implements Serializable {
    private static final long serialVersionUID = 6633792468705838255L;

    @Id
    @SequenceGenerator(name="SEQ_GATEIN_TOKEN_ID_GENERATOR", sequenceName="SEQ_GATEIN_TOKEN_ID_GENERATOR", allocationSize = 1)
    @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_GATEIN_TOKEN_ID_GENERATOR")
    @Column(name = "ID")
    private Long            id;

    @Column(name = "TOKEN_ID")
    private String tokenId;

    @Column(name = "TOKEN_HASH")
    private String tokenHash;

    @Column(name = "USERNAME")
    private String            username;

    @Column(name = "PASSWORD", length = 500)
    private String            password;

    @Column(name="EXPIRATION_TIME", nullable = false)
    private Long expirationTime;
    
    @Column(name="TOKEN_TYPE", nullable = false)
    private String tokenType;
    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getExpirationTime() {
        return expirationTime != null && expirationTime > 0 ? new Date(expirationTime) : null;
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = (expirationTime != null ? expirationTime.getTime() : -1);
    }

    public String getTokenType() {
      return tokenType;
    }

    public void setTokenType(String tokenType) {
      this.tokenType = tokenType;
    }
}
