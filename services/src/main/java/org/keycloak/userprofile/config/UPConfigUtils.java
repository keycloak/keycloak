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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.keycloak.common.util.StreamUtil;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.util.JsonSerialization;
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
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    private static final Set<String> PSEUDOROLES = new HashSet<>();

    static {
        PSEUDOROLES.add(ROLE_ADMIN);
        PSEUDOROLES.add(ROLE_USER);
    }


    /**
     * Load configuration from JSON file.
     * <p>
     * Configuration is not validated, use {@link #validate(UPConfig)} to validate it and get list of errors.
     *
     * @param is JSON file to be loaded
     * @return object representation of the configuration
     * @throws IOException if JSON configuration can't be loaded (eg due to JSON format errors etc)
     */
    public static UPConfig readConfig(InputStream is) throws IOException {
        return JsonSerialization.readValue(is, UPConfig.class);
    }

    /**
     * Validate object representation of the configuration. Validations:
     * <ul>
     * <li>defaultProfile is defined and exists in profiles
     * <li>parent exists for type
     * <li>type exists for attribute
     * <li>validator (from Validator SPI) exists for validation and it's config is correct
     * </ul>
     *
     * @param session to be used for Validator SPI integration
     * @param config to validate
     * @return list of errors, empty if no error found
     */
    public static List<String> validate(KeycloakSession session, UPConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getAttributes() != null) {
            Set<String> attNamesCache = new HashSet<>();
            config.getAttributes().forEach((attribute) -> validate(session, attribute, errors, attNamesCache));
        } else {
            errors.add("UserProfile configuration without 'attributes' section is not allowed");
        }

        return errors;
    }

    /**
     * Validate attribute configuration
     *
     * @param session to be used for Validator SPI integration
     * @param attributeConfig config to be validated
     * @param errors to add error message in if something is invalid
     * @param attNamesCache cache of already existing attribute names so we can check uniqueness
     */
    private static void validate(KeycloakSession session, UPAttribute attributeConfig, List<String> errors, Set<String> attNamesCache) {
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
        return Pattern.matches("[a-zA-Z0-9\\._\\-]+", attributeName);
    }

    /**
     * Validate list of configured roles - must contain only supported {@link #PSEUDOROLES} for now.
     *
     * @param roles to validate
     * @param fieldName we are validating for use in error messages
     * @param errors to ass error message into
     * @param attributeName we are validating for use in erorr messages
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

    /**
     * Break string to substrings of given length.
     * 
     * @param src to break
     * @param partLength
     * @return list of string parts, never null (but can be empty if src is null)
     */
    public static List<String> getChunks(String src, int partLength) {
        List<String> ret = new ArrayList<>();
        if (src != null) {
            int pieces = (src.length() / partLength) + 1;
            for (int i = 0; i < pieces; i++) {
                if ((i + 1) < pieces)
                    ret.add(src.substring(i * partLength, (i + 1) * partLength));
                else if (i == 0 || (i * partLength) < src.length())
                    ret.add(src.substring(i * partLength));
            }
        }

        return ret;
    }

    /**
     * Check if context CAN BE part of the AuthenticationFlow.
     * 
     * @param context to check
     * @return true if context CAN BE part of the auth flow
     */
    public static boolean canBeAuthFlowContext(UserProfileContext context) {
        return context != UserProfileContext.USER_API && context != UserProfileContext.ACCOUNT
                && context != UserProfileContext.ACCOUNT_OLD;
    }

    /**
     * Check if roles configuration contains role given current context.
     * 
     * @param context to be checked
     * @param roles to be inspected
     * @return true if roles list contains role representing checked context
     */
    public static boolean isRoleForContext(UserProfileContext context, Set<String> roles) {
        if (roles == null)
            return false;
        if (context == UserProfileContext.USER_API)
            return roles.contains(ROLE_ADMIN);
        else
            return roles.contains(ROLE_USER);
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String readDefaultConfig() {
        try (InputStream is = UPConfigUtils.class.getResourceAsStream(SYSTEM_DEFAULT_CONFIG_RESOURCE)) {
            return StreamUtil.readString(is, Charset.defaultCharset());
        } catch (IOException cause) {
            throw new RuntimeException("Failed to load default user profile config file", cause);
        }
    }
}
