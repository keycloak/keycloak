package org.keycloak.services.validation;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.messages.Messages;
import org.picketlink.idm.credential.util.TimeBasedOTP;

public class Validation {

    public static String validateRegistrationForm(MultivaluedMap<String, String> formData, List<String> requiredCredentialTypes) {
        if (isEmpty(formData.getFirst("name"))) {
            return Messages.MISSING_NAME;
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

        if (requiredCredentialTypes.contains(CredentialRepresentation.TOTP)) {
            if (isEmpty(formData.getFirst("totp"))) {
                return Messages.MISSING_TOTP;
            }

            boolean validTotp = new TimeBasedOTP().validate(formData.getFirst("totp"), formData.getFirst("totpSecret")
                    .getBytes());
            if (!validTotp) {
                return Messages.INVALID_TOTP;
            }
        }

        return null;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

}
