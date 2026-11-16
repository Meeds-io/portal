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
package io.meeds.portal.identity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationRegistry;

import io.meeds.services.organization.listener.GroupLinkGroupListener;

/**
 * The members of a linked group are read from the real store, on a thread that
 * holds the container of the thread that linked the groups.
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"),
})
public class TestGroupLinkGroupListener extends AbstractKernelTest {// NOSONAR

  private static final String GROUP = "/organization/management/executive-board";

  public void testTheMembersAreReadOnTheContainerOfTheCaller() throws Exception {
    OrganizationService organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    ConversationRegistry conversationRegistry = mock(ConversationRegistry.class);
    AtomicReference<ExoContainer> containerOfTheRead = new AtomicReference<>();
    org.mockito.Mockito.doAnswer(invocation -> {
      containerOfTheRead.set(ExoContainerContext.getCurrentContainerIfPresent());
      return null;
    }).when(conversationRegistry).invalidateUser("root");
    GroupLinkGroupListener listener = new GroupLinkGroupListener();
    ReflectionTestUtils.setField(listener, "conversationRegistry", conversationRegistry);
    ReflectionTestUtils.setField(listener, "organizationService", organizationService);
    ExoContainerContext.setCurrentContainer(getContainer());

    listener.linkGroups(NestedMembership.builder()
                                        .groupId("/organization/management/human-resources")
                                        .nestedGroupId(GROUP)
                                        .build());

    verify(conversationRegistry, timeout(10000)).invalidateUser("root");
    assertSame(getContainer(), containerOfTheRead.get());
    listener.shutdown();
  }

}
