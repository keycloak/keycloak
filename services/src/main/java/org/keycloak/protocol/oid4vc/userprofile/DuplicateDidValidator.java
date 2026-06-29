package org.keycloak.protocol.oid4vc.userprofile;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validator to check that the DID attribute value is unique among users in the realm.
 * Expects List of Strings as input.
 */
public class DuplicateDidValidator implements SimpleValidator {

    public static final String ID = "up-duplicate-did";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        if (values == null || values.isEmpty()) {
            return context;
        }

        String value = values.get(0);

        if (Validation.isBlank(value)) {
            return context;
        }

        KeycloakSession session = context.getSession();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = UserProfileAttributeValidationContext.from(context).getAttributeContext().getUser();

        // Skip validation if the DID value hasn't changed for an existing user
        if (user != null && Objects.equals(user.getFirstAttribute(UserModel.DID), value)) {
            return context;
        }

        // Search for existing users with the same DID attribute value
        session.users().searchForUserByUserAttributeStream(realm, UserModel.DID, value)
                .filter(existing -> user == null || !Objects.equals(existing.getId(), user.getId()))
                .findFirst()
                .ifPresent(existing -> {
                    context.addError(new ValidationError(ID, inputHint, Messages.DID_EXISTS)
                            .setStatusCode(Response.Status.CONFLICT));
                });

        return context;
    }

}
