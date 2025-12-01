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

package org.keycloak.protocol.oidc.installation;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.authorization.admin.AuthorizationService;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakOIDCClientInstallation implements ClientInstallationProvider {

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI baseUri) {
        ClientManager.InstallationAdapterConfig rep = new ClientManager.InstallationAdapterConfig();
        rep.setAuthServerUrl(baseUri.toString());
        rep.setRealm(realm.getName());
        rep.setSslRequired(realm.getSslRequired().name().toLowerCase());

        if (client.isPublicClient() && !client.isBearerOnly()) rep.setPublicClient(true);
        if (client.isBearerOnly()) rep.setBearerOnly(true);
        if (client.getRolesStream().count() > 0) rep.setUseResourceRoleMappings(true);

        rep.setResource(client.getClientId());

        if (showClientCredentialsAdapterConfig(client)) {
            Map<String, Object> adapterConfig = getClientCredentialsAdapterConfig(session, client);
            rep.setCredentials(adapterConfig);
        }

        if (showVerifyTokenAudience(client)) {
            rep.setVerifyTokenAudience(true);
        }

        configureAuthorizationSettings(session, client, rep);

        String json = null;
        try {
            json = JsonSerialization.writeValueAsPrettyString(rep);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Response.ok(json, MediaType.TEXT_PLAIN_TYPE).build();
    }

    public static Map<String, Object> getClientCredentialsAdapterConfig(KeycloakSession session, ClientModel client) {
        String clientAuthenticator = client.getClientAuthenticatorType();
        ClientAuthenticatorFactory authenticator = (ClientAuthenticatorFactory) session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, clientAuthenticator);
        return authenticator.getAdapterConfiguration(client);
    }


    public static boolean showClientCredentialsAdapterConfig(ClientModel client) {
        if (client.isPublicClient()) {
            return false;
        }

        if (client.isBearerOnly() && !client.isServiceAccountsEnabled() && client.getNodeReRegistrationTimeout() <= 0) {
            return false;
        }

        return true;
    }


    static boolean showVerifyTokenAudience(ClientModel client) {
        // We want to verify-token-audience if service client has any client roles
        if (client.getRolesStream().count() > 0) {
            return true;
        }

        // Check if there is client scope with audience protocol mapper created for particular client. If yes, admin wants verifying token audience
        String clientId = client.getClientId();

        return client.getRealm().getClientScopesStream().anyMatch(clientScope ->
            clientScope.getProtocolMappersStream().anyMatch(protocolMapper ->
                    Objects.equals(protocolMapper.getProtocolMapper(), AudienceProtocolMapper.PROVIDER_ID) &&
                    Objects.equals(clientId, protocolMapper.getConfig().get(AudienceProtocolMapper.INCLUDED_CLIENT_AUDIENCE)))
        );
    }


    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Keycloak OIDC JSON";
    }

    @Override
    public String getHelpText() {
        return "keycloak.json file used by the Keycloak OIDC client adapter to configure clients.  This must be saved to a keycloak.json file and put in your WEB-INF directory of your WAR file.  You may also want to tweak this file after you download it.";
    }

    @Override
    public void close() {

    }

    @Override
    public ClientInstallationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return "keycloak-oidc-keycloak-json";
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }

    @Override
    public String getFilename() {
        return "keycloak.json";
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

    private void configureAuthorizationSettings(KeycloakSession session, ClientModel client, ClientManager.InstallationAdapterConfig rep) {
        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION) && new AuthorizationService(session, client, null, null).isEnabled()) {
            PolicyEnforcerConfig enforcerConfig = new PolicyEnforcerConfig();

            enforcerConfig.setEnforcementMode(null);
            enforcerConfig.setLazyLoadPaths(null);

            rep.setEnforcerConfig(enforcerConfig);

            Iterator<RoleModel> it = client.getRolesStream().iterator();

            RoleModel role = hasOnlyOne(it);
            if (role != null && role.getName().equals(Constants.AUTHZ_UMA_PROTECTION)) {
                rep.setUseResourceRoleMappings(null);
            }
        }
    }

    private RoleModel hasOnlyOne(Iterator<RoleModel> it) {
        if (!it.hasNext()) return null;
        else {
            RoleModel role = it.next();
            if (it.hasNext()) return null;
            else return role;
        }
    }
}
