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

package org.keycloak.tests.conformance.oidc.configs;

import java.util.List;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.clientregistration.policy.impl.ConsentRequiredClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.impl.MaxClientsClientRegistrationPolicyFactory;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.conformance.ConformanceRealmUtil;

/**
 * The realm of the Dynamic OP certification plan, in which the suite registers its clients itself through Keycloak's
 * anonymous dynamic client registration. Instead of Keycloak's default anonymous client registration policies, whose
 * trusted hosts policy rejects the suite's registrations, the realm has:
 * <ul>
 * <li>the consent required policy, as in Keycloak's defaults, so the consent page shows the logo, terms of service and
 * policy the modules register</li>
 * <li>a higher limit of clients, as every module registers its own</li>
 * </ul>
 * The registered clients get the realm's default client scopes, so a default client scope holds the ID token mappers
 * the static clients of {@link OidcRealmConfig} have. The test makes the built-in profile and email client scopes
 * optional ({@code OidcDynamicTest}).
 */
public class OidcDynamicRealmConfig implements RealmConfig {

    public static final String REALM = "oidcc-dynamic";

    private static final String ID_TOKEN_CLAIMS_SCOPE = "conformance-id-token-claims";

    private static final String MAX_CLIENTS = "1000";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm.name(REALM)
                .eventsEnabled(true)
                .eventsListeners("jboss-logging")
                .users(OidcRealmConfig.user(REALM))
                // Keycloak's built-in client scopes, which a realm defining client scopes does not get otherwise
                .attribute(Constants.CREATE_DEFAULT_CLIENT_SCOPES, "true")
                .clientScopes(ClientScopeBuilder.create()
                        .name(ID_TOKEN_CLAIMS_SCOPE)
                        .description("The ID token mappers of the conformance tests")
                        .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                        .attribute("include.in.token.scope", "false")
                        .attribute("display.on.consent.screen", "false")
                        .mappers(
                                ConformanceRealmUtil.idTokenScriptMapper("conformance id token claims", "oidc-id-token-claims.js"),
                                ConformanceRealmUtil.claimsParameterMapper()))
                .update(rep -> rep.setDefaultDefaultClientScopes(List.of(ID_TOKEN_CLAIMS_SCOPE)),
                        OidcDynamicRealmConfig::anonymousClientRegistrationPolicies,
                        OidcRealmConfig::enableUnmanagedAttributes);
    }

    // Keycloak only adds its default client registration policies to a realm without any
    private static void anonymousClientRegistrationPolicies(RealmRepresentation rep) {
        MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();
        if (components == null) {
            components = new MultivaluedHashMap<>();
            rep.setComponents(components);
        }

        ComponentExportRepresentation consentRequired = new ComponentExportRepresentation();
        consentRequired.setName("Consent Required");
        consentRequired.setProviderId(ConsentRequiredClientRegistrationPolicyFactory.PROVIDER_ID);
        consentRequired.setSubType(ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.ANONYMOUS));
        components.add(ClientRegistrationPolicy.class.getName(), consentRequired);

        ComponentExportRepresentation maxClients = new ComponentExportRepresentation();
        maxClients.setName("Max Clients Limit");
        maxClients.setProviderId(MaxClientsClientRegistrationPolicyFactory.PROVIDER_ID);
        maxClients.setSubType(ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.ANONYMOUS));
        maxClients.setConfig(new MultivaluedHashMap<>());
        maxClients.getConfig().putSingle(MaxClientsClientRegistrationPolicyFactory.MAX_CLIENTS, MAX_CLIENTS);
        components.add(ClientRegistrationPolicy.class.getName(), maxClients);
    }
}
