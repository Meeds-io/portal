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
package org.exoplatform.web.logout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.web.login.LoginUtils;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

/**
 * The remember-me cookie is written by LoginHandler at path "/" and a browser
 * deletes a cookie only on a Set-Cookie naming the same path: logout must
 * clear it at "/", whatever the context path of the logout request.
 */
@ExtendWith(MockitoExtension.class)
class LogoutHandlerRememberMeCookieTest {

  private static final String CONTEXT_PATH = "/portal";

  private static final String TOKEN        = "rememberme-token";

  @Mock
  private HttpServletRequest  request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private CookieTokenService  cookieTokenService;

  @Test
  void logoutClearsTheRememberMeCookieAtTheRootPath() {
    lenient().when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(LoginUtils.COOKIE_NAME, TOKEN) });
    try (MockedStatic<AbstractTokenService> tokenService = mockStatic(AbstractTokenService.class)) {
      tokenService.when(() -> AbstractTokenService.getInstance(CookieTokenService.class)).thenReturn(cookieTokenService);

      invokeDeleteRememberMeCookie();

      verify(cookieTokenService).deleteToken(TOKEN);
      ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
      verify(response).addCookie(cookie.capture());
      assertEquals(LoginUtils.COOKIE_NAME, cookie.getValue().getName());
      assertEquals("/", cookie.getValue().getPath());
      assertEquals(0, cookie.getValue().getMaxAge());
    }
  }

  @Test
  void logoutWithoutRememberMeCookieSetsNoCookie() {
    when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("JSESSIONID", "session") });

    invokeDeleteRememberMeCookie();

    verify(response, never()).addCookie(any());
  }

  @SneakyThrows
  private void invokeDeleteRememberMeCookie() {
    Method method = LogoutHandler.class.getDeclaredMethod("deleteRememberMeCookie",
                                                          HttpServletRequest.class,
                                                          HttpServletResponse.class);
    method.setAccessible(true); // NOSONAR
    method.invoke(new LogoutHandler(), request, response);
  }

}
