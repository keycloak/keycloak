package org.keycloak.models;

import java.util.HashMap;
import java.util.Map;

public class IdentityProviderQuery {

    IdentityProviderType type;
    IdentityProviderCapability capability;
    Map<String, String> options;

    public static IdentityProviderQuery any() {
        IdentityProviderQuery query = new IdentityProviderQuery();
        query.type = IdentityProviderType.ANY;
        return query;
    }

    public static IdentityProviderQuery userAuthentication() {
        IdentityProviderQuery query = new IdentityProviderQuery();
        query.type = IdentityProviderType.USER_AUTHENTICATION;
        return query;
    }

    public static IdentityProviderQuery type(IdentityProviderType type) {
        IdentityProviderQuery query = new IdentityProviderQuery();
        query.type = type;
        return query;
    }

    public static IdentityProviderQuery capability(IdentityProviderCapability capability) {
        IdentityProviderQuery query = new IdentityProviderQuery();
        query.capability = capability;
        return query;
    }

    public IdentityProviderQuery with(String key, String value) {
        if (this.options == null) {
            this.options = new HashMap<>();
        }
        this.options.put(key, value);
        return this;
    }

    public IdentityProviderQuery with(Map<String, String> options) {
        if (this.options == null) {
            this.options = new HashMap<>(options);
        } else {
            this.options.putAll(options);
        }
        return this;
    }

    public IdentityProviderType getType() {
        return type;
    }

    public IdentityProviderCapability getCapability() {
        return capability;
    }

    public Map<String, String> getOptions() {
        return options;
    }

}
