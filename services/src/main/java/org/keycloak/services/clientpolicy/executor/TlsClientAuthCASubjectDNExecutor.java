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

import javax.security.auth.x500.X500Principal;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author rmartinc
 */
public class TlsClientAuthCASubjectDNExecutor implements ClientPolicyExecutorProvider<TlsClientAuthCASubjectDNExecutor.Configuration> {

    private Configuration configuration;

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty(TlsClientAuthCASubjectDNExecutorFactory.ENFORCED)
        protected Boolean enforced;
        @JsonProperty(TlsClientAuthCASubjectDNExecutorFactory.CA_SUBJECT_DN)
        protected String caSubjectDn;

        public Boolean isEnforced() {
            return enforced;
        }

        public void setEnforced(Boolean enforced) {
            this.enforced = enforced;
        }

        public String getCaSubjectDn() {
            return caSubjectDn;
        }

        public void setCaSubjectDn(String caSubjectDn) {
            this.caSubjectDn = caSubjectDn;
        }
    }

    @Override
    public String getProviderId() {
        return TlsClientAuthCASubjectDNExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTERED, UPDATED -> check(((ClientCRUDContext)context).getTargetClient());
        }
    }

    private void check(ClientModel clientModel) throws ClientPolicyException {
        OIDCAdvancedConfigWrapper oidcClient = OIDCAdvancedConfigWrapper.fromClientModel(clientModel);
        if (X509ClientAuthenticator.PROVIDER_ID.equals(clientModel.getClientAuthenticatorType())) {
            final String dn = configuration.getCaSubjectDn();
            if (oidcClient.getTlsClientAuthCASubjectDn() == null) {
                oidcClient.setTlsClientAuthCASubjectDn(dn);
            } else if (Boolean.TRUE.equals(configuration.isEnforced())) {
                try {
                    X500Principal forcedDn = X509ClientAuthenticator.constructX500Principal(dn);
                    X500Principal passedDn = X509ClientAuthenticator.constructX500Principal(oidcClient.getTlsClientAuthCASubjectDn());
                    if (!forcedDn.equals(passedDn)) {
                        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Certificate Authority subject DN must be " + dn);
                    }
                } catch (IllegalArgumentException e) {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Certificate Authority subject DN must be " + dn);
                }
            }
        }
    }
}
