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
package org.exoplatform.portal.branding;

import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_COMPANY_LINK_INIT_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_COMPANY_LINK_SETTING_KEY;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_COMPANY_NAME_SETTING_KEY;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_CONTEXT;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_FAVICON_ID_SETTING_KEY;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_FAVICON_INIT_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_LOGIN_BG_ID_SETTING_KEY;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_LOGIN_BG_INIT_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_LOGIN_SUBTITLE_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_LOGIN_TITLE_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_LOGO_ID_SETTING_KEY;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_LOGO_INIT_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_RESET_ATTACHMENT_ID;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SCOPE;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SITE_NAME_INIT_PARAM;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SITE_NAME_SETTING_KEY;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SUBTITLE_SETTING_KEY;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_THEME_VARIABLES;
import static org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_TITLE_SETTING_KEY;
import static org.exoplatform.portal.branding.BrandingServiceImpl.FILE_API_NAME_SPACE;
import static org.exoplatform.portal.branding.BrandingServiceImpl.LOGIN_BACKGROUND_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.branding.model.Background;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.Favicon;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

@SuppressWarnings({
    "rawtypes", "unchecked"
})
public class BrandingServiceImplTest {

  @Test
  public void shouldGetDefaultBrandingInformationWhenNoUpdate() {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);
    ValueParam companyLink = new ValueParam();
    companyLink.setName(BRANDING_COMPANY_LINK_INIT_PARAM);
    companyLink.setValue("https://meeds.io");
    initParams.addParam(companyLink);
    ValueParam siteName = new ValueParam();
    siteName.setName(BRANDING_SITE_NAME_INIT_PARAM);
    siteName.setValue("Meeds");
    initParams.addParam(siteName);

    BrandingService brandingService = newBrandingService(settingService,
                                                         fileService,
                                                         uploadService,
                                                         localeConfigService,
                                                         configurationManager,
                                                         container,
                                                         initParams);

    // When
    Branding brandingInformation = brandingService.getBrandingInformation();

