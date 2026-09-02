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
package org.keycloak.protocol.oid4vc.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class Claims extends ArrayList<Claim> {

    public static Claims parse(KeycloakSession keycloakSession, CredentialScopeModel credentialScope) {
        Claims claims = new Claims();
        credentialScope.getOid4vcProtocolMappersStream().forEach(protocolMapper -> {
            Optional<Claim> claim = Claim.parse(keycloakSession, credentialScope.getFormat(), protocolMapper);
            claim.ifPresent(claims::add);
        });
        return claims;
    }

    public String toJsonString(){
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Claims fromJsonString(String jsonString){
        try {
            return JsonSerialization.readValue(jsonString, Claims.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
