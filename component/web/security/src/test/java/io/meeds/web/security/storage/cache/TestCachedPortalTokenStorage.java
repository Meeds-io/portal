/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.web.security.storage.cache;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.web.security.model.TokenData;
import io.meeds.web.security.storage.PortalTokenStorage;

import jakarta.persistence.EntityTransaction;

/**
 * What a delete leaves in the token cache. In a cluster, the tokens a delete
 * removes may be cached on another node, under ids the deleting node does not
 * hold: the cache of every node is cleared, which a clear of the kernel cache
 * does, and a token the cache holds stands here for the one another node holds.
 */
@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.security-configuration-local.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration-local.xml") })
public class TestCachedPortalTokenStorage extends AbstractKernelTest {

  private static final String         TOKEN_TYPE = "cache-test";

  private PortalTokenStorage          storage;

  private ExoCache<String, TokenData> cache;

  private EntityManagerService        entityManagerService;

  @Override
  public void setUp() throws Exception {
    PortalContainer container = getContainer();
    RequestLifeCycle.begin(container);
    storage = container.getComponentInstanceOfType(PortalTokenStorage.class);
    cache = container.getComponentInstanceOfType(CacheService.class).getCacheInstance(CachedPortalTokenStorage.CACHE_NAME);
    entityManagerService = container.getComponentInstanceOfType(EntityManagerService.class);
  }

  @Override
  public void tearDown() throws Exception {
    // The token store is shared with the other token tests, which count its
    // rows: nothing is left behind
    storage.deleteTokensByUsernameAndType("jdoe", TOKEN_TYPE);
    storage.deleteTokensByUsernameAndType("mary", TOKEN_TYPE);
    RequestLifeCycle.end();
  }

  public void testDeleteOfTheTokensOfAUserClearsTheCache() throws Exception {
    createToken("jdoe-token", "jdoe");
    createToken("mary-token", "mary");
    assertNotNull(cache.get("mary-token"));

    storage.deleteTokensByUsernameAndType("jdoe", TOKEN_TYPE);

    assertNull(storage.getToken("jdoe-token"));
    assertNull(cache.get("mary-token"));
  }

  public void testDeleteThatRemovesNoTokenKeepsTheCache() throws Exception {
    createToken("mary-token", "mary");

    storage.deleteTokensByUsernameAndType("jdoe", TOKEN_TYPE);

    assertNotNull(cache.get("mary-token"));
  }

  public void testTokenCachedAgainBeforeTheCommitIsEvictedAfterIt() throws Exception {
    createToken("jdoe-token", "jdoe");
    TokenData token = cache.get("jdoe-token");
    EntityTransaction transaction = entityManagerService.getEntityManager().getTransaction();
    transaction.begin();
    try {
      storage.deleteTokensByUsernameAndType("jdoe", TOKEN_TYPE);
      // A read on another thread, before the commit, still finds the token
      cache.put("jdoe-token", token);
    } finally {
      transaction.commit();
    }

    assertNull(cache.get("jdoe-token"));
  }

  private void createToken(String tokenId, String username) throws Exception {
    storage.createToken(new TokenData(tokenId,
                                      "hash",
                                      username,
                                      new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)),
                                      TOKEN_TYPE));
    // Loaded, hence cached
    assertNotNull(storage.getToken(tokenId));
  }
}