    // Then
    assertNotNull(brandingInformation);
    assertEquals("Default Company Name", brandingInformation.getCompanyName());
    assertEquals("https://meeds.io", brandingInformation.getCompanyLink());
    assertEquals("Meeds", brandingInformation.getSiteName());
  }

  @Test
  public void shouldGetDefaultBrandingThemeColorsWhenNoUpdate() {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);

    String primaryColor = "#3f8487";
    String primaryBackground = "#f0f0f0";
    String secondaryColor = "#000000";
    String secondaryBackground = "#e25d5d";
    String borderRadius = "8px";

    InitParams initParams = new InitParams();
    ValuesParam themeStyle = new ValuesParam();
    themeStyle.setName(BRANDING_THEME_VARIABLES);
    List<String> variables = new ArrayList<>();
    variables.add("primaryColor:" + primaryColor);
    variables.add("primaryBackground:" + primaryBackground);
    variables.add("secondaryColor:" + secondaryColor);
    variables.add("secondaryBackground:" + secondaryBackground);
    variables.add("borderRadius:" + borderRadius);
    themeStyle.setValues(variables);

    ValueParam companyName = new ValueParam();
    companyName.setName(BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");

    initParams.addParam(companyName);
    initParams.addParam(themeStyle);

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);
    brandingService.start();

    // When
    Branding brandingInformation = brandingService.getBrandingInformation();

    // Then
    assertNotNull(brandingInformation);
    assertNotNull("Default Theme style shouldn't be null", brandingInformation.getThemeStyle());

    assertEquals(5, brandingInformation.getThemeStyle().size());
    assertEquals(primaryColor, brandingInformation.getThemeStyle().get("primaryColor"));
    assertEquals(primaryBackground, brandingInformation.getThemeStyle().get("primaryBackground"));
    assertEquals(secondaryColor, brandingInformation.getThemeStyle().get("secondaryColor"));
    assertEquals(secondaryBackground, brandingInformation.getThemeStyle().get("secondaryBackground"));
    assertEquals(borderRadius, brandingInformation.getThemeStyle().get("borderRadius"));
  }

  @Test
  public void shouldGetSavedBrandingThemeColorsWhenUpdate() {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    String primaryColorNewValue = "#3f8488";
    String primaryBackgroundNewValue = "#f0f0f1";
    String secondaryColorNewValue = "#000001";
    String secondaryBackgroundNewValue = "#e25d5e";
    String borderRadiusNewValue = "12px";

    when(settingService.get(BRANDING_CONTEXT,
                            BRANDING_SCOPE,
                            "primaryColor")).thenReturn((SettingValue) SettingValue.create(primaryColorNewValue));
    when(settingService.get(BRANDING_CONTEXT,
                            BRANDING_SCOPE,
                            "primaryBackground")).thenReturn((SettingValue) SettingValue.create(primaryBackgroundNewValue));
    when(settingService.get(BRANDING_CONTEXT,
                            BRANDING_SCOPE,
                            "secondaryColor")).thenReturn((SettingValue) SettingValue.create(secondaryColorNewValue));
    when(settingService.get(BRANDING_CONTEXT,
                            BRANDING_SCOPE,
                            "secondaryBackground")).thenReturn((SettingValue) SettingValue.create(secondaryBackgroundNewValue));
    when(settingService.get(BRANDING_CONTEXT,
                            BRANDING_SCOPE,
                            "borderRadius")).thenReturn((SettingValue) SettingValue.create(borderRadiusNewValue));

    String primaryColor = "#3f8487";
    String primaryBackground = "#f0f0f0";
    String secondaryColor = "#000000";
    String secondaryBackground = "#e25d5d";
    String borderRadius = "8px";

    InitParams initParams = new InitParams();

    ValuesParam themeStyle = new ValuesParam();
    themeStyle.setName(BRANDING_THEME_VARIABLES);
    List<String> variables = new ArrayList<>();
    variables.add("primaryColor:" + primaryColor);
    variables.add("primaryBackground:" + primaryBackground);
    variables.add("secondaryColor:" + secondaryColor);
    variables.add("secondaryBackground:" + secondaryBackground);
    variables.add("borderRadius:" + borderRadius);
    themeStyle.setValues(variables);

    ValueParam companyName = new ValueParam();
    companyName.setName(BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");

    initParams.addParam(companyName);
    initParams.addParam(themeStyle);

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);
    brandingService.start();

    // When
    Branding brandingInformation = brandingService.getBrandingInformation();

    // Then
    assertNotNull(brandingInformation);
    assertNotNull("Default Theme colors shouldn't be null", brandingInformation.getThemeStyle());

    assertEquals(5, brandingInformation.getThemeStyle().size());
    assertEquals(primaryColorNewValue, brandingInformation.getThemeStyle().get("primaryColor"));
    assertEquals(primaryBackgroundNewValue, brandingInformation.getThemeStyle().get("primaryBackground"));
    assertEquals(secondaryColorNewValue, brandingInformation.getThemeStyle().get("secondaryColor"));
    assertEquals(secondaryBackgroundNewValue, brandingInformation.getThemeStyle().get("secondaryBackground"));
    assertEquals(borderRadiusNewValue, brandingInformation.getThemeStyle().get("borderRadius"));
  }

  @Test
  public void shouldGetUpdatedBrandingInformationWhenInformationUpdated() {
    // Given
    SettingService settingService = mock(SettingService.class);
    when(settingService.get(Context.GLOBAL,
                            Scope.GLOBAL,
                            BRANDING_COMPANY_NAME_SETTING_KEY)).thenReturn(new SettingValue("Updated Company Name"));
    when(settingService.get(Context.GLOBAL,
                            Scope.GLOBAL,
                            BRANDING_COMPANY_LINK_SETTING_KEY)).thenReturn(new SettingValue("https://investors.meeds.io"));
    when(settingService.get(Context.GLOBAL,
                            Scope.GLOBAL,
                            BRANDING_SITE_NAME_SETTING_KEY)).thenReturn(new SettingValue("Meeds.io"));
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);
    ValueParam companyLink = new ValueParam();
    companyLink.setName(BRANDING_COMPANY_LINK_INIT_PARAM);
    companyLink.setValue("https://meeds.io");
    initParams.addParam(companyLink);
    ValueParam siteName = new ValueParam();
    siteName.setName(BRANDING_SITE_NAME_INIT_PARAM);
    siteName.setValue("Meeds");
    initParams.addParam(siteName);

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    // When
    Branding brandingInformation = brandingService.getBrandingInformation();

    // Then
    assertNotNull(brandingInformation);
    assertEquals("Updated Company Name", brandingInformation.getCompanyName());
    assertEquals("https://investors.meeds.io", brandingInformation.getCompanyLink());
    assertEquals("Meeds.io", brandingInformation.getSiteName());
  }

  @Test
  public void shouldGetBrandingInformationWithoutBinaries() throws FileStorageException, IOException {

    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    InitParams initParams = new InitParams();

    String loginBgPath = "loginBgPath";
    String faviconPath = "faviconPath";
    String logoPath = "logoPath";

    ValueParam logoPathParam = new ValueParam();
    logoPathParam.setName(BRANDING_LOGO_INIT_PARAM);
    logoPathParam.setValue(logoPath);
    initParams.addParam(logoPathParam);

    ValueParam faviconPathParam = new ValueParam();
    faviconPathParam.setName(BRANDING_FAVICON_INIT_PARAM);
    faviconPathParam.setValue(faviconPath);
    initParams.addParam(faviconPathParam);

    ValueParam loginBgPathParam = new ValueParam();
    loginBgPathParam.setName(BRANDING_LOGIN_BG_INIT_PARAM);
    loginBgPathParam.setValue(loginBgPath);
    initParams.addParam(loginBgPathParam);

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    ServletContext context = mock(ServletContext.class);
    when(container.getPortalContext()).thenReturn(context);
    when(context.getResourceAsStream(loginBgPath)).thenReturn(new ByteArrayInputStream(new byte[] {
        1, 2, 3
    }));
    when(context.getResourceAsStream(faviconPath)).thenReturn(new ByteArrayInputStream(new byte[] {
        1, 2, 3
    }));
    when(container.getPortalContext()).thenReturn(context);
    when(context.getResourceAsStream(logoPath)).thenReturn(new ByteArrayInputStream(new byte[] {
        1, 2, 3
    }));

    when(fileService.writeFile(any())).thenAnswer(invocation -> invocation.getArgument(0));
    assertNotNull(brandingService.getLoginBackgroundPath());
    assertNotNull(brandingService.getLogoPath());
    assertNotNull(brandingService.getLoginBackgroundPath());

    Branding branding = brandingService.getBrandingInformation(false);
    assertNotNull(branding);
    assertNotNull(branding.getLogo());
    assertNotNull(branding.getFavicon());
    assertNotNull(branding.getLoginBackground());
    assertNull(branding.getLogo().getData());
    assertNull(branding.getFavicon().getData());
    assertNull(branding.getLoginBackground().getData());

    branding = brandingService.getBrandingInformation(true);
    assertNotNull(branding);
    assertNotNull(branding.getLogo());
    assertNotNull(branding.getFavicon());
    assertNotNull(branding.getLoginBackground());
    assertNotNull(branding.getLogo().getData());
    assertNotNull(branding.getFavicon().getData());
    assertNotNull(branding.getLoginBackground().getData());
  }

  @Test
  public void shouldUpdateCompanyNameAndTopBarThemeWhenInformationUpdated() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    String primaryColor = "#3f8487";
    String primaryBackground = "#f0f0f0";
    String secondaryColor = "#000000";
    String secondaryBackground = "#e25d5d";
    String borderRadius = "8px";

    String primaryColorNewValue = "#3f8488";
    String primaryBackgroundNewValue = "#f0f0f1";
    String secondaryColorNewValue = null;
    String secondaryBackgroundNewValue = null;
    String borderRadiusNewValue = null;

    ValuesParam themeStyle = new ValuesParam();
    themeStyle.setName(BRANDING_THEME_VARIABLES);
    List<String> variables = new ArrayList<>();
    variables.add("primaryColor:" + primaryColor);
    variables.add("primaryBackground:" + primaryBackground);
    variables.add("secondaryColor:" + secondaryColor);
    variables.add("secondaryBackground:" + secondaryBackground);
    variables.add("borderRadius:" + borderRadius);
    themeStyle.setValues(variables);
    initParams.addParam(themeStyle);

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    Branding newBranding = new Branding();
    newBranding.setCompanyName("New Company Name");
    newBranding.setThemeStyle(new HashMap<>());
    newBranding.getThemeStyle().put("primaryColor", primaryColorNewValue);
    newBranding.getThemeStyle().put("primaryBackground", primaryBackgroundNewValue);
    newBranding.getThemeStyle().put("secondaryColor", secondaryColorNewValue);
    newBranding.getThemeStyle().put("secondaryBackground", secondaryBackgroundNewValue);
    newBranding.getThemeStyle().put("borderRadius", borderRadiusNewValue);

    ArgumentCaptor<Context> settingContext = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> settingScope = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<String> settingKey = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue> settingValue = ArgumentCaptor.forClass(SettingValue.class);

    // When
    brandingService.updateBrandingInformation(newBranding);

    // Then
    verify(settingService, times(4)).set(settingContext.capture(),
                                         settingScope.capture(),
                                         settingKey.capture(),
                                         settingValue.capture());
    verify(settingService, times(1)).remove(BRANDING_CONTEXT,
                                            BRANDING_SCOPE,
                                            "secondaryColor");
    verify(settingService, times(1)).remove(BRANDING_CONTEXT,
                                            BRANDING_SCOPE,
                                            "secondaryBackground");
    verify(settingService, times(1)).remove(BRANDING_CONTEXT,
                                            BRANDING_SCOPE,
                                            "borderRadius");

    List<Context> contexts = settingContext.getAllValues();
    List<Scope> scopes = settingScope.getAllValues();
    List<String> keys = settingKey.getAllValues();
    List<SettingValue> values = settingValue.getAllValues();

    assertEquals(Context.GLOBAL, contexts.get(0));
    assertEquals(Scope.GLOBAL, scopes.get(0));
    assertEquals(BRANDING_COMPANY_NAME_SETTING_KEY, keys.get(0));
    assertEquals("New Company Name", values.get(0).getValue());

    assertEquals(BRANDING_CONTEXT, contexts.get(1));
    assertEquals(BRANDING_SCOPE, scopes.get(1));
    assertEquals(BRANDING_CONTEXT, contexts.get(2));
    assertEquals(BRANDING_SCOPE, scopes.get(2));

    String firstThemeColorParam = keys.get(1);
    String firstThemeColorValue = values.get(1).getValue().toString();

    String secondThemeColorParam = keys.get(2);
    String secondThemeColorValue = values.get(2).getValue().toString();

    assertEquals("primaryColor", secondThemeColorParam);
    assertEquals("primaryBackground", firstThemeColorParam);
    assertEquals(primaryColorNewValue, secondThemeColorValue);
    assertEquals(primaryBackgroundNewValue, firstThemeColorValue);
  }

  @Test
  public void shouldUpdateLogoWhenLogoUpdatedByUploadId() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    FileInfo fileInfo = new FileInfo(2L,
                                     "myLogo",
                                     "image/png",
                                     FILE_API_NAME_SPACE,
                                     "myLogo".getBytes().length,
                                     new Date(),
                                     "john",
                                     null,
                                     false);
    FileItem fileItem = new FileItem(fileInfo, null);
    when(fileService.writeFile(any(FileItem.class))).thenReturn(fileItem);
    when(fileService.getFileInfo(anyLong())).thenReturn(fileInfo);
    String uploadId = "1";
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    UploadResource uploadResource = new UploadResource(uploadId);
    URL resource = this.getClass().getResource("/branding/logo.png");
    uploadResource.setStoreLocation(resource.getPath());
    when(uploadService.getUploadResource(uploadId)).thenReturn(uploadResource);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    Branding newBranding = new Branding();
    Logo logo = new Logo();
    logo.setUploadId(uploadId);
    newBranding.setLogo(logo);

    ArgumentCaptor<Context> settingContextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> settingScopeArgumentCaptor = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<String> settingKeyArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue> settingValueArgumentCaptor = ArgumentCaptor.forClass(SettingValue.class);
    ArgumentCaptor<FileItem> fileItemArgumentCaptor = ArgumentCaptor.forClass(FileItem.class);

    // When
    brandingService.updateBrandingInformation(newBranding);

    // Then
    verify(settingService, times(2)).set(settingContextArgumentCaptor.capture(),
                                         settingScopeArgumentCaptor.capture(),
                                         settingKeyArgumentCaptor.capture(),
                                         settingValueArgumentCaptor.capture());
    verify(fileService, times(1)).writeFile(fileItemArgumentCaptor.capture());
    List<Context> contexts = settingContextArgumentCaptor.getAllValues();
    List<Scope> scopes = settingScopeArgumentCaptor.getAllValues();
    List<String> keys = settingKeyArgumentCaptor.getAllValues();
    List<SettingValue> values = settingValueArgumentCaptor.getAllValues();
    assertEquals(Context.GLOBAL, contexts.get(0));
    assertEquals(Scope.GLOBAL, scopes.get(0));
    assertEquals(BRANDING_LOGO_ID_SETTING_KEY, keys.get(0));
    assertEquals("2", values.get(0).getValue());
    List<FileItem> fileItems = fileItemArgumentCaptor.getAllValues();
    assertTrue(Arrays.equals(IOUtils.toByteArray(resource), fileItems.get(0).getAsByte()));
  }

  @Test
  public void shouldUpdateFaviconWhenFaviconUpdatedByUploadId() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    FileInfo fileInfo = new FileInfo(2L,
                                     "myFavicon",
                                     "image/png",
                                     FILE_API_NAME_SPACE,
                                     "myFavicon".getBytes().length,
                                     new Date(),
                                     "john",
                                     null,
                                     false);
    FileItem fileItem = new FileItem(fileInfo, null);
    when(fileService.writeFile(any(FileItem.class))).thenReturn(fileItem);
    when(fileService.getFileInfo(anyLong())).thenReturn(fileInfo);
    String uploadId = "1";
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    UploadResource uploadResource = new UploadResource(uploadId);
    URL resource = this.getClass().getResource("/branding/favicon.ico");
    uploadResource.setStoreLocation(resource.getPath());
    when(uploadService.getUploadResource(uploadId)).thenReturn(uploadResource);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    Branding newBranding = new Branding();
    Favicon favicon = new Favicon();
    favicon.setUploadId(uploadId);
    newBranding.setFavicon(favicon);

    ArgumentCaptor<Context> settingContextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> settingScopeArgumentCaptor = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<String> settingKeyArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue> settingValueArgumentCaptor = ArgumentCaptor.forClass(SettingValue.class);
    ArgumentCaptor<FileItem> fileItemArgumentCaptor = ArgumentCaptor.forClass(FileItem.class);

    // When
    brandingService.updateBrandingInformation(newBranding);

    // Then
    verify(settingService, times(2)).set(settingContextArgumentCaptor.capture(),
                                         settingScopeArgumentCaptor.capture(),
                                         settingKeyArgumentCaptor.capture(),
                                         settingValueArgumentCaptor.capture());
    verify(fileService, times(1)).writeFile(fileItemArgumentCaptor.capture());
    List<Context> contexts = settingContextArgumentCaptor.getAllValues();
    List<Scope> scopes = settingScopeArgumentCaptor.getAllValues();
    List<String> keys = settingKeyArgumentCaptor.getAllValues();
    List<SettingValue> values = settingValueArgumentCaptor.getAllValues();
    assertEquals(Context.GLOBAL, contexts.get(0));
    assertEquals(Scope.GLOBAL, scopes.get(0));
    assertEquals(BRANDING_FAVICON_ID_SETTING_KEY, keys.get(0));
    assertEquals("2", values.get(0).getValue());
    List<FileItem> fileItems = fileItemArgumentCaptor.getAllValues();
    assertTrue(Arrays.equals(IOUtils.toByteArray(resource), fileItems.get(0).getAsByte()));
  }

  @Test
  public void testGetDefaultLoginBackground() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    InitParams initParams = new InitParams();

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    assertNull(brandingService.getLoginBackgroundPath());

    String imagePath = "loginBgPath";

    ValueParam loginBgPath = new ValueParam();
    loginBgPath.setName(BRANDING_LOGIN_BG_INIT_PARAM);
    loginBgPath.setValue(imagePath);
    initParams.addParam(loginBgPath);

    brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    ServletContext context = mock(ServletContext.class);
    when(container.getPortalContext()).thenReturn(context);
    when(context.getResourceAsStream(imagePath)).thenReturn(new ByteArrayInputStream(new byte[] {
        1, 2, 3
    }));
    when(fileService.writeFile(any())).thenAnswer(invocation -> invocation.getArgument(0));
    Background loginBackground = brandingService.getLoginBackground();
    assertNotNull(loginBackground);
    assertEquals(3, loginBackground.getSize());
    assertNotNull(brandingService.getLoginBackgroundPath());
  }

  @Test
  public void testGetDefaultFavicon() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    InitParams initParams = new InitParams();

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    assertNull(brandingService.getFaviconPath());

    String imagePath = "faviconPath";

    ValueParam faviconPathParam = new ValueParam();
    faviconPathParam.setName(BRANDING_FAVICON_INIT_PARAM);
    faviconPathParam.setValue(imagePath);
    initParams.addParam(faviconPathParam);

    brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    ServletContext context = mock(ServletContext.class);
    when(container.getPortalContext()).thenReturn(context);
    when(context.getResourceAsStream(imagePath)).thenReturn(new ByteArrayInputStream(new byte[] {
        1, 2, 3
    }));
    when(fileService.writeFile(any())).thenAnswer(invocation -> invocation.getArgument(0));
    Favicon favicon = brandingService.getFavicon();
    assertNotNull(favicon);
    assertEquals(3, favicon.getSize());
    assertNotNull(brandingService.getFaviconPath());
  }

  @Test
  public void testGetDefaultLogo() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    InitParams initParams = new InitParams();

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    assertNull(brandingService.getLogoPath());

    String imagePath = "logoPath";

    ValueParam logoPathParam = new ValueParam();
    logoPathParam.setName(BRANDING_LOGO_INIT_PARAM);
    logoPathParam.setValue(imagePath);
    initParams.addParam(logoPathParam);

    brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    ServletContext context = mock(ServletContext.class);
    when(container.getPortalContext()).thenReturn(context);
    when(context.getResourceAsStream(imagePath)).thenReturn(new ByteArrayInputStream(new byte[] {
        1, 2, 3
    }));
    when(fileService.writeFile(any())).thenAnswer(invocation -> invocation.getArgument(0));
    Logo logo = brandingService.getLogo();
    assertNotNull(logo);
    assertEquals(3, logo.getSize());
    assertNotNull(brandingService.getLogoPath());
  }

  @Test
  public void testUpdateLoginBackground() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    InitParams initParams = new InitParams();

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    assertNull(brandingService.getLoginBackgroundPath());

    String uploadId = "uploadId";
    long fileId = 2l;

    UploadResource uploadResource = new UploadResource(uploadId, "fileName.png");
    uploadResource.setStoreLocation(this.getClass().getClassLoader().getResource("branding/favicon.ico").getPath());
    when(uploadService.getUploadResource(uploadId)).thenReturn(uploadResource);
    when(fileService.writeFile(any())).thenAnswer(invocation -> {
      FileItem fileItem = invocation.getArgument(0);
      fileItem.getFileInfo().setId(fileId);
      return fileItem;
    });

    Background loginBackground = new Background(uploadId, 0, null, 0, 0);
    brandingService.updateLoginBackground(loginBackground);

    verify(fileService, times(1)).writeFile(argThat(fileItem -> {
      assertNotNull(fileItem);
      assertNotNull(fileItem.getFileInfo());
      assertEquals(LOGIN_BACKGROUND_NAME, fileItem.getFileInfo().getName());
      assertEquals("image/png", fileItem.getFileInfo().getMimetype());
      assertEquals(FILE_API_NAME_SPACE, fileItem.getFileInfo().getNameSpace());
      return true;
    }));

    verify(settingService, times(1)).set(any(),
                                         any(),
                                         eq(BRANDING_LOGIN_BG_ID_SETTING_KEY),
                                         argThat(value -> value != null && value.getValue() != null
                                             && StringUtils.equals(value.getValue().toString(), String.valueOf(fileId))));
  }

  @Test
  public void testRemoveLoginBackground() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    InitParams initParams = new InitParams();

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);
    long fileId = 2l;

    when(settingService.get(Context.GLOBAL,
                            Scope.GLOBAL,
                            BRANDING_LOGIN_BG_ID_SETTING_KEY)).thenReturn(new SettingValue(String.valueOf(fileId)));
    FileItem fileItem = new FileItem(fileId,
                                     LOGIN_BACKGROUND_NAME,
                                     "image/png",
                                     FILE_API_NAME_SPACE,
                                     5l,
                                     new Date(),
                                     "testuser",
                                     false,
                                     new ByteArrayInputStream(new byte[] {
                                         1, 2, 3
                                     }));
    when(fileService.getFile(fileId)).thenReturn(fileItem);

    Background loginBackground = new Background(BRANDING_RESET_ATTACHMENT_ID, 0, null, 0, 0);
    brandingService.updateLoginBackground(loginBackground);

    verify(fileService, times(1)).deleteFile(fileId);
    verify(settingService, times(1)).remove(Context.GLOBAL,
                                            Scope.GLOBAL,
                                            BRANDING_LOGIN_BG_ID_SETTING_KEY);
  }

  @Test
  public void testUpdateLoginTitle() throws Exception {
    String defaultLoginTitle = "Login Title";

    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);

    InitParams initParams = new InitParams();
    ValueParam defaultTitleParam = new ValueParam();
    defaultTitleParam.setName(BRANDING_LOGIN_TITLE_PARAM);
    defaultTitleParam.setValue(defaultLoginTitle);
    initParams.addParam(defaultTitleParam);

    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(newLocaleConfig(Locale.ENGLISH));
    when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(newLocaleConfig(Locale.ENGLISH),
                                                                         newLocaleConfig(Locale.FRENCH)));

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    assertNotNull(brandingService.getLoginTitle());
    assertEquals(1, brandingService.getLoginTitle().size());
    assertEquals(defaultLoginTitle, brandingService.getLoginTitle(Locale.ENGLISH));
    assertEquals(defaultLoginTitle, brandingService.getLoginTitle(Locale.FRENCH));

    Map<String, String> customValues = new HashMap<>();
    doAnswer(invocation -> {
      customValues.put(invocation.getArgument(2).toString().replace(BRANDING_TITLE_SETTING_KEY, ""),
                       invocation.getArgument(3, SettingValue.class).getValue().toString());
      return null;
    }).when(settingService)
      .set(eq(BRANDING_CONTEXT),
           eq(BRANDING_SCOPE),
           argThat(key -> StringUtils.startsWith(key, BRANDING_TITLE_SETTING_KEY)),
           any());
    when(settingService
                       .get(eq(BRANDING_CONTEXT),
                            eq(BRANDING_SCOPE),
                            argThat(key -> StringUtils.startsWith(key, BRANDING_TITLE_SETTING_KEY)))).thenAnswer(invocation -> {
                              String language = invocation.getArgument(2).toString().replace(BRANDING_TITLE_SETTING_KEY, "");
                              return customValues.containsKey(language) ? SettingValue.create(customValues.get(language)) : null;
                            });
    doAnswer(invocation -> {
      customValues.remove(invocation.getArgument(2).toString().replace(BRANDING_TITLE_SETTING_KEY, ""));
      return null;
    }).when(settingService)
      .remove(eq(BRANDING_CONTEXT),
              eq(BRANDING_SCOPE),
              argThat(key -> StringUtils.startsWith(key, BRANDING_TITLE_SETTING_KEY)));

    String customLocaleTitle = "FR Title";

    Branding branding = new Branding();
    branding.setLoginTitle(Collections.singletonMap(Locale.FRENCH.getLanguage(), customLocaleTitle));
    brandingService.updateBrandingInformation(branding);

    assertEquals(2, brandingService.getLoginTitle().size());
    assertEquals(defaultLoginTitle, brandingService.getLoginTitle(Locale.ENGLISH));
    assertEquals(customLocaleTitle, brandingService.getLoginTitle(Locale.FRENCH));

    branding.setLoginTitle(Collections.singletonMap(Locale.ENGLISH.getLanguage(), ""));
    brandingService.updateBrandingInformation(branding);
    assertEquals(1, brandingService.getLoginTitle().size());
    assertEquals("", brandingService.getLoginTitle(Locale.ENGLISH));
    assertEquals("", brandingService.getLoginTitle(Locale.FRENCH));
  }

  @Test
  public void testUpdateLoginSubtitle() throws Exception {
    String defaultLoginTitle = "Login Subtitle";

    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);

    InitParams initParams = new InitParams();
    ValueParam defaultTitleParam = new ValueParam();
    defaultTitleParam.setName(BRANDING_LOGIN_SUBTITLE_PARAM);
    defaultTitleParam.setValue(defaultLoginTitle);
    initParams.addParam(defaultTitleParam);

    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(newLocaleConfig(Locale.ENGLISH));
    when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(newLocaleConfig(Locale.ENGLISH),
                                                                         newLocaleConfig(Locale.FRENCH)));

    BrandingServiceImpl brandingService = newBrandingService(settingService,
                       fileService,
                       uploadService,
                       localeConfigService,
                       configurationManager,
                       container,
                       initParams);

    assertNotNull(brandingService.getLoginSubtitle());
    assertEquals(1, brandingService.getLoginSubtitle().size());
    assertEquals(defaultLoginTitle, brandingService.getLoginSubtitle(Locale.ENGLISH));
    assertEquals(defaultLoginTitle, brandingService.getLoginSubtitle(Locale.FRENCH));

    Map<String, String> customValues = new HashMap<>();
    doAnswer(invocation -> {
      customValues.put(invocation.getArgument(2).toString().replace(BRANDING_SUBTITLE_SETTING_KEY, ""),
                       invocation.getArgument(3, SettingValue.class).getValue().toString());
      return null;
    }).when(settingService)
      .set(eq(BRANDING_CONTEXT),
           eq(BRANDING_SCOPE),
           argThat(key -> StringUtils.startsWith(key, BRANDING_SUBTITLE_SETTING_KEY)),
           any());
    when(settingService
                       .get(eq(BRANDING_CONTEXT),
                            eq(BRANDING_SCOPE),
                            argThat(key -> StringUtils.startsWith(key, BRANDING_SUBTITLE_SETTING_KEY))))
                                                                                                        .thenAnswer(invocation -> {
                                                                                                          String language =
                                                                                                                          invocation.getArgument(2)
                                                                                                                                    .toString()
                                                                                                                                    .replace(BRANDING_SUBTITLE_SETTING_KEY,
                                                                                                                                             "");
                                                                                                          return customValues.containsKey(language) ? SettingValue.create(customValues.get(language))
                                                                                                                                                    : null;
                                                                                                        });
    doAnswer(invocation -> {
      customValues.remove(invocation.getArgument(2).toString().replace(BRANDING_SUBTITLE_SETTING_KEY, ""));
      return null;
    }).when(settingService)
      .remove(eq(BRANDING_CONTEXT),
              eq(BRANDING_SCOPE),
              argThat(key -> StringUtils.startsWith(key, BRANDING_SUBTITLE_SETTING_KEY)));

    String customLocaleTitle = "FR Subtitle";

    Branding branding = new Branding();
    branding.setLoginSubtitle(Collections.singletonMap(Locale.FRENCH.getLanguage(), customLocaleTitle));
    brandingService.updateBrandingInformation(branding);

    assertEquals(2, brandingService.getLoginSubtitle().size());
    assertEquals(defaultLoginTitle, brandingService.getLoginSubtitle(Locale.ENGLISH));
    assertEquals(customLocaleTitle, brandingService.getLoginSubtitle(Locale.FRENCH));

    branding.setLoginSubtitle(Collections.singletonMap(Locale.ENGLISH.getLanguage(), ""));
    brandingService.updateBrandingInformation(branding);
    assertEquals(1, brandingService.getLoginSubtitle().size());
    assertEquals("", brandingService.getLoginSubtitle(Locale.ENGLISH));
    assertEquals("", brandingService.getLoginSubtitle(Locale.FRENCH));
  }

  private LocaleConfigImpl newLocaleConfig(Locale locale) {
    LocaleConfigImpl localeConfig = new LocaleConfigImpl();
    localeConfig.setLocale(locale);
    return localeConfig;
  }

  private BrandingServiceImpl newBrandingService(SettingService settingService,
                                                 FileService fileService,
                                                 UploadService uploadService,
                                                 LocaleConfigService localeConfigService,
                                                 ConfigurationManager configurationManager,
                                                 PortalContainer container,
                                                 InitParams initParams) {
    return new BrandingServiceImpl(container,
                                   configurationManager,
                                   settingService,
                                   fileService,
                                   uploadService,
                                   localeConfigService,
                                   mock(ListenerService.class),
                                   mock(UserACL.class),
                                   initParams);
  }

}
