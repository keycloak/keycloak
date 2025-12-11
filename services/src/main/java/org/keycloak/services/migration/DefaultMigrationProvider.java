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

package org.keycloak.services.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.migration.MigrationProvider;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.managers.RealmManager;

/**
 * Various common utils needed for migration from older version to newer
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultMigrationProvider implements MigrationProvider {

    private final KeycloakSession session;

    public DefaultMigrationProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public List<ProtocolMapperRepresentation> getMappersForClaimMask(Long claimMask) {
        Map<String, ProtocolMapperRepresentation> allMappers = getAllDefaultMappers(session);

        if (claimMask == null) {
            return new ArrayList<ProtocolMapperRepresentation>(allMappers.values());
        }

        if (!ClaimMask.hasUsername(claimMask)) {
            allMappers.remove(OIDCLoginProtocolFactory.USERNAME);
        }
        if (!ClaimMask.hasEmail(claimMask)) {
            allMappers.remove(OIDCLoginProtocolFactory.EMAIL);
        }
        if (!ClaimMask.hasName(claimMask)) {
            allMappers.remove(OIDCLoginProtocolFactory.FAMILY_NAME);
            allMappers.remove(OIDCLoginProtocolFactory.FULL_NAME);
            allMappers.remove(OIDCLoginProtocolFactory.GIVEN_NAME);
        }

        return new ArrayList<ProtocolMapperRepresentation>(allMappers.values());
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers(String protocol) {
        LoginProtocolFactory providerFactory = (LoginProtocolFactory) session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, protocol);
        return providerFactory.getBuiltinMappers();
    }

    @Override
    public void setupAdminCli(RealmModel realm) {
        new RealmManager(session).setupAdminCli(realm);
    }

    private OIDCLoginProtocolFactory getOIDCLoginProtocolFactory() {
        return (OIDCLoginProtocolFactory) session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    @Override
    public ClientScopeModel addOIDCRolesClientScope(RealmModel realm) {
        return getOIDCLoginProtocolFactory().addRolesClientScope(realm);
    }


    @Override
    public ClientScopeModel addOIDCWebOriginsClientScope(RealmModel realm) {
        return getOIDCLoginProtocolFactory().addWebOriginsClientScope(realm);
    }

    @Override
    public ClientScopeModel addOIDCMicroprofileJWTClientScope(RealmModel realm) {
        return getOIDCLoginProtocolFactory().addMicroprofileJWTClientScope(realm);
    }

    @Override
    public ClientScopeModel addOIDCAcrClientScope(RealmModel realm) {
        return getOIDCLoginProtocolFactory().addAcrClientScope(realm);
    }

    @Override
    public ClientScopeModel addOIDCBasicClientScope(RealmModel realm) {
        return getOIDCLoginProtocolFactory().addBasicClientScope(realm);
    }

    @Override
    public ClientScopeModel addOIDCServiceAccountClientScope(RealmModel realm) {
        return getOIDCLoginProtocolFactory().addServiceAccountClientScope(realm);
    }

    @Override
    public void close() {
    }


    // With change to client scopes, there are not default protocolMappers dedicated to single client anymore. Instead, there are default client scopes
    // and mappers dedicated to those scopes. So returning empty map for now
    private static Map<String, ProtocolMapperRepresentation> getAllDefaultMappers(KeycloakSession session) {
        return Collections.emptyMap();
    }
}
