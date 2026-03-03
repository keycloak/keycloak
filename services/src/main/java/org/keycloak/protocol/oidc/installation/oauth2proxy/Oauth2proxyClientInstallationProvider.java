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

package org.keycloak.protocol.oidc.installation.oauth2proxy;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;


/**
 * @author <a href="mailto:mathieu.passenaud@please-open.it">Mathieu Passenaud</a>
 * @version $Revision: 1 $
 */
public class Oauth2proxyClientInstallationProvider implements ClientInstallationProvider {

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI baseUri) {
        StringBuilder envVars = new StringBuilder();
        
        // Provider configuration
        envVars.append("OAUTH2_PROXY_PROVIDER=\"keycloak-oidc\"\n");
        String displayName = realm.getName() != null && !realm.getName().isEmpty() ? realm.getName() : realm.getId();
        envVars.append("OAUTH2_PROXY_PROVIDER_DISPLAY_NAME=\"").append(displayName).append("\"\n");
        
        // Client ID
        envVars.append("OAUTH2_PROXY_CLIENT_ID=\"").append(client.getClientId()).append("\"\n");
        
        // Client Secret (only for confidential clients)
        if (showClientCredentialsAdapterConfig(client)) {
            Map<String, Object> adapterConfig = getClientCredentialsAdapterConfig(session, client);
            if (adapterConfig != null && adapterConfig.containsKey("secret")) {
                envVars.append("OAUTH2_PROXY_CLIENT_SECRET=\"").append(adapterConfig.get("secret")).append("\"\n");
            }
        }
        
        // Optional cookie settings
        envVars.append("# Set HttpOnly flag on cookies (recommended for security)\n");
        envVars.append("#OAUTH2_PROXY_COOKIE_HTTPONLY=\"true\"\n");
        envVars.append("# Cookie name for the OAuth2 Proxy session\n");
        envVars.append("#OAUTH2_PROXY_COOKIE_NAME=\"_oauth2_proxy\"\n");
        
        // Generate a secure random cookie secret
        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);
        String cookieSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        envVars.append("OAUTH2_PROXY_COOKIE_SECRET=\"").append(cookieSecret).append("\"\n");
        
        envVars.append("# Set Secure flag on cookies (true for HTTPS, false for HTTP)\n");
        envVars.append("#OAUTH2_PROXY_COOKIE_SECURE=\"false\"\n");
        envVars.append("# Restrict authentication to specific email domains (* allows all)\n");
        envVars.append("#OAUTH2_PROXY_EMAIL_DOMAINS=\"*\"\n");
        envVars.append("# HTTP listening address and port\n");
        envVars.append("#OAUTH2_PROXY_HTTP_ADDRESS=\"0.0.0.0:8080\"\n");
        
        // OIDC Issuer URL
        String issuerUrl = baseUri.toString();
        if (!issuerUrl.endsWith("/")) {
            issuerUrl += "/";
        }
        issuerUrl += "realms/" + realm.getName();
        envVars.append("OAUTH2_PROXY_OIDC_ISSUER_URL=\"").append(issuerUrl).append("\"\n");
        
        // Optional pass tokens settings
        envVars.append("# Pass access token to upstream via X-Forwarded-Access-Token header\n");
        envVars.append("#OAUTH2_PROXY_PASS_ACCESS_TOKEN=\"true\"\n");
        
        // Redirect URL
        envVars.append("# The OAuth Redirect URL - Adjust this to match your deployment\n");
        if (client.getRedirectUris() != null && !client.getRedirectUris().isEmpty()) {
            String redirectUrl = client.getRedirectUris().iterator().next();
            envVars.append("OAUTH2_PROXY_REDIRECT_URL=\"").append(redirectUrl).append("\"\n");
        }
        
        // Optional reverse proxy settings
        envVars.append("# Enable if running behind a reverse proxy (trusts X-Forwarded headers)\n");
        envVars.append("#OAUTH2_PROXY_REVERSE_PROXY=\"true\"\n");
        
        // Scopes
        envVars.append("# OIDC scopes to request (openid is required for OIDC)\n");
        envVars.append("#OAUTH2_PROXY_SCOPE=\"openid email profile\"\n");
        
        // Optional authorization headers
        envVars.append("# Set Authorization Bearer header with access token for upstream\n");
        envVars.append("#OAUTH2_PROXY_SET_AUTHORIZATION_HEADER=\"true\"\n");
        envVars.append("# Set X-Auth-Request-* headers with user info for upstream\n");
        envVars.append("#OAUTH2_PROXY_SET_XAUTHREQUEST=\"true\"\n");
        envVars.append("# Skip the provider selection button (go directly to login)\n");
        envVars.append("#OAUTH2_PROXY_SKIP_PROVIDER_BUTTON=\"false\"\n");
        
        // Upstream configuration
        envVars.append("# Backend service URL(s) to proxy to (required)\n");
        envVars.append("#OAUTH2_PROXY_UPSTREAMS=\"http://127.0.0.1:9000\"\n");
        envVars.append("\n");
        envVars.append("# Full documentation: https://oauth2-proxy.github.io/oauth2-proxy/docs/configuration/overview\n");

        return Response.ok(envVars.toString(), MediaType.TEXT_PLAIN_TYPE).build();
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

    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Oauth2-proxy environment variables";
    }

    @Override
    public String getHelpText() {
        return "Environment variables used by the Oauth2-proxy to configure clients.  You must set these environment variables in your Oauth2-proxy deployment.  You may also want to tweak these variables after you download them.";
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
        return "oauth2-proxy-env";
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }

    @Override
    public String getFilename() {
        return "oauth2proxy.env";
    }

    @Override
    public String getMediaType() {
        return MediaType.TEXT_PLAIN;
    }
}
