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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.RequestDispatcher;
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
@DirtiesContext
class PortalErrorAttributesTest {

  /**
   * Set JVM-wide by the embedded Tomcat: the Kernel tests sharing the surefire
   * JVM would then resolve their configuration under it and start no container
   */
  private static final String[]            CATALINA_PROPERTIES = { "catalina.home", "catalina.base" };

  private static final Map<String, String> SAVED_PROPERTIES    = new HashMap<>();

  private static final String              REFUSAL_CODE        = "x.code";

  private static final String              INTERNAL_MESSAGE    = "select * from SECRET_TABLE";

  @LocalServerPort
  private int                              port;

  @BeforeAll
  static void saveCatalinaProperties() {
    for (String name : CATALINA_PROPERTIES) {
      SAVED_PROPERTIES.put(name, System.getProperty(name));
    }
  }

  @AfterAll
  static void restoreCatalinaProperties() {
    SAVED_PROPERTIES.forEach((name, value) -> {
      if (value == null) {
        System.clearProperty(name);
      } else {
        System.setProperty(name, value);
      }
    });
  }

  @Test
  void refusalReasonReachesTheClient() throws Exception {
    HttpResponse<String> response = send(get("refusal"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    assertEquals(REFUSAL_CODE, JsonMapper.shared().readTree(response.body()).path("message").asString());
  }

  @Test
  void refusalWithoutReasonKeepsNoMessage() throws Exception {
    HttpResponse<String> response = send(request("body").header("Content-Type", "application/json")
                                                        .POST(HttpRequest.BodyPublishers.noBody()));
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    assertNoMessage(response, HttpStatus.BAD_REQUEST, "ErrorTestController");
  }

  @Test
  void serverErrorMessageNeverReachesTheClient() throws Exception {
    HttpResponse<String> response = send(get("failure"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.statusCode());
    assertNoMessage(response, HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_MESSAGE);
  }

  @Test
  void serverErrorReasonNeverReachesTheClient() throws Exception {
    HttpResponse<String> response = send(get("unavailable"));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.statusCode());
    assertNoMessage(response, HttpStatus.SERVICE_UNAVAILABLE, REFUSAL_CODE);
  }

  /**
   * The status is read from the attributes, which a caller excluding
   * {@link Include#STATUS} does not get: the message is then dropped too.
   */
  @Test
  void errorWithoutStatusAttributeKeepsNoMessage() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.BAD_REQUEST.value());
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, REFUSAL_CODE);
    PortalErrorAttributes errorAttributes = new PortalErrorAttributes();

    assertEquals(REFUSAL_CODE,
                 errorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.of(Include.STATUS, Include.MESSAGE))
                                .get("message"));
    assertFalse(errorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.of(Include.MESSAGE))
                               .containsKey("message"));
  }

  @Test
  void refusalReasonStaysOutWhenMessageIsNotIncluded() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.BAD_REQUEST.value());
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, REFUSAL_CODE);

    assertFalse(new PortalErrorAttributes().getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.of(Include.STATUS))
                                           .containsKey("message"));
  }

  /**
   * A validation error's own message names the bound object or the handler
   * method: the refusal's reason replaces it.
   */
  @Test
  void refusalReasonReplacesTheValidationErrorText() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.BAD_REQUEST.value());
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, REFUSAL_CODE);
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "payload");
    bindingResult.reject("payload.invalid");
    MethodParameter parameter = new MethodParameter(ErrorTestController.class.getMethod("body", Map.class), 0);
    PortalErrorAttributes errorAttributes = new PortalErrorAttributes();
    errorAttributes.resolveException(request,
                                     new MockHttpServletResponse(),
                                     null,
                                     new MethodArgumentNotValidException(parameter, bindingResult));

    assertEquals(REFUSAL_CODE,
                 errorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.of(Include.STATUS, Include.MESSAGE))
                                .get("message"));
  }

  private void assertNoMessage(HttpResponse<String> response, HttpStatus status, String internalText) {
    JsonNode body = JsonMapper.shared().readTree(response.body());
    assertEquals(status.value(), body.path("status").asInt());
    assertFalse(body.has("message"), response.body());
    assertFalse(response.body().contains(internalText), response.body());
  }

  private HttpRequest.Builder get(String path) {
    return request(path).GET();
  }

  private HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/rest/test/error/" + path))
                      .header("Accept", "application/json");
  }

  private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
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

    @PostMapping("body")
    public String body(@RequestBody Map<String, Object> body) {
      return body.toString();
    }

    @GetMapping("failure")
    public String failure() {
      throw new IllegalStateException(INTERNAL_MESSAGE);
    }

    @GetMapping("unavailable")
    public String unavailable() {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, REFUSAL_CODE);
    }

  }

}
