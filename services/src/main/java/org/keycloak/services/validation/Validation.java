package org.keycloak.services.validation;

import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.regex.Pattern;

public class Validation {

    // Actually allow same emails like angular. See ValidationTest.testEmailValidation()
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*");

    public static String validateRegistrationForm(RealmModel realm, MultivaluedMap<String, String> formData, List<String> requiredCredentialTypes) {
        if (isEmpty(formData.getFirst("firstName"))) {
            return Messages.MISSING_FIRST_NAME;
        }

        if (isEmpty(formData.getFirst("lastName"))) {
            return Messages.MISSING_LAST_NAME;
        }

        if (isEmpty(formData.getFirst("email"))) {
            return Messages.MISSING_EMAIL;
        }

        if (!isEmailValid(formData.getFirst("email"))) {
            return Messages.INVALID_EMAIL;
        }

        if (!realm.isRegistrationEmailAsUsername() && isEmpty(formData.getFirst("username"))) {
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

    public static PasswordPolicy.Error validatePassword(MultivaluedMap<String, String> formData, PasswordPolicy policy) {
        return policy.validate(formData.getFirst("username"), formData.getFirst("password"));
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

        if (!isEmailValid(formData.getFirst("email"))) {
            return Messages.INVALID_EMAIL;
        }

        return null;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isEmailValid(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }


}
