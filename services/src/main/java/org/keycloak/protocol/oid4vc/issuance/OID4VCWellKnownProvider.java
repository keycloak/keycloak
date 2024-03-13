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

package org.keycloak.protocol.oid4vc.issuance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.OID4VCAbstractWellKnownProvider;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Extension of the OIDC Wellknown Provider to also support the pre-authorized grant type
 *
 * TODO: might be removed in the future
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCWellKnownProvider extends OID4VCAbstractWellKnownProvider {

    public OID4VCWellKnownProvider(KeycloakSession keycloakSession, ObjectMapper objectMapper) {
        super(keycloakSession, objectMapper);
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Object getConfig() {
        // some wallets use the openid-config well-known to also gather the issuer metadata. In
        // the future(when everyone uses .well-known/openid-credential-issuer), that can be removed.
        Map<String, Object> configAsMap = objectMapper.convertValue(
                new OIDCWellKnownProvider(keycloakSession, null, false).getConfig(),
                Map.class);

        List<String> supportedGrantTypes = Optional.ofNullable(configAsMap.get("grant_types_supported"))
                .map(grantTypesObject -> objectMapper.convertValue(
                        grantTypesObject, new TypeReference<List<String>>() {
                        })).orElse(new ArrayList<>());
        // newly invented by OID4VCI and supported by this implementation
        supportedGrantTypes.add(PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE);
        configAsMap.put("grant_types_supported", supportedGrantTypes);
        configAsMap.put("credential_endpoint", getCredentialsEndpoint(keycloakSession.getContext()));

        return configAsMap;
    }


}