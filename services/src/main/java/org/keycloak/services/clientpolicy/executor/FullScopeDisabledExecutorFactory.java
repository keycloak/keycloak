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
 *
 */

package org.keycloak.services.clientpolicy.executor;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Check that switch "fullScopeAllowed" is not enabled for the clients
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FullScopeDisabledExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "full-scope-disabled";

    public static final String AUTO_CONFIGURE = "auto-configure";

    private static final ProviderConfigProperty AUTO_CONFIGURE_PROPERTY = new ProviderConfigProperty(
            AUTO_CONFIGURE, "Auto-configure", "If On, the configuration of the client will be auto-configured to disable fullScopeAllowed during client creation or update." +
            "If off, the clients are validated to not have fullScopeAllowed enabled during create/update client", ProviderConfigProperty.BOOLEAN_TYPE, true);

    @Override
    public FullScopeDisabledExecutor create(KeycloakSession session) {
        return new FullScopeDisabledExecutor();
    }

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
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "When present, then registered/updated clients will be verified to have 'fullScopeAllowed' switch disabled and eventually will be auto-configured for 'fullScopeAllowed' switch to be disabled";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.singletonList(AUTO_CONFIGURE_PROPERTY);
    }
}
