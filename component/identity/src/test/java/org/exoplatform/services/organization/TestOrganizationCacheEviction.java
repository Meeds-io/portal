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
package org.exoplatform.services.organization;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.organization.cache.MembershipCacheKey;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.MembershipDAOImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.UserProfileDAOImpl;
import org.exoplatform.services.organization.idm.cache.CacheableUserHandlerImpl;
import org.exoplatform.services.organization.impl.MembershipImpl;

/**
 * When the user and membership caches are evicted around an IDM write: before
 * the write's listeners run, and again once the IDM transaction has ended.
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"),
})
public class TestOrganizationCacheEviction extends AbstractKernelTest {// NOSONAR

  private static final String GROUP_ID        = "/platform/users";

  private static final String MEMBERSHIP_TYPE = "member";

  /** A type the store keeps as a role, where "member" is kept as an association */
  private static final String ROLE_TYPE       = "manager";

  private OrganizationService organizationService;                    // NOSONAR

  private String              userName;

  @Override
  protected void setUp() throws Exception {
    organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    begin();
    userName = UUID.randomUUID().toString();
    User user = organizationService.getUserHandler().createUserInstance(userName);
    user.setFirstName("First Name");
    user.setLastName("Last Name");
    user.setEmail(userName + "@test.com");
    organizationService.getUserHandler().createUser(user, true);
    restartTransaction();
  }

  @Override
  protected void tearDown() throws Exception {
    organizationService.getUserHandler().removeUser(userName, true);
    end();
  }

  @SuppressWarnings("unchecked")
  public void testUserIsEvictedBeforeItsListenersRun() throws Exception {
    assertTrue(organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY).isEnabled());
    ExoCache<String, User> userCache = cacheHandler().getUserCache();
    assertNotNull(userCache.get(userName));

    List<Object> cachedWhenNotified = new ArrayList<>();
    UserEventListener listener = new UserEventListener() {
      @Override
      public void postSetEnabled(User user) {
        if (userName.equals(user.getUserName())) {
          cachedWhenNotified.add(String.valueOf(userCache.get(userName)));
        }
      }
    };
    organizationService.getUserHandler().addUserEventListener(listener);
    try {
      organizationService.getUserHandler().setEnabled(userName, false, true);
    } finally {
      organizationService.getUserHandler().removeUserEventListener(listener);
    }

    assertEquals(List.of("null"), cachedWhenNotified);
    assertFalse(organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY).isEnabled());
  }

  public void testUserWrittenUnderAnotherSpellingIsEvictedByItsStoredName() throws Exception {
    String typedName = userName.toUpperCase();
    assertNotNull("The store matches names whatever their case",
                  organizationService.getUserHandler().findUserByName(typedName, UserStatus.ANY));
    assertTrue(organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY).isEnabled());

    organizationService.getUserHandler().setEnabled(typedName, false, true);

    assertFalse(organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY).isEnabled());
  }

  @SuppressWarnings("unchecked")
  public void testProfileIsEvictedBeforeItsListenersRun() throws Exception {
    UserProfileHandler profileHandler = organizationService.getUserProfileHandler();
    saveProfile("first", true);
    restartTransaction();
    assertEquals("first", profileHandler.findUserProfileByName(userName).getAttribute("user.jobtitle"));
    ExoCache<String, Object> userProfileCache = cacheHandler().getUserProfileCache();
    assertNotNull(userProfileCache.get(userName));

    List<Object> cachedWhenNotified = new ArrayList<>();
    UserProfileEventListener listener = new UserProfileEventListener() {
      @Override
      public void postSave(UserProfile profile, boolean isNew) {
        if (userName.equals(profile.getUserName())) {
          cachedWhenNotified.add(String.valueOf(userProfileCache.get(userName)));
        }
      }
    };
    profileHandler.addUserProfileEventListener(listener);
    try {
      saveProfile("second", true);
    } finally {
      profileHandler.removeUserProfileEventListener(listener);
    }

    assertEquals(List.of("null"), cachedWhenNotified);
    assertEquals("second", profileHandler.findUserProfileByName(userName).getAttribute("user.jobtitle"));
  }

  @SuppressWarnings("unchecked")
  public void testProfileIsNotCachedAgainBeforeTheCommit() throws Exception {
    UserProfileHandler profileHandler = organizationService.getUserProfileHandler();
    saveProfile("first", true);
    restartTransaction();
    profileHandler.findUserProfileByName(userName);
    ExoCache<String, Object> userProfileCache = cacheHandler().getUserProfileCache();
    Object cachedProfile = userProfileCache.get(userName);
    assertNotNull(cachedProfile);

    saveProfile("second", false);
    // Another thread or node reads the profile before the commit
    userProfileCache.put(userName, cachedProfile);
    restartTransaction();

    assertEquals("second", profileHandler.findUserProfileByName(userName).getAttribute("user.jobtitle"));
  }

  public void testLoginUnderAnotherSpellingIsRefusedOnceTheUserIsDisabled() throws Exception {
    String typedName = userName.toUpperCase();
    assertTrue(organizationService.getUserHandler().findUserByName(typedName, UserStatus.ANY).isEnabled());

    organizationService.getUserHandler().setEnabled(userName, false, true);

    assertFalse(organizationService.getUserHandler().findUserByName(typedName, UserStatus.ANY).isEnabled());
    assertThrows(DisabledUserException.class, () -> organizationService.getUserHandler().authenticate(typedName, "any"));
  }

  public void testMembershipsReadUnderAnotherSpellingFollowTheWrites() throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    String typedName = userName.toUpperCase();
    assertTrue(membershipHandler.findMembershipsByUser(typedName).isEmpty());

    link();
    restartTransaction();
    Collection<Membership> memberships = membershipHandler.findMembershipsByUser(typedName);
    assertEquals(1, memberships.size());
    Membership membership = memberships.iterator().next();
    assertNotNull(membershipHandler.findMembershipByUserGroupAndType(typedName, GROUP_ID, MEMBERSHIP_TYPE));

    // Revoked by an id that carries another spelling, as a client may send it
    membershipHandler.removeMembership(membership.getId().replace(userName, typedName), false);
    restartTransaction();

    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
    assertTrue(membershipHandler.findMembershipsByUser(typedName).isEmpty());
    assertNull(membershipHandler.findMembershipByUserGroupAndType(typedName, GROUP_ID, MEMBERSHIP_TYPE));

    // Granted again, read under the other spelling, revoked by the stored id
    link();
    restartTransaction();
    assertNotNull(membershipHandler.findMembershipByUserGroupAndType(typedName, GROUP_ID, MEMBERSHIP_TYPE));
    membershipHandler.removeMembership(membership.getId(), false);
    restartTransaction();

    assertNull(membershipHandler.findMembershipByUserGroupAndType(typedName, GROUP_ID, MEMBERSHIP_TYPE));
  }

  public void testProfileReadUnderAnotherSpellingFollowsTheWrites() throws Exception {
    UserProfileHandler profileHandler = organizationService.getUserProfileHandler();
    String typedName = userName.toUpperCase();
    saveProfile("first", true);
    restartTransaction();
    assertEquals("first", profileHandler.findUserProfileByName(typedName).getAttribute("user.jobtitle"));

    saveProfile("second", false);
    restartTransaction();

    assertEquals("second", profileHandler.findUserProfileByName(typedName).getAttribute("user.jobtitle"));
  }

  public void testProfileGotUnderAnotherSpellingFollowsTheWrites() throws Exception {
    UserProfileDAOImpl profileHandler = (UserProfileDAOImpl) organizationService.getUserProfileHandler();
    String typedName = userName.toUpperCase();
    saveProfile("first", true);
    restartTransaction();
    assertEquals("first", profileHandler.getProfile(typedName).getAttribute("user.jobtitle"));

    saveProfile("second", false);
    restartTransaction();

    assertEquals("second", profileHandler.getProfile(typedName).getAttribute("user.jobtitle"));
  }

  @SuppressWarnings("unchecked")
  public void testRemovedUserIsEvictedBeforeItsListenersRun() throws Exception {
    link();
    restartTransaction();
    assertNotNull(organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY));
    assertEquals(1, organizationService.getMembershipHandler().findMembershipsByUser(userName).size());
    ExoCache<String, User> userCache = cacheHandler().getUserCache();
    ExoCache<MembershipCacheKey, Object> membershipCache = cacheHandler().getMembershipCache();
    organizationService.getUserProfileHandler().findUserProfileByName(userName);
    ExoCache<String, Object> userProfileCache = cacheHandler().getUserProfileCache();
    assertNotNull(userCache.get(userName));
    assertNotNull(userProfileCache.get(userName));
    // A membership of the user cached under its own key, which no role
    // removal of the user reaches
    MembershipImpl cachedMembership = new MembershipImpl();
    cachedMembership.setUserName(userName);
    cachedMembership.setGroupId("/organization");
    cachedMembership.setMembershipType("manager");
    MembershipCacheKey membershipKey = new MembershipCacheKey(cachedMembership);
    membershipCache.put(membershipKey, cachedMembership);

    List<Object> cachedWhenNotified = new ArrayList<>();
    UserEventListener listener = new UserEventListener() {
      @Override
      public void postDelete(User user) {
        if (userName.equals(user.getUserName())) {
          cachedWhenNotified.add(String.valueOf(userCache.get(userName)));
          cachedWhenNotified.add(String.valueOf(userProfileCache.get(userName)));
          cachedWhenNotified.add(String.valueOf(membershipCache.get(membershipKey)));
        }
      }
    };
    organizationService.getUserHandler().addUserEventListener(listener);
    try {
      organizationService.getUserHandler().removeUser(userName, true);
    } finally {
      organizationService.getUserHandler().removeUserEventListener(listener);
    }

    assertEquals(List.of("null", "null", "null"), cachedWhenNotified);
  }

  @SuppressWarnings("unchecked")
  public void testMembershipListIsEvictedBeforeItsListenersRun() throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
    ExoCache<MembershipCacheKey, Object> membershipCache = cacheHandler().getMembershipCache();
    MembershipCacheKey listKey = new MembershipCacheKey(userName, null, null);
    assertNotNull(membershipCache.get(listKey));

    List<Object> cachedWhenNotified = new ArrayList<>();
    MembershipEventListener listener = new MembershipEventListener() {
      @Override
      public void postSave(Membership membership, boolean isNew) {
        if (userName.equals(membership.getUserName())) {
          cachedWhenNotified.add(String.valueOf(membershipCache.get(listKey)));
        }
      }
    };
    membershipHandler.addMembershipEventListener(listener);
    try {
      link();
    } finally {
      membershipHandler.removeMembershipEventListener(listener);
    }

    assertEquals(List.of("null"), cachedWhenNotified);
    assertEquals(1, membershipHandler.findMembershipsByUser(userName).size());
  }

  @SuppressWarnings("unchecked")
  public void testRemovedUserIsNotCachedAgainBeforeTheCommit() throws Exception {
    organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    organizationService.getUserProfileHandler().findUserProfileByName(userName);
    ExoCache<String, Object> userCache = cacheHandler().getUserCache();
    ExoCache<String, Object> userProfileCache = cacheHandler().getUserProfileCache();
    Object cachedUser = userCache.get(userName);
    Object cachedProfile = userProfileCache.get(userName);
    assertNotNull(cachedUser);
    assertNotNull(cachedProfile);

    organizationService.getUserHandler().removeUser(userName, false);
    // Another thread or node reads the user and its profile before the commit
    userCache.put(userName, cachedUser);
    userProfileCache.put(userName, cachedProfile);
    restartTransaction();

    assertNull(userCache.get(userName));
    assertNull(userProfileCache.get(userName));
  }

  @SuppressWarnings("unchecked")
  public void testFailedLinkEvictsTheMembershipList() throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
    MembershipCacheKey listKey = new MembershipCacheKey(userName, null, null);
    assertNotNull(cacheHandler().getMembershipCache().get(listKey));

    MembershipEventListener listener = new MembershipEventListener() {
      @Override
      public void preSave(Membership membership, boolean isNew) {
        throw new IllegalStateException("refused");
      }
    };
    membershipHandler.addMembershipEventListener(listener);
    try {
      link();
      fail("The listener refuses the link");
    } catch (IllegalStateException e) {
      // expected
    } finally {
      membershipHandler.removeMembershipEventListener(listener);
    }

    assertNull(cacheHandler().getMembershipCache().get(listKey));
  }

  @SuppressWarnings("unchecked")
  public void testRevokedMembershipIsNotCachedAgainBeforeTheCommit() throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    link();
    restartTransaction();
    Collection<Membership> memberships = membershipHandler.findMembershipsByUser(userName);
    assertEquals(1, memberships.size());

    membershipHandler.removeMembership(memberships.iterator().next().getId(), false);
    // Another thread or node loads the list before the commit, and caches the
    // committed state, the membership still granted
    cacheHandler().getMembershipCache().put(new MembershipCacheKey(userName, null, null), memberships);
    restartTransaction();

    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
  }

  @SuppressWarnings("unchecked")
  public void testUserLoadInFlightDoesNotCacheWhatItReadBeforeTheWrite() throws Exception {
    User stale = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    ExoCache<String, User> userCache = cacheHandler().getUserCache();
    userCache.remove(userName);

    assertLoadInFlightIsNotCached(organizationService.getUserHandler(), "futureUserCache", userCache, userName, stale,
                                  () -> organizationService.getUserHandler().setEnabled(userName, false, false));
  }

  @SuppressWarnings("unchecked")
  public void testMembershipLoadInFlightDoesNotCacheWhatItReadBeforeTheWrite() throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    link();
    restartTransaction();
    Collection<Membership> stale = membershipHandler.findMembershipsByUser(userName);
    assertEquals(1, stale.size());
    ExoCache<MembershipCacheKey, Object> membershipCache = cacheHandler().getMembershipCache();
    MembershipCacheKey listKey = new MembershipCacheKey(userName, null, null);
    membershipCache.remove(listKey);

    assertLoadInFlightIsNotCached(membershipHandler, "futureMembershipCache", membershipCache, listKey, stale,
                                  () -> membershipHandler.removeMembership(stale.iterator().next().getId(), false));
  }

  @SuppressWarnings("unchecked")
  public void testMembershipByKeyLoadInFlightDoesNotCacheWhatItReadBeforeTheWrite() throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    link();
    restartTransaction();
    Membership stale = membershipHandler.findMembershipByUserGroupAndType(userName, GROUP_ID, MEMBERSHIP_TYPE);
    assertNotNull(stale);
    ExoCache<MembershipCacheKey, Object> membershipCache = cacheHandler().getMembershipCache();
    MembershipCacheKey key = new MembershipCacheKey(stale);
    membershipCache.remove(key);

    assertLoadInFlightIsNotCached(membershipHandler, "futureMembershipCache", membershipCache, key, stale,
                                  () -> membershipHandler.removeMembership(stale.getId(), false));
  }

  @SuppressWarnings("unchecked")
  public void testProfileLoadInFlightDoesNotCacheWhatItReadBeforeTheWrite() throws Exception {
    saveProfile("first", true);
    restartTransaction();
    Object stale = organizationService.getUserProfileHandler().findUserProfileByName(userName);
    ExoCache<String, Object> userProfileCache = cacheHandler().getUserProfileCache();
    userProfileCache.remove(userName);

    assertLoadInFlightIsNotCached(organizationService.getUserProfileHandler(), "futureUserProfile", userProfileCache,
                                  userName, stale, () -> saveProfile("second", false));
  }

  @SuppressWarnings("unchecked")
  public void testFailedSaveEvictsTheMembershipList() throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
    MembershipCacheKey listKey = new MembershipCacheKey(userName, null, null);
    assertNotNull(cacheHandler().getMembershipCache().get(listKey));

    MembershipImpl membership = new MembershipImpl();
    membership.setUserName(userName);
    membership.setGroupId(GROUP_ID);
    membership.setMembershipType(MEMBERSHIP_TYPE);
    MembershipEventListener listener = new MembershipEventListener() {
      @Override
      public void preSave(Membership saved, boolean isNew) {
        throw new IllegalStateException("refused");
      }
    };
    membershipHandler.addMembershipEventListener(listener);
    try {
      // saveMembership is the DAO's, reached through createMembership
      ((MembershipDAOImpl) membershipHandler).saveMembership(membership, true);
      fail("The listener refuses the save");
    } catch (IllegalStateException e) {
      // expected
    } finally {
      membershipHandler.removeMembershipEventListener(listener);
    }

    assertNull(cacheHandler().getMembershipCache().get(listKey));
  }

  /**
   * Starts a load of the key on another thread, through a loader that reads
   * the stale value and waits, runs the write while it waits, then lets it
   * finish: the write's eviction must keep it from caching what it read.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <K extends Serializable, V> void assertLoadInFlightIsNotCached(Object handler,
                                                                         String fieldName,
                                                                         ExoCache<K, V> cache,
                                                                         K key,
                                                                         V stale,
                                                                         Write write) throws Exception {
    Field field = futureField(handler.getClass(), fieldName);
    Object original = field.get(handler);
    CountDownLatch loading = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    FutureExoCache<K, V, Object> blocked = new FutureExoCache<>((context, k) -> {
      loading.countDown();
      release.await(10, SECONDS);
      return stale;
    }, cache);
    field.set(handler, blocked);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<V> load = executor.submit(() -> blocked.get(null, key));
      assertTrue(loading.await(10, SECONDS));

      write.run();
      release.countDown();
      assertSame(stale, load.get(10, SECONDS));

      assertNull(cache.get(key));
    } finally {
      release.countDown();
      executor.shutdownNow();
      field.set(handler, original);
    }
  }

  private static Field futureField(Class<?> type, String name) throws NoSuchFieldException {
    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      try {
        Field field = current.getDeclaredField(name);
        field.setAccessible(true);
        return field;
      } catch (NoSuchFieldException e) {
        // look in the superclass
      }
    }
    throw new NoSuchFieldException(name);
  }

  @SuppressWarnings("unchecked")
  public void testDisablingADisabledUserEvictsACopyStillEnabled() throws Exception {
    organizationService.getUserHandler().setEnabled(userName, false, true);
    restartTransaction();
    User disabled = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    assertFalse(disabled.isEnabled());
    // A copy cached before the first write, by another thread or node
    org.exoplatform.services.organization.idm.UserImpl staleCopy =
        (org.exoplatform.services.organization.idm.UserImpl) ((ExtendedCloneable) disabled).clone();
    staleCopy.setEnabled(true);
    cacheHandler().getUserCache().put(userName, staleCopy);

    organizationService.getUserHandler().setEnabled(userName, false, true);

    assertFalse(organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY).isEnabled());
  }

  @SuppressWarnings("unchecked")
  public void testLinkingAnExistingMembershipEvictsAListThatMissesIt() throws Exception {
    link(ROLE_TYPE);
    restartTransaction();
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    assertEquals(1, membershipHandler.findMembershipsByUser(userName).size());
    // A list cached before the first link, by another thread or node
    cacheHandler().getMembershipCache().put(new MembershipCacheKey(userName, null, null), new ArrayList<Membership>());

    link(ROLE_TYPE);

    assertEquals(1, membershipHandler.findMembershipsByUser(userName).size());
  }

  @SuppressWarnings("unchecked")
  public void testSavingAnExistingMembershipEvictsAListThatMissesIt() throws Exception {
    link(ROLE_TYPE);
    restartTransaction();
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    Membership membership = membershipHandler.findMembershipsByUser(userName).iterator().next();
    // A list cached before the first save, by another thread or node
    cacheHandler().getMembershipCache().put(new MembershipCacheKey(userName, null, null), new ArrayList<Membership>());

    ((MembershipDAOImpl) membershipHandler).saveMembership(membership, false);

    assertEquals(1, membershipHandler.findMembershipsByUser(userName).size());
  }

  public void testTheMembershipsRemovedWithTheirUserCarryTheStoredName() throws Exception {
    link(ROLE_TYPE);
    restartTransaction();
    PicketLinkIDMOrganizationServiceImpl idmOrganizationService =
        getContainer().getComponentInstanceOfType(PicketLinkIDMOrganizationServiceImpl.class);
    PicketLinkIDMService idmService = getContainer().getComponentInstanceOfType(PicketLinkIDMService.class);
    List<Boolean> storedNames = new ArrayList<>();
    MembershipDAOImpl membershipDAO = new MembershipDAOImpl(idmOrganizationService, idmService) {
      @Override
      protected void onMembershipWritten(Membership membership, boolean storedUserName) {
        storedNames.add(storedUserName);
      }
    };

    membershipDAO.removeMembershipByUser(userName, false);

    assertTrue("The removed role and the user's list are evicted", storedNames.size() >= 2);
    assertFalse("The hook resolves no user per removed membership", storedNames.contains(false));
  }

  @SuppressWarnings("unchecked")
  public void testAUserReadUnderAnotherSpellingDropsItsEntryOnThisNodeOnly() throws Exception {
    CacheableUserHandlerImpl userHandler = (CacheableUserHandlerImpl) organizationService.getUserHandler();
    Field userCacheField = CacheableUserHandlerImpl.class.getDeclaredField("userCache");
    userCacheField.setAccessible(true);
    ExoCache<String, User> userCache = (ExoCache<String, User>) userCacheField.get(userHandler);
    List<String> removals = new ArrayList<>();
    ExoCache<String, User> recordingCache = (ExoCache<String, User>) Proxy.newProxyInstance(getClass().getClassLoader(),
                                                                                           new Class<?>[] { ExoCache.class },
                                                                                           (proxy, method, args) -> {
                                                                                             if (method.getName()
                                                                                                       .startsWith("remove")) {
                                                                                               removals.add(method.getName());
                                                                                             }
                                                                                             return method.invoke(userCache,
                                                                                                                  args);
                                                                                           });
    userCacheField.set(userHandler, recordingCache);
    try {
      assertNotNull(userHandler.findUserByName(userName.toUpperCase(), UserStatus.ANY));
    } finally {
      userCacheField.set(userHandler, userCache);
    }

    assertEquals(List.of("removeLocal"), removals);
  }

  @SuppressWarnings("unchecked")
  public void testRevokingAGoneMembershipEvictsAListThatStillHoldsIt() throws Exception {
    link(ROLE_TYPE);
    restartTransaction();
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    Collection<Membership> memberships = membershipHandler.findMembershipsByUser(userName);
    assertEquals(1, memberships.size());
    Membership membership = memberships.iterator().next();
    membershipHandler.removeMembership(membership.getId(), false);
    restartTransaction();
    // A list cached before the first revoke, by another thread or node
    cacheHandler().getMembershipCache().put(new MembershipCacheKey(userName, null, null), new ArrayList<>(memberships));

    assertNull(membershipHandler.removeMembership(membership.getId(), false));

    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
  }

  @SuppressWarnings("unchecked")
  public void testRemovingAMissingProfileEvictsACopyStillCached() throws Exception {
    saveProfile("first", false);
    restartTransaction();
    UserProfileHandler profileHandler = organizationService.getUserProfileHandler();
    UserProfile profile = profileHandler.findUserProfileByName(userName);
    assertNotNull(profile);
    profileHandler.removeUserProfile(userName, false);
    restartTransaction();
    // A profile cached before the first removal, by another thread or node
    cacheHandler().getUserProfileCache().put(userName, profile);

    assertNull(profileHandler.removeUserProfile(userName, false));

    assertNull(profileHandler.findUserProfileByName(userName));
  }

  public void testRemovingTheMembershipsOfAUserEvictsItsAssociations() throws Exception {
    link();
    restartTransaction();
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    assertEquals(1, membershipHandler.findMembershipsByUser(userName).size());
    assertNotNull(membershipHandler.findMembershipByUserGroupAndType(userName, GROUP_ID, MEMBERSHIP_TYPE));

    membershipHandler.removeMembershipByUser(userName, false);

    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
    assertNull(membershipHandler.findMembershipByUserGroupAndType(userName, GROUP_ID, MEMBERSHIP_TYPE));
  }

  @SuppressWarnings("unchecked")
  public void testRemovingTheMembershipsOfAUserWithNoneEvictsAStaleList() throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
    MembershipImpl stale = new MembershipImpl();
    stale.setUserName(userName);
    stale.setGroupId(GROUP_ID);
    stale.setMembershipType(ROLE_TYPE);
    // A list cached before another thread or node revoked its last membership
    cacheHandler().getMembershipCache().put(new MembershipCacheKey(userName, null, null), new ArrayList<>(List.of(stale)));

    membershipHandler.removeMembershipByUser(userName, false);

    assertTrue(membershipHandler.findMembershipsByUser(userName).isEmpty());
  }

  @SuppressWarnings("unchecked")
  public void testRemovingAMissingUserEvictsItsCachedMemberships() throws Exception {
    String missingUser = UUID.randomUUID().toString();
    MembershipCacheKey listKey = new MembershipCacheKey(missingUser, null, null);
    // A list cached before the user was removed, by another thread or node
    cacheHandler().getMembershipCache().put(listKey, new ArrayList<Membership>());

    assertNull(organizationService.getUserHandler().removeUser(missingUser, false));

    assertNull(cacheHandler().getMembershipCache().get(listKey));
  }

  @FunctionalInterface
  private interface Write {
    void run() throws Exception;
  }

  private void saveProfile(String jobTitle, boolean broadcast) throws Exception {
    UserProfile profile = organizationService.getUserProfileHandler().createUserProfileInstance(userName);
    profile.setAttribute("user.jobtitle", jobTitle);
    organizationService.getUserProfileHandler().saveUserProfile(profile, broadcast);
  }

  private void link() throws Exception {
    link(MEMBERSHIP_TYPE);
  }

  private void link(String membershipType) throws Exception {
    Group group = organizationService.getGroupHandler().findGroupById(GROUP_ID);
    MembershipType type = organizationService.getMembershipTypeHandler().findMembershipType(membershipType);
    assertNotNull(group);
    assertNotNull(type);
    User user = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    organizationService.getMembershipHandler().linkMembership(user, group, type, true);
  }

  private OrganizationCacheHandler cacheHandler() {
    return getContainer().getComponentInstanceOfType(OrganizationCacheHandler.class);
  }
}
