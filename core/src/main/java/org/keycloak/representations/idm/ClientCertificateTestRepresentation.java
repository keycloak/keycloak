/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

public class ClientCertificateTestRepresentation {

    private String keyProviderId;
    private String clientId;
    private String tokenUrl;
    private String mtlsTokenUrl;

    public String getKeyProviderId() {
        return keyProviderId;
    }

    public void setKeyProviderId(String keyProviderId) {
        this.keyProviderId = keyProviderId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getMtlsTokenUrl() {
        return mtlsTokenUrl;
    }

    public void setMtlsTokenUrl(String mtlsTokenUrl) {
        this.mtlsTokenUrl = mtlsTokenUrl;
    }
}
