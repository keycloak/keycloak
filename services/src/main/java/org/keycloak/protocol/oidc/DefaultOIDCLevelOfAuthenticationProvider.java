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

package org.keycloak.protocol.oidc;

import java.util.Map;
import java.util.OptionalInt;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.AcrUtils;

public class DefaultOIDCLevelOfAuthenticationProvider implements OIDCLevelOfAuthenticationProvider {
    private final KeycloakSession keycloakSession;

    public DefaultOIDCLevelOfAuthenticationProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public OptionalInt getRequestedLoa(String acrValue) {
        Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(keycloakSession.getContext().getAuthenticationSession().getClient());

        try {
            Integer loa = acrLoaMap.get(acrValue);
            return OptionalInt.of(loa == null ? Integer.parseInt(acrValue) : loa);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    public void close() {

    }
}
