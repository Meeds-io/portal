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
package org.exoplatform.services.organization.auth;

import junit.framework.TestCase;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.organization.DisabledUserException;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.security.*;

import java.net.URL;
import java.util.List;

import javax.security.auth.login.LoginException;

/**
 * Created y the eXo platform team User: Benjamin Mestrallet Date: 28 avr. 2004
 */
public class TestOrganizationAuthenticator extends TestCase
{

   protected ConversationRegistry registry;

   protected Authenticator authenticator;

   protected OrganizationService orgService;

   public TestOrganizationAuthenticator(String name)
   {
      super(name);
   }

   protected void setUp() throws Exception
   {

      if (registry == null)
      {
         URL containerConfURL =
            TestOrganizationAuthenticator.class.getResource("/conf/standalone/test-authenticator-configuration.xml");
         assertNotNull(containerConfURL);
         String containerConf = containerConfURL.toString();
         URL loginConfURL = TestOrganizationAuthenticator.class.getResource("/login.conf");
         assertNotNull(loginConfURL);
         String loginConf = loginConfURL.toString();
         StandaloneContainer.addConfigurationURL(containerConf);
         if (System.getProperty("java.security.auth.login.config") == null)
            System.setProperty("java.security.auth.login.config", loginConf);

         StandaloneContainer container = StandaloneContainer.getInstance();

         authenticator = (Authenticator)container.getComponentInstanceOfType(OrganizationAuthenticatorImpl.class);
         assertNotNull(authenticator);

         registry = (ConversationRegistry)container.getComponentInstanceOfType(ConversationRegistry.class);
         assertNotNull(registry);

         orgService = (OrganizationService)container.getComponentInstanceOfType(OrganizationService.class);
         assertNotNull(orgService);

      }

   }

   public void testAuthenticator() throws Exception
   {
     assertNotNull(authenticator);
     assertTrue(authenticator instanceof OrganizationAuthenticatorImpl);
     Credential[] cred = new Credential[]{new UsernameCredential("admin"), new PasswordCredential("admin")};
     String userId = authenticator.validateUser(cred);
     assertEquals("admin", userId);
     Identity identity = authenticator.createIdentity(userId);
     assertTrue(identity.isMemberOf("/platform/administrators", "manager"));
     assertTrue(identity.getGroups().size() > 0);
  }

   public void testAuthenticateWithEmptyPassword() throws Exception
   {
     assertNotNull(authenticator);
     assertTrue(authenticator instanceof OrganizationAuthenticatorImpl);
     Credential[] cred = new Credential[]{new UsernameCredential(IdentityConstants.ANONIM), new PasswordCredential("")};
     String userId = authenticator.validateUser(cred);
     assertEquals(IdentityConstants.ANONIM, userId);
   }

   public void testAuthenticatorPlugin() throws Exception
   {
     OrganizationAuthenticatorImpl organizationAuthenticatorImpl = (OrganizationAuthenticatorImpl) authenticator;
     List<AuthenticatorPlugin> originalPlugins = organizationAuthenticatorImpl.getPlugins();
     try {
       String username = "0xABCD";
       String userId = "testuser";
       String password = "0xDCBA";

       Credential[] cred = new Credential[] { new UsernameCredential(username), new PasswordCredential(password) };

       try {
         organizationAuthenticatorImpl.validateUser(cred);
         fail("Must fail with invalid credentials");
       } catch (LoginException e) {
         // Expected
       }

       // Add plugins that invalidate credentials after and before valid one
       organizationAuthenticatorImpl.addAuthenticatorPlugin(new AuthenticatorPlugin() {
         @Override
         public String validateUser(Credential[] credentials) {
           throw new IllegalStateException("Fake Login Error");
         }
       });

       organizationAuthenticatorImpl.addAuthenticatorPlugin(new AuthenticatorPlugin() {
         @Override
         public String validateUser(Credential[] credentials) {
           boolean valid = credentials != null && credentials.length == 2
               && StringUtils.equals(username, ((UsernameCredential) credentials[0]).getUsername())
               && StringUtils.equals(password, ((PasswordCredential) credentials[1]).getPassword());
           return valid ? userId : null;
         }
       });

       organizationAuthenticatorImpl.addAuthenticatorPlugin(new AuthenticatorPlugin() {
         @Override
         public String validateUser(Credential[] credentials) {
           return null;
         }
       });

       String authenticatedUser = organizationAuthenticatorImpl.validateUser(cred);
       assertEquals(userId, authenticatedUser);

       Identity identity = organizationAuthenticatorImpl.createIdentity(userId);
       assertTrue(identity.isMemberOf("/platform/users", "member"));
       assertFalse(identity.getGroups().isEmpty());

       try {
         organizationAuthenticatorImpl.validateUser(new Credential[] { new UsernameCredential("badUsername"), new PasswordCredential(password) });
         fail("Must fail with invalid credentials");
       } catch (LoginException e) {
         // Expected
       }

       try {
         organizationAuthenticatorImpl.validateUser(new Credential[] { new UsernameCredential(username), new PasswordCredential("badPassword") });
         fail("Must fail with invalid credentials");
       } catch (LoginException e) {
         // Expected
       }
     } finally {
       organizationAuthenticatorImpl.setPlugins(originalPlugins);
     }
  }

   public void testGetLastExceptionOnValidateUser() throws Exception
   {
      assertNotNull(orgService);
      UserHandler uh = orgService.getUserHandler();
      User user = uh.createUserInstance("testGetLastExceptionOnValidateUser");
      user.setPassword("foo");
      assertNotNull(authenticator);
      assertTrue(authenticator instanceof OrganizationAuthenticatorImpl);
      Credential[] cred = new Credential[]{new UsernameCredential("testGetLastExceptionOnValidateUser"), new PasswordCredential("foo")};
      String userId = authenticator.validateUser(cred);
      assertEquals("testGetLastExceptionOnValidateUser", userId);
      assertNull(authenticator.getLastExceptionOnValidateUser());
      assertNull(authenticator.getLastExceptionOnValidateUser());
      uh.setEnabled("testGetLastExceptionOnValidateUser", false, false);
      try
      {
         authenticator.validateUser(cred);
         fail("a LoginException was expected");
      }
      catch (LoginException e)
      {
         // expected
      }
      assertTrue(authenticator.getLastExceptionOnValidateUser() instanceof DisabledUserException);
      assertNull(authenticator.getLastExceptionOnValidateUser());
      uh.setEnabled("testGetLastExceptionOnValidateUser", true, false);
      userId = authenticator.validateUser(cred);
      assertEquals("testGetLastExceptionOnValidateUser", userId);
      assertNull(authenticator.getLastExceptionOnValidateUser());
      assertNull(authenticator.getLastExceptionOnValidateUser());
   }
}
