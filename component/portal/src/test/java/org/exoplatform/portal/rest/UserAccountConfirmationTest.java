/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UserAccountConfirmationTest {

  private static final String             ADMIN = "admin";

  @Mock
  private HttpServletRequest              request;

  @Mock
  private HttpSession                     session;

  @Mock
  private UserAccountConfirmationSettings settings;

  @Before
  public void setUp() {
    when(request.getRemoteUser()).thenReturn(ADMIN);
    when(settings.isEnabled()).thenReturn(true);
  }

  @Test
  public void testConfirmedSessionPasses() {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(UserAccountConfirmation.SESSION_ATTRIBUTE)).thenReturn(ADMIN);
    assertTrue(UserAccountConfirmation.isConfirmed(request, settings));
  }

  @Test
  public void testSessionWithoutMarkerIsRefused() {
    when(request.getSession(false)).thenReturn(session);
    assertFalse(UserAccountConfirmation.isConfirmed(request, settings));
  }

  @Test
  public void testNoSessionIsRefused() {
    assertFalse(UserAccountConfirmation.isConfirmed(request, settings));
  }

  @Test
  public void testMarkerOfAnotherUserIsRefused() {
    // A session shared after a re-login never carries another user's
    // confirmation
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(UserAccountConfirmation.SESSION_ATTRIBUTE)).thenReturn("john");
    assertFalse(UserAccountConfirmation.isConfirmed(request, settings));
  }

  @Test
  public void testCredentialsOfTheRequestChangeNothing() {
    when(request.getHeader("Authorization")).thenReturn("Basic YWRtaW46Z3RuCg==");
    when(request.getAuthType()).thenReturn(HttpServletRequest.BASIC_AUTH);
    assertFalse(UserAccountConfirmation.isConfirmed(request, settings));
    verify(request, never()).getHeader(anyString());
  }

  @Test
  public void testAnonymousRequestIsRefused() {
    when(request.getRemoteUser()).thenReturn(null);
    when(settings.hasExemptedUsers()).thenReturn(true);
    assertFalse(UserAccountConfirmation.isConfirmed(request, settings));
    verify(settings, never()).isExempted(anyString());
  }

  @Test
  public void testNoRequestIsRefused() {
    assertFalse(UserAccountConfirmation.isConfirmed(null, settings));
  }

  @Test
  public void testSwitchOffLetsEveryRequestThrough() {
    when(settings.isEnabled()).thenReturn(false);
    assertTrue(UserAccountConfirmation.isConfirmed(request, settings));
    assertTrue(UserAccountConfirmation.isConfirmed(null, settings));
  }

  @Test
  public void testExemptedTechnicalAccountPasses() {
    when(settings.hasExemptedUsers()).thenReturn(true);
    when(settings.isExempted(ADMIN)).thenReturn(true);
    assertTrue(UserAccountConfirmation.isConfirmed(request, settings));
  }

  @Test
  public void testNotExemptedAccountIsRefused() {
    when(settings.hasExemptedUsers()).thenReturn(true);
    when(settings.isExempted(ADMIN)).thenReturn(false);
    assertFalse(UserAccountConfirmation.isConfirmed(request, settings));
  }

  @Test
  public void testNoExemptedAccountMeansNoLookup() {
    when(settings.hasExemptedUsers()).thenReturn(false);
    assertFalse(UserAccountConfirmation.isConfirmed(request, settings));
    verify(settings, never()).isExempted(anyString());
  }

  @Test
  public void testMissingSettingsKeepsTheGuardOn() {
    assertFalse(UserAccountConfirmation.isConfirmed(request, null));
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(UserAccountConfirmation.SESSION_ATTRIBUTE)).thenReturn(ADMIN);
    assertTrue(UserAccountConfirmation.isConfirmed(request, null));
  }

  @Test
  public void testConfirmStoresTheConfirmingUser() {
    when(request.getSession(true)).thenReturn(session);
    UserAccountConfirmation.confirm(request, ADMIN);
    verify(session).setAttribute(UserAccountConfirmation.SESSION_ATTRIBUTE, ADMIN);
  }

  @Test
  public void testOtpRequiredResponse() {
    Response response = UserAccountConfirmation.otpRequired();
    assertEquals(403, response.getStatus());
    assertEquals("OTP_REQUIRED", response.getEntity());
  }

}
