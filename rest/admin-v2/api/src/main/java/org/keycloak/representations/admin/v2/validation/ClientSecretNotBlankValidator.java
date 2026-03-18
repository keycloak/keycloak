package org.keycloak.representations.admin.v2.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.utils.StringUtil;

/**
 * Validates that requires the {@link OIDCClientRepresentation.Auth#getSecret()} is not blank
 * when {@link OIDCClientRepresentation.Auth#getMethod()} is the (JWT) client secret.
 */
public class ClientSecretNotBlankValidator implements ConstraintValidator<ClientSecretNotBlank, OIDCClientRepresentation.Auth> {

    @Override
    public boolean isValid(OIDCClientRepresentation.Auth auth, ConstraintValidatorContext context) {
        if (auth != null && isClientSecret(auth.getMethod())) {
            if (StringUtil.isBlank(auth.getSecret())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("must not be blank when authentication method requires a secret")
                        .addPropertyNode("secret")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }

    public static boolean isClientSecret(String method) {
        return ClientIdAndSecretAuthenticator.PROVIDER_ID.equals(method)
                || JWTClientSecretAuthenticator.PROVIDER_ID.equals(method);
    }
}
