package org.exoplatform.portal.config;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.services.resources.LocaleConfigService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NewPortalConfigListenerTest {

  @Mock
  private UserPortalConfigService owner;

  @Mock
  private LayoutService           layoutService;

  @Mock
  private ConfigurationManager    configurationManager;

  @Mock
  private NavigationService       navigationService;

  @Mock
  private InitParams              initParams;

  @Mock
  private DescriptionStorage      descriptionStorage;

  @Mock
  private UserACL                 userACL;

  @Mock
  private LocaleConfigService     localeConfigService;

  @Test
  public void testInitPageDB() {
    ValueParam valueParam = new ValueParam();
    valueParam.setName("meta.portal");
    valueParam.setValue("classic");

    when(initParams.getValueParam("meta.portal")).thenReturn(valueParam);

    NewPortalConfigListener newPortalConfigListener = new NewPortalConfigListener(owner,
                                                                                  layoutService,
                                                                                  configurationManager,
                                                                                  initParams,
                                                                                  navigationService,
                                                                                  descriptionStorage,
                                                                                  userACL,
                                                                                  localeConfigService);

    newPortalConfigListener.createdOwners.add("global");
    HashSet<String> predefinedOwner = new HashSet<>();
    predefinedOwner.add("global");

    NewPortalConfig newPortalConfig = mock(NewPortalConfig.class);

    when(newPortalConfig.getPredefinedOwner()).thenReturn(predefinedOwner);
    when(newPortalConfig.getImportMode()).thenReturn("overwrite");
    when(newPortalConfig.getOwnerType()).thenReturn(PortalConfig.PORTAL_TYPE);

    PageContext pageContext = mock(PageContext.class);
    List<PageContext> allPages = new ArrayList<>();
    allPages.add(pageContext);

    when(layoutService.findPages(any(SiteKey.class))).thenReturn(allPages);

    newPortalConfigListener.initPageDB(newPortalConfig);

    verify(layoutService, times(1)).removePages(any());

    Mockito.reset(layoutService);

    when(newPortalConfig.getImportMode()).thenReturn("merge");

    newPortalConfigListener.initPageDB(newPortalConfig);

    verify(layoutService, times(0)).removePages(any());
  }
}
