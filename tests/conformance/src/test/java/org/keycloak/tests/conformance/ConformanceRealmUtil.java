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

package org.keycloak.tests.conformance;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ClaimsParameterTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

/**
 * Realm building blocks shared by the conformance tests of several specifications.
 */
public final class ConformanceRealmUtil {

    private ConformanceRealmUtil() {
    }

    /**
     * An ID token mapper running one of the scripts of this module (src/main/resources/META-INF/keycloak-scripts.json),
     * which the conformance servers deploy. Script mappers run after the attribute and property mappers, so they can
     * remove the claims those add.
     */
    public static ProtocolMapperRepresentation idTokenScriptMapper(String name, String fileName) {
        return idTokenMapper(name, "script-" + fileName);
    }

    /**
     * Keycloak's claims parameter mapper, which returns the essential identity claims requested with the claims
     * parameter, e.g. name, in the ID token and from the userinfo endpoint.
     */
    public static ProtocolMapperRepresentation claimsParameterMapper() {
        ProtocolMapperRepresentation mapper = idTokenMapper("claims parameter", ClaimsParameterTokenMapper.PROVIDER_ID);
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        return mapper;
    }

    private static ProtocolMapperRepresentation idTokenMapper(String name, String providerId) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(providerId);
        mapper.setConfig(new HashMap<>(Map.of(
                OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true",
                OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false",
                OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "false")));
        return mapper;
    }
}
