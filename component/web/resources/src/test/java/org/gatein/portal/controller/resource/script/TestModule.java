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
package org.gatein.portal.controller.resource.script;

import java.io.File;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import jakarta.servlet.ServletContext;

import org.exoplatform.commons.utils.MapResourceBundle;
import org.exoplatform.component.test.AbstractGateInTest;
import org.exoplatform.component.test.web.ServletContextImpl;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;

import org.exoplatform.services.resources.ResourceBundleService;
import org.gatein.common.io.IOTools;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.script.Module.Local.Content;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.lenient;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestModule extends AbstractGateInTest {

  /** . */
  private ServletContext                     servletContext;

  @Mock
  private PortalContainer                    portalContainer;

  @Mock
  private ResourceBundleService              resourceBundleService;

  public static final ResourceBundle.Control CONTROL = new ResourceBundle.Control() {
                                                       @Override
                                                       public Locale getFallbackLocale(String baseName, Locale locale) {
                                                         return locale.equals(Locale.ENGLISH) ? null : Locale.ENGLISH;
                                                       }
                                                     };

  /** . */
  private ClassLoader                        classLoader;

  @Override
  @Before
  public void setUp() throws Exception {
    ExoContainerContext.setCurrentContainer(portalContainer);

    MapResourceBundle resourceBundle = new MapResourceBundle(Locale.getDefault());
    resourceBundle.add("foo", "foo_en");
    MapResourceBundle resourceBundle1 = new MapResourceBundle(Locale.getDefault());
    resourceBundle1.add("foo", "foo_en");
    MapResourceBundle resourceBundle2 = new MapResourceBundle(Locale.getDefault());
    resourceBundle2.add("foo", "foo_en");
    MapResourceBundle resourceBundle3 = new MapResourceBundle(Locale.getDefault());
    resourceBundle3.add("foo", "foo_fr_FR");
    MapResourceBundle resourceBundle4 = new MapResourceBundle(Locale.getDefault());
    resourceBundle4.add("foo", "\"");
    MapResourceBundle resourceBundle5 = new MapResourceBundle(Locale.getDefault());
    resourceBundle5.add("foo", "'");
    MapResourceBundle resourceBundle6 = new MapResourceBundle(Locale.getDefault());
    resourceBundle6.add("foo", "foo_fr");

    lenient().when(resourceBundleService.getResourceBundle(Mockito.anyString(), Mockito.any(), Mockito.any()))
             .thenReturn(resourceBundle,
                         resourceBundle1,
                         resourceBundle2,
                         resourceBundle3,
                         resourceBundle4,
                         resourceBundle5,
                         resourceBundle6);
    lenient().when(portalContainer.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);

    URL classesURL = TestModule.class.getResource("");
    assertNotNull(classesURL);
    File classes = new File(classesURL.toURI());
    assertTrue(classes.exists());
    assertTrue(classes.isDirectory());
    ClassLoader classLoader = new URLClassLoader(new URL[] { new File(classes, "WEB-INF/classes").toURI().toURL() },
                                                 ClassLoader.getSystemClassLoader());
    ResourceBundle bundle = ResourceBundle.getBundle("bundle", Locale.ENGLISH, classLoader, CONTROL);
    assertNotNull(bundle);

    //
    ServletContextImpl servletContext = new ServletContextImpl(TestModule.class, "/webapp", "webapp");
    assertNotNull(servletContext.getResource("/simple.js"));

    //
    this.servletContext = servletContext;
    this.classLoader = classLoader;
  }

  @After
  public void tearDown() throws Exception {
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testScriptServing() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource module = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local bar = module.addLocalModule("/webapp", "/simple.js", null, 0);
    Reader reader = bar.read(null, servletContext, classLoader);
    assertReader("pass", reader);
  }

  @Test
  public void testScriptNotFound() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource module = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local bar = module.addLocalModule("/webapp", "/notfound.js", null, 0);
    assertNull(bar.read(null, servletContext, classLoader));
  }

  @Test
  public void testResolveNotLocalized() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource foo = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local module = foo.addLocalModule("/webapp", "/localized.js", null, 0);
    Reader reader = module.read(null, servletContext, classLoader);
    assertReader("${foo}", reader);
  }

  @Test
  public void testEscapeDoubleQuote() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource foo = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local module = foo.addLocalModule("/webapp", "/localized.js", "double_quote_bundle", 0);
    Reader reader = module.read(Locale.ENGLISH, servletContext, classLoader);
    assertReader("\"", reader);
  }

  @Test
  public void testEscapeSimpleQuote() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource foo = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local module = foo.addLocalModule("/webapp", "/localized.js", "simple_quote_bundle", 0);
    Reader reader = module.read(Locale.ENGLISH, servletContext, classLoader);
    assertReader("'", reader);
  }

  @Test
  public void testEnglishAsDefaultLocale() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource foo = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local module = foo.addLocalModule("/webapp", "/localized.js", "bundle", 0);
    Reader reader = module.read(null, servletContext, classLoader);
    assertReader("foo_en", reader);
  }

  @Test
  public void testEnglishAsFallbackLocale() throws Exception {

    ScriptGraph graph = new ScriptGraph();
    ScriptResource foo = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local module = foo.addLocalModule("/webapp", "/localized.js", "bundle", 0);
    Reader reader = module.read(Locale.CANADA, servletContext, classLoader);
    assertReader("foo_en", reader);
  }

  @Test
  public void testSpecificLanguage() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource foo = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local module = foo.addLocalModule("/webapp", "/localized.js", "bundle", 0);
    Reader reader = module.read(Locale.FRENCH, servletContext, classLoader);
    assertReader("foo_fr", reader);
  }

  @Test
  public void testSpecificCountry() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource foo = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Module.Local module = foo.addLocalModule("/webapp", "/localized.js", "bundle", 0);
    Reader reader = module.read(Locale.FRANCE, servletContext, classLoader);
    assertReader("foo_fr_FR", reader);
  }

  @Test
  public void testAdapter() throws Exception {
    ScriptGraph graph = new ScriptGraph();
    ScriptResource foo = graph.addResource(ResourceScope.SHARED.create("testModule"));
    Content[] contents =
                       new Content[] { new Content("var a;", false), new Content("/localized.js"), new Content("var b;", false) };
    Module.Local module = foo.addLocalModule("/webapp", contents, "bundle", 0);
    Reader reader = module.read(Locale.ENGLISH, servletContext, classLoader);
    assertReader("foo_en", reader);
  }

  private void assertReader(Object expected, Reader reader) {
    try {
      assertNotNull(reader);
      StringWriter script = new StringWriter();
      IOTools.copy(reader, script);
      ScriptEngineManager mgr = new ScriptEngineManager();
      ScriptEngine engine = mgr.getEngineByName("JavaScript");
      engine.eval(script.toString());
      Object test = engine.get("test");
      assertEquals(expected, test);
    } catch (Exception e) {
      throw failure(e);
    }
  }
}
