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

/**
 * Interface that extends {@link ApplicationLifecycle} with request phase methods that allow interception of before/after ACTION
 * phase, and before/after RENDER phase of request processing.
 *
 */
public interface ApplicationRequestPhaseLifecycle<E extends RequestContext> extends ApplicationLifecycle<E> {
    /**
     * Perform any processing required at the beginning of {@link Phase#ACTION} or {@link Phase#RENDER} phase.
     *
     * @param app Application
     * @param context current RequestContext
     * @param phase starting phase
     */
    void onStartRequestPhase(Application app, E context, Phase phase);

    /**
     * Perform any processing required at the end of {@link Phase#ACTION} or {@link Phase#RENDER} phase.
     *
     * @param app Application
     * @param context current RequestContext
     * @param phase ending phase
     */
    void onEndRequestPhase(Application app, E context, Phase phase);
}
