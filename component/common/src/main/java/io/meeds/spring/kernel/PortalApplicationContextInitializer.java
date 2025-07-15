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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import org.exoplatform.container.ExoContainer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * A super class used to define {@link PortalApplicationContext} as the Spring
 * Application Builder to use
 */
public abstract class PortalApplicationContextInitializer extends SpringBootServletInitializer {

  private DefaultListableBeanFactory beanFactory;

  private ServletContext             servletContext;

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    // Used to disable LogBack initialization in WebApp context after having
    // initialized it already in Meeds Server globally
    System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");

    this.servletContext = servletContext;
    this.servletContext.setInitParameter("spring.profiles.active", StringUtils.join(ExoContainer.getProfiles(), ","));
    this.beanFactory = new DefaultListableBeanFactory();
    super.onStartup(servletContext);
  }

  @Override
  protected SpringApplicationBuilder createSpringApplicationBuilder() {
    return new SpringApplicationBuilder() {
      @Override
      public SpringApplicationBuilder contextFactory(ApplicationContextFactory factory) {
        return super.contextFactory(w -> new PortalApplicationContext(servletContext, beanFactory));
      }
    };
  }

}
