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
package org.exoplatform.portal.webui.application;

import java.util.List;
import java.util.Map;

import org.gatein.pc.api.PortletContext;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.StatefulPortletContext;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pc.ExoPortletState;
import org.exoplatform.portal.pc.ExoPortletStateType;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.PortletBuilder;
import org.exoplatform.portal.pom.spi.portlet.Preference;

public class PortletModelAdapter { // NOSONAR

  private static final String              LOCAL_STATE_ID = PortletContext.LOCAL_CONSUMER_CLONE.getId();

  private static final PortletModelAdapter SINGLETON      = new PortletModelAdapter();

  private PortletModelAdapter() {
    // Singleton Pattern
  }

  public static PortletModelAdapter getInstance() {
    return SINGLETON;
  }

  /**
   * StatefulPortletContext returned by getPortletContext is actually of type
   * PortletStateType.OPAQUE so that it can be properly handled in WSRP... This
   * model needs to be revisited if we want to properly support consumer-side
   * state management. See GTNPORTAL-736.
   */
  public PortletContext getProducerOfferedPortletContext(String applicationState) {
    int indexOfSeparator = applicationState.lastIndexOf("/");
    String appName = applicationState.substring(0, indexOfSeparator);
    String portletName = applicationState.substring(indexOfSeparator + 1);
    return PortletContext.reference(PortletInvoker.LOCAL_PORTLET_INVOKER_ID,
                                    PortletContext.createPortletContext(appName, portletName));
  }

  public StatefulPortletContext<ExoPortletState> getPortletContext(ExoContainer container,
                                                                   String applicationId,
                                                                   ApplicationState applicationState) {
    LayoutService layoutService = container.getComponentInstanceOfType(LayoutService.class);
    Portlet preferences = layoutService.load(applicationState);
    PortletContext producerOfferedPortletContext = getProducerOfferedPortletContext(applicationId);
    ExoPortletState map = new ExoPortletState(producerOfferedPortletContext.getId());
    if (preferences != null) {
      for (Preference pref : preferences) {
        map.getState().put(pref.getName(), pref.getValues());
      }
    }
    return StatefulPortletContext.create(LOCAL_STATE_ID, ExoPortletStateType.getInstance(), map);
  }

  public ApplicationState update(ExoContainer container,
                                 ExoPortletState updateState,
                                 ApplicationState applicationState) {
    // Compute new preferences
    PortletBuilder builder = new PortletBuilder();
    for (Map.Entry<String, List<String>> entry : updateState.getState().entrySet()) {
      builder.add(entry.getKey(), entry.getValue());
    }

    if (applicationState instanceof TransientApplicationState transientState) {
      transientState.setContentState(builder.build());
      return transientState;
    } else {
      LayoutService layoutService = container.getComponentInstanceOfType(LayoutService.class);
      return layoutService.save(applicationState, builder.build());
    }
  }

  /**
   * Returns the state of the gadget as preferences or null if the preferences
   * cannot be edited as such.
   *
   * @param container the container
   * @param applicationState the application state
   * @return the preferences
   */
  public Portlet getState(ExoContainer container, ApplicationState applicationState) {
    if (applicationState instanceof TransientApplicationState transientState) {
      Portlet pref = transientState.getContentState();
      if (pref == null) {
        pref = new Portlet();
      }
      return pref;
    } else {
      LayoutService layoutService = container.getComponentInstanceOfType(LayoutService.class);
      Portlet pref = layoutService.load(applicationState);
      if (pref == null) {
        pref = new Portlet();
      }
      return pref;
    }
  }

  /**
   * Extracts the state based on what the current PortletContext is and the new
   * modified PortletContext.
   *
   * @param originalPortletContext The current PortletContext for the Portlet
   * @param modifiedPortletContext The new modified PortletContext
   * @return
   */
  public ExoPortletState getStateFromModifiedContext(PortletContext originalPortletContext,
                                                     PortletContext modifiedPortletContext) {
    if (modifiedPortletContext instanceof StatefulPortletContext statefulContext // NOSONAR
        && statefulContext.getState() instanceof ExoPortletState exoPortletState) {
      return exoPortletState;
    } else {
      return null;
    }
  }

  /**
   * Extracts the state based on what the current PortletContext is and the new
   * cloned PortletContext
   *
   * @param originalPortletContext The current PortletContext for the Portlet
   * @param clonedPortletContext The new cloned PortletContext
   * @return
   */
  public ExoPortletState getStateFromClonedContext(PortletContext originalPortletContext,
                                                   PortletContext clonedPortletContext) {
    if (clonedPortletContext instanceof StatefulPortletContext statefulContext // NOSONAR
        && statefulContext.getState() instanceof ExoPortletState exoPortletState) {
      return exoPortletState;
    } else {
      return null;
    }
  }

}
