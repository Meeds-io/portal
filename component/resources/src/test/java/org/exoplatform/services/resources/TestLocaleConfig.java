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
package org.exoplatform.services.resources;

import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;

import org.exoplatform.container.PortalContainer;

/**
 * Thu, May 15, 2004 @
 *
 */
public class TestLocaleConfig extends AbstractResourceBundleTest {

    private LocaleConfigService service_;

    public TestLocaleConfig(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        service_ = PortalContainer.getInstance().getComponentInstanceOfType(LocaleConfigService.class);
    }
    
    public void testGetNavigationResourceBundle() throws Exception {
      LocaleConfig localeConfig = service_.getLocaleConfig("en");
      assertNotNull(localeConfig);

      ResourceBundle resourceBundle = localeConfig.getNavigationResourceBundle("group", "notfound");
      assertNotNull(resourceBundle);
    }

    public void testLocaleConfigManager() throws Exception {
        // -------------------default locale is English-------------
        LocaleConfig locale = service_.getDefaultLocaleConfig();
        assertTrue("expect defautl locale config is found", locale != null);
        assertEquals("expect default locale is English", Locale.ENGLISH, locale.getLocale());
        // --------------get a locale------------------
        locale = service_.getLocaleConfig("fr");
        assertTrue("expect locale config is found", locale != null);
        assertTrue("expect France locale is found", locale.getLocale().equals(Locale.FRENCH));

        locale = service_.getLocaleConfig("vi");
        assertTrue("expect locale config is found", locale != null);
        assertEquals("expect Viet Nam locale is found", "vi", locale.getLocale().toString());
        /*-------------------get all locale config-------------------
         * 4 preconfig locales: English, France, Arabic, Vietnamese
         **/
        Collection<LocaleConfig> locales = service_.getLocalConfigs();
        assertTrue(locales.size() == 4);

        Locale vnlocale = service_.getLocaleConfig("vi").getLocale();
        assertContains(locales, vnlocale);
        assertContains(locales, Locale.ENGLISH);
        assertContains(locales, Locale.FRENCH);
    }

    private void assertContains(Collection<LocaleConfig> configs, Locale locale) {
        for (LocaleConfig config : configs) {
            if (config.getLocale().equals(locale)) {
                return;
            }
        }
        fail("Was expecting locale " + locale + " to be present in " + configs);
    }
}
