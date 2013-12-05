package org.keycloak.services.validation;

import org.keycloak.models.PasswordPolicy;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

public class Validation {

    public static String validateRegistrationForm(MultivaluedMap<String, String> formData, List<String> requiredCredentialTypes) {
        if (isEmpty(formData.getFirst("firstName"))) {
            return Messages.MISSING_FIRST_NAME;
        }

        if (isEmpty(formData.getFirst("lastName"))) {
            return Messages.MISSING_LAST_NAME;
        }

        if (isEmpty(formData.getFirst("email"))) {
            return Messages.MISSING_EMAIL;
        }

        if (isEmpty(formData.getFirst("username"))) {
            return Messages.MISSING_USERNAME;
        }

        if (requiredCredentialTypes.contains(CredentialRepresentation.PASSWORD)) {
            if (isEmpty(formData.getFirst(CredentialRepresentation.PASSWORD))) {
                return Messages.MISSING_PASSWORD;
            }

            if (!formData.getFirst("password").equals(formData.getFirst("password-confirm"))) {
                return Messages.INVALID_PASSWORD_CONFIRM;
            }
        }

        return null;
    }

    public static String validatePassword(MultivaluedMap<String, String> formData, PasswordPolicy policy) {
        return policy.validate(formData.getFirst("password"));
    }

    public static String validateUpdateProfileForm(MultivaluedMap<String, String> formData) {
        if (isEmpty(formData.getFirst("firstName"))) {
            return Messages.MISSING_FIRST_NAME;
        }

        if (isEmpty(formData.getFirst("lastName"))) {
            return Messages.MISSING_LAST_NAME;
        }

        if (isEmpty(formData.getFirst("email"))) {
            return Messages.MISSING_EMAIL;
        }

        return null;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

}
