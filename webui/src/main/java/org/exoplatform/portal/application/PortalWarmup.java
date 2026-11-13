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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.monitor.jvm.J2EEServerInfo;
import org.exoplatform.container.monitor.jvm.ServerStartupWaiter;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.ServletContext;

/**
 * Once the server has started, compiles the portal's templates, then requests
 * the login page once, anonymously, so that the first user's page waits for
 * neither.
 */
public class PortalWarmup implements Runnable {

  /** false: neither the templates' compilation nor the request runs. */
  public static final String  ENABLED_PROPERTY  = "exo.portal.warmup.enabled";

  /** The URL of the warm-up request; empty: the login page on the loopback. */
  public static final String  URL_PROPERTY      = "exo.portal.warmup.url";

  static final String         THREAD_NAME       = "portal-warmup";

  static final String         TEMPLATES_DIR     = "/groovy/";

  static final Duration       CONNECT_TIMEOUT   = Duration.ofSeconds(10);

  static final Duration       RESPONSE_TIMEOUT  = Duration.ofMinutes(5);

  private static final Log    LOG               = ExoLogger.getLogger(PortalWarmup.class);

  private static final String CONNECTOR_PATTERN = "Catalina:type=Connector,*";

  private final ExoContainer     container;

  private final ServletContext   servletContext;

  private final ResourceResolver resolver;

  PortalWarmup(ExoContainer container, ServletContext servletContext, ResourceResolver resolver) {
    this.container = container;
    this.servletContext = servletContext;
    this.resolver = resolver;
  }

