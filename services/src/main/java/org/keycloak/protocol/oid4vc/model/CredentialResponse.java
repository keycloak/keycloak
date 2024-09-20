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

package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a CredentialResponse according to the OID4VCI Spec
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-response}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialResponse {

    // concrete type depends on the format
    private Object credential;

    @JsonProperty("c_nonce")
    private String cNonce;

    @JsonProperty("c_nonce_expires_in")
    private String cNonceExpiresIn;

    @JsonProperty("notification_id")
    private String notificationId;

    public Object getCredential() {
        return credential;
    }

    public CredentialResponse setCredential(Object credential) {
        this.credential = credential;
        return this;
    }

    public String getcNonce() {
        return cNonce;
    }

    public CredentialResponse setcNonce(String cNonce) {
        this.cNonce = cNonce;
        return this;
    }

    public String getcNonceExpiresIn() {
        return cNonceExpiresIn;
    }

    public CredentialResponse setcNonceExpiresIn(String cNonceExpiresIn) {
        this.cNonceExpiresIn = cNonceExpiresIn;
        return this;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public CredentialResponse setNotificationId(String notificationId) {
        this.notificationId = notificationId;
        return this;
    }
}