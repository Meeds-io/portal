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
package org.exoplatform.portal.application.localization;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;

/**
 * This filter provides {@link HttpServletRequest#getLocale()} and {@link HttpServletRequest#getLocales()} override for
 * extra-portlet requests (i.e. unbridged .jsp). Thanks to it dynamic resources can be localized to keep in sync with the rest
 * of the portal. This filter is re-entrant, and can safely be installed for INCLUDE, FORWARD, and ERROR dispatch methods.
 * <p>
 * A concrete example of re-entrant use is login/jsp/login.jsp used when authentication fails at portal login.
 * <p>
 * By default {@link HttpServletRequest#getLocale()} and {@link HttpServletRequest#getLocales()} reflect browser language
 * preference. When using this filter these two calls employ the same Locale determination algorithm as
 * {@link LocalizationLifecycle} does.
 * <p>
 * This filter can be activated / deactivated via portal module's web.xml
 * <p>
 * If default portal language is other than English, it can be configured for the filter by using PortalLocale init param:
 * <p>
 *
 * <pre>
 * &lt;filter&gt;
 *   &lt;filter-name&gt;LocalizationFilter&lt;/filter-name&gt;
 *   &lt;filter-class&gt;org.exoplatform.portal.application.localization.LocalizationFilter&lt;/filter-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;PortalLocale&lt;/param-name&gt;
 *     &lt;param-value&gt;fr_FR&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 * </pre>
 *
 */
public class LocalizationFilter extends AbstractFilter {
    private static Log log = ExoLogger.getLogger("portal:LocalizationFilter");

    private static ThreadLocal<Locale> currentLocale = new ThreadLocal<Locale>();

    private Locale portalLocale = Locale.ENGLISH;

    @Override
    protected void afterInit(FilterConfig config) throws ServletException {
        String locale = config.getInitParameter("PortalLocale");
        locale = locale != null ? locale.trim() : null;
        if (locale != null && locale.length() > 0)
            portalLocale = LocaleContextInfo.getLocale(locale);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        try {
            // Due to forwards, and includes the filter might be reentered
            // If current requestContext exists use its Locale
            PortalRequestContext context = PortalRequestContext.getCurrentInstance();
            if (context != null && context.getLocale() != null) {
                // No need to wrap if reentered
                boolean skipWrapping = currentLocale.get() != null;
                // overwrite any already set currentLocale
                currentLocale.set(context.getLocale());
                if (!skipWrapping) {
                    req = new HttpRequestWrapper(req);
                }
                chain.doFilter(req, res);
                return;
            }

            // If reentered we don't need to wrap
            if (currentLocale.get() != null) {
                chain.doFilter(request, response);
                return;
            }

            // Initialize currentLocale
            ExoContainer container = getContainer();
            if (container == null) {
                // Nothing we can do, move on
                chain.doFilter(req, res);
                return;
            }

            LocalePolicy localePolicy = (LocalePolicy) container.getComponentInstanceOfType(LocalePolicy.class);

            LocaleContextInfo localeCtx = LocaleContextInfoUtils.buildLocaleContextInfo(req);

            Set<Locale> supportedLocales = LocaleContextInfoUtils.getSupportedLocales();
            
            Locale locale = localePolicy.determineLocale(localeCtx);
            boolean supported = supportedLocales.contains(locale);

            if (!supported && !"".equals(locale.getCountry())) {
                locale = new Locale(locale.getLanguage());
                supported = supportedLocales.contains(locale);
            }
            if (!supported) {
                if (log.isWarnEnabled())
                    log.warn("Unsupported locale returned by LocalePolicy: " + localePolicy + ". Falling back to 'en'.");
                locale = Locale.ENGLISH;
            }

            currentLocale.set(locale);
            chain.doFilter(new HttpRequestWrapper(req), res);
        } catch (Exception e) {
            throw new RuntimeException("LocalizationFilter exception: ", e);
        } finally {
            currentLocale.remove();
        }
    }
    
    public void destroy() {
    }

    public static Locale getCurrentLocale() {
        return currentLocale.get();
    }
}
