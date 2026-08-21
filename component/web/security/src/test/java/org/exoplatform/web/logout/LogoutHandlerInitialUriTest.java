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
package org.exoplatform.web.logout;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat; // NOSONAR
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

@RunWith(Parameterized.class)
public class LogoutHandlerInitialUriTest {

  private static final String CONTEXT_PATH = "/portal";          // NOSONAR

  private static final String SERVER_NAME  = "meeds.example.org";

  private final String        initialURI;

  private final String        expectedRedirectURI;

  private LogoutHandler       logoutHandler;

  public LogoutHandlerInitialUriTest(String initialURI, String expectedRedirectURI) {
    this.initialURI = initialURI;
    this.expectedRedirectURI = expectedRedirectURI;
  }

  @Before
  public void setUp() {
    logoutHandler = new LogoutHandler();
  }

  @Parameterized.Parameters(name = "{index}: initialURI={0} -> {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { // NOSONAR
      // --------------------------------------------------------------------
      // Valid internal redirects
      // --------------------------------------------------------------------
      { "/portal", "/portal" }, // NOSONAR
      { "/portal/", "/portal/" },
      { "/portal/dw/home", "/portal/dw/home" }, // NOSONAR
      { "/portal/dw/home?param=value", "/portal/dw/home?param=value" },
      { "/portal/dw/home#section", "/portal/dw/home#section" },
      { "/portal/dw/home?param=value#section", "/portal/dw/home?param=value#section" },
      { "/portal/account-deactivated", "/portal/account-deactivated" },

      // --------------------------------------------------------------------
      // Empty / missing values keep the historical default redirect
      // --------------------------------------------------------------------
      { null, "/" },
      { "", "/" },
      { " ", "/" },
      { "\t", "/" },

      // --------------------------------------------------------------------
      // External absolute URLs
      // --------------------------------------------------------------------
      { "http://redirect.ywh.at", CONTEXT_PATH },
      { "https://redirect.ywh.at", CONTEXT_PATH },
      { "HTTP://redirect.ywh.at", CONTEXT_PATH },
      { "HTTPS://redirect.ywh.at", CONTEXT_PATH },
      { "http://meeds.example.org.evil.tld/portal", CONTEXT_PATH },
      { "https://meeds.example.org.evil.tld/portal", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Specific reported issue
      // URI#getHost() may return null, but browser may treat it externally
      // --------------------------------------------------------------------
      { "http:@redirect.ywh.at", CONTEXT_PATH },
      { "https:@redirect.ywh.at", CONTEXT_PATH },
      { "http:redirect.ywh.at", CONTEXT_PATH },
      { "https:redirect.ywh.at", CONTEXT_PATH },
      { "javascript:alert(1)", CONTEXT_PATH },
      { "data:text/html,evil", CONTEXT_PATH },
      { "mailto:test@example.org", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Scheme-relative URLs
      // --------------------------------------------------------------------
      { "//redirect.ywh.at", CONTEXT_PATH },
      { "///redirect.ywh.at", CONTEXT_PATH },
      { "//meeds.example.org/portal", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Relative paths not under portal context
      // --------------------------------------------------------------------
      { "/", CONTEXT_PATH },
      { "/dw/home", CONTEXT_PATH },
      { "/other-context/home", CONTEXT_PATH },
      { "/portal-other/home", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Non absolute-path references
      // --------------------------------------------------------------------
      { "portal/dw/home", CONTEXT_PATH },
      { "./portal/dw/home", CONTEXT_PATH },
      { "../portal/dw/home", CONTEXT_PATH },
      { "redirect.ywh.at", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Backslash based browser parsing bypasses
      // --------------------------------------------------------------------
      { "\\\\redirect.ywh.at", CONTEXT_PATH },
      { "/\\redirect.ywh.at", CONTEXT_PATH },
      { "/portal\\redirect.ywh.at", CONTEXT_PATH },
      { "/portal/dw\\home", CONTEXT_PATH },
      { "https:\\\\redirect.ywh.at", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Encoded slash / backslash bypass attempts
      // --------------------------------------------------------------------
      { "%2F%2Fredirect.ywh.at", CONTEXT_PATH },
      { "%2f%2fredirect.ywh.at", CONTEXT_PATH },
      { "/portal/%2F%2Fredirect.ywh.at", CONTEXT_PATH },
      { "/portal/%2f%2fredirect.ywh.at", CONTEXT_PATH },
      { "/portal/%5Credirect.ywh.at", CONTEXT_PATH },
      { "/portal/%5credirect.ywh.at", CONTEXT_PATH },
      { "/portal/dw%2Fhome", CONTEXT_PATH },
      { "/portal/dw%5Chome", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Encoded absolute URLs
      // --------------------------------------------------------------------
      { "http%3A%2F%2Fredirect.ywh.at", CONTEXT_PATH },
      { "https%3A%2F%2Fredirect.ywh.at", CONTEXT_PATH },
      { "http%3a%2f%2fredirect.ywh.at", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // CRLF / header injection attempts
      // --------------------------------------------------------------------
      { "/portal/dw/home\r\nLocation: http://redirect.ywh.at", CONTEXT_PATH },
      { "/portal/dw/home\nLocation: http://redirect.ywh.at", CONTEXT_PATH },
      { "/portal/dw/home\rLocation: http://redirect.ywh.at", CONTEXT_PATH },
      { "/portal/dw/home%0d%0aLocation:%20http://redirect.ywh.at", CONTEXT_PATH },
      { "/portal/dw/home%0D%0ALocation:%20http://redirect.ywh.at", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Malformed URI values
      // --------------------------------------------------------------------
      { "http://[::1", CONTEXT_PATH },
      { "/portal/dw/home[", CONTEXT_PATH },
      { "/portal/dw/home]", CONTEXT_PATH },
      { "/portal/dw/home|test", CONTEXT_PATH },

      // --------------------------------------------------------------------
      // Normalization cases
      // Depending on RedirectUrlValidator implementation, these may normalize.
      // Keep these expected values aligned with the validator behavior.
      // --------------------------------------------------------------------
      { "/portal/dw/../home", "/portal/home" },
      { "/portal/./dw/home", "/portal/dw/home" }
    });
  }

  @Test
  @SneakyThrows
  public void shouldSanitizeInitialURI() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("initialURI")).thenReturn(initialURI);
    when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    when(request.getServerName()).thenReturn(SERVER_NAME);

    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

    String result = invokeGetRedirectUri(request, response);

    assertThat(result, is(expectedRedirectURI));
  }

  @SneakyThrows
  private String invokeGetRedirectUri(HttpServletRequest request, HttpServletResponse response) {
    Method method = LogoutHandler.class.getDeclaredMethod("getRedirectUri", HttpServletRequest.class, HttpServletResponse.class);
    method.setAccessible(true);// NOSONAR
    return (String) method.invoke(logoutHandler, request, response);
  }
}
