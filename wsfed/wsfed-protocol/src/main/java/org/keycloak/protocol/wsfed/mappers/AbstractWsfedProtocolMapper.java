/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.wsfed.mappers;

import org.keycloak.protocol.wsfed.WSFedLoginProtocol;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ProtocolMapper;

public abstract class AbstractWsfedProtocolMapper implements ProtocolMapper {
    public static final String TOKEN_MAPPER_CATEGORY = "OIDC Token mapper";
    public static final String ATTRIBUTE_STATEMENT_CATEGORY = "SAML AttributeStatement Mapper";
    public static final String SAML_ROLE_MAPPER = "SAML Role Mapper";

    @Override
    public String getProtocol() {
        return WSFedLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public void close() {

    }

    @Override
    public final ProtocolMapper create(KeycloakSession session) {
        throw new RuntimeException("UNSUPPORTED METHOD");
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }
}
