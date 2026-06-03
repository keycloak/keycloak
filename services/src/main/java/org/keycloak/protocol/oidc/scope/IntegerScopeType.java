package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.models.ClientScopeModel;

public class IntegerScopeType implements ParameterizedScopeTypeProvider {

    public static final String TYPE = "integer";

    @Override
    public String getTypeName() {
        return TYPE;
    }

    @Override
    public void validateParameter(@Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        try {
            // Long to accept any whole number (no fractional part), not just int-range values
            Long.parseLong(parameter);
        } catch (NumberFormatException e) {
            throw new InvalidScopeParameterException(String.format("'%s' is not a valid integer", parameter));
        }
    }
}
