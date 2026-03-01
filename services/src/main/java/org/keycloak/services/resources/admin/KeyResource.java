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

package org.keycloak.services.resources.admin;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @resource Key
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class KeyResource {

    private RealmModel realm;
    private KeycloakSession session;
    private AdminPermissionEvaluator auth;

    public KeyResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.KEY)
    @Operation()
    public KeysMetadataRepresentation getKeyMetadata() {
        auth.realm().requireViewRealm();

        KeysMetadataRepresentation keys = new KeysMetadataRepresentation();
        keys.setActive(new HashMap<>());

        List<KeysMetadataRepresentation.KeyMetadataRepresentation> realmKeys = session.keys().getKeysStream(realm)
                .map(key -> {
                    if (key.getStatus().isActive()) {
                        if (!keys.getActive().containsKey(key.getAlgorithmOrDefault())) {
                            keys.getActive().put(key.getAlgorithmOrDefault(), key.getKid());
                        }
                    }
                    return toKeyMetadataRepresentation(key);
                })
                .collect(Collectors.toList());
        keys.setKeys(realmKeys);

        return keys;
    }

    private KeysMetadataRepresentation.KeyMetadataRepresentation toKeyMetadataRepresentation(KeyWrapper key) {
        KeysMetadataRepresentation.KeyMetadataRepresentation r = new KeysMetadataRepresentation.KeyMetadataRepresentation();
        r.setProviderId(key.getProviderId());
        r.setProviderPriority(key.getProviderPriority());
        r.setKid(key.getKid());
        r.setStatus(key.getStatus() != null ? key.getStatus().name() : null);
        r.setType(key.getType());
        r.setAlgorithm(key.getAlgorithmOrDefault());
        r.setPublicKey(key.getPublicKey() != null ? PemUtils.encodeKey(key.getPublicKey()) : null);
        if (key.getCertificate() != null ||
                (key.getCertificateChain() != null && !key.getCertificateChain().isEmpty())) {
            try {
                final String base64Certificate;
                if (key.getCertificate() != null) {
                    base64Certificate = Base64.getEncoder().encodeToString(key.getCertificate().getEncoded());
                }
                else {
                    base64Certificate = Base64.getEncoder().encodeToString(key.getCertificateChain().get(0).getEncoded());
                }
                r.setCertificate(base64Certificate);
            } catch (CertificateEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        r.setUse(key.getUse());

        X509Certificate cert = key.getCertificate();
        if (cert != null) {
            r.setCertificate(PemUtils.encodeCertificate(cert));
            r.setValidTo(cert.getNotAfter() != null ? cert.getNotAfter().getTime() : null);
        }

        return r;
    }
}
