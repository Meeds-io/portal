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
package org.exoplatform.portal.pc.aspects;

import javax.portlet.PortletRequest;

import org.gatein.pc.api.PortletInvokerException;
import org.gatein.pc.api.invocation.ActionInvocation;
import org.gatein.pc.api.invocation.EventInvocation;
import org.gatein.pc.api.invocation.PortletInvocation;
import org.gatein.pc.api.invocation.RenderInvocation;
import org.gatein.pc.api.invocation.ResourceInvocation;
import org.gatein.pc.api.invocation.response.PortletInvocationResponse;
import org.gatein.pc.api.spi.WindowContext;
import org.gatein.pc.portlet.PortletInvokerInterceptor;

/**
 * This interceptor exposes the current phase (i.e. ACTION_PHASE, RENDER_PHASE, etc) and the current window id for the
 * portlet request/invocation.
 *
 */
public class PortletLifecyclePhaseInterceptor extends PortletInvokerInterceptor {

    private static final ThreadLocal<PortletInvocation> INVOCATION_THREAD_LOCAL = new ThreadLocal<PortletInvocation>();

    @Override
    public PortletInvocationResponse invoke(PortletInvocation invocation) throws IllegalArgumentException, PortletInvokerException {
        try {
            INVOCATION_THREAD_LOCAL.set(invocation);
            return super.invoke(invocation);
        } finally {
            INVOCATION_THREAD_LOCAL.remove();
        }
    }

    public static String currentPhase() {
        PortletInvocation invocation = getCurrentInvocation();
        if (invocation == null) return null;

        if (invocation instanceof ActionInvocation) {
            return PortletRequest.ACTION_PHASE;
        } else if (invocation instanceof EventInvocation) {
            return PortletRequest.EVENT_PHASE;
        } else if (invocation instanceof RenderInvocation) {
            return PortletRequest.RENDER_PHASE;
        } else if (invocation instanceof ResourceInvocation) {
            return PortletRequest.RESOURCE_PHASE;
        } else {
            return null;
        }
    }

    public static String currentWindowId() {
        PortletInvocation invocation = getCurrentInvocation();
        if (invocation == null) return null;

        WindowContext wc = invocation.getWindowContext();
        if (wc == null) return null;

        return wc.getId();
    }

    private static PortletInvocation getCurrentInvocation() {
        return INVOCATION_THREAD_LOCAL.get();
    }
}