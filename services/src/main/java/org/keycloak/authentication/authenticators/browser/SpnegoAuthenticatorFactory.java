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

package org.keycloak.authentication.authenticators.browser;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SpnegoAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-spnego";
    public static final SpnegoAuthenticator SINGLETON = new SpnegoAuthenticator();
    public static final SpnegoAuthenticator SINGLETON_DISABLED = new SpnegoAuthenticator() {

        @Override
        public void authenticate(AuthenticationFlowContext context) {
            throw new IllegalStateException("Not possible to authenticate as Kerberos feature is disabled");
        }
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        return isKerberosFeatureEnabled() ? SINGLETON : SINGLETON_DISABLED;
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
    public String getReferenceCategory() {
        return UserCredentialModel.KERBEROS;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return isKerberosFeatureEnabled() ? REQUIREMENT_CHOICES : new AuthenticationExecutionModel.Requirement[]{ AuthenticationExecutionModel.Requirement.DISABLED };
    }


    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Kerberos";
    }

    @Override
    public String getHelpText() {
        return isKerberosFeatureEnabled()
                ? "Initiates the SPNEGO protocol.  Most often used with Kerberos."
                : "DISABLED. Please enable Kerberos feature and make sure Kerberos available in your platform. Initiates the SPNEGO protocol. Most often used with Kerberos.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    private boolean isKerberosFeatureEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.KERBEROS);
    }
}
