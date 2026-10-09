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
package io.meeds.spring.web.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Boots the servlet error page on an embedded server: a standalone MockMvc
 * registers none, so it cannot tell what the error body carries. The error
 * attributes are found by component scan, as the WARs scanning
 * {@code io.meeds.spring.web} find them.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = PortalErrorAttributesTest.ErrorTestConfiguration.class)
@ImportAutoConfiguration({
  PropertyPlaceholderAutoConfiguration.class,
  TomcatServletWebServerAutoConfiguration.class,
  DispatcherServletAutoConfiguration.class,
  WebMvcAutoConfiguration.class,
  HttpMessageConvertersAutoConfiguration.class,
  JacksonAutoConfiguration.class,
  ErrorMvcAutoConfiguration.class,
})
@TestPropertySource(locations = "classpath:application-common.properties")
class PortalErrorAttributesTest {

  private static final String REFUSAL_CODE      = "x.code";

  private static final String INTERNAL_MESSAGE  = "select * from SECRET_TABLE";

  @LocalServerPort
  private int                 port;

  @Test
  void refusalReasonReachesTheClient() throws Exception {
    HttpResponse<String> response = get("refusal");
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    assertEquals(REFUSAL_CODE, JsonMapper.shared().readTree(response.body()).path("message").asString());
  }

  @Test
  void serverErrorMessageNeverReachesTheClient() throws Exception {
    HttpResponse<String> response = get("failure");
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.statusCode());
    JsonNode body = JsonMapper.shared().readTree(response.body());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.path("status").asInt());
    assertFalse(body.has("message"), response.body());
    assertFalse(response.body().contains(INTERNAL_MESSAGE), response.body());
  }

  private HttpResponse<String> get(String path) throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/rest/test/error/" + path))
                                       .header("Accept", "application/json")
                                       .build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }

  @Configuration
  @ComponentScan(basePackageClasses = PortalErrorAttributes.class)
  static class ErrorTestConfiguration {
  }

  @RestController
  @RequestMapping("test/error")
  static class ErrorTestController {

    @GetMapping("refusal")
    public String refusal() {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, REFUSAL_CODE);
    }

    @GetMapping("failure")
    public String failure() {
      throw new IllegalStateException(INTERNAL_MESSAGE);
    }

  }

}
