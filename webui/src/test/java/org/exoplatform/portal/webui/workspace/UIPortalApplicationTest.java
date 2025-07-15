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
package org.exoplatform.portal.webui.workspace;

import java.util.*;

import org.exoplatform.portal.webui.application.UIPortlet;
import org.gatein.pc.portlet.impl.info.ContainerPortletInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UIPortalApplicationTest {

  @Test
  public void testGetInitParamsOfPagePortlets() {
    UIPortalApplication uiPortalApplication = mock(UIPortalApplication.class);
    uiPortalApplication.uiWorkingWorkspace = mock(UIWorkingWorkspace.class);
    doNothing().when(uiPortalApplication.uiWorkingWorkspace).findComponentOfType(new ArrayList<>(), UIPortlet.class);
    when(uiPortalApplication.getInitParamsOfPagePortlets(any())).thenCallRealMethod();
    ContainerPortletInfo portletInfo = mock(ContainerPortletInfo.class);

    when(portletInfo.getInitParameter("separator")).thenReturn("|");
    when(portletInfo.getInitParameter("prefetch.resource.rest")).thenReturn("/portal/rest/api/test?offset=0&limit=20&expand=test1,test2,test3");

    when(uiPortalApplication.getPagePortletInfos()).thenReturn(Collections.singletonList(portletInfo));

    Set<String> initParamsOfPagePortlets = new HashSet<String>();
    initParamsOfPagePortlets.add("/portal/rest/api/test?offset=0&limit=20&expand=test1,test2,test3");

    assertEquals(initParamsOfPagePortlets, uiPortalApplication.getInitParamsOfPagePortlets("prefetch.resource.rest"));

    when(portletInfo.getInitParameter("separator")).thenReturn(null);
    when(portletInfo.getInitParameter("prefetch.resource.rest")).thenReturn("/portal/rest/api/test1,/portal/rest/api/test2,/portal/rest/api/test3?limit=20&returnSize=true");

    when(uiPortalApplication.getPagePortletInfos()).thenReturn(Collections.singletonList(portletInfo));

    initParamsOfPagePortlets = new HashSet<String>();
    initParamsOfPagePortlets.add("/portal/rest/api/test1");
    initParamsOfPagePortlets.add("/portal/rest/api/test2");
    initParamsOfPagePortlets.add("/portal/rest/api/test3?limit=20&returnSize=true");
    assertEquals(initParamsOfPagePortlets, uiPortalApplication.getInitParamsOfPagePortlets("prefetch.resource.rest"));
  }

}
