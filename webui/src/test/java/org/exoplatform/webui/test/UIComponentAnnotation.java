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
package org.exoplatform.webui.test;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.config.annotation.EventInterceptorConfig;
import org.exoplatform.webui.config.annotation.ParamConfig;
import org.exoplatform.webui.config.annotation.ValidatorConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(type = UIComponentAnnotation.class, lifecycle = UILifecycle.class, template = ".....................", decorator = "....................", events = { @EventConfig(name = "name", phase = Phase.DECODE, listeners = UIComponentAnnotation.UIComponentEvent.class, initParams = { @ParamConfig(name = "name", value = "value") }) }, initParams = { @ParamConfig(name = "name", value = "value") }, validators = { @ValidatorConfig(type = UIComponentAnnotation.UIComponentValidator.class, initParams = { @ParamConfig(name = "name", value = "value") }) }, eventInterceptors = { @EventInterceptorConfig(type = UIComponentAnnotation.UIComponentEventInterceptor.class, interceptors = { "inter1" }, initParams = { @ParamConfig(name = "name", value = "value") }) }

)
public class UIComponentAnnotation {

    public static class UIComponentValidator {

    }

    public static class UIComponentEventInterceptor {

    }

    public static class UIComponentEvent extends EventListener {
        @SuppressWarnings("unused")
        public void execute(Event event) throws Exception {

        }
    }
}
