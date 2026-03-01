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
import java.util.LinkedList;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureSigningAlgorithmExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-signature-algorithm";

    public static final String DEFAULT_ALGORITHM = "default-algorithm";

    private static final ProviderConfigProperty DEFAULT_ALGORITHM_PROPERTY = new ProviderConfigProperty(
            DEFAULT_ALGORITHM, "Default Algorithm", "Default signature algorithm, which will be set to clients during client registration/update in case that client does not specify any algorithm",
            ProviderConfigProperty.LIST_TYPE, Algorithm.PS256, new LinkedList<>(FapiConstant.ALLOWED_ALGORITHMS).toArray(new String[] {}));

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureSigningAlgorithmExecutor(session);
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
        return "It refuses the client whose signature algorithms are considered not to be secure. This is applied by server for signing ID Token, UserInfo and Access Token. Also it is used by client for Token Endpoint Authentication signature algorithm (for JWT client authenticators) and OIDC Request object. It accepts ES256, ES384, ES512, PS256, PS384 and PS512.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(Arrays.asList(DEFAULT_ALGORITHM_PROPERTY));
    }

}
