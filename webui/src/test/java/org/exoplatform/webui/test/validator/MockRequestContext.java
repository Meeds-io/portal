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
package org.exoplatform.webui.test.validator;

import java.util.Locale;

import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.application.URLBuilder;
import org.exoplatform.web.url.PortalURL;
import org.exoplatform.web.url.ResourceType;
import org.exoplatform.web.url.URLFactory;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;

public class MockRequestContext extends WebuiRequestContext {
    private Locale locale;

    public MockRequestContext(Locale locale) {
        super(null);
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    @Override
    public URLBuilder<UIComponent> getURLBuilder() {
        return null;
    }

    @Override
    public String getRequestContextPath() {
        return null;
    }

    @Override
    public String getPortalContextPath() {
        return null;
    }

    @Override
    public <T> T getRequest() {
        return null;
    }

    @Override
    public <T> T getResponse() {
        return null;
    }

    @Override
    public void sendRedirect(String url) throws Exception {

    }

    @Override
    public URLFactory getURLFactory() {
        return null;
    }

    @Override
    public <R, U extends PortalURL<R, U>> U newURL(ResourceType<R, U> resourceType, URLFactory urlFactory) {
        return null;
    }

    @Override
    public Orientation getOrientation() {
        return null;
    }

    @Override
    public String getRequestParameter(String name) {
        return null;
    }

    @Override
    public String[] getRequestParameterValues(String name) {
        return null;
    }

    @Override
    public boolean useAjax() {
        return false;
    }

    @Override
    public UserPortal getUserPortal() {
        return null;
    }
}
