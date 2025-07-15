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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.resources.ResourceBundleService;

public class UserFieldValidator {

  private static final int             DEFAULT_MIN_LENGTH = 3;

  private static final int             DEFAULT_MAX_LENGTH = 255;

  private static final int             MAX_FIELD_LENGTH   = 255;

  public static final String           DEFAULT_MAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

  private static final String          KEY_PREFIX         = "gatein.validators.";

  private static final String          ALLOWED_SYMBOLS    = "'_', '.'";

  private static ResourceBundleService resourceBundleService;

  private int                          minLength          = DEFAULT_MIN_LENGTH;

  private int                          maxLength          = DEFAULT_MAX_LENGTH;

  private boolean                      usernameValidation = false;

  private boolean                      personalNameValidation = false;

  private boolean                      emailValidation    = false;

  private boolean                      passwordValidation    = false;

  private String                       field              = null;

  private String                       pattern            = null;

  private String                       formatMessage      = null;

  public UserFieldValidator(String field, boolean usernameValidation, boolean personalNameValidation) {
    this(field, usernameValidation, personalNameValidation, DEFAULT_MIN_LENGTH, DEFAULT_MAX_LENGTH);
  }

  public UserFieldValidator(String field, boolean usernameValidation, boolean personalNameValidation, int defaultMin, int defaultMax) {
    this.field = field;
    this.usernameValidation = usernameValidation;
    this.personalNameValidation = personalNameValidation;
    this.emailValidation = StringUtils.contains(this.field, "email");
    this.passwordValidation = StringUtils.contains(this.field, "password");

    String prefixedKey = KEY_PREFIX + field;

    pattern = PropertyManager.getProperty(prefixedKey + ".regexp");
    formatMessage = PropertyManager.getProperty(prefixedKey + ".format.message");

    if (this.emailValidation && StringUtils.isBlank(pattern)) {
      pattern = DEFAULT_MAIL_REGEX;
    }

    String minProperty = PropertyManager.getProperty(prefixedKey + ".length.min");
    String maxProperty = PropertyManager.getProperty(prefixedKey + ".length.max");
    if (StringUtils.isNotBlank(minProperty)) {
      minLength = Integer.valueOf(minProperty);
    } else {
      minLength = defaultMin;
    }
    if (StringUtils.isNotBlank(maxProperty)) {
      maxLength = Integer.valueOf(maxProperty);
    } else {
      maxLength = defaultMax;
    }
    if (maxLength > MAX_FIELD_LENGTH) {
      maxLength = MAX_FIELD_LENGTH;
    }
  }

