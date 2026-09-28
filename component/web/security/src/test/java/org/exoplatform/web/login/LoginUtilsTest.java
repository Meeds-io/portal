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
package org.exoplatform.web.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class LoginUtilsTest {

  @Mock
  private HttpServletRequest  request;

  @Mock
  private HttpServletResponse response;

  /**
   * LoginHandler writes the cookie at "/": the deletion must name that path,
   * whatever the context path of the request carrying it.
   */
  @Test
  void clearRememberMeCookieDeletesItAtTheRootPath() {
    lenient().when(request.getContextPath()).thenReturn("/portal");
    when(request.isSecure()).thenReturn(true);

    LoginUtils.clearRememberMeCookie(request, response);

    ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookie.capture());
    assertEquals(LoginUtils.COOKIE_NAME, cookie.getValue().getName());
    assertEquals("", cookie.getValue().getValue());
    assertEquals("/", cookie.getValue().getPath());
    assertEquals(0, cookie.getValue().getMaxAge());
    assertTrue(cookie.getValue().isHttpOnly());
    assertTrue(cookie.getValue().getSecure());
  }

}
