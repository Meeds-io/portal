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
package org.exoplatform.portal.mop.storage;

import static org.exoplatform.container.ExoContainerContext.getService;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.NewPortalConfig;
import org.exoplatform.portal.config.NewPortalConfigListener;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.resources.LocaleConfigService;

@ConfiguredBy({
                @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
                @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
})
public class TestNavigationInsertAtIndex extends AbstractKernelTest {

  protected NavigationService       navigationService;

  protected NewPortalConfigListener newPortalConfigListener;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PortalContainer container = PortalContainer.getInstance();
    this.navigationService = container.getComponentInstanceOfType(NavigationService.class);
    begin();
  }

  @Override
  protected void tearDown() throws Exception {
    end();
    super.tearDown();
  }

  public void testInsertNavigationAtIndex() throws Exception {
    importSiteNavigation("jar:/org/exoplatform/portal/config/conf");
    importSiteNavigation("jar:/org/exoplatform/portal/config/conf/portal/portal1/insertAt1");
    importSiteNavigation("jar:/org/exoplatform/portal/config/conf/portal/portal1/insertAt2");
    importSiteNavigation("jar:/org/exoplatform/portal/config/conf/portal/portal1/insertAt3");

    SiteKey siteKey = SiteKey.portal("portal1");
    NodeContext<NodeContext<Object>> rootNode = navigationService.loadNode(siteKey);
    assertNotNull(rootNode);
    assertEquals(4, rootNode.getSize());
    assertEquals("a", rootNode.get(0).getName());
    assertEquals("b", rootNode.get(1).getName());
    assertEquals("h", rootNode.get(2).getName());
    assertEquals("c", rootNode.get(3).getName());
    assertEquals("h", rootNode.get(3).get(0).get(0).getName());
    assertEquals("h", rootNode.get(0).get(0).getName());
  }

  private void importSiteNavigation(String location) throws Exception {
    InitParams initParams = mock(InitParams.class);
    ValueParam valueParam = new ValueParam();
    valueParam.setName("meta.portal");
    valueParam.setValue("classic");
    when(initParams.getValueParam("meta.portal")).thenReturn(valueParam);

    NewPortalConfig newPortalConfig = new NewPortalConfig();
    newPortalConfig.setImportMode("insert");
    newPortalConfig.setLocation(location);
    newPortalConfig.setOverrideMode(true);
    newPortalConfig.setOwnerType("portal");
    newPortalConfig.setPredefinedOwner(new HashSet<>(Arrays.asList("portal1")));
    when(initParams.getObjectParamValues(NewPortalConfig.class)).thenReturn(Collections.singletonList(newPortalConfig));
    new NewPortalConfigListener(getService(UserPortalConfigService.class),
                                getService(LayoutService.class),
                                getService(ConfigurationManager.class),
                                initParams,
                                navigationService,
                                getService(DescriptionStorage.class),
                                getService(UserACL.class),
                                getService(LocaleConfigService.class)).run();
  }

}
