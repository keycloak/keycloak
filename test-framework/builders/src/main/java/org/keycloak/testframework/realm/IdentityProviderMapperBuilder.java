package org.keycloak.testframework.realm;

import java.util.HashMap;

import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;

public class IdentityProviderMapperBuilder extends Builder<IdentityProviderMapperRepresentation> {

    private IdentityProviderMapperBuilder(IdentityProviderMapperRepresentation rep) {
        super(rep);
    }

    public static IdentityProviderMapperBuilder create() {
        return new IdentityProviderMapperBuilder(new IdentityProviderMapperRepresentation());
    }

    public IdentityProviderMapperBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public IdentityProviderMapperBuilder identityProviderAlias(String identityProviderAlias) {
        rep.setIdentityProviderAlias(identityProviderAlias);
        return this;
    }

    public IdentityProviderMapperBuilder identityProviderMapper(String identityProviderMapper) {
        rep.setIdentityProviderMapper(identityProviderMapper);
        return this;
    }

    public IdentityProviderMapperBuilder attribute(String name, String value) {
        rep.setConfig(createIfNull(rep.getConfig(), HashMap::new));
        rep.getConfig().put(name, value);
        return this;
    }

    public IdentityProviderMapperRepresentation build() {
        return rep;
    }
}
