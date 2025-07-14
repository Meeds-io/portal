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
package exo.portal.component.identiy.opendsconfig;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import org.picketlink.idm.common.io.IOTools;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DSConfig
{

   private String name;

   private String description;

   private String host;

   private String port;

   private String contextFactory;

   private String adminDN;

   private String adminPassword;

   private String configFile;

   private String populateLdif;

   private String cleanUpDN;

   public DSConfig(String name, String description, String host, String port, String contextFactory, String adminDN, String adminPass, String configFile, String populateLdif, String cleanUpDN)
   {
      this.name = name;
      this.description = description;
      this.host = host;
      this.port = port;
      this.contextFactory = contextFactory;
      this.adminDN = adminDN;
      this.adminPassword = adminPass;
      this.configFile = configFile;
      this.populateLdif = populateLdif;
      this.cleanUpDN = cleanUpDN;
   }

   public String toString()
   {
      return new StringBuffer().append("[").
         append(description).
         append("]/[").
         append(host).
         append(":").
         append(port).
         append("]").
         toString();
   }


   public static DSConfig obtainConfig(String directories, String directoryName) throws Exception
   {
      if (directoryName != null && directoryName.length() > 0)
      {
         URL url = Thread.currentThread().getContextClassLoader().getResource(directories);

         DSConfig[] configs = fromXML(url);

         for (DSConfig config : configs)
         {
            if (config.getName().equals(directoryName))
            {
               return config;
            }
         }

         throw new IllegalStateException("Could not obtain Config for {directoryName:directories} - {" + directoryName + ":" + directories + "}" );
      }
      else
      {
         return null;
      }
   }

   public static DSConfig[] fromXML(URL url) throws Exception
   {
      ArrayList configs = new ArrayList();
      InputStream in = null;
      try
      {
         in = IOTools.safeBufferedWrapper(url.openStream());
         Document doc = XMLTools.getDocumentBuilderFactory().newDocumentBuilder().parse(in);
         for (Iterator i = XMLTools.getChildrenIterator(doc.getDocumentElement(), "directory"); i.hasNext();)
         {
            Element childElt = (Element)i.next();
            Element nm = XMLTools.getUniqueChild(childElt, "directory-name", true);
            Element desc = XMLTools.getUniqueChild(childElt, "description", true);
            Element config = XMLTools.getUniqueChild(childElt, "config-file", true);
            Element h = XMLTools.getUniqueChild(childElt, "host", true);
            Element p = XMLTools.getUniqueChild(childElt, "port", true);
            Element context = XMLTools.getUniqueChild(childElt, "context-factory", true);
            Element admin = XMLTools.getUniqueChild(childElt, "admin-dn", true);
            Element password = XMLTools.getUniqueChild(childElt, "admin-password", true);
            Element populate = XMLTools.getUniqueChild(childElt, "populate-ldif", true);
            Element cleanup = XMLTools.getUniqueChild(childElt, "cleanup-dn", true);

            String name = XMLTools.asString(nm);
            String description = XMLTools.asString(desc);
            String configFile = XMLTools.asString(config);
            String host = XMLTools.asString(h);
            String port = XMLTools.asString(p);
            String contextFactory = XMLTools.asString(context);
            String adminDN = XMLTools.asString(admin);
            String adminPassword = XMLTools.asString(password);
            String populateLdif = XMLTools.asString(populate);
            String cleanUpDN = XMLTools.asString(cleanup);

            DSConfig dsCfg = new DSConfig(
               name,
               description,
               host,
               port,
               contextFactory,
               adminDN,
               adminPassword,
               configFile,
               populateLdif,
               cleanUpDN);
            configs.add(dsCfg);
         }
         return (DSConfig[])configs.toArray(new DSConfig[configs.size()]);
      }
      finally
      {
         IOTools.safeClose(in);
      }
   }

   public String getURL()
   {
      return new StringBuffer("ldap://").append(getHost())
         .append(":")
         .append(getPort())
         .toString();
   }

   public String getHost()
   {
      return host;
   }

   public void setHost(String host)
   {
      this.host = host;
   }

   public String getContextFactory()
   {
      return contextFactory;
   }

   public void setContextFactory(String contextFactory)
   {
      this.contextFactory = contextFactory;
   }

   public String getAdminDN()
   {
      return adminDN;
   }

   public void setAdminDN(String adminDN)
   {
      this.adminDN = adminDN;
   }

   public String getAdminPassword()
   {
      return adminPassword;
   }

   public void setAdminPassword(String adminPassword)
   {
      this.adminPassword = adminPassword;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String getDescription()
   {
      return description;
   }

   public void setDescription(String description)
   {
      this.description = description;
   }


   public String getPopulateLdif()
   {
      return populateLdif;
   }

   public void setPopulateLdif(String populateLdif)
   {
      this.populateLdif = populateLdif;
   }

   public String getCleanUpDN()
   {
      return cleanUpDN;
   }

   public void setCleanUpDN(String cleanUpDN)
   {
      this.cleanUpDN = cleanUpDN;
   }

   public String getPort()
   {
      return port;
   }

   public void setPort(String port)
   {
      this.port = port;
   }

   public String getConfigFile()
   {
      return configFile;
   }

   public void setConfigFile(String configFile)
   {
      this.configFile = configFile;
   }

}
