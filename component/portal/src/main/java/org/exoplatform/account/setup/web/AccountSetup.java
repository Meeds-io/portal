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
package org.exoplatform.account.setup.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.security.Credentials;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class AccountSetup extends HttpServlet {

  private static final Log    LOG                   = ExoLogger.getLogger(AccountSetup.class);

  private static final long   serialVersionUID      = 6467955354840693802L;

  private final static String USER_NAME_ACCOUNT     = "username";

  private final static String FIRST_NAME_ACCOUNT    = "firstNameAccount";

  private final static String LAST_NAME_ACCOUNT     = "lastNameAccount";

  private final static String EMAIL_ACCOUNT         = "emailAccount";

  private final static String USER_PASSWORD_ACCOUNT = "password";

  private final static String ADMIN_PASSWORD        = "adminPassword";

  private final static String INITIAL_URI_PARAM     = "initialURI";

  private final static String ACCOUNT_SETUP_BUTTON  = "setupbutton";

  private final static String SETUP_SKIP_BUTTON     = "skipform";

  private AccountSetupService accountSetupService;

  public AccountSetup() {
    accountSetupService = PortalContainer.getInstance().getComponentInstanceOfType(AccountSetupService.class);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String redirectURI;
    String accountsetupbutton = request.getParameter(ACCOUNT_SETUP_BUTTON);

    if (accountsetupbutton.equals(SETUP_SKIP_BUTTON) || accountSetupService.mustSkipAccountSetup()) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Direct access to Account Setup Form.");
      }
      accountSetupService.setSkipSetup(true);
      redirectURI = "/" + PortalContainer.getCurrentPortalContainerName();
    } else {
      String userNameAccount = request.getParameter(USER_NAME_ACCOUNT);
      String firstNameAccount = request.getParameter(FIRST_NAME_ACCOUNT);
      String lastNameAccount = request.getParameter(LAST_NAME_ACCOUNT);
      String emailAccount = request.getParameter(EMAIL_ACCOUNT);
      String userPasswordAccount = request.getParameter(USER_PASSWORD_ACCOUNT);
      String adminPassword = request.getParameter(ADMIN_PASSWORD);

      accountSetupService.createAccount(userNameAccount,
                                        firstNameAccount,
                                        lastNameAccount,
                                        emailAccount,
                                        userPasswordAccount,
                                        adminPassword);

      // Redirect to requested page
      HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
        @Override
        public String getContextPath() {
          return "/portal";
        }
      };
      Credentials credentials = new Credentials(userNameAccount, userPasswordAccount);
      ServletContainerFactory.getServletContainer().login(wrappedRequest, response, credentials);;
      redirectURI = "/" + PortalContainer.getCurrentPortalContainerName();
    }
    response.setCharacterEncoding("UTF-8");
    response.sendRedirect(redirectURI);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

}
