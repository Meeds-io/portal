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
package org.exoplatform.portal.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.xml.DocumentSource;
import org.exoplatform.portal.resource.config.tasks.SkinConfigTask;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;

import jakarta.servlet.ServletContext;

@RunWith(MockitoJUnitRunner.class)
public class TestGateInResourceParser {

  private static final String FILTERED_MODULE = "FilteredModule";

  private static final String SKIN_NAME       = "Default";

  private static final String CONTEXT_PATH    = "portal";

  @Mock
  private SkinService         skinService;

  @Mock
  private ServletContext      scontext;

  @Before
  public void setUp() {
    when(scontext.getContextPath()).thenReturn(CONTEXT_PATH);
  }

  @Test
  public void testResources1() {
    assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_0.xml");
  }

  @Test
  public void testResources1WithSkinModule() {
    assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_0-with-skin-module.xml");
  }

  @Test
  public void testResources11() {
    assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_1.xml");
  }

  @Test
  public void testResources12() {
    assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_2.xml");
  }

  @Test
  public void testResources13() {
    assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_3.xml");
  }

  @Test
  public void testResources14() {
    assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_4.xml");
  }

  @Test
  public void testResources15() {
    List<SkinConfigTask> tasks = getTasks("org/exoplatform/portal/resource/gatein-resources-1_5.xml");
    assertFalse(tasks.isEmpty());
    assertEquals(5, tasks.size());
    tasks.forEach(skinConfigTask -> skinConfigTask.execute(skinService, scontext));
    verify(skinService, times(1)).addSkin(eq("web/BannerPortlet"),
                                          eq(SKIN_NAME),
                                          argThat(s -> s.contains("/skin/portal/webui/component/UIBannerPortlet/DefaultStylesheet.css")),
                                          anyInt(),
                                          eq(false),
                                          argThat(list -> CollectionUtils.isNotEmpty(list)
                                                          && list.size() == 1
                                                          && list.contains(FILTERED_MODULE)));
    verify(skinService, times(1)).addSkin(eq("web/FooterPortlet"),
                                          eq(SKIN_NAME),
                                          argThat(s -> s.contains("/skin/portal/webui/component/UIFooterPortlet/DefaultStylesheet.css")),
                                          anyInt(),
                                          eq(false),
                                          argThat(CollectionUtils::isEmpty));
    verify(skinService, times(1)).addSkin(eq("web/NavigationPortlet"),
                                          eq(SKIN_NAME),
                                          argThat(s -> s == null),
                                          anyInt(),
                                          eq(false),
                                          argThat(list -> CollectionUtils.isNotEmpty(list)
                                                          && list.size() == 1
                                                          && list.contains(FILTERED_MODULE)));
    verify(skinService, times(1)).addPortalSkin(eq("MyModule"),
                                                eq(SKIN_NAME),
                                                argThat(s -> s.contains("/skin/Stylesheet.css")),
                                                anyInt(),
                                                eq(false),
                                                eq(false));
    verify(skinService, times(1)).addPortalSkin(eq(FILTERED_MODULE),
                                                eq(SKIN_NAME),
                                                argThat(s -> s.contains("/skin/FilteredStylesheet.css")),
                                                anyInt(),
                                                eq(false),
                                                eq(true));
  }

  private List<SkinConfigTask> assertDescriptorCanBeLoaded(String descriptorPath) {
    List<SkinConfigTask> tasks = getTasks(descriptorPath);
    assertNotNull("There are no tasks", tasks);
    assertEquals(8, tasks.size());
    return tasks;
  }

  private List<SkinConfigTask> getTasks(String descriptorPath) {
    URL url = Thread.currentThread().getContextClassLoader().getResource(descriptorPath);
    assertNotNull("The " + descriptorPath + " can not be found", url);
    DocumentSource source = DocumentSource.create(url);
    return SkinConfigParser.fetchTasks(source);
  }
}
