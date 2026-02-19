package org.keycloak.protocol.oid4vc.policy;

import java.util.Optional;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;

public class CredentialPolicyUtils {

    // Hide ctor
    private CredentialPolicyUtils() {}

    public static <T> T getCredentialPolicyValue(CredentialScopeModel credScope, CredentialPolicy<?> policy) {
        return getCredentialPolicyValue(new CredentialScopeRepresentation(credScope), policy);
    }

    public static <T> T getCredentialPolicyValue(CredentialScopeRepresentation credScope, CredentialPolicy<?> policy) {
        String value = Optional.ofNullable(credScope.getAttribute(policy.getAttrKey())).orElse(String.valueOf(policy.getDefaultValue()));
        Class<?> policyType = policy.getType();
        Object converted;
        if (policyType == String.class) {
            converted = value;
        } else if (policyType == Integer.class) {
            converted = Integer.valueOf(value);
        } else if (policyType == Boolean.class) {
            converted = Boolean.valueOf(value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + policyType.getName());
        }
        //noinspection unchecked
        return (T) policyType.cast(converted);
    }
}
