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
package org.exoplatform.web.security.csrf;

import java.lang.annotation.Annotation;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.ApplicationContextImpl;
import org.exoplatform.services.rest.method.MethodInvokerFilter;
import org.exoplatform.services.rest.resource.GenericMethodResource;
import org.exoplatform.services.rest.servlet.ServletContainerRequest;


/**
 * Method filter to check the CSRF token when the annotation ExoCSRFCheck is set
 */
public class CSRFAccessFilter implements MethodInvokerFilter
{
   private static final Log LOG = ExoLogger.getLogger(CSRFAccessFilter.class);

   /**
    * Check does <code>method</code> contains CSRF security annotations ExoCSRFCheck
    *
    * @see ExoCSRFCheck
    *
    */
   public void accept(GenericMethodResource method) {
      for (Annotation a : method.getMethod().getAnnotations())
      {
         Class<?> ac = a.annotationType();

         if (ac == ExoCSRFCheck.class)
         {
            //get token in context
            ServletContainerRequest request = (ServletContainerRequest) ApplicationContextImpl.getCurrent().getContainerRequest();
            if (request == null) {
              LOG.warn("HTTP Request not found. Can't check CSRF token on method (method={})", method.getMethod().getName());
              throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                                                        .entity("You do not have the permissions to perform this operation")
                                                        .type(MediaType.TEXT_PLAIN)
                                                        .build());
            }
            if (!CSRFTokenUtil.check(request.getServletRequest())) {
              LOG.warn("CSRF token is lost or this is an CSRF attack (method={})", method.getMethod().getName());
              throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                                                        .entity("You do not have the permissions to perform this operation")
                                                        .type(MediaType.TEXT_PLAIN)
                                                        .build());
            }
         }
      }
   }
}
