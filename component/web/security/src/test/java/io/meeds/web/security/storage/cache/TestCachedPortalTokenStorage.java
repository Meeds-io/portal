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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.web.security.dao.TokenDAO;
import io.meeds.web.security.dao.TokenDAOImpl;
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

  private static final String         TOKEN_TYPE  = "cache-test";

  /**
   * A user whose all tokens a test deletes, which no other test creates
   */
  private static final String         TOKEN_OWNER = "cache-test-owner";

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
    storage.deleteTokensByUsernameAndType(TOKEN_OWNER, TOKEN_TYPE);
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

  public void testDeleteOfAllTheTokensOfAUserEvictsThemOnly() throws Exception {
    createToken("owner-token", TOKEN_OWNER);
    createToken("mary-token", "mary");

    storage.deleteTokenOfUser(TOKEN_OWNER);

    assertNull(storage.getToken("owner-token"));
    assertNotNull(cache.get("mary-token"));
  }

  public void testCleanOfExpiredTokensClearsTheCache() throws Exception {
    storage.createToken(new TokenData("jdoe-expired-token",
                                      "hash",
                                      "jdoe",
                                      new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)),
                                      TOKEN_TYPE));
    createToken("mary-token", "mary");

    storage.cleanExpired();

    assertNull(storage.getToken("jdoe-expired-token"));
    assertNull(cache.get("mary-token"));
  }

  public void testCleanThatRemovesNoTokenKeepsTheCache() throws Exception {
    // No expired token is left in the store the token tests share
    storage.cleanExpired();
    createToken("mary-token", "mary");

    storage.cleanExpired();

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

  public void testAFailedDeleteOfAllTheTokensOfAUserDeletesNone() throws Exception {
    // The store itself, whatever DAO the container the tests share was built with
    TokenDAO tokenDAO = new TokenDAOImpl();
    TokenDAO failingDAO = spy(tokenDAO);
    // The store fails on the second token
    AtomicInteger deletes = new AtomicInteger();
    doAnswer(invocation -> {
      if (deletes.incrementAndGet() > 1) {
        throw new IllegalStateException("store unavailable");
      }
      return invocation.callRealMethod();
    }).when(failingDAO).delete(any());
    CachedPortalTokenStorage failingStorage =
                                            new CachedPortalTokenStorage(getContainer().getComponentInstanceOfType(CacheService.class),
                                                                         failingDAO);
    failingStorage.createToken(token("owner-token", TOKEN_OWNER));
    failingStorage.createToken(token("owner-second-token", TOKEN_OWNER));
    try {
      try {
        failingStorage.deleteTokenOfUser(TOKEN_OWNER);
        fail("The failure of the store reaches the caller");
      } catch (IllegalStateException e) {
        // The second delete failed
      }

      // Read from the store, not from the persistence context of the rolled
      // back transaction: the tokens of the user are all kept
      entityManagerService.getEntityManager().clear();
      assertEquals(2, tokenDAO.findByUsername(TOKEN_OWNER).size());
    } finally {
      tokenDAO.deleteTokensByUsernameAndType(TOKEN_OWNER, TOKEN_TYPE);
    }
  }

  private static TokenData token(String tokenId, String username) {
    return new TokenData(tokenId,
                         "hash",
                         username,
                         new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)),
                         TOKEN_TYPE);
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
