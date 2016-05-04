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

package org.keycloak.testsuite.util;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderBuilder {

    private IdentityProviderRepresentation rep = new IdentityProviderRepresentation();

    public static IdentityProviderBuilder create() {
        return new IdentityProviderBuilder();
    }

    private IdentityProviderBuilder() {
        rep.setEnabled(true);
    }

    public IdentityProviderBuilder alias(String alias) {
        rep.setAlias(alias);
        return this;
    }

    public IdentityProviderBuilder providerId(String providerId) {
        rep.setProviderId(providerId);
        return this;
    }

    public IdentityProviderRepresentation build() {
        return rep;
    }

}