  public String validate(Locale locale, String value) {
    if (StringUtils.isBlank(value)) {
      String label = getFieldLabel(locale);
      return getLabel(locale, "EmptyFieldValidator.msg.empty-input", label);
    }
    char[] buff = value.toCharArray();
    if ((buff.length < minLength || buff.length > maxLength) && !passwordValidation) {
      String label = getFieldLabel(locale);
      return getLabel(locale, "StringLengthValidator.msg.length-invalid", label, minLength, maxLength);
    }

    if (StringUtils.isNotBlank(pattern) && !Pattern.matches(pattern, value)) {
      if (StringUtils.isNotBlank(formatMessage)) {
        return formatMessage;
      } else if (emailValidation) {
        return getLabel(locale, "EmailAddressValidator.msg.Invalid-input");
      } else {
        String label = getFieldLabel(locale);
        return getLabel(locale, "ExpressionValidator.msg.value-invalid", label, pattern);
      }
    } else if (personalNameValidation) {
      for (int i = 0; i < buff.length - 1; i++) {
        char c = buff[i];
        if (Character.isLetter(c) || Character.isSpaceChar(c) || c == '\'' || c == '-') {
          continue;
        } else {
          String label = getFieldLabel(locale);
          return getLabel(locale, "PersonalNameValidator.msg.Invalid-char", label);
        }
      }
    } else if (usernameValidation) {
      if (!isLowerCaseLetterOrDigit(buff[0])) {
        String label = getFieldLabel(locale);
        return getLabel(locale, "FirstCharacterUsernameValidator.msg", label);
      }

      char c = buff[buff.length - 1];
      if (!isLowerCaseLetterOrDigit(c)) {
        String label = getFieldLabel(locale);
        return getLabel(locale, "LastCharacterUsernameValidator.msg", label, String.valueOf(c));
      }

      for (int i = 1; i < buff.length - 1; i++) {
        c = buff[i];
        if (isLowerCaseLetterOrDigit(c)) {
          continue;
        }
        if (isSymbol(c)) {
          char next = buff[i + 1];
          if (isSymbol(next)) {
            String label = getFieldLabel(locale);
            return getLabel(locale, "ConsecutiveSymbolValidator.msg", label, ALLOWED_SYMBOLS);
          } else if (!Character.isLetterOrDigit(next)) {
            String label = getFieldLabel(locale);
            return getLabel(locale, "UsernameValidator.msg.Invalid-char", label);
          }
        } else {
          String label = getFieldLabel(locale);
          return getLabel(locale, "UsernameValidator.msg.Invalid-char", label);
        }
      }
    } else if (passwordValidation) {
      String regexProperty = PropertyManager.getProperty("gatein.validators.passwordpolicy.regexp");
      String maxLengthProperty = PropertyManager.getProperty("gatein.validators.passwordpolicy.length.max");
      String minLengthProperty = PropertyManager.getProperty("gatein.validators.passwordpolicy.length.min");
      Pattern customPasswordPattern = null;
      int customPasswordMaxlength = -1;
      int customPasswordMinlength = -1;
      if (StringUtils.isNotBlank(regexProperty)) {
        customPasswordPattern = Pattern.compile(regexProperty);
      }
      if (StringUtils.isNotBlank(maxLengthProperty)) {
        customPasswordMaxlength = Integer.parseInt(maxLengthProperty);
      } else {
        customPasswordMaxlength = maxLength;
      }
      if (StringUtils.isNotBlank(minLengthProperty)) {
        customPasswordMinlength = Integer.parseInt(minLengthProperty);
      } else {
        customPasswordMinlength = minLength;
      }
      if (customPasswordPattern != null && !customPasswordPattern.matcher(value).matches() ||
              customPasswordMaxlength != -1 && customPasswordMaxlength < value.length() ||
              customPasswordMinlength != -1 && customPasswordMinlength > value.length()) {

        String passwordPolicyProperty = PropertyManager.getProperty("gatein.validators.passwordpolicy.format.message");
        return passwordPolicyProperty != null ? passwordPolicyProperty : getLabel(locale, "onboarding.login.passwordCondition");

      }
    }
    return null;
  }

  public String getField() {
    return field;
  }

  public boolean isEmailValidation() {
    return emailValidation;
  }

  public boolean isPersonalNameValidation() {
    return personalNameValidation;
  }

  public boolean isUsernameValidation() {
    return usernameValidation;
  }

  private String getFieldLabel(Locale locale) {
    String label = getLabel(locale, "UIRegisterForm.label." + field);
    return label == null ? null : label.replace(" :", "").replace(":", "");
  }
  private static boolean isLowerCaseLetterOrDigit(char character) {
    return Character.isDigit(character) || (character >= 'a' && character <= 'z');
  }

  private static boolean isSymbol(char c) {
    return c == '_' || c == '.' || c == '-' || c == '@' ;
  }

  private static String getLabel(Locale locale, String key, Object... values) {
    ResourceBundle resourceBundle = getResourceBundle(locale);
    if (resourceBundle == null || !resourceBundle.containsKey(key)) {
      resourceBundle = getResourceBundle(Locale.ENGLISH);
    }
    if (resourceBundle != null && resourceBundle.containsKey(key)) {
      String label = resourceBundle.getString(key);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          String messageArg = String.valueOf(values[i]);
          if (messageArg != null) {
            label = label.replace("{" + i + "}", messageArg);
          }
        }
      }
      return label;
    }
    return key;
  }

  private static ResourceBundle getResourceBundle(Locale locale) {
    return getResourceBundleService().getResourceBundle(getResourceBundleService().getSharedResourceBundleNames(), locale);
  }

  private static ResourceBundleService getResourceBundleService() {
    if (resourceBundleService == null) {
      resourceBundleService = ExoContainerContext.getService(ResourceBundleService.class);
    }
    return resourceBundleService;
  }
}
