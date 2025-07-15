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
package org.exoplatform.webui.form.validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.CompoundApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;

/**
 * A user-configurable validator. Several aspects of this validator can be configured via properties in the
 * configuration.properties file found in the GateIn configuration directory (${gatein.conf.dir}). The validator supports
 * several configurations that can be activated when a validator instance is created by passing it the name of the configuration
 * to be activated. A configuration is created by adding an entry in configuration.properties using the {@link #KEY_PREFIX}
 * prefix followed by the name of the configuration, a period '.' and the name of the validation aspect to modify.
 * <br>
 * Currently supported validation aspects, where {configuration} is a configuration's name:
 * <ul>
 * <li>{@link #KEY_PREFIX}{configuration}.length.min: the minimal length of the validated field</li>
 * <li>{@link #KEY_PREFIX}{configuration}.length.max: the maximal length of the validated field</li>
 * <li>{@link #KEY_PREFIX}{configuration}.regexp: the regular expression to which the validated field must conform</li>
 * <li>{@link #KEY_PREFIX}{configuration}.format.message: a message to display providing details about the format of values the
 * regular expression allows in case the validated field doesn't conform to it</li>
 * </ul>
 *
 * Currently used configurations in the code are defined by the {@link #USERNAME} and {@link #GROUPMEMBERSHIP} names.
 *
 */
@Serialized
public class UserConfigurableValidator extends MultipleConditionsValidator {
    protected static Log log = ExoLogger.getLogger(UserConfigurableValidator.class);

    public static final String USERNAME = "username";
    public static final String GROUPMEMBERSHIP = "groupmembership";
    public static final String PAGE_NAME = "pagename";
    public static final String EMAIL = "email";
    public static final String DEFAULT_LOCALIZATION_KEY = "ExpressionValidator.msg.value-invalid";
    /**
     * Note that this regular expression should actually validate comma-separated usernames. This is not the case as some
     * constraints (consecutive symbols for examples) are not taken into account.
     */
    public static final String GROUP_MEMBERSHIP_VALIDATION_REGEX = "^(\\p{Lower}[\\p{Lower}\\d\\._]+)(\\s*,\\s*(\\p{Lower}[\\p{Lower}\\d\\._]+))*$";
    public static final String GROUP_MEMBERSHIP_LOCALIZATION_KEY = "UIGroupMembershipForm.msg.Invalid-char";

    public static final String EMAIL_VALIDATION_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static Map<String, ValidatorConfiguration> configurations;

    public static final String KEY_PREFIX = "gatein.validators.";

    private static Map<String, ValidatorConfiguration> getConfigurations() {
        if (configurations == null) {
            synchronized (UserConfigurableValidator.class) {
                if (configurations == null) {
                    configurations = new HashMap<String, ValidatorConfiguration>(3);

                    String gateinConfDir = System.getProperty("gatein.conf.dir");
                    File conf = new File(gateinConfDir, "configuration.properties");
                    Properties properties = null;
                    if (conf.exists()) {
                        try {
                            properties = new Properties();
                            properties.load(new FileInputStream(conf));
                        } catch (IOException e) {
                            log.info(e.getLocalizedMessage());
                            log.debug(e);
                        }
                    } else {
                        properties = System.getProperties();
                    }

                    if (properties != null) {
                        int length = KEY_PREFIX.length();
                        for (Object objectKey : properties.keySet()) {
                            String key = (String) objectKey;
                            if (key.startsWith(KEY_PREFIX)) {
                                // extract property key
                                String propertyKey = key.substring(length, key.indexOf('.', length));
                                if (!configurations.containsKey(propertyKey)) {
                                    configurations.put(propertyKey, new ValidatorConfiguration(propertyKey, properties));
                                }
                            }
                        }
                    }
                }
            }
        }
        return configurations;
    }

    private final String validatorName;
    private final String localizationKey;

    // needed by @Serialized
    public UserConfigurableValidator() {
        this(USERNAME, DEFAULT_LOCALIZATION_KEY);
    }

    public UserConfigurableValidator(String configurationName, String messageLocalizationKey) {
        this(configurationName, messageLocalizationKey, true);
    }

    public UserConfigurableValidator(String configurationName, String messageLocalizationKey,
            Boolean exceptionOnMissingMandatory) {
        this.exceptionOnMissingMandatory = exceptionOnMissingMandatory;
        this.trimValue = configurationName == null || !USERNAME.equals(configurationName);
        localizationKey = messageLocalizationKey != null ? messageLocalizationKey : DEFAULT_LOCALIZATION_KEY;
        this.validatorName = configurationName != null ? configurationName : USERNAME;
    }

    public UserConfigurableValidator(String configurationName) {
        this(configurationName, DEFAULT_LOCALIZATION_KEY);
    }

    @Override
    public void validate(UIFormInput uiInput) throws Exception {
        if (exceptionOnMissingMandatory) {
            super.validate(uiInput);
        } else {
            String label = getLabelFor(uiInput);

            CompoundApplicationMessage messages = new CompoundApplicationMessage();

            validate((String) uiInput.getValue(), label, messages, uiInput);

            if (!messages.isEmpty()) {
                throw new MessageException(messages);
            }

        }
    }

