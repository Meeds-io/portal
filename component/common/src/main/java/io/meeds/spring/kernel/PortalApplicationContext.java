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
package io.meeds.spring.kernel;

import static io.meeds.spring.kernel.KernelContainerLifecyclePlugin.addSpringContext;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;

import org.exoplatform.container.PortalContainer;

import jakarta.servlet.ServletContext;

/**
 * A class to let Spring context initialized once the
 * {@link PortalContainer}finishes startup to bring all defined Kernel Services
 * into Spring Context in order to be able to use annotations to inject Kernel
 * services
 */
public class PortalApplicationContext extends AnnotationConfigServletWebServerApplicationContext {

  private static final Logger LOG = LoggerFactory.getLogger(PortalApplicationContext.class);

  private ServletContext      servletContext;

  private boolean             async;

  public PortalApplicationContext(ServletContext servletContext,
                                  DefaultListableBeanFactory beanFactory,
                                  boolean async) {
    super(beanFactory);
    this.servletContext = servletContext;
    this.async = async;
  }

  @Override
  protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    // Declare spring Context for integration on Kernel when it will start
    // ( Kernel container is always started after all Spring contexts are
    // started, see "PortalContainersCreator" in Meeds-io/meeds project )
    if (async) {
      addSpringContext(servletContext.getServletContextName(),
                       this,
                       (BeanDefinitionRegistry) beanFactory,
                       () -> finishSpringContextStartupAsync(beanFactory));
    } else {
      addSpringContext(servletContext.getServletContextName(),
                       this,
                       (BeanDefinitionRegistry) beanFactory,
                       () -> finishSpringContextStartup(beanFactory));
    }
  }

  @Override
  protected void finishRefresh() {
    // Override to not be invoked in Context startup time
  }

  private void finishSpringContextStartupAsync(ConfigurableListableBeanFactory beanFactory) {
    CompletableFuture.runAsync(() -> {
      Thread currentThread = Thread.currentThread();
      ClassLoader originalClassLoader = currentThread.getContextClassLoader();
      currentThread.setContextClassLoader(servletContext.getClassLoader());
      try {
        do {
          Thread.sleep(3000);
        } while (!PortalContainer.getInstance().isStarted());
        Thread.sleep(3000);
        finishSpringContextStartup(beanFactory);
      } catch (InterruptedException e) {
        currentThread.interrupt();
      } catch (Exception e) {
        LOG.error("Error Initializing Context: {}", servletContext.getServletContextName(), e);
      } finally {
        currentThread.setContextClassLoader(originalClassLoader);
      }
    });
  }

  private void finishSpringContextStartup(ConfigurableListableBeanFactory beanFactory) {
    long start = System.currentTimeMillis();
    LOG.info("Continue Spring context '{}' initialization", servletContext.getServletContextName());
    try {
      PortalApplicationContext.super.finishBeanFactoryInitialization(beanFactory);
      PortalApplicationContext.super.finishRefresh();
      LOG.info("Spring context '{}' initialized in {}ms",
               servletContext.getServletContextName(),
               System.currentTimeMillis() - start);
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error While finishing Beans Initialization in context '%s' with Bean names [%s] (May be related to issue Meeds-io/meeds#2469)",
                                                    servletContext.getServletContextName(),
                                                    StringUtils.join(beanFactory.getBeanDefinitionNames(), " , ")),
                                      e);
    }
  }

}
