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
package org.exoplatform.commons.file.resource;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class WorkerThread extends Thread
{
   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger(WorkerThread.class.getName());

   protected boolean stopped = false;

   protected long timeout;

   public WorkerThread(String name, long timeout)
   {
      super(name);
      this.timeout = timeout;
   }

   public WorkerThread(long timeout)
   {
      super();
      this.timeout = timeout;
   }

   @Override
   public void run()
   {
      while (!stopped)
      {
         try
         {
            callPeriodically();
            sleep(timeout);
         }
         catch (InterruptedException e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
         }
         catch (Exception e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
      }
   }

   public void halt()
   {
      stopped = true;
   }

   protected abstract void callPeriodically() throws Exception;

}
