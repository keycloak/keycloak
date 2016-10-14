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
import org.keycloak.keys.KeyMetadata;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeyManager;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.KeysMetadataRepresentation;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeyResource {

    private RealmModel realm;
    private KeycloakSession session;
    private RealmAuth auth;

    public KeyResource(RealmModel realm, KeycloakSession session, RealmAuth auth) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public KeysMetadataRepresentation getKeyMetadata() {
        auth.requireView();

        KeyManager keystore = session.keys();

        KeysMetadataRepresentation keys = new KeysMetadataRepresentation();
        keys.setActive(Collections.singletonMap(KeyMetadata.Type.RSA.name(), keystore.getActiveKey(realm).getKid()));

        List<KeysMetadataRepresentation.KeyMetadataRepresentation> l = new LinkedList<>();
        for (KeyMetadata m : session.keys().getKeys(realm, true)) {
            KeysMetadataRepresentation.KeyMetadataRepresentation r = new KeysMetadataRepresentation.KeyMetadataRepresentation();
            r.setProviderId(m.getProviderId());
            r.setProviderPriority(m.getProviderPriority());
            r.setKid(m.getKid());
            r.setStatus(m.getStatus() != null ? m.getStatus().name() : null);
            r.setType(m.getType() != null ? m.getType().name() : null);
            r.setPublicKey(PemUtils.encodeKey(m.getPublicKey()));
            r.setCertificate(PemUtils.encodeCertificate(m.getCertificate()));
            l.add(r);
        }

        keys.setKeys(l);

        return keys;
    }

}
