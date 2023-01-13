/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authentication.authenticators.browser;


import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;

/**
 * Factory used only when KERBEROS feature is disabled. This exists due the KERBEROS authenticator is added by default to realm browser flow (even if DISABLED by default)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SpnegoDisabledAuthenticatorFactory extends SpnegoAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public Authenticator create(KeycloakSession session) {
        return new SpnegoDisabledAuthenticator();
    }

    @Override
    public String getHelpText() {
        return "DISABLED. Please enable Kerberos feature and make sure Kerberos available in your platform. Initiates the SPNEGO protocol. Most often used with Kerberos.";
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{ AuthenticationExecutionModel.Requirement.DISABLED };
    }

    @Override
    public boolean isSupported() {
        return !Profile.isFeatureEnabled(Profile.Feature.KERBEROS);
    }

    public static class SpnegoDisabledAuthenticator extends SpnegoAuthenticator {

        @Override
        public void authenticate(AuthenticationFlowContext context) {
            throw new IllegalStateException("Not possible to authenticate as Kerberos feature is disabled");
        }
    }

}
