/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureRequestObjectExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-request-object";

    public static final String VERIFY_NBF = "verify-nbf";

    private static final ProviderConfigProperty VERIFY_NBF_PROPERTY = new ProviderConfigProperty(
            VERIFY_NBF, "Verify Not-Before", "If ON, then it will be verified if 'request' object used in OIDC authorization request contains not-before " +
            "claim and this claim will be validated", ProviderConfigProperty.BOOLEAN_TYPE, true);

    public static final String AVAILABLE_PERIOD = "available-period";
    public static final String ENCRYPTION_REQUIRED = "encryption-required";

    private static final ProviderConfigProperty AVAILABLE_PERIOD_PROPERTY = new ProviderConfigProperty(
            AVAILABLE_PERIOD, "Available Period", "The maximum period in seconds for which the 'request' object used in OIDC authorization request is considered valid. " +
            "It is used if 'Verify Not-Before' is ON.", ProviderConfigProperty.STRING_TYPE, "3600");

    private static final ProviderConfigProperty ENCRYPTION_REQUIRED_PROPERTY = new ProviderConfigProperty(
            ENCRYPTION_REQUIRED, "Encryption Required", "Whether request object encryption is required. If enabled, request objects must be encrypted. Otherwise, encryption is optional.",
            ProviderConfigProperty.BOOLEAN_TYPE, Boolean.FALSE);

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureRequestObjectExecutor(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "The executor checks whether the client treats the request object in its authorization request by following Financial-grade API Security Profile : Read and Write API Security Profile.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(Arrays.asList(VERIFY_NBF_PROPERTY, AVAILABLE_PERIOD_PROPERTY, ENCRYPTION_REQUIRED_PROPERTY));
    }

}
