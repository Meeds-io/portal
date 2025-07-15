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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.NodeState.Builder;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.listener.Event;

import io.meeds.portal.security.constant.UserRegistrationType;

@RunWith(MockitoJUnitRunner.class)
public class PortalRegistrationUpdateListenerTest {

  private static final String                 NAV_NODE_ID  = "1225";

  private static final String                 NAV_NODE_URI = "publicSiteNodeUri";

  @Mock
  private NavigationService                   navigationService;

  @Mock
  private InitParams                          params;

  @Mock
  private NodeContext<NodeContext<Object>>    navNode;

  @Mock
  private NodeState                           navState;

  @Mock
  private Builder                             navStateBuilder;

  @Mock
  private Event<Object, UserRegistrationType> event;

  @Test
  public void testUpdateRegistrationType() throws Exception {
    ValuesParam valuesParam = new ValuesParam();
    valuesParam.setValues(Arrays.asList(NAV_NODE_URI));
    when(params.getValuesParam("managed-pages")).thenReturn(valuesParam);
    when(navigationService.loadNode(SiteKey.portal("public"), NAV_NODE_URI)).thenReturn(navNode);
    when(navNode.getId()).thenReturn(NAV_NODE_ID);
    when(navNode.getState()).thenReturn(navState);
    when(navState.builder()).thenReturn(navStateBuilder);
    when(navStateBuilder.visibility(any())).thenReturn(navStateBuilder);
    when(navStateBuilder.build()).thenReturn(navState);

    PortalRegistrationUpdateListener registrationUpdateListener = new PortalRegistrationUpdateListener(navigationService, params);

    when(event.getData()).thenReturn(UserRegistrationType.OPEN);
    registrationUpdateListener.onEvent(event);

    verify(navStateBuilder, times(1)).visibility(Visibility.DISPLAYED);
    verify(navStateBuilder, never()).visibility(Visibility.HIDDEN);
    verify(navigationService, times(1)).updateNode(Long.parseLong(NAV_NODE_ID), navState);

    when(event.getData()).thenReturn(UserRegistrationType.RESTRICTED);
    registrationUpdateListener.onEvent(event);
    verify(navStateBuilder, times(1)).visibility(Visibility.HIDDEN);
    verify(navigationService, times(2)).updateNode(Long.parseLong(NAV_NODE_ID), navState);
  }

}
