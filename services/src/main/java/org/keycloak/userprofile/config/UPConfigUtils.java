/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.userprofile.config;

import static org.keycloak.common.util.ObjectUtil.isBlank;
import static org.keycloak.userprofile.UserProfileUtil.isRootAttribute;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.util.StreamUtil;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.userprofile.UserProfileConstants;
import org.keycloak.util.JsonSerialization;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.Validators;

/**
 * Utility methods to work with User Profile Configurations
 *
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPConfigUtils {

    private static final String SYSTEM_DEFAULT_CONFIG_RESOURCE = "keycloak-default-user-profile.json";
    public static final String ROLE_USER = UserProfileConstants.ROLE_USER;
    public static final String ROLE_ADMIN = UserProfileConstants.ROLE_ADMIN;

    private static final Set<String> PSEUDOROLES = new HashSet<>();
    public static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9\\._\\-]+");

    static {
        PSEUDOROLES.add(ROLE_ADMIN);
        PSEUDOROLES.add(ROLE_USER);
    }


    /**
     * Load configuration from JSON file.
     * <p>
     * Configuration is not validated, use {@link #validate(KeycloakSession, UPConfig)} to validate it and get list of errors.
     *
     * @param is JSON file to be loaded
     * @return object representation of the configuration
     * @throws IOException if JSON configuration can't be loaded (eg due to JSON format errors etc)
     */
    public static UPConfig readConfig(InputStream is) throws IOException {
        return JsonSerialization.readValue(is, UPConfig.class);
    }

    /**
     * Parse configuration of user-profile from String
     *
     * @param rawConfig Configuration in String format
     * @return object representation of the configuration
     * @throws IOException if JSON configuration can't be loaded (eg due to JSON format errors etc)
     */
    public static UPConfig parseConfig(String rawConfig) throws IOException {
        return readConfig(new ByteArrayInputStream(rawConfig.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Validate object representation of the configuration. Validations:
     * <ul>
     * <li>defaultProfile is defined and exists in profiles</li>
     * <li>parent exists for type</li>
     * <li>type exists for attribute</li>
     * <li>validator (from Validator SPI) exists for validation and it's config is correct</li>
     * <li>if an attribute group is configured it is verified that this group exists</li>
     * <li>all groups have a name != null</li>
     * </ul>
     *
     * @param session to be used for Validator SPI integration
     * @param config to validate
     * @return list of errors, empty if no error found
     */
    public static List<String> validate(KeycloakSession session, UPConfig config) {
        List<String> errors = validateAttributes(session, config);
        errors.addAll(validateAttributeGroups(config));
        return errors;
    }

    private static List<String> validateAttributeGroups(UPConfig config) {
        long groupsWithoutName = config.getGroups().stream().filter(g -> g.getName() == null).collect(Collectors.counting());

        if (groupsWithoutName > 0) {
            String errorMessage = "Name is mandatory for groups, found " + groupsWithoutName + " group(s) without name.";
            return Collections.singletonList(errorMessage);
        }
        return Collections.emptyList();
    }

    private static List<String> validateAttributes(KeycloakSession session, UPConfig config) {
        List<String> errors = new ArrayList<>();
        Set<String> groups = config.getGroups().stream()
                .map(g -> g.getName())
                .collect(Collectors.toSet());

        if (config.getAttributes() != null) {
            Set<String> attNamesCache = new HashSet<>();
            config.getAttributes().forEach((attribute) -> validateAttribute(session, attribute, groups, errors, attNamesCache));
            errors.addAll(validateRootAttributes(config));
        }

        return errors;
    }

    private static List<String> validateRootAttributes(UPConfig config) {
        List<UPAttribute> attributes = config.getAttributes();

        if (attributes == null) {
            return Collections.emptyList();
        }

        List<String> errors = new ArrayList<>();
        List<String> attributeNames = attributes.stream().map(UPAttribute::getName).toList();

        for (String name : Arrays.asList(UserModel.USERNAME, UserModel.EMAIL)) {
            if (!attributeNames.contains(name)) {
                errors.add("The attribute '" + name + "' can not be removed");
            }
        }

        return errors;
    }

    /**
     * Validate attribute configuration
     *
     * @param session to be used for Validator SPI integration
     * @param attributeConfig config to be validated
     * @param groups set of groups that are configured
     * @param errors to add error message in if something is invalid
     * @param attNamesCache cache of already existing attribute names so we can check uniqueness
     */
    private static void validateAttribute(KeycloakSession session, UPAttribute attributeConfig, Set<String> groups, List<String> errors, Set<String> attNamesCache) {
        String attributeName = attributeConfig.getName();
        if (isBlank(attributeName)) {
            errors.add("Attribute configuration without 'name' is not allowed");
        } else {
            if (attNamesCache.contains(attributeName)) {
                errors.add("Attribute configuration already exists with 'name':'" + attributeName + "'");
            } else {
                attNamesCache.add(attributeName);
                if(!isValidAttributeName(attributeName)) {
                    errors.add("Invalid attribute name (only letters, numbers and '.' '_' '-' special characters allowed): " + attributeName + "'");
                }
            }
        }
        if (attributeConfig.getValidations() != null) {
            attributeConfig.getValidations().forEach((validator, validatorConfig) -> validateValidationConfig(session, validator, validatorConfig, attributeName, errors));
            validateDefaultValue(session, attributeConfig, errors);
        }
        if (attributeConfig.getPermissions() != null) {
            if (attributeConfig.getPermissions().getView() != null) {
                validateRoles(attributeConfig.getPermissions().getView(), "permissions.view", errors, attributeName);
            }
            if (attributeConfig.getPermissions().getEdit() != null) {
                validateRoles(attributeConfig.getPermissions().getEdit(), "permissions.edit", errors, attributeName);
            }
        }
        if (attributeConfig.getRequired() != null) {
            validateRoles(attributeConfig.getRequired().getRoles(), "required.roles", errors, attributeName);
            validateScopes(attributeConfig.getRequired().getScopes(), "required.scopes", attributeName, errors, session);
        }
        if (attributeConfig.getSelector() != null) {
            validateScopes(attributeConfig.getSelector().getScopes(), "selector.scopes", attributeName, errors, session);
        }

        if (attributeConfig.getGroup() != null) {
            if (!groups.contains(attributeConfig.getGroup())) {
                errors.add("Attribute '" + attributeName + "' references unknown group '" + attributeConfig.getGroup() + "'");
            }
        }

        if (attributeConfig.getAnnotations()!=null) {
            validateAnnotations(attributeConfig.getAnnotations(), errors, attributeName);
        }
    }

    private static void validateDefaultValue(KeycloakSession session, UPAttribute attributeConfig, List<String> errors) {
        String defaultValue = attributeConfig.getDefaultValue();

        if (defaultValue == null) {
            return;
        }

        String attributeName = attributeConfig.getName();

        if (isRootAttribute(attributeName)) {
            errors.add("Default value not supported for attribute '" + attributeName + "'");
        } else {
            attributeConfig.getValidations().forEach((validator, validatorConfig) -> {
                ValidationContext context = Validators.validator(session, validator).validate(defaultValue, attributeName, ValidatorConfig.configFromMap(validatorConfig));
                if (!context.isValid()) {
                    errors.add("Default value for attribute '" + attributeName + "' is invalid");
                }
            });
        }
    }

    private static void validateAnnotations(Map<String, Object> annotations, List<String> errors, String attributeName) {
        if (annotations.containsKey("inputOptions") && !(annotations.get("inputOptions") instanceof List)) {
            errors.add(new StringBuilder("Annotation 'inputOptions' configured for attribute '").append(attributeName).append("' must be an array of values!'").toString());
        }
        if (annotations.containsKey("inputOptionLabels") && !(annotations.get("inputOptionLabels") instanceof Map)) {
            errors.add(new StringBuilder("Annotation 'inputOptionLabels' configured for attribute '").append(attributeName).append("' must be an object!'").toString());
        }
    }

    private static void validateScopes(Set<String> scopes, String propertyName, String attributeName, List<String> errors, KeycloakSession session) {
        if (scopes == null) {
            return;
        }

        for (String scope : scopes) {
            RealmModel realm = session.getContext().getRealm();
            Stream<ClientScopeModel> realmScopes = realm.getClientScopesStream();

            if (!realmScopes.anyMatch(cs -> cs.getName().equals(scope))) {
                errors.add(new StringBuilder("'").append(propertyName).append("' configuration for attribute '").append(attributeName).append("' contains unsupported scope '").append(scope).append("'").toString());
            }
        }
    }

    /**
     * @param attributeName to validate
     * @return
     */
    public static boolean isValidAttributeName(String attributeName) {
        return ATTRIBUTE_NAME_PATTERN.matcher(attributeName).matches();
    }

    /**
     * Validate list of configured roles - must contain only supported {@link #PSEUDOROLES} for now.
     *
     * @param roles to validate
     * @param fieldName we are validating for use in error messages
     * @param errors to pass error message into
     * @param attributeName we are validating for use in error messages
     */
    private static void validateRoles(Set<String> roles, String fieldName, List<String> errors, String attributeName) {
        if (roles != null) {
            for (String role : roles) {
                if (!PSEUDOROLES.contains(role)) {
                    errors.add("'" + fieldName + "' configuration for attribute '" + attributeName + "' contains unsupported role '" + role + "'");
                }
            }
        }
    }

    /**
     * Validate that validation configuration is correct.
     *
     * @param session to be used for Validator SPI integration
     * @param validatorConfig config to be checked
     * @param errors to add error message in if something is invalid
     */
    private static void validateValidationConfig(KeycloakSession session, String validator, Map<String, Object> validatorConfig, String attributeName, List<String> errors) {

        if (isBlank(validator)) {
            errors.add("Validation without validator id is defined for attribute '" + attributeName + "'");
        } else {
        	if(session!=null) {
            	if(Validators.validator(session, validator) == null) {
            		errors.add("Validator '" + validator + "' defined for attribute '" + attributeName + "' doesn't exist");
            	} else {
            		ValidationResult result = Validators.validateConfig(session, validator, ValidatorConfig.configFromMap(validatorConfig));
            		if(!result.isValid()) {
            			final StringBuilder sb = new StringBuilder();
            			result.forEachError(err -> sb.append(err.toString()+", "));
            			errors.add("Validator '" + validator + "' defined for attribute '" + attributeName + "' has incorrect configuration: " + sb.toString());
            		}
            	}
        	}
        }
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String readSystemDefaultConfig() {
        try (InputStream is = getSystemDefaultConfig()) {
            return StreamUtil.readString(is, Charset.defaultCharset());
        } catch (IOException cause) {
            throw new RuntimeException("Failed to load default user profile config file", cause);
        }
    }

    public static UPConfig parseSystemDefaultConfig() {
        return parseConfig(getSystemDefaultConfig());
    }

    public static UPConfig parseConfig(Path configPath) {
        if (configPath == null) {
            throw new IllegalArgumentException("Null configPath");
        }

        try (InputStream is = new FileInputStream(configPath.toFile())) {
            return parseConfig(is);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to reaad default user profile configuration: " + configPath, ioe);
        }
    }

    private static UPConfig parseConfig(InputStream is) {
        try {
            return JsonSerialization.readValue(is, UPConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse default user profile configuration stream", e);
        }
    }

    private static InputStream getSystemDefaultConfig() {
        return UPConfigUtils.class.getResourceAsStream(SYSTEM_DEFAULT_CONFIG_RESOURCE);
    }
}
