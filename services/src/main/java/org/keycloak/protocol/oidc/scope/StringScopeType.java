package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.models.ClientScopeModel;

public class StringScopeType implements ParameterizedScopeTypeProvider {

    public static final String TYPE = "string";

    @Override
    public String getTypeName() {
        return TYPE;
    }

    @Override
    public void validateParameter(@Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
    }
}
