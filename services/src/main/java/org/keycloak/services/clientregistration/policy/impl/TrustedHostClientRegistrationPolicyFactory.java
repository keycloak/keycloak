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

package org.keycloak.services.clientregistration.policy.impl;

import java.util.Arrays;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.policy.AbstractClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TrustedHostClientRegistrationPolicyFactory extends AbstractClientRegistrationPolicyFactory {

    public static final String PROVIDER_ID = "trusted-hosts";

    public static final String TRUSTED_HOSTS = "trusted-hosts";

    public static final String HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH = "host-sending-registration-request-must-match";
    public static final String CLIENT_URIS_MUST_MATCH = "client-uris-must-match";

    private static final ProviderConfigProperty TRUSTED_HOSTS_PROPERTY = new ProviderConfigProperty(TRUSTED_HOSTS, "trusted-hosts.label", "trusted-hosts.tooltip", ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null);

    private static final ProviderConfigProperty HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH_PROPERTY = new ProviderConfigProperty(HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "host-sending-registration-request-must-match.label",
            "host-sending-registration-request-must-match.tooltip", ProviderConfigProperty.BOOLEAN_TYPE, "true");

    private static final ProviderConfigProperty CLIENT_URIS_MUST_MATCH_PROPERTY = new ProviderConfigProperty(CLIENT_URIS_MUST_MATCH, "client-uris-must-match.label",
            "client-uris-must-match.tooltip", ProviderConfigProperty.BOOLEAN_TYPE, "true");


    @Override
    public ClientRegistrationPolicy create(KeycloakSession session, ComponentModel model) {
        return new TrustedHostClientRegistrationPolicy(session, model);
    }

    @Override
    public String getHelpText() {
        return "Allows to specify from which hosts is user able to register and which redirect URIs can client use in it's configuration";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(TRUSTED_HOSTS_PROPERTY, HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH_PROPERTY, CLIENT_URIS_MUST_MATCH_PROPERTY);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        ConfigurationValidationHelper.check(config)
                .checkBoolean(HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH_PROPERTY, true)
                .checkBoolean(CLIENT_URIS_MUST_MATCH_PROPERTY, true);

        TrustedHostClientRegistrationPolicy policy = new TrustedHostClientRegistrationPolicy(session, config);
        if (!policy.isHostMustMatch() && !policy.isClientUrisMustMatch()) {
            throw new ComponentValidationException("At least one of hosts verification or client URIs validation must be enabled");
        }

    }
}
