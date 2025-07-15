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
package io.meeds.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;

/**
 * An aspect processing point that will allow to encapsulate two behavior:
 * - Set current container to PortalContainer
 * - Start a transaction for IDM and JPA sessions
 */
@Aspect
public class ContainerTransactionalAspect {

  @Around("execution(* *(..)) && @annotation(io.meeds.common.ContainerTransactional)")
  public Object around(ProceedingJoinPoint point) throws Throwable {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ExoContainer transactionContainer = container;
    if (container instanceof RootContainer) {
      transactionContainer = PortalContainer.getInstance();
      ExoContainerContext.setCurrentContainer(transactionContainer);
    }
    RequestLifeCycle.begin(transactionContainer);
    try {
      return point.proceed();
    } finally {
      RequestLifeCycle.end();
      if (transactionContainer != container) {
        ExoContainerContext.setCurrentContainer(container);
      }
    }
  }

}