  /**
   * Starts the warm-up thread, unless in developer mode or disabled by
   * {@link #ENABLED_PROPERTY}.
   *
   * @param container the portal container
   * @param application the portal application, whose pages render the
   *          templates the warm-up compiles
   * @return the started thread, else null
   */
  public static Thread start(ExoContainer container, PortalApplication application) {
    if (!isEnabled(PropertyManager.isDevelopping(), PropertyManager.getProperty(ENABLED_PROPERTY))) {
      return null;
    }
    Thread thread = new Thread(of(container, application), THREAD_NAME);
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  @Override
  public void run() {
    ExoContainerContext.setCurrentContainer(container);
    try {
      ServerStartupWaiter.awaitServerStartup(container, "Portal warm-up");
      compileTemplates(container.getComponentInstanceOfType(TemplateService.class));
      String configuredUrl = PropertyManager.getProperty(URL_PROPERTY);
      URI uri = warmupUri(configuredUrl, getMBeanServer(), container.getContext().getName());
      if (uri == null) {
        LOG.info("No started HTTP connector to send the portal warm-up request to. Set {} to send it", URL_PROPERTY);
      } else {
        request(uri, StringUtils.isBlank(configuredUrl));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      LOG.warn("Portal warm-up failed", e);
    } finally {
      ExoContainerContext.setCurrentContainer(null);
    }
  }

  /**
   * The templates are listed through the application's context, which lists
   * those of every webapp registered with the portal container, and compiled
   * through the application's resolver, so that each lands in the template
   * cache under the key its first render reads, an extension's override
   * included.
   */
  static PortalWarmup of(ExoContainer container, PortalApplication application) {
    return new PortalWarmup(container, application.getServletConfig().getServletContext(), application.getResourceResolver());
  }

  static boolean isEnabled(boolean developing, String enabled) {
    return !developing && (StringUtils.isBlank(enabled) || Boolean.parseBoolean(enabled.trim()));
  }

  /**
   * @return the templates that failed to compile, each with the message
   *         logged for it
   */
  Map<String, String> compileTemplates(TemplateService templateService) {
    long start = System.currentTimeMillis();
    List<String> templates = listTemplates(servletContext);
    Map<String, String> failures = new LinkedHashMap<>();
    for (String path : templates) {
      String name = "system:" + path;
      try {
        templateService.getTemplate(name, resolver);
      } catch (Exception e) {
        // The template cache wraps the compiler's exception, whose message is the root cause's
        String message = ExceptionUtils.getRootCauseMessage(e);
        failures.put(name, message);
        LOG.warn("Portal warm-up can't compile template {}: {}", name, message);
      }
    }
    LOG.info("Portal warm-up compiled {} templates, {} failures, within {}ms",
             templates.size() - failures.size(),
             failures.size(),
             System.currentTimeMillis() - start);
    return failures;
  }

  static List<String> listTemplates(ServletContext servletContext) {
    List<String> templates = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Deque<String> directories = new ArrayDeque<>();
    directories.push(TEMPLATES_DIR);
    while (!directories.isEmpty()) {
      String directory = directories.pop();
      Set<String> paths = visited.add(directory) ? servletContext.getResourcePaths(directory) : null;
      if (paths != null) {
        for (String path : new TreeSet<>(paths)) {
          if (path.endsWith("/")) {
            directories.push(path);
          } else if (path.endsWith(".gtmpl")) {
            templates.add(path);
          }
        }
      }
    }
    templates.sort(null);
    return templates;
  }

  /**
   * @param configuredUrl the value of {@link #URL_PROPERTY}
   * @return the configured URL, else the login page on the loopback, on the
   *         lowest port among the started HTTP connectors without TLS, else
   *         null
   */
  static URI warmupUri(String configuredUrl, MBeanServer mbeanServer, String portalContainerName) {
    if (StringUtils.isNotBlank(configuredUrl)) {
      return URI.create(configuredUrl.trim());
    }
    int port = lowestHttpPort(mbeanServer);
    return port < 0 ? null : URI.create("http://127.0.0.1:" + port + "/" + portalContainerName + "/login");
  }

  static int lowestHttpPort(MBeanServer mbeanServer) {
    if (mbeanServer == null) {
      return -1;
    }
    Set<ObjectName> connectors;
    try {
      connectors = mbeanServer.queryNames(new ObjectName(CONNECTOR_PATTERN), null);
    } catch (Exception e) {
      LOG.debug("Can't list the server connectors", e);
      return -1;
    }
    int lowest = -1;
    for (ObjectName connector : connectors) {
      int port = httpPort(mbeanServer, connector);
      if (port > 0 && (lowest < 0 || port < lowest)) {
        lowest = port;
      }
    }
    return lowest;
  }

  /**
   * @return the connector's port when it is a started HTTP connector without
   *         TLS, else -1
   */
  private static int httpPort(MBeanServer mbeanServer, ObjectName connector) {
    try {
      String state = String.valueOf(mbeanServer.getAttribute(connector, "stateName"));
      String protocol = String.valueOf(mbeanServer.getAttribute(connector, "protocol"));
      if (!"STARTED".equals(state) || !StringUtils.containsIgnoreCase(protocol, "http") || isSslEnabled(mbeanServer, connector)) {
        return -1;
      }
      Object port = mbeanServer.getAttribute(connector, "port");
      return port instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(port));
    } catch (Exception e) {
      LOG.debug("Can't read connector {}", connector, e);
      return -1;
    }
  }

  private static boolean isSslEnabled(MBeanServer mbeanServer, ObjectName connector) {
    try {
      return Boolean.parseBoolean(String.valueOf(mbeanServer.getAttribute(connector, "SSLEnabled")));
    } catch (Exception e) {
      LOG.debug("Can't read whether connector {} uses TLS", connector, e);
      return false;
    }
  }

  /**
   * Sends one GET, with no cookie and no credential, following no redirect,
   * and waits at most {@link #RESPONSE_TIMEOUT} for the whole response.
   *
   * @param loopback whether the URL is the derived loopback one, requested
   *          without proxy
   * @return the response's status, or -1 when the request failed
   */
  int request(URI uri, boolean loopback) throws InterruptedException {
    return request(uri, loopback, RESPONSE_TIMEOUT);
  }

  /**
   * @param timeout bounds the whole exchange, body included: past it, the
   *          client is shut down, which closes the connection
   */
  int request(URI uri, boolean loopback, Duration timeout) throws InterruptedException {
    HttpClient.Builder builder = HttpClient.newBuilder()
                                           .followRedirects(HttpClient.Redirect.NEVER)
                                           .connectTimeout(CONNECT_TIMEOUT);
    if (loopback) {
      builder.proxy(HttpClient.Builder.NO_PROXY);
    }
    long start = System.currentTimeMillis();
    try (HttpClient client = builder.build()) {
      try {
        HttpResponse<Void> response = client.sendAsync(HttpRequest.newBuilder(uri).GET().build(),
                                                       HttpResponse.BodyHandlers.discarding())
                                            .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        LOG.info("Portal warm-up request to {} answered {} within {}ms",
                 uri,
                 response.statusCode(),
                 System.currentTimeMillis() - start);
        return response.statusCode();
      } catch (ExecutionException e) {
        LOG.warn("Portal warm-up request to {} failed within {}ms: {}",
                 uri,
                 System.currentTimeMillis() - start,
                 String.valueOf(e.getCause()));
        return -1;
      } catch (TimeoutException e) {
        client.shutdownNow();
        LOG.warn("Portal warm-up request to {} got no complete response within {}ms", uri, timeout.toMillis());
        return -1;
      }
    }
  }

  private MBeanServer getMBeanServer() {
    J2EEServerInfo serverInfo = container.getComponentInstanceOfType(J2EEServerInfo.class);
    return serverInfo == null ? null : serverInfo.getMBeanServer();
  }

}
