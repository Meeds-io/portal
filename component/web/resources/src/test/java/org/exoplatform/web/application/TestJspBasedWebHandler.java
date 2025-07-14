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
package org.exoplatform.web.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestJspBasedWebHandler {

  @Test
  public void testPrepareDispatch() throws Exception {
    String bootstrapPath = "bootstrapUrl";
    String bootstrapModuleName = "SHARED/bootstrap";
    String applicationJSModuleName = "SHARED/applicationJSModule";
    String applicationJSModulePath = "applicationJSModulePath";
    String additionalJSModuleName = "SHARED/additionalJSModule";
    String additionalJSModulePath = "additionalJSModulePath";
    String additionalCSSModuleName = "additionalCSSModule";
    String companyName = "companyNameAcme";
    String primarycolor = "#445588";

    String uiParamKey = "uiParamKey";
    String uiParamValue = "uiParamValue";

    long brandingLastModifiedTime = System.currentTimeMillis();

    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    BrandingService brandingService = mock(BrandingService.class);
    JavascriptConfigService javascriptConfigService = mock(JavascriptConfigService.class);
    SkinService skinService = mock(SkinService.class);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("/portal");

    Map<String, Object> attributes = new HashMap<>();

    Mockito.doAnswer(invocation -> {
      String key = invocation.getArgument(0, String.class);
      Object value = invocation.getArgument(1);
      attributes.put(key, value);
      return null;
    }).when(request).setAttribute(any(), any());

    LocaleConfigImpl localeConfig = new LocaleConfigImpl();
    localeConfig.setLocale(Locale.ENGLISH);

    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(localeConfig);

    when(brandingService.getCompanyName()).thenReturn(companyName);
    when(brandingService.getLastUpdatedTime()).thenReturn(brandingLastModifiedTime);
    when(brandingService.getThemeStyle()).thenReturn(Collections.singletonMap("primaryColor", primarycolor));

    JSONObject jsConfig = new JSONObject("{paths:{}}");
    JSONObject jsConfigPaths = jsConfig.getJSONObject("paths");
    jsConfigPaths.put(bootstrapModuleName, bootstrapPath);
    jsConfigPaths.put(additionalJSModuleName, additionalJSModulePath);
    jsConfigPaths.put(applicationJSModuleName, applicationJSModulePath);

    when(javascriptConfigService.getJSConfig()).thenReturn(jsConfig);

    HttpServletResponse response = mock(HttpServletResponse.class);

    ControllerContext controllerContext = new ControllerContext(null, null, request, response, null);

    JspBasedWebHandler jspBasedWebHandler = new JspBasedWebHandler(localeConfigService,
                                                                   brandingService,
                                                                   javascriptConfigService,
                                                                   skinService) {};

    jspBasedWebHandler.prepareDispatch(controllerContext,
                                       applicationJSModuleName,
                                       Arrays.asList(additionalJSModuleName, bootstrapModuleName),
                                       Collections.singletonList(additionalCSSModuleName),
                                       params -> {
                                         try {
                                           params.put(uiParamKey, uiParamValue);
                                         } catch (JSONException e) {
                                           fail(e.getMessage()); // NOSONAR
                                         }
                                       });

    assertFalse(attributes.isEmpty());
    assertTrue(attributes.containsKey("jsConfig"));
    assertTrue(attributes.containsKey("localeConfig"));
    assertTrue(attributes.containsKey("headerScripts"));
    assertTrue(attributes.containsKey("pageScripts"));
    assertTrue(attributes.containsKey("inlineScripts"));
    assertTrue(attributes.containsKey("brandingPrimaryColor"));
    assertTrue(attributes.containsKey("brandingThemeUrl"));
    assertTrue(attributes.containsKey("skinUrls"));

    assertEquals(jsConfig.toString(), attributes.get("jsConfig"));
    assertEquals(localeConfig, attributes.get("localeConfig"));
    assertEquals(Collections.emptyList(), attributes.get("headerScripts"));
    String inlineScripts = attributes.get("inlineScripts").toString();
    assertTrue(inlineScripts.contains(bootstrapModuleName));
    assertTrue(inlineScripts.contains(applicationJSModuleName));
    assertTrue(inlineScripts.contains(additionalJSModuleName));
    assertTrue(inlineScripts.contains(uiParamKey));
    assertTrue(inlineScripts.contains(uiParamValue));
    assertTrue(inlineScripts.contains(companyName));
    assertTrue(inlineScripts.contains("brandingLogo"));
    assertTrue(attributes.get("brandingThemeUrl").toString().contains(String.valueOf(brandingLastModifiedTime)));
    assertEquals(attributes.get("brandingPrimaryColor").toString(), primarycolor);
    verify(response, times(1)).setContentType(JspBasedWebHandler.TEXT_HTML_CONTENT_TYPE);
  }
}
