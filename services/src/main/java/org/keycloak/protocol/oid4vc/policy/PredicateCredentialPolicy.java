package org.keycloak.protocol.oid4vc.policy;

import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;

public class PredicateCredentialPolicy extends CredentialPolicy<Boolean> {

    public PredicateCredentialPolicy(String name, String key, Boolean exp, Boolean def) {
        super(name, key, Boolean.class, exp, def);
    }

    public boolean validate(CredentialScopeRepresentation credScope) {
        Boolean scopeValue = Optional.ofNullable(credScope.getAttribute(getAttrKey()))
                .map(Boolean::parseBoolean)
                .orElse(getDefaultValue());
        return scopeValue == getExpectedValue();
    }
}
