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
package org.exoplatform.portal.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.codehaus.groovy.control.CompilationFailedException;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import org.exoplatform.groovyscript.GroovyCompilationException;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.resolver.ResourceResolver;

import groovy.lang.GroovyShell;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

public class PortalWarmupTest {

  @Test
  public void testEnabledOutsideDeveloperModeUnlessDisabled() {
    assertTrue(PortalWarmup.isEnabled(false, null));
    assertTrue(PortalWarmup.isEnabled(false, " "));
    assertTrue(PortalWarmup.isEnabled(false, "true"));
    assertFalse(PortalWarmup.isEnabled(false, "false"));
    assertFalse(PortalWarmup.isEnabled(true, null));
    assertFalse(PortalWarmup.isEnabled(true, "true"));
  }

  @Test
  public void testListsTemplatesOfEveryDirectory() {
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getResourcePaths("/groovy/")).thenReturn(Set.of("/groovy/b.gtmpl",
                                                                        "/groovy/portal/",
                                                                        "/groovy/script.js"));
    when(servletContext.getResourcePaths("/groovy/portal/")).thenReturn(Set.of("/groovy/portal/a.gtmpl"));
    assertEquals(List.of("/groovy/b.gtmpl", "/groovy/portal/a.gtmpl"), PortalWarmup.listTemplates(servletContext));
  }

  @Test
  public void testCompilesThroughTheResolverAndGoesOnAfterAFailure() throws Exception {
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getResourcePaths("/groovy/")).thenReturn(Set.of("/groovy/a.gtmpl", "/groovy/b.gtmpl"));
    ResourceResolver resolver = mock(ResourceResolver.class);
    TemplateService templateService = mock(TemplateService.class);
    CompilationFailedException compilerFailure = compilerFailure("def x = ;");
    // As the template cache throws it
    Exception cacheFailure = new IllegalStateException("Error retrieving value with key system:/groovy/a.gtmpl",
                                                       new ExecutionException(new GroovyCompilationException(compilerFailure,
                                                                                                             "<% def x = ; %>",
                                                                                                             "def x = ;")));
    when(templateService.getTemplate("system:/groovy/a.gtmpl", resolver)).thenThrow(cacheFailure);

    Map<String, String> failures = new PortalWarmup(null, servletContext, resolver).compileTemplates(templateService);
    assertEquals(Set.of("system:/groovy/a.gtmpl"), failures.keySet());
    assertTrue(failures.toString(), failures.get("system:/groovy/a.gtmpl").contains(compilerFailure.getMessage()));
    verify(templateService).getTemplate("system:/groovy/a.gtmpl", resolver);
    verify(templateService).getTemplate("system:/groovy/b.gtmpl", resolver);
  }

  @Test
  public void testCompilesThroughThePortalApplication() throws Exception {
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getResourcePaths("/groovy/")).thenReturn(Set.of("/groovy/a.gtmpl"));
    ServletConfig servletConfig = mock(ServletConfig.class);
    when(servletConfig.getServletContext()).thenReturn(servletContext);
    PortalApplication application = new PortalApplication(servletConfig);
    TemplateService templateService = mock(TemplateService.class);

    assertTrue(PortalWarmup.of(null, application).compileTemplates(templateService).isEmpty());
    // The key and the resolver of the template's first render
    verify(templateService).getTemplate("system:/groovy/a.gtmpl", application.getResourceResolver());
    assertSame(servletContext, application.getServletConfig().getServletContext());
  }

  @Test
  public void testRequestsTheLowestStartedHttpPortWithoutTls() throws Exception {
    MBeanServer mbeanServer = mock(MBeanServer.class);
    Set<ObjectName> connectors = new LinkedHashSet<>();
    connectors.add(connector(mbeanServer, 8009, "AJP/1.3", "STARTED", false));
    connectors.add(connector(mbeanServer, 7443, "HTTP/1.1", "STARTED", true));
    connectors.add(connector(mbeanServer, 7070, "HTTP/1.1", "FAILED", false));
    connectors.add(connector(mbeanServer, 8081, "org.apache.coyote.http11.Http11NioProtocol", "STARTED", false));
    connectors.add(connector(mbeanServer, 8080, "HTTP/1.1", "STARTED", false));
    when(mbeanServer.queryNames(any(ObjectName.class), isNull())).thenReturn(connectors);

    assertEquals(URI.create("http://127.0.0.1:8080/portal/login"), PortalWarmup.warmupUri(null, mbeanServer, "portal"));

    // Behind a TLS proxy, a plain connector declares the https scheme: it still serves plain HTTP
    connectors.add(connector(mbeanServer, 7080, "HTTP/1.1", "STARTED", false, "https"));
    assertEquals(URI.create("http://127.0.0.1:7080/portal/login"), PortalWarmup.warmupUri(null, mbeanServer, "portal"));
    assertEquals(URI.create("http://host:9090/portal/home"),
                 PortalWarmup.warmupUri(" http://host:9090/portal/home ", mbeanServer, "portal"));
  }

  @Test
  public void testNoRequestWithoutStartedHttpConnector() throws Exception {
    MBeanServer mbeanServer = mock(MBeanServer.class);
    Set<ObjectName> connectors = new LinkedHashSet<>();
    connectors.add(connector(mbeanServer, 8009, "AJP/1.3", "STARTED", false));
    connectors.add(connector(mbeanServer, 8443, "HTTP/1.1", "STARTED", true));
    when(mbeanServer.queryNames(any(ObjectName.class), isNull())).thenReturn(connectors);
    assertNull(PortalWarmup.warmupUri("", mbeanServer, "portal"));

    MBeanServer failing = mock(MBeanServer.class);
    when(failing.queryNames(any(ObjectName.class), isNull())).thenThrow(new IllegalStateException("JMX"));
    assertNull(PortalWarmup.warmupUri(null, failing, "portal"));
    assertNull(PortalWarmup.warmupUri(null, null, "portal"));
  }

  @Test
  public void testSendsOneAnonymousRequestAndFollowsNoRedirect() throws Exception {
    List<String> requests = new CopyOnWriteArrayList<>();
    List<Map<String, List<String>>> headers = new CopyOnWriteArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", exchange -> {
      requests.add(exchange.getRequestURI().getPath());
      headers.add(exchange.getRequestHeaders());
      exchange.getResponseHeaders().add("Location", "/portal/redirected");
      exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=1");
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });
    server.start();
    try {
      URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/portal/login");
      PortalWarmup warmup = new PortalWarmup(null, null, null);
      assertEquals(302, warmup.request(uri, true));
      assertEquals(302, warmup.request(uri, true));
    } finally {
      server.stop(0);
    }
    assertEquals(List.of("/portal/login", "/portal/login"), requests);
    for (Map<String, List<String>> requestHeaders : headers) {
      List<String> names = new ArrayList<>(requestHeaders.keySet());
      names.replaceAll(String::toLowerCase);
      assertFalse(names.toString(), names.contains("cookie"));
      assertFalse(names.toString(), names.contains("authorization"));
    }
  }

  @Test(timeout = 30000)
  public void testStalledBodyIsAbandonedAfterTheTimeout() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    ExecutorService executor = Executors.newCachedThreadPool();
    server.setExecutor(executor);
    server.createContext("/", exchange -> {
      // Headers and a first chunk, then the body stalls
      exchange.sendResponseHeaders(200, 0);
      exchange.getResponseBody().write("<html>".getBytes(StandardCharsets.UTF_8));
      exchange.getResponseBody().flush();
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      exchange.close();
    });
    server.start();
    try {
      URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/portal/login");
      long start = System.currentTimeMillis();
      assertEquals(-1, new PortalWarmup(null, null, null).request(uri, true, Duration.ofSeconds(1)));
      long elapsed = System.currentTimeMillis() - start;
      assertTrue("returned after " + elapsed + "ms", elapsed < 10000);
    } finally {
      release.countDown();
      server.stop(0);
      executor.shutdownNow();
    }
  }

  @Test
  public void testFailedRequestIsLoggedNotThrown() throws Exception {
    int port;
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      port = socket.getLocalPort();
    }
    // Nothing listens on that port any more: the request fails without throwing
    assertEquals(-1, new PortalWarmup(null, null, null).request(URI.create("http://127.0.0.1:" + port + "/portal/login"), true));
  }

  private static CompilationFailedException compilerFailure(String script) {
    try {
      new GroovyShell().parse(script);
    } catch (CompilationFailedException e) {
      return e;
    }
    throw new AssertionError(script + " compiles");
  }

  private ObjectName connector(MBeanServer mbeanServer, int port, String protocol, String state, boolean ssl) throws Exception {
    return connector(mbeanServer, port, protocol, state, ssl, ssl ? "https" : "http");
  }

  private ObjectName connector(MBeanServer mbeanServer,
                               int port,
                               String protocol,
                               String state,
                               boolean ssl,
                               String scheme) throws Exception {
    ObjectName name = new ObjectName("Catalina:type=Connector,port=" + port);
    when(mbeanServer.getAttribute(eq(name), anyString())).thenAnswer(invocation -> switch ((String) invocation.getArgument(1)) {
    case "port" -> port;
    case "protocol" -> protocol;
    case "stateName" -> state;
    case "SSLEnabled" -> String.valueOf(ssl);
    case "scheme" -> scheme;
    default -> null;
    });
    return name;
  }

}
