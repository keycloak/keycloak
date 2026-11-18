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
package org.keycloak.tests.authzen;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.util.ApiUtil;

import static org.keycloak.authorization.authzen.AuthZen.SCOPE;
import static org.keycloak.authorization.authzen.AuthZenWellKnownProvider.audience;

/**
 * Creates the "authzen" client scope as described in the AuthZEN guide.
 */
final class AuthZenScope {

    static final String AUDIENCE_MAPPER = "authzen-audience";

    private AuthZenScope() {
    }

    static String create(ManagedRealm realm, String... clientIds) {
        ClientScopeRepresentation scope = ClientScopeBuilder.create()
              .name(SCOPE)
              .protocol("openid-connect")
              .mappers(audienceMapper(audience(realm.getBaseUrl())))
              .build();

        RealmResource admin = realm.admin();
        String scopeId = ApiUtil.getCreatedId(admin.clientScopes().create(scope));

        for (String clientId : clientIds) {
            ClientResource client = admin.clients().get(admin.clients().findByClientId(clientId).get(0).getId());
            client.addDefaultClientScope(scopeId);
        }
        return scopeId;
    }

    static ProtocolMapperRepresentation audienceMapper(String audience) {
        return ProtocolMapperBuilder.create()
              .name(AUDIENCE_MAPPER)
              .protocol("openid-connect")
              .protocolMapper("oidc-audience-mapper")
              .config("included.custom.audience", audience)
              .config("access.token.claim", "true")
              .build();
    }
}
