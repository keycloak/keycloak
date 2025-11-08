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

import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.JWTVCIssuerMetadata;
import org.keycloak.protocol.oidc.utils.JWKSServerUtils;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;

/**
 * {@link WellKnownProvider} implementation for JWT VC Issuer metadata at endpoint /.well-known/jwt-vc-issuer
 * <p/>
 * {@see https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-03.html#name-jwt-vc-issuer-metadata}
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class JWTVCIssuerWellKnownProvider implements WellKnownProvider {
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

        JWTVCIssuerMetadata config = new JWTVCIssuerMetadata();
        config.setIssuer(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));

        JSONWebKeySet jwks = JWKSServerUtils.getRealmJwks(session, realm);
        config.setJwks(jwks);

        return config;
    }
}
