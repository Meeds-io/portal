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
package io.meeds.portal.security.listener;

import java.util.List;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

import io.meeds.portal.security.constant.UserRegistrationType;

public class PortalRegistrationUpdateListener extends Listener<Object, UserRegistrationType> {

  private NavigationService navigationService;

  private List<String>      managedPages;

  public PortalRegistrationUpdateListener(NavigationService navigationService, InitParams params) {
    this.navigationService = navigationService;
    this.managedPages = params.getValuesParam("managed-pages").getValues();
  }

  @Override
  public void onEvent(Event<Object, UserRegistrationType> event) throws Exception {
    boolean isOpen = event.getData() == UserRegistrationType.OPEN;
    managedPages.forEach(navUri -> {
      NodeContext<NodeContext<Object>> navNode = navigationService.loadNode(SiteKey.portal("public"), navUri);
      if (navNode != null) {
        NodeState state = navNode.getState()
                                 .builder()
                                 .visibility(isOpen ? Visibility.DISPLAYED : Visibility.HIDDEN)
                                 .build();
        navNode.setState(state);
        navigationService.updateNode(Long.parseLong(navNode.getId()), state);
      }
    });
  }

}
