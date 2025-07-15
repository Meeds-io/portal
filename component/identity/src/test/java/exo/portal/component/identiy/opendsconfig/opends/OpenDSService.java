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
package exo.portal.component.identiy.opendsconfig.opends;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;

import exo.portal.component.identiy.opendsconfig.DSConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.opends.server.tools.LDAPModify;
import org.opends.server.types.DirectoryEnvironmentConfig;
import org.opends.server.types.InitializationException;
import org.opends.server.util.EmbeddedUtils;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

public class OpenDSService
{
   private static Log log                             = ExoLogger.getLogger(OpenDSService.class.getName());

   private String serverRoot = "";

   public static final String              LDAP_HOST                       = "localhost";

   public static final String              LDAP_PORT                       = "10389";

   public static final String              LDAP_PROVIDER_URL               = "ldap://" + LDAP_HOST + ":" + LDAP_PORT;

   public static final String              LDAP_PRINCIPAL                  = "cn=Directory Manager";

   public static final String              LDAP_CREDENTIALS                = "password";

   public String                           EMBEDDED_OPEN_DS_DIRECTORY_NAME = "EmbeddedOpenDS";

   protected DSConfig directoryConfig;

   public String                           directories                     = "ldap/datasources/directories.xml";

   // By default use embedded OpenDS
   private String                          directoryName                   = EMBEDDED_OPEN_DS_DIRECTORY_NAME;

   public static Hashtable<String, String> env                             = new Hashtable<String, String>();

   String                                  identityConfig;


   public OpenDSService(String serverRoot)
   {
      this.serverRoot = serverRoot;
   }

   public DirectoryEnvironmentConfig getConfig()
   {
      DirectoryEnvironmentConfig config = new DirectoryEnvironmentConfig();


      try
      {
         File root;

         if (getServerRoot() != null)
         {
            root = new File(getServerRoot());
         }
         else
         {
            
            //Find opends root based on where the config.ldif file is:

           URL rootURL = Thread.currentThread().getContextClassLoader().getResource("ldap/opends/config/config.ldif");

            if (rootURL == null)
            {
               throw new IllegalStateException("opends root doesn't exist");
            }



            try
            {
               root = new File(rootURL.toURI());
            }
            catch (URISyntaxException e)
            {
               root = new File(rootURL.getPath());
            }

            if (root != null)
            {
               root = root.getParentFile().getParentFile();
            }

         }


         if (root == null || !root.exists())
         {
            throw new IllegalStateException("opends root doesn't exist: " + getServerRoot());
         }
         if (!root.isDirectory())
         {
            throw new IllegalStateException("opends root is not a directory: " + getServerRoot());
         }

         // Server root points to the directory with opends configuration
         config.setServerRoot(root);
         config.setForceDaemonThreads(true);

      }
      catch (InitializationException e)
      {
         log.error("An unexpected error occurs ",e);
      }

      return config;
   }


   public void start()
   {
      if (!EmbeddedUtils.isRunning())
      {
         try
         {
            EmbeddedUtils.startServer(getConfig());
         }
         catch (Exception e)
         {
            log.error("An unexpected error occurs ",e);
         }
      }
   }

   public void stop()
   {
      if (EmbeddedUtils.isRunning())
      {
         EmbeddedUtils.stopServer(this.getClass().getName(), null);
      }
   }

   public String getServerRoot()
   {
      return serverRoot;
   }

   public void setServerRoot(String serverRoot)
   {
      this.serverRoot = serverRoot;
   }

   public void initLDAPServer() throws Exception{
      loadConfig();
      populateLDIF();
      populate();
   }
   public void populate() throws Exception {
      populateLDIFFile("ldap/ldap/initial-opends.ldif");
   }

   public void populateLDIF() throws Exception {
      String ldif = directoryConfig.getPopulateLdif();
      URL ldifURL = Thread.currentThread().getContextClassLoader().getResource(ldif);

      log.info("LDIF: " + ldifURL.toURI().getPath());
      String[] cmd = new String[] { "-h", directoryConfig.getHost(), "-p", directoryConfig.getPort(), "-D",
              directoryConfig.getAdminDN(), "-w", directoryConfig.getAdminPassword(), "-a", "-f", ldifURL.toURI().getPath() };

      // Not sure why... but it actually does make a difference...
      if (directoryName.equals(EMBEDDED_OPEN_DS_DIRECTORY_NAME)) {
         log.info("Populate success: " + (LDAPModify.mainModify(cmd, false, System.out, System.err) == 0));
      } else {
         log.info("Populate success: " + (LDAPModify.mainModify(cmd) == 0));
      }
   }

   public void loadConfig() throws Exception {
      directoryConfig = DSConfig.obtainConfig(directories, directoryName);

      identityConfig = directoryConfig.getConfigFile();

      env.put(Context.INITIAL_CONTEXT_FACTORY, directoryConfig.getContextFactory());
      // Use description to store URL to be able to prefix with "ldaps://"
      env.put(Context.PROVIDER_URL, directoryConfig.getDescription());
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, directoryConfig.getAdminDN());
      env.put(Context.SECURITY_CREDENTIALS, directoryConfig.getAdminPassword());
   }

   public LdapContext getLdapContext() throws Exception {
      Hashtable<String, String> env = new Hashtable<String, String>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.PROVIDER_URL, LDAP_PROVIDER_URL);
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, LDAP_PRINCIPAL);
      env.put(Context.SECURITY_CREDENTIALS, LDAP_CREDENTIALS);

      return new InitialLdapContext(env, null);
   }

   public void cleanUpDN(String dn) throws Exception {
      DirContext ldapCtx = getLdapContext();

      try {
         log.info("Removing: " + dn);

         removeContext(ldapCtx, dn);
      } catch (Exception e) {
         log.error("An unexpected error occurs ",e);
      } finally {
         ldapCtx.close();
      }
   }

   public void populateLDIFFile(String ldif) throws Exception {

      URL ldifURL = Thread.currentThread().getContextClassLoader().getResource(ldif);

      log.info("LDIF: " + ldifURL.toURI().getPath());
      String[] cmd = new String[] { "-h", directoryConfig.getHost(), "-p", directoryConfig.getPort(), "-D",
              directoryConfig.getAdminDN(), "-w", directoryConfig.getAdminPassword(), "-a", "-c", "-f", ldifURL.toURI().getPath() };

      // Not sure why... but it actually does make a difference...
      if (directoryName.equals(EMBEDDED_OPEN_DS_DIRECTORY_NAME)) {
         log.info("Populate success: " + (LDAPModify.mainModify(cmd, false, System.out, System.err) == 0));
      } else {
         log.info("Populate success: " + (LDAPModify.mainModify(cmd) == 0));
      }
   }

   public void removeContext(Context mainCtx, String name) throws Exception {
      Context deleteCtx = (Context) mainCtx.lookup(name);
      NamingEnumeration subDirs = mainCtx.listBindings(name);

      while (subDirs.hasMoreElements()) {
         Binding binding = (Binding) subDirs.nextElement();
         String subName = binding.getName();

         removeContext(deleteCtx, subName);
      }

      mainCtx.unbind(name);
   }
}