    @Override
    protected void validate(String value, String label, CompoundApplicationMessage messages, UIFormInput uiInput) {
        ValidatorConfiguration configuration = getConfigurations().get(validatorName);

        if (value == null) {
            value = "";
        }

        if (configuration == null) {
            // we don't have a user-configured validator for this validator name

            if (USERNAME.equals(validatorName)) {
                // if the validator name is USERNAME constant, we have a username to validate with the original, non-configured
                // behavior
                UsernameValidator.validate(value, label, messages, UsernameValidator.DEFAULT_MIN_LENGTH,
                        UsernameValidator.DEFAULT_MAX_LENGTH);
            } else if (EMAIL.equals(validatorName)) {
                // if the validator name is the EMAIL constant, we use the default e-mail validator
                EmailAddressValidator.validate(value, label, messages);
            } else if (GROUPMEMBERSHIP.equals(validatorName)) {
                // else, we assume that we need to validate a group membership, replicating original behavior
                if (!Pattern.matches(GROUP_MEMBERSHIP_VALIDATION_REGEX, value)) {
                    messages.addMessage(localizationKey, new Object[] { label });
                }
            } else if (PAGE_NAME.equals(validatorName)) {
                ConfigurableIdentifierValidator.validate(value, label, messages, uiInput, ConfigurableIdentifierValidator.DEFAULT_MIN_LENGTH, ConfigurableIdentifierValidator.DEFAULT_MAX_LENGTH);
            }
        } else {
            // otherwise, use the user-provided configuration
            if (value.length() < configuration.minLength || value.length() > configuration.maxLength) {
                messages.addMessage("StringLengthValidator.msg.length-invalid",
                        new Object[] { label, configuration.minLength.toString(), configuration.maxLength.toString() });
            }

            if (!Pattern.matches(configuration.pattern, value)) {
                messages.addMessage(localizationKey, new Object[] { label, configuration.formatMessage });
            }
        }
    }

    /**
     * Provides a way to query what are the currently known policies from the configuration files. For instance,
     * if an entry exists as gatein.validators.mycompanypasspolicy.regexp , then there's a configuration called
     * "mycompanypasspolicy", and this will be returned by this method.
     *
     * Note: the built-in configurations are <b>not</b> returned by this method, only the custom-provided ones.
     *
     * @return a set containing all known configuration key names.
     */
    public static final Set<String> getConfigurationNames() {
        return Collections.unmodifiableSet(getConfigurations().keySet());
    }

    private static class ValidatorConfiguration {
        private Integer minLength;
        private Integer maxLength;
        private String pattern;
        private String formatMessage;

        private ValidatorConfiguration(String propertyKey, Properties properties) {
            // used to assign backward compatible default values
            String prefixedKey = KEY_PREFIX + propertyKey;
            String minProperty = properties.getProperty(prefixedKey + ".length.min");
            String maxProperty = properties.getProperty(prefixedKey + ".length.max");
            if (USERNAME.equals(propertyKey)) {
                minLength = minProperty != null ? Integer.valueOf(minProperty) : UsernameValidator.DEFAULT_MIN_LENGTH;
                maxLength = maxProperty != null ? Integer.valueOf(maxProperty) : UsernameValidator.DEFAULT_MAX_LENGTH;
                pattern = properties.getProperty(prefixedKey + ".regexp", Utils.USER_NAME_VALIDATOR_REGEX);
            } else if (PAGE_NAME.equals(propertyKey)) {
                minLength = minProperty != null ? Integer.valueOf(minProperty) : ConfigurableIdentifierValidator.DEFAULT_MIN_LENGTH;
                maxLength = maxProperty != null ? Integer.valueOf(maxProperty) : ConfigurableIdentifierValidator.DEFAULT_MAX_LENGTH;
                pattern = properties.getProperty(prefixedKey + ".regexp", ConfigurableIdentifierValidator.IDENTIFER_VALIDATOR_REGEX);
            } else if (EMAIL.equals(propertyKey)) {
                minLength = minProperty != null ? Integer.valueOf(minProperty) : ConfigurableIdentifierValidator.DEFAULT_MIN_LENGTH;
                maxLength = maxProperty != null ? Integer.valueOf(maxProperty) : ConfigurableIdentifierValidator.DEFAULT_MAX_LENGTH;
                pattern = properties.getProperty(prefixedKey + ".regexp", EMAIL_VALIDATION_REGEX);
            } else {
                minLength = minProperty != null ? Integer.valueOf(minProperty) : 0;
                maxLength = maxProperty != null ? Integer.valueOf(maxProperty) : Integer.MAX_VALUE;
                pattern = properties.getProperty(prefixedKey + ".regexp", Utils.USER_NAME_VALIDATOR_REGEX);
            }
            formatMessage = properties.getProperty(prefixedKey + ".format.message", pattern);
        }

    }
}
