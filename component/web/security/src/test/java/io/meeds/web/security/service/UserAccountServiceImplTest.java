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
package io.meeds.web.security.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.rest.UserAccountService;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountServiceImplTest {

  @Mock
  private OtpService             otpService;

  @InjectMocks
  private UserAccountServiceImpl userAccountService;

  /**
   * The method is fixed server-side: a code sent for another flow never
   * confirms an administrator session, and the code is consumed
   */
  @Test
  public void testConfirmSessionValidatesAndConsumesTheDedicatedMethodCode() throws Exception {
    userAccountService.confirmSession("admin", "12345");
    verify(otpService).validateAndConsumeOtp("admin", UserAccountService.OTP_METHOD, "12345");
  }

  @Test(expected = IllegalAccessException.class)
  public void testConfirmSessionRefusesAnInvalidCode() throws Exception {
    doThrow(new IllegalAccessException()).when(otpService)
                                         .validateAndConsumeOtp("admin", UserAccountService.OTP_METHOD, "00000");
    userAccountService.confirmSession("admin", "00000");
  }

}
