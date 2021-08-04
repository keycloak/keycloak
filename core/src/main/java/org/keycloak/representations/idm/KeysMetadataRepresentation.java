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

package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.crypto.KeyUse;
import org.keycloak.enums.AuthProtocol;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeysMetadataRepresentation {

    private Map<String, List<String>> active;

    private List<KeyMetadataRepresentation> keys;

    public Map<String, List<String>> getActive() {
        return active;
    }

    public void setActive(Map<String, List<String>> active) {
        this.active = active;
    }

    public void addActive(String alg, String id) {
        if(getActive()==null) setActive(new HashMap<String, List<String>>());
        List<String> activeIds = getActive().get(alg);
        if(activeIds == null) {
            activeIds = new ArrayList<String>();
            getActive().put(alg, activeIds);
        }
        activeIds.add(id);
    }

    public List<KeyMetadataRepresentation> getKeys() {
        return keys;
    }

    public void setKeys(List<KeyMetadataRepresentation> keys) {
        this.keys = keys;
    }

    public static class KeyMetadataRepresentation {
        private String providerId;
        private long providerPriority;

        private String kid;

        private String status;

        private String type;
        private String algorithm;

        private String publicKey;
        private String certificate;
        private List<KeyUse> uses;
        private List<AuthProtocol> authProtocols;

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public long getProviderPriority() {
            return providerPriority;
        }

        public void setProviderPriority(long providerPriority) {
            this.providerPriority = providerPriority;
        }

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }

        public List<KeyUse> getUses() {
            return uses;
        }

        public void setUses(List<KeyUse> uses) {
            this.uses = uses;
        }

        public List<AuthProtocol> getAuthProtocols() { return authProtocols; }

        public void setAuthProtocols(List<AuthProtocol> authProtocols) { this.authProtocols = authProtocols; }

    }
}
