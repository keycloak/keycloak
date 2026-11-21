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
public class Oauth2proxyConfigClientInstallationProvider implements ClientInstallationProvider {

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI baseUri) {
        StringBuilder config = new StringBuilder();
        
        config.append("## OAuth2 Proxy Configuration File\n");
        config.append("## Generated from Keycloak for client: ").append(client.getClientId()).append("\n");
        config.append("## Realm: ").append(realm.getName()).append("\n");
        config.append("##\n");
        config.append("## Documentation: https://oauth2-proxy.github.io/oauth2-proxy/docs/configuration/overview\n");
        config.append("##\n\n");
        
        config.append("###########################################################\n");
        config.append("## PROVIDER CONFIGURATION\n");
        config.append("###########################################################\n\n");
        
        config.append("# Provider type - keycloak-oidc for Keycloak OIDC integration\n");
        config.append("provider = \"keycloak-oidc\"\n\n");
        
        config.append("# Display name shown on the login button\n");
        String displayName = realm.getName() != null && !realm.getName().isEmpty() ? realm.getName() : realm.getId();
        config.append("provider_display_name = \"").append(displayName).append("\"\n\n");
        
        config.append("###########################################################\n");
        config.append("## CLIENT CREDENTIALS\n");
        config.append("###########################################################\n\n");
        
        config.append("# OAuth2 Client ID - Unique identifier for this client\n");
        config.append("client_id = \"").append(client.getClientId()).append("\"\n\n");
        
        if (showClientCredentialsAdapterConfig(client)) {
            Map<String, Object> adapterConfig = getClientCredentialsAdapterConfig(session, client);
            if (adapterConfig != null && adapterConfig.containsKey("secret")) {
                config.append("# OAuth2 Client Secret - Keep this secure!\n");
                config.append("client_secret = \"").append(adapterConfig.get("secret")).append("\"\n\n");
            }
        } else {
            config.append("# Client Secret not required for public clients\n");
            config.append("# client_secret = \"\"\n\n");
        }
        
        config.append("###########################################################\n");
        config.append("## OIDC CONFIGURATION\n");
        config.append("###########################################################\n\n");
        
        // OIDC Issuer URL
        String issuerUrl = baseUri.toString();
        if (!issuerUrl.endsWith("/")) {
            issuerUrl += "/";
        }
        issuerUrl += "realms/" + realm.getName();
        config.append("# OIDC Issuer URL - This is the base URL of your Keycloak realm\n");
        config.append("# OAuth2 Proxy will use this to discover OIDC endpoints automatically\n");
        config.append("oidc_issuer_url = \"").append(issuerUrl).append("\"\n\n");
        
        config.append("# OAuth2 scopes to request from Keycloak\n");
        config.append("# - openid: Required for OIDC authentication\n");
        config.append("# - email: Access to user's email address\n");
        config.append("# - profile: Access to user's profile information\n");
        config.append("scope = \"openid email profile\"\n\n");
        
        config.append("###########################################################\n");
        config.append("## REDIRECT CONFIGURATION\n");
        config.append("###########################################################\n\n");
        
        if (client.getRedirectUris() == null || client.getRedirectUris().isEmpty()) {
            config.append("# Redirect URL - This is where Keycloak will redirect after authentication\n");
            config.append("# REQUIRED: Replace YOUR_DOMAIN with your actual OAuth2 Proxy domain\n");
            config.append("# This must match the 'Valid Redirect URIs' configured in your Keycloak client\n");
            config.append("# Format: https://YOUR_DOMAIN/oauth2/callback\n");
            config.append("redirect_url = \"https://YOUR_DOMAIN/oauth2/callback\"\n\n");
        } else {
            config.append("# Redirect URL - Configured from Keycloak client redirect URIs\n");
            String redirectUrl = client.getRedirectUris().iterator().next();
            config.append("redirect_url = \"").append(redirectUrl).append("\"\n\n");
        }
        
        config.append("###########################################################\n");
        config.append("## COOKIE CONFIGURATION\n");
        config.append("###########################################################\n\n");
        
        config.append("# Cookie secret - Used to encrypt the OAuth2 Proxy session cookie\n");
        config.append("# IMPORTANT: Generate your own secure random secret (32 bytes base64-encoded)\n");
        config.append("# To generate your own: openssl rand -base64 32 | tr -- '+/' '-_'\n");
        config.append("# IMPORTANT: This must be exactly 32 bytes when base64 decoded\n");
        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);
        String cookieSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        config.append("cookie_secret = \"").append(cookieSecret).append("\"\n\n");
        
        config.append("# Cookie security settings\n");
        config.append("# Set to 'true' in production with HTTPS\n");
        config.append("cookie_secure = false\n\n");
        
        config.append("# HttpOnly flag - Prevents JavaScript access to the cookie (recommended: true)\n");
        config.append("cookie_httponly = true\n\n");
        
        config.append("# SameSite policy - Protects against CSRF attacks\n");
        config.append("# Options: \"lax\", \"strict\", \"none\"\n");
        config.append("# Use \"lax\" for most cases, \"strict\" for higher security (may break some flows)\n");
        config.append("cookie_samesite = \"lax\"\n\n");
        
        config.append("# Cookie expiration - How long the session cookie is valid\n");
        config.append("# Format: Duration (e.g., \"168h\" = 7 days, \"24h\" = 1 day)\n");
        config.append("# cookie_expire = \"168h\"\n\n");
        
        config.append("# Cookie refresh interval - How often to refresh the cookie\n");
        config.append("# Format: Duration (e.g., \"1h\" = 1 hour)\n");
        config.append("# cookie_refresh = \"1h\"\n\n");
        
        config.append("###########################################################\n");
        config.append("## UPSTREAM CONFIGURATION\n");
        config.append("###########################################################\n\n");
        
        config.append("# Upstreams - The backend service(s) to proxy to after authentication\n");
        config.append("# REQUIRED: Replace with your actual backend service URL(s)\n");
        config.append("# Format: [\"http://service1:port\", \"http://service2:port\"]\n");
        config.append("# Examples:\n");
        config.append("#   - Single service: [\"http://backend:8080\"]\n");
        config.append("#   - Multiple services: [\"http://backend1:8080\", \"http://backend2:9000\"]\n");
        config.append("#   - With path routing: [\"http://backend:8080/api\"]\n");
        config.append("upstreams = [\"http://127.0.0.1:9000\"]\n\n");
        
        config.append("###########################################################\n");
        config.append("## HTTP SERVER CONFIGURATION\n");
        config.append("###########################################################\n\n");
        
        config.append("# HTTP address and port for OAuth2 Proxy to listen on\n");
        config.append("# Format: \"host:port\" or \":port\" to listen on all interfaces\n");
        config.append("http_address = \"0.0.0.0:4180\"\n\n");
        
        config.append("###########################################################\n");
        config.append("## EMAIL AND ACCESS CONTROL\n");
        config.append("###########################################################\n\n");
        
        config.append("# Email domains - Restrict access to specific email domains\n");
        config.append("# Use \"*\" to allow all domains\n");
        config.append("# Examples:\n");
        config.append("#   - Single domain: [\"example.com\"]\n");
        config.append("#   - Multiple domains: [\"example.com\", \"company.com\"]\n");
        config.append("#   - All domains: [\"*\"]\n");
        config.append("email_domains = [\"*\"]\n\n");
        
        config.append("# Authenticated emails file - Optional file containing allowed email addresses\n");
        config.append("# Format: One email per line\n");
        config.append("# authenticated_emails_file = \"/etc/oauth2-proxy/authenticated-emails.txt\"\n\n");
        
        config.append("###########################################################\n");
        config.append("## TOKEN HANDLING\n");
        config.append("###########################################################\n\n");
        
        config.append("# Pass the access token to the upstream application\n");
        config.append("# Set to true if your backend needs to validate or use the access token\n");
        config.append("# pass_access_token = true\n\n");
        
        config.append("# Pass the authorization header to the upstream application\n");
        config.append("# Set to true if your backend expects \"Authorization: Bearer <token>\"\n");
        config.append("# pass_authorization_header = true\n\n");
        
        config.append("# Set X-Auth-Request-* headers with user information\n");
        config.append("# Useful for passing user details to the upstream application\n");
        config.append("# set_xauthrequest = true\n\n");
        
        config.append("###########################################################\n");
        config.append("## REVERSE PROXY SETTINGS\n");
        config.append("###########################################################\n\n");
        
        config.append("# Reverse proxy mode - Enable if OAuth2 Proxy is behind another reverse proxy\n");
        config.append("# This affects how real client IP addresses are determined\n");
        config.append("# reverse_proxy = true\n\n");
        
        config.append("# Real client IP header - Header containing the real client IP (when behind a proxy)\n");
        config.append("# Common values: \"X-Real-IP\", \"X-Forwarded-For\"\n");
        config.append("# real_client_ip_header = \"X-Forwarded-For\"\n\n");
        
        config.append("###########################################################\n");
        config.append("## USER INTERFACE\n");
        config.append("###########################################################\n\n");
        
        config.append("# Skip the provider selection button (go directly to login)\n");
        config.append("# Set to true if you only have one provider configured\n");
        config.append("# skip_provider_button = true\n\n");
        
        config.append("# Custom sign-in page URL\n");
        config.append("# Use this to provide a custom login page design\n");
        config.append("# custom_sign_in_logo = \"https://your-domain.com/logo.png\"\n\n");
        
        config.append("###########################################################\n");
        config.append("## LOGGING AND DEBUGGING\n");
        config.append("###########################################################\n\n");
        
        config.append("# Request logging - Enable to log all HTTP requests\n");
        config.append("# Useful for debugging but can be verbose in production\n");
        config.append("# request_logging = true\n\n");
        
        config.append("# Standard logging - Enable for general application logs\n");
        config.append("# standard_logging = true\n\n");
        
        config.append("# Authentication logging - Log authentication events\n");
        config.append("# auth_logging = true\n\n");
        
        config.append("###########################################################\n");
        config.append("## SESSION MANAGEMENT\n");
        config.append("###########################################################\n\n");
        
        config.append("# Session store type - Where to store session data\n");
        config.append("# Options: \"cookie\" (default), \"redis\"\n");
        config.append("# Use \"redis\" for distributed deployments or large cookies\n");
        config.append("# session_store_type = \"cookie\"\n\n");
        
        config.append("# Redis session store configuration (if using Redis)\n");
        config.append("# redis_connection_url = \"redis://localhost:6379\"\n");
        config.append("# redis_password = \"your-redis-password\"\n");
        config.append("# redis_sentinel_master_name = \"mymaster\"\n");
        config.append("# redis_sentinel_connection_urls = [\"redis://sentinel1:26379\", \"redis://sentinel2:26379\"]\n\n");
        
        config.append("###########################################################\n");
        config.append("## ADDITIONAL CONFIGURATION\n");
        config.append("###########################################################\n\n");
        
        config.append("# Skip authentication for specific paths (public endpoints)\n");
        config.append("# Use regex patterns to match paths\n");
        config.append("# skip_auth_routes = [\n");
        config.append("#   \"^/health$\",\n");
        config.append("#   \"^/api/public/.*\"\n");
        config.append("# ]\n\n");
        
        config.append("# Allow regex paths for more flexible path matching\n");
        config.append("# skip_auth_regex = [\n");
        config.append("#   \"^/static/.*\"\n");
        config.append("# ]\n\n");
        
        config.append("###########################################################\n");
        config.append("## IMPORTANT NOTES\n");
        config.append("###########################################################\n");
        config.append("##\n");
        config.append("## 1. COOKIE_SECRET: You MUST generate a secure random secret\n");
        config.append("##    Uncomment the cookie_secret line and replace with generated value\n");
        config.append("##\n");
        config.append("## 2. UPSTREAMS: Configure your backend service URLs\n");
        config.append("##    Replace the default value with your actual backend\n");
        config.append("##\n");
        config.append("## 3. REDIRECT_URL: Verify the redirect URL matches your deployment\n");
        config.append("##    Must also be configured in Keycloak client settings\n");
        config.append("##\n");
        config.append("## 4. HTTPS: In production, set cookie_secure = true and use HTTPS\n");
        config.append("##\n");
        config.append("## 5. EMAIL_DOMAINS: Restrict to your organization's domains in production\n");
        config.append("##\n");
        config.append("###########################################################\n");

        return Response.ok(config.toString(), MediaType.TEXT_PLAIN_TYPE).build();
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
        return "Oauth2-proxy configuration file";
    }

    @Override
    public String getHelpText() {
        return "Configuration file for Oauth2-proxy with detailed comments and examples. This file contains all necessary settings to configure OAuth2-proxy with Keycloak, including extensive documentation for each parameter.";
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
        return "oauth2-proxy-config";
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }

    @Override
    public String getFilename() {
        return "oauth2proxy.cfg";
    }

    @Override
    public String getMediaType() {
        return MediaType.TEXT_PLAIN;
    }
}
