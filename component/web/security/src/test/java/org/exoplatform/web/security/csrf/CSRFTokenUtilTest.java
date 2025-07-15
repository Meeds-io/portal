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
package org.exoplatform.web.security.csrf;

import static org.exoplatform.web.security.csrf.CSRFTokenUtil.CSRF_TOKEN;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Test;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

public class CSRFTokenUtilTest {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = (HttpSession)mock(HttpSession.class);

    @After
    public void tearDown() {
        ConversationState.setCurrent(null);
    }

    @Test
    public void shouldGenerateTokenWhenAuthenticated() {
        startSessionAs("root");
        when(request.getRemoteUser()).thenReturn("root");
        String token = CSRFTokenUtil.getToken(request);

        assertNotNull(token);
    }

    @Test
    public void shouldGenerateTokenWhenNonAuthenticated() {
        startSessionAs("root");
        when(request.getRemoteUser()).thenReturn(null);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(CSRF_TOKEN)).thenReturn("B56F1F537F0001012F46539853E23BCD");
        String token = CSRFTokenUtil.getToken(request);

        assertNotNull(token);
    }

    @Test
    public void shouldGetSameTokenWhenFetchingTokenTwiceWhenAuthenticated() {

        startSessionAs("root");
        when(request.getRemoteUser()).thenReturn("root");
        String token1 = CSRFTokenUtil.getToken(request);
        String token2 = CSRFTokenUtil.getToken(request);

        assertNotNull(token1);
        assertNotNull(token2);
        assertEquals(token1, token2);
    }

    @Test
    public void shouldGetSameTokenWhenFetchingTokenTwiceWhenNonAuthenticated() {

        startSessionAs("root");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(CSRF_TOKEN)).thenReturn("B56F1F537F0001012F46539853E23BCD");
        String token1 = CSRFTokenUtil.getToken(request);
        String token2 = CSRFTokenUtil.getToken(request);

        assertNotNull(token1);
        assertNotNull(token2);
        assertEquals(token1, token2);
        assertEquals(token1,"B56F1F537F0001012F46539853E23BCD");
    }

    @Test
    public void shouldSucceedCheckWhenSameTokenInRequestWhenAuthenticated() {
        startSessionAs("root");
        when(request.getRemoteUser()).thenReturn("root");
        String token = CSRFTokenUtil.getToken(request);

        when(request.getParameter(CSRF_TOKEN)).thenReturn(token);

        boolean check = CSRFTokenUtil.check(request);

        assertTrue(check);
    }

    @Test
    public void shouldSucceedCheckWhenSameTokenInRequestWhenNonAuthenticated() {
        startSessionAs("root");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(CSRF_TOKEN)).thenReturn("B56F1F537F0001012F46539853E23BCD");
        String token = CSRFTokenUtil.getToken(request);

        when(request.getParameter(CSRF_TOKEN)).thenReturn(token);

        boolean check = CSRFTokenUtil.check(request);

        assertTrue(check);
    }

    @Test
    public void shouldFailCheckWhenNoTokenInRequestWhenAuthenticated() {
        startSessionAs("root");

        when(request.getParameter(CSRF_TOKEN)).thenReturn(null);
        when(request.getRemoteUser()).thenReturn("root");

        boolean check = CSRFTokenUtil.check(request);

        assertFalse(check);
    }

    @Test
    public void shouldFailCheckWhenNoTokenInRequestWhenNonAuthenticated() {
        startSessionAs("root");

        when(request.getSession()).thenReturn(session);
        when(request.getParameter(CSRF_TOKEN)).thenReturn(null);
        when(request.getRemoteUser()).thenReturn(null);

        boolean check = CSRFTokenUtil.check(request);

        assertFalse(check);
    }

    @Test
    public void shouldFailCheckWhenWrongTokenInRequestInAuthenticatedMode() {
        startSessionAs("root");
        when(request.getParameter(CSRF_TOKEN)).thenReturn("WrongToken");
        when(request.getRemoteUser()).thenReturn("root");

        boolean check = CSRFTokenUtil.check(request);

        assertFalse(check);
    }

    @Test
    public void shouldFailCheckWhenWrongTokenInRequestInNonAuthenticatedMode() {
        startSessionAs("root");
        when(request.getSession()).thenReturn(session);
        when(request.getParameter(CSRF_TOKEN)).thenReturn("WrongToken");
        when(request.getRemoteUser()).thenReturn(null);

        boolean check = CSRFTokenUtil.check(request);

        assertFalse(check);
    }

    protected void startSessionAs(String user) {
        Identity identity = new Identity(user, Collections.EMPTY_LIST);
        ConversationState state = new ConversationState(identity);
        ConversationState.setCurrent(state);
    }

}