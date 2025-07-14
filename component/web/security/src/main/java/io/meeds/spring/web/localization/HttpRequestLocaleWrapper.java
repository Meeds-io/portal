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
package io.meeds.spring.web.localization;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Locale;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class HttpRequestLocaleWrapper extends HttpServletRequestWrapper {

  private Locale locale;

  public HttpRequestLocaleWrapper(HttpServletRequest request) {
    super(request);
  }

  @Override
  public Locale getLocale() {
    if (locale == null) {
      LocaleContextInfo localeCtx = LocaleContextInfoUtils.buildLocaleContextInfo((HttpServletRequest) getRequest());
      LocalePolicy localePolicyService = ExoContainerContext.getService(LocalePolicy.class);
      locale = localePolicyService == null ? getRequest().getLocale() : localePolicyService.determineLocale(localeCtx);
    }
    return locale;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    Locale userLocale = getLocale();
    if (userLocale == null) {
      return getRequest().getLocales();
    } else {
      LinkedList<Locale> locales = new LinkedList<>(Collections.list(getRequest().getLocales()));
      locales.add(userLocale);
      return Collections.enumeration(locales);
    }
  }

}
