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
import jakarta.servlet.http.*;

import org.exoplatform.container.PortalContainer;

public class AccountSetupViewServlet extends HttpServlet {

  private final static String AS_JSP_RESOURCE    = "/WEB-INF/jsp/welcome-screens/accountSetup.jsp";

  private AccountSetupService accountSetupService;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    PortalContainer container = PortalContainer.getInstance();
    accountSetupService = container.getComponentInstanceOfType(AccountSetupService.class);
    if (accountSetupService.mustSkipAccountSetup()) {
      response.sendRedirect("/");
    } else {
      // Redirect to requested page
      HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
        @Override
        public String getContextPath() {
          return "/portal";
        }
      };
      container.getPortalContext().getRequestDispatcher(AS_JSP_RESOURCE).forward(wrappedRequest, response);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

}
