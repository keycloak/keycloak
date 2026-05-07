/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testframework.realm;

import java.util.HashMap;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderBuilder extends Builder<IdentityProviderRepresentation> {

    private IdentityProviderBuilder(IdentityProviderRepresentation rep) {
        super(rep);
    }

    public static IdentityProviderBuilder create() {
        return new IdentityProviderBuilder(new IdentityProviderRepresentation());
    }

    public IdentityProviderBuilder alias(String alias) {
        rep.setAlias(alias);
        return this;
    }

    public IdentityProviderBuilder providerId(String providerId) {
        rep.setProviderId(providerId);
        return this;
    }

    public IdentityProviderBuilder displayName(String displayName) {
        rep.setDisplayName(displayName);
        return this;
    }

    public IdentityProviderBuilder hideOnLoginPage() {
        rep.setHideOnLogin(true);
        return this;
    }

    public IdentityProviderBuilder storeToken(boolean storeToken) {
        rep.setStoreToken(storeToken);
        return this;
    }

    public IdentityProviderBuilder addReadTokenRoleOnCreate(boolean addReadTokenRoleOnCreate) {
        rep.setAddReadTokenRoleOnCreate(addReadTokenRoleOnCreate);
        return this;
    }

    public IdentityProviderBuilder attribute(String name, String value) {
        rep.setConfig(createIfNull(rep.getConfig(), HashMap::new));
        rep.getConfig().put(name, value);
        return this;
    }

    public IdentityProviderRepresentation build() {
        return rep;
    }

}
