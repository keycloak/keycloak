package org.keycloak.protocol.oid4vc.clientpolicy;

import java.util.Objects;
import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;

public class PredicateCredentialClientPolicy extends CredentialClientPolicy<Boolean> {

    public PredicateCredentialClientPolicy(String name, String key, Boolean exp, Boolean def) {
        super(name, key, Boolean.class, exp, def);
    }

    public Boolean getCurrentValue(CredentialScopeRepresentation credScope) {
        Boolean scopeValue = Optional.ofNullable(credScope.getAttribute(getAttrName()))
                .map(Boolean::parseBoolean)
                .orElse(getDefaultValue());
        return scopeValue;
    }

    public Boolean validate(CredentialScopeRepresentation credScope) {
        Boolean scopeValue = credScope.getCredentialPolicyValue(this);
        return Objects.equals(scopeValue, getExpectedValue());
    }

}
