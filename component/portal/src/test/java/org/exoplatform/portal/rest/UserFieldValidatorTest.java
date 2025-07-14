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
package org.exoplatform.portal.rest;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.resources.ResourceBundleService;

public class UserFieldValidatorTest {

  @BeforeClass
  public static void setup() {
    PortalContainer container = PortalContainer.getInstance();
    ResourceBundleService resourceBundleService = container.getComponentInstanceOfType(ResourceBundleService.class);
    if (resourceBundleService == null) {
      resourceBundleService = Mockito.mock(ResourceBundleService.class);
      container.registerComponentInstance(resourceBundleService);
    }
    ExoContainerContext.setCurrentContainer(container);
  }

  @Test
  public void testValidateFieldLength() {
    UserFieldValidator fieldValidator = new UserFieldValidator("field", false, false);
    assertNull(fieldValidator.validate(Locale.ENGLISH, "123"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, null));

    fieldValidator = new UserFieldValidator("field", false, false, 8, 10);
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "1234567"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "12345678"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "12345678901"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "1234567890"));
  }

  @Test
  public void testValidateUsername() {
    UserFieldValidator fieldValidator = new UserFieldValidator("field", true, false);
    assertNull(fieldValidator.validate(Locale.ENGLISH, "123"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "1aa"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "a1aa"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "aaA"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "aaa"));
  }

  @Test
  public void testValidatePersonalName() {
    UserFieldValidator fieldValidator = new UserFieldValidator("field", false, true);
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "123"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "1aa"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "&aa"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "a@a"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "aaa bb"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "aaa-bb"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "aaa'bb"));
  }

  @Test
  public void testValidateEmail() {
    UserFieldValidator fieldValidator = new UserFieldValidator("email", false, false);
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, ""));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "WRONG_FORMAT"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "WRONG_FORMAT@"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "WRONG_FORMAT@test"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "@test.com"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "GOOD_FORMAT@test.com"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "GOOD_FORMAT@test.test"));
  }

  @Test
  public void testValidateFieldRegex() {
    System.setProperty("gatein.validators.fieldregexp.regexp", "[1-9]*");

    try {
      UserFieldValidator fieldValidator = new UserFieldValidator("fieldregexp", false, false);
      assertNull(fieldValidator.validate(Locale.ENGLISH, "123"));
      assertNotNull(fieldValidator.validate(Locale.ENGLISH, "1aa"));

      String message = "FORMAT_MESSAGE";
      System.setProperty("gatein.validators.fieldregexp.format.message", message);
      fieldValidator = new UserFieldValidator("fieldregexp", false, false);
      assertEquals(message, fieldValidator.validate(Locale.ENGLISH, "1aa"));
    } finally {
      System.setProperty("gatein.validators.fieldregexp.regexp", "");
    }
  }

  @Test
  public void testValidatePassword() {
    System.setProperty("gatein.validators.passwordpolicy.regexp", "((?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{9,256})");
    System.setProperty("gatein.validators.passwordpolicy.length.max", "256");
    System.setProperty("gatein.validators.passwordpolicy.length.min", "9");

    UserFieldValidator passwordValidator = new UserFieldValidator("password", false, false);
    assertNotNull(passwordValidator.validate(Locale.ENGLISH, "passw"));
    assertNotNull(passwordValidator.validate(Locale.ENGLISH, "1aa"));
    assertNotNull(passwordValidator.validate(Locale.ENGLISH, "newPassword"));
    assertNotNull(passwordValidator.validate(Locale.ENGLISH, "Aa123456"));
    assertNull(passwordValidator.validate(Locale.ENGLISH, "newPassword1"));
  }


  @Test
  public void testValidateFieldRegexWithHyphen() {
    System.setProperty("gatein.validators.fieldregexphyphen.regexp", "[a-z1-9-]*");

    try {
      UserFieldValidator fieldValidator = new UserFieldValidator("fieldregexphyphen", true, false);
      assertNull(fieldValidator.validate(Locale.ENGLISH, "a123-456"));
    } finally {
      System.setProperty("gatein.validators.fieldregexphyphen.regexp", "");
    }
  }
}
