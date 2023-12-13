/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;

/**
 * A class to register and resolve OAuth 2.0 Grant Type implementations according to provider priorities
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public class OAuth2GrantManager {

    private static final Map<String, OAuth2GrantType> MAP = new HashMap<>();

    public static void register(OAuth2GrantType grant) {
        MAP.merge(grant.getGrantType(), grant, (g1, g2) -> g1.order() > g2.order() ? g1 : g2);
    }

    public static Optional<OAuth2GrantType> resolve(KeycloakSession session, String grantType) {
        return Optional.ofNullable(MAP.get(grantType)).map(g -> g.create(session));
    }

}
