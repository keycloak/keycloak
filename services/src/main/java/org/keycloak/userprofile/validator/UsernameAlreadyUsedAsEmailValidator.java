package org.keycloak.userprofile.validator;

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

import java.util.List;

public class UsernameAlreadyUsedAsEmailValidator implements SimpleValidator {
    public static final String ID="up-duplicate-as-mail-username";
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        if (values.isEmpty()) {
            return context;
        }

        String value = values.get(0);

        if (Validation.isBlank(value))
            return context;

        KeycloakSession session = context.getSession();
        UserModel existing = session.users().getUserByEmail(session.getContext().getRealm(), value);
        UserModel user = UserProfileAttributeValidationContext.from(context).getAttributeContext().getUser();
        if(existing!=null && (user==null || !user.getId().equals(existing.getId()))) {
            context.addError(new ValidationError(ID,inputHint, Messages.USERNAME_ALREADY_USED_AS_EMAIL).setStatusCode(Response.Status.CONFLICT));
        }
        return context;
    }
}
