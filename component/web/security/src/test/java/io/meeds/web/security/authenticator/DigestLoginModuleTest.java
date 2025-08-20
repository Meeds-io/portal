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
package io.meeds.web.security.authenticator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;

import io.meeds.web.security.service.DigestAuthenticatorService;

import lombok.SneakyThrows;

@RunWith(MockitoJUnitRunner.class)
public class DigestLoginModuleTest {

  private static final String        SECRET = "secret"; // NOSONAR

  @Mock
  private DigestAuthenticatorService digestService;

  @Mock
  private IdentityRegistry           identityRegistry;

  @Mock
  private ExoContainer               container;

  @Mock
  private Identity                   identity;

  private DigestLoginModule          module;

  private Subject                    subject;

  @Before
  public void setUp() {

    module = new DigestLoginModule() {
      @Override
      protected ExoContainer getContainer() {
        return container;
      }
    };

    DigestLoginModule.digestAuthenticatorService = null; // NOSONAR
    DigestLoginModule.identityRegistry = null; // NOSONAR
    when(container.getComponentInstanceOfType(DigestAuthenticatorService.class)).thenReturn(digestService);
    when(container.getComponentInstanceOfType(IdentityRegistry.class)).thenReturn(identityRegistry);

    subject = new Subject();
    Map<String, Object> sharedState = new HashMap<>();

    try {
      module.initialize(subject, createTestCallbackHandler(), sharedState, new HashMap<>());
    } catch (Exception e) {
      // EXpected
    }
    when(identity.getUserId()).thenReturn("john");
    when(identity.getRoles()).thenReturn(Arrays.asList("users", "admin"));
  }

  @Test
  @SneakyThrows
  public void testLoginAndCommitSuccess() {
    when(digestService.validateUser(anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString())).thenReturn("john");
    when(digestService.createIdentity("john")).thenReturn(identity);

    boolean loginResult = module.login();
    assertTrue("Login should succeed", loginResult);

    boolean commitResult = module.commit();
    assertTrue("Commit should succeed", commitResult);

    verify(identityRegistry).register(identity);

    Set<Principal> principals = subject.getPrincipals();
    assertTrue(principals.stream().anyMatch(r -> r.getName().equals("users")));
    assertTrue(principals.stream().anyMatch(r -> r.getName().equals("admin")));
    assertTrue(principals.stream().anyMatch(r -> r.getName().equals("john")));
  }

  @Test
  @SneakyThrows
  public void testLoginFailsWhenValidationFails() {
    when(digestService.validateUser(anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString()))
                                                 .thenReturn(null);

    boolean loginResult = module.login();
    assertFalse("Login should fail when validateUser returns null", loginResult);
  }

  @Test
  @SneakyThrows
  public void testCommitFailsWhenIdentityIsNull() {
    boolean commitResult = module.commit();
    assertFalse("Commit should fail when identity is null", commitResult);
  }

  private CallbackHandler createTestCallbackHandler() {
    return callbacks -> {
      ((NameCallback) callbacks[0]).setName("john");
      ((PasswordCallback) callbacks[1]).setPassword(SECRET.toCharArray()); // NOSONAR
      ((TextInputCallback) callbacks[2]).setText("nonceVal");
      ((TextInputCallback) callbacks[3]).setText("ncVal");
      ((TextInputCallback) callbacks[4]).setText("cnonceVal");
      ((TextInputCallback) callbacks[5]).setText("auth");
      ((TextInputCallback) callbacks[6]).setText("realm");
      ((TextInputCallback) callbacks[7]).setText("digestA2Val");
      ((TextInputCallback) callbacks[8]).setText("MD5");
      ((TextInputCallback) callbacks[9]).setText("BASIC");
    };
  }
}
