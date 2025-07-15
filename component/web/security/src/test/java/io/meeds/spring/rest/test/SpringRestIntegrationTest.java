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
package io.meeds.spring.rest.test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.meeds.spring.module.rest.TestController;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;

@SpringBootTest(classes = { TestController.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@RunWith(SpringRunner.class)
public class SpringRestIntegrationTest {

  private static final String   ANONYMOUS_ENDPOINT      = "/test/anonymous";

  private static final String   USERS_ENDPOINT          = "/test/users";

  private static final String   ADMINISTRATORS_ENDPOINT = "/test/administrators";

  private static final String   ADMIN_USER              = "admin";

  private static final String   SIMPLE_USER             = "simple";

  private static final String   TEST_PASSWORD           = "testPassword";

  @Autowired
  private SecurityFilterChain   filterChain;

  @Autowired
  private WebApplicationContext context;

  private MockMvc               mockMvc;

  @Before
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
  }

  @Test
  public void adminAccessAdminEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(ADMINISTRATORS_ENDPOINT).with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  @Test
  public void adminAccessUserEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(USERS_ENDPOINT).with(testAdminUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  public void adminAccessAnonymousEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(ANONYMOUS_ENDPOINT).with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  @Test
  public void userAccessUserEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(USERS_ENDPOINT).with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  public void userAccessAdminEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(ADMINISTRATORS_ENDPOINT).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  public void userAccessAnonymousEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(ANONYMOUS_ENDPOINT).with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  public void anonymousAccessUserEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(USERS_ENDPOINT));
    response.andExpect(status().isForbidden());
  }

  @Test
  public void anonymousAccessAdminEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(ADMINISTRATORS_ENDPOINT));
    response.andExpect(status().isForbidden());
  }

  @Test
  public void anonymousAccessAnonymousEndpoint() throws Exception {
    ResultActions response = mockMvc.perform(get(ANONYMOUS_ENDPOINT));
    response.andExpect(status().isOk());
  }

  private RequestPostProcessor testAdminUser() {
    return user(ADMIN_USER).password(TEST_PASSWORD)
                           .authorities(new SimpleGrantedAuthority("administrators"));
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

}
