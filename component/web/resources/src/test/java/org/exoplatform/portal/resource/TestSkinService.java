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

import static org.exoplatform.web.controller.metadata.DescriptorBuilder.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.exoplatform.commons.utils.BinaryOutput;
import org.exoplatform.portal.resource.SkinResourceRequestHandler.SkinResourceRenderer;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.test.mocks.servlet.MockHttpServletResponse;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

public class TestSkinService extends AbstractSkinServiceTest {
  private static boolean isFirstStartup = true;

  boolean isDevelopingMode() {
    return false;
  }

  @Override
  boolean setUpTestEnvironment() {
    return isFirstStartup;
  }

  Router getRouter() {
    Router router;
    try {
      router = router().add(
                            route("/skins/{gtn:version}/{gtn:resource}{gtn:compress}{gtn:orientation}.css")
                                                                                                           .with(routeParam("gtn:handler").withValue("skin"))
                                                                                                           .with(pathParam("gtn:version").matchedBy("[^/]*")
                                                                                                                                         .preservePath())
                                                                                                           .with(pathParam("gtn:orientation").matchedBy("-(lt)|-(rt)|")
                                                                                                                                             .captureGroup(true))
                                                                                                           .with(pathParam("gtn:compress").matchedBy("-(min)|")
                                                                                                                                          .captureGroup(true))
                                                                                                           .with(pathParam("gtn:resource").matchedBy(".+?")
                                                                                                                                          .preservePath()))
                       .build();
      return router;
    } catch (RouterConfigException e) {
      return null;
    }
  }

  @Override
  void touchSetUp() {
    isFirstStartup = false;
  }

  @SuppressWarnings({ "deprecation", "removal" })
  public void testRenderCss() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final BinaryOutput output = new BinaryOutput() {
      public Charset getCharset() {
        return StandardCharsets.UTF_8;
      }

      public void write(byte b) throws IOException {
        outputStream.write(b);
      }

      public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
      }

      public void write(byte[] bytes, int off, int len) throws IOException {
        outputStream.write(bytes, off, len);
      }
    };
    ControllerContext controllerContext = newControllerContext(getRouter(), "/skins/7.0/mockwebapp/skin/FirstPortlet-min-lt.css");
    ResourceRenderer renderer = new SkinResourceRenderer(new MockHttpServletResponse(), controllerContext, output);
    assertTrue(skinService.renderCSS(controllerContext, renderer, true));
    String result = outputStream.toString();
    assertEquals(".FirstPortlet {foo1 : bar1}", result);
  }

  public void testGetCustomPortalSkins() {
    Collection<SkinConfig> customSkins = skinService.getCustomPortalSkins("TestSkin");
    assertNotNull(customSkins);
    assertEquals(0, customSkins.size());

    customSkins = skinService.getCustomPortalSkins("OtherSkin");
    assertNotNull(customSkins);
    assertEquals(1, customSkins.size());
  }

  public void testGetSkinList() {
    String[] skinList = skinService.getSkinList();
    assertNotNull(skinList);
    assertEquals(3, skinList.length);
  }

  public void testRenderURL() {
    SkinURL skinURL = skinService.getSkin("mockwebapp/FirstPortlet", "TestSkin").createURL();
    assertEquals("/mockwebapp/skin/FirstPortlet.css?orientation=LT&minify=true&hash=1107379639", skinURL.toString());
    skinURL.setOrientation(Orientation.RT);
    assertEquals("/mockwebapp/skin/FirstPortlet.css?orientation=RT&minify=true&hash=1107379639", skinURL.toString());
  }

  public void testGetSkinModuleFileContent() throws IOException {
    assertEquals(".FirstPortlet {foo1 : bar1}", skinService.getSkinModuleFileContent("/mockwebapp/skin/FirstPortlet.css"));
    assertEquals(".ThirdPortlet {foo1 : bar1}\n", skinService.getSkinModuleFileContent("/mockwebapp-test/skin/ThirdPortlet.css"));


  }

  public void testGetSkinModuleContent() throws IOException {
    assertEquals(".FirstPortlet {foo1 : bar1}",
                 skinService.getSkinModuleContent("/mockwebapp/skin/FirstPortlet.css", 0, Orientation.LT, true));
  }

}
