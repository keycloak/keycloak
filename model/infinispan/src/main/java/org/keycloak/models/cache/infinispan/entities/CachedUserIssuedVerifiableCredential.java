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
package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.IssuedVerifiableCredentialModel;

public class CachedUserIssuedVerifiableCredential {

    private final String id;
    private final String userId;
    private final String verifiableCredentialId;
    private final Long issuedAt;
    private final Long expiresAt;
    private final String clientId;
    private final String revision;

    public CachedUserIssuedVerifiableCredential(IssuedVerifiableCredentialModel credentialModel) {
        this.id = credentialModel.getId();
        this.userId = credentialModel.getUserId();
        this.verifiableCredentialId = credentialModel.getVerifiableCredentialId();
        this.issuedAt = credentialModel.getIssuedAt();
        this.expiresAt = credentialModel.getExpiresAt();
        this.clientId = credentialModel.getClientId();
        this.revision = credentialModel.getRevision();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getVerifiableCredentialId() {
        return verifiableCredentialId;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRevision() {
        return revision;
    }
}
