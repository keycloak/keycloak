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
package org.keycloak.userprofile.validator;

import static org.keycloak.common.util.ObjectUtil.isBlank;
import static org.keycloak.userprofile.DeclarativeUserProfileProviderFactory.DEFAULT_ADMIN_READ_ONLY_TIMESTAMP_ATTRIBUTES;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validator to check that User Profile attribute value is not changed if attribute is read-only. Expects List of
 * Strings as input.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class ReadOnlyAttributeUnchangedValidator implements SimpleValidator {

    private static final Logger logger = Logger.getLogger(ReadOnlyAttributeUnchangedValidator.class);

    public static final String ID = "up-readonly-attribute-unchanged";

    public static final String CFG_PATTERN = "pattern";

    public static String UPDATE_READ_ONLY_ATTRIBUTES_REJECTED_MSG = "updateReadOnlyAttributesRejectedMessage";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        AttributeContext attributeContext = UserProfileAttributeValidationContext.from(context).getAttributeContext();
        Map.Entry<String, List<String>> attribute = attributeContext.getAttribute();
        String key = attribute.getKey();

        Pattern pattern = (Pattern) config.get(CFG_PATTERN);
        if (!pattern.matcher(key).find()) {
            return context;
        }

        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        UserModel user = attributeContext.getUser();
        String existingValue = user == null ? null : user.getFirstAttribute(key);
        
        if (values == null || values.isEmpty()) {
            // Allow omitting timestamp attributes as they're auto-managed by external systems
            if (isLDAPTimestampAttribute(key)) {
                return context;
            }
            // For other read-only attributes, treat empty values as explicit removal attempts
        }

        String value = null;
        if (!values.isEmpty()) {
            value = values.get(0);
        }

        if (!isUnchanged(existingValue, value)) {
            logger.debugf("Attempt to edit denied for attribute '%s' with pattern '%s' of user '%s'", key, pattern, user == null ? "new user" : user.getFirstAttribute(UserModel.USERNAME));
            context.addError(new ValidationError(ID, key, UPDATE_READ_ONLY_ATTRIBUTES_REJECTED_MSG));
        }

        return context;
    }

    private boolean isUnchanged(String existingValue, String value) {
        if (existingValue == null && isBlank(value)) {
            // if attribute not set to the user and value is blank/null, then pass validation
            return true;
        }

        return ObjectUtil.isEqualOrBothNull(existingValue, value);
    }

    private boolean isLDAPTimestampAttribute(String attributeName) {
        // Only allow omitting auto-managed timestamp attributes, not permanent identifiers
        // LDAP timestamps are auto-updated by LDAP server, so omitting them is safe
        return Arrays.asList(DEFAULT_ADMIN_READ_ONLY_TIMESTAMP_ATTRIBUTES).contains(attributeName);
    }

}
