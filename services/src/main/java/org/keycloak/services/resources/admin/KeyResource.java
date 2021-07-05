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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @resource Key
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
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
    public KeysMetadataRepresentation getKeyMetadata() {
        auth.realm().requireViewRealm();

        KeysMetadataRepresentation keys = new KeysMetadataRepresentation();
        keys.setActive(new HashMap<>());

        List<KeysMetadataRepresentation.KeyMetadataRepresentation> realmKeys = session.keys().getKeysStream(realm)
                .map(key -> {
                    if (key.getStatus().isActive()) {
                        if (!keys.getActive().containsKey(key.getAlgorithm())) {
                            keys.getActive().put(key.getAlgorithm(), key.getKid());
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
        r.setAlgorithm(key.getAlgorithm());
        r.setPublicKey(key.getPublicKey() != null ? PemUtils.encodeKey(key.getPublicKey()) : null);
        r.setCertificate(key.getCertificate() != null ? PemUtils.encodeCertificate(key.getCertificate()) : null);
        r.setUse(key.getUse());
        return r;
    }
}
