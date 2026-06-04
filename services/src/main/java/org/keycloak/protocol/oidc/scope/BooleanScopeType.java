package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.models.ClientScopeModel;

public class BooleanScopeType implements ParameterizedScopeTypeProvider {

    public static final String TYPE = "boolean";

    @Override
    public String getTypeName() {
        return TYPE;
    }

    @Override
    public void validateParameter(@Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        if (!"true".equalsIgnoreCase(parameter) && !"false".equalsIgnoreCase(parameter)) {
            throw new InvalidScopeParameterException(String.format("'%s' is not a valid boolean, expected 'true' or 'false'", parameter));
        }
    }
}
