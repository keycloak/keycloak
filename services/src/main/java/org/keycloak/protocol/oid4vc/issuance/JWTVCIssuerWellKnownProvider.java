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

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.http.HttpResponse;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.JWTVCIssuerMetadata;
import org.keycloak.protocol.oidc.utils.JWKSServerUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.ServerMetadataResource;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;

import org.jboss.logging.Logger;

/**
 * {@link WellKnownProvider} implementation for JWT VC Issuer metadata at endpoint /.well-known/jwt-vc-issuer
 * <p/>
 * {@see https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-03.html#name-jwt-vc-issuer-metadata}
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class JWTVCIssuerWellKnownProvider implements WellKnownProvider {
    private static final Logger LOGGER = Logger.getLogger(JWTVCIssuerWellKnownProvider.class);
    private final KeycloakSession session;

    public JWTVCIssuerWellKnownProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Object getConfig() {
        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        RealmModel realm = session.getContext().getRealm();

        addDeprecationHeadersIfOldRoute();

        JWTVCIssuerMetadata config = new JWTVCIssuerMetadata();
        config.setIssuer(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));

        JSONWebKeySet jwks = JWKSServerUtils.getRealmJwks(session, realm);
        config.setJwks(jwks);

        return config;
    }

    /**
     * Attach deprecation headers/log for the legacy realm-scoped route:
     * old: /realms/{realm}/.well-known/jwt-vc-issuer
     * new: /.well-known/jwt-vc-issuer/realms/{realm}
     */
    private void addDeprecationHeadersIfOldRoute() {
        String requestPath = session.getContext().getUri().getRequestUri().getPath();
        if (requestPath == null) {
            return;
        }

        int idxRealms = requestPath.indexOf("/realms/");
        int idxWellKnown = requestPath.indexOf("/.well-known/");
        boolean isOldRoute = idxRealms >= 0 && idxWellKnown > idxRealms;
        if (!isOldRoute) {
            return;
        }

        var realm = session.getContext().getRealm();
        if (realm == null) {
            return;
        }

        var base = session.getContext().getUri().getBaseUriBuilder();
        var successor = ServerMetadataResource.wellKnownOAuthProviderUrl(base)
                .build(JWTVCIssuerWellKnownProviderFactory.PROVIDER_ID, realm.getName());

        HttpResponse httpResponse = session.getContext().getHttpResponse();
        httpResponse.setHeader("Warning", "299 - \"Deprecated endpoint; use " + successor + "\"");
        httpResponse.setHeader("Deprecation", "true");
        httpResponse.setHeader("Link", "<" + successor + ">; rel=\"successor-version\"");

        LOGGER.warnf("Deprecated realm-scoped well-known endpoint accessed for JWT VC issuer in realm '%s'. Use %s instead.", realm.getName(), successor);
    }
}
