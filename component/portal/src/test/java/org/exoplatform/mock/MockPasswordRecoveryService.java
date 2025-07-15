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
package org.exoplatform.mock;

import org.exoplatform.services.organization.User;
import org.exoplatform.web.login.recovery.ChangePasswordConnector;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.gatein.wci.security.Credentials;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

public class MockPasswordRecoveryService implements PasswordRecoveryService {
  
  ChangePasswordConnector mockChangePasswordConnector;
  
  public MockPasswordRecoveryService() {
    this.mockChangePasswordConnector = new MockChangePasswordConnector();
    
  }
  
  @Override
  public void addConnector(ChangePasswordConnector connector) {
  
  }
  
  @Override
  public String verifyToken(String tokenId, String type) {
    return null;
  }
  
  @Override
  public String verifyToken(String tokenId) {
    return null;
  }
  
  @Override
  public boolean changePass(String tokenId, String tokenType, String username, String password) {
    return false;
  }
  
  @Override
  public boolean sendRecoverPasswordEmail(User user, Locale defaultLocale, HttpServletRequest req) {
    return false;
  }
  
  @Override
  public boolean sendOnboardingEmail(User user, Locale defaultLocale, StringBuilder url) {
    return false;
  }
  
  @Override
  public String sendExternalRegisterEmail(String sender, String email, Locale locale, String space, StringBuilder url) throws
                                                                                                                       Exception {
    return null;
  }
  
  @Override
  public boolean sendAccountCreatedConfirmationEmail(String sender, Locale locale, StringBuilder url) {
    return false;
  }

  @Override
  public boolean allowChangePassword(String username) throws Exception {
    return true;
  }
  
  @Override
  public String getPasswordRecoverURL(String tokenId, String lang) {
    return null;
  }
  
  @Override
  public String getOnboardingURL(String tokenId, String lang) {
    return null;
  }
  
  @Override
  public String getExternalRegistrationURL(String tokenId, String lang) {
    return null;
  }
  
  @Override
  public ChangePasswordConnector getActiveChangePasswordConnector() {
    return this.mockChangePasswordConnector;
  }

  @Override
  public void deleteToken(String tokenId, String type) {
    // Delete Token
  }

  @Override
  public boolean sendAccountVerificationEmail(String data, String username, String firstName, String lastName, String email, Locale locale, StringBuilder url) {
    return false;
  }
}
