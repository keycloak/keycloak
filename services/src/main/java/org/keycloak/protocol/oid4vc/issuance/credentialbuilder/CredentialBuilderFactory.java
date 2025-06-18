/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import org.keycloak.Config;
import org.keycloak.component.ComponentFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;

/**
 * Provider Factory to create {@link  CredentialBuilder}'s
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public interface CredentialBuilderFactory extends
        ComponentFactory<CredentialBuilder, CredentialBuilder>,
        OID4VCEnvironmentProviderFactory {

    /**
     * Returns the credential format supported by the credential builder.
     */
    String getSupportedFormat();

    @Override
    default String getId() {
        return getSupportedFormat();
    }

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    default void close() {
    }
}
