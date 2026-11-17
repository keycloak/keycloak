/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.executor;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 *
 * @author rmartinc
 */
public class TlsClientAuthCASubjectDNExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "tls-client-auth-ca-subject-dn";
    public static final String CA_SUBJECT_DN = "ca-subject-dn";
    public static final String ENFORCED = "enforced";

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new TlsClientAuthCASubjectDNExecutor();
    }

    @Override
    public String getHelpText() {
        return """
               Executor to add a default Certificate Authority (CA) Subject DN when X509 cauthentication (tls_client_auth) is used
               in the client registration service. The value can also be enforced to reject unwanted CA names.
               """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CA_SUBJECT_DN)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Certificate Authority subject DN")
                .helpText(
                        """
                        Default subject DN of the root Certificate Authority (CA) that issued the certificate for the client (trust anchor).
                        The CA Subject DN can be in the RFC4514 or RFC1779 format.
                        """
                )
                .required(true)
                .add()
                .property()
                .name(ENFORCED)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Enforced")
                .helpText(
                        """
                        If false, the executor allows to pass other CA DN and the configured value is only used as default
                        value in client registration service.
                        If true, the executor rejects any CA subject DN different to the one configured in the policy.
                        """
                )
                .defaultValue(false)
                .add()
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
