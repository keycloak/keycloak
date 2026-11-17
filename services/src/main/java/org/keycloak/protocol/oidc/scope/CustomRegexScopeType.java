package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.saml.common.util.StringUtil;

public class CustomRegexScopeType implements ParameterizedScopeTypeProvider {

    public static final String TYPE = "custom";

    @Override
    public String getTypeName() {
        return TYPE;
    }

    @Override
    public void validateParameter(@Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        String regexp = scope.getParameterizedScopeRegexp();
        if (StringUtil.isNullOrEmpty(regexp)) {
            throw new InvalidScopeParameterException("custom scope type requires a regex pattern");
        }
        if (!parameter.matches(regexp)) {
            throw new InvalidScopeParameterException("does not match pattern '" + regexp + "'");
        }
    }
}
