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

import java.util.ArrayList;
import java.util.List;

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

    @JsonProperty("credentials")
    private List<Credential> credentials;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("notification_id")
    private String notificationId;

    public List<Credential> getCredentials() {
        return credentials;
    }

    public CredentialResponse setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
        return this;
    }

    public CredentialResponse addCredential(Object credential) {
        if (this.credentials == null) {
            this.credentials = new ArrayList<>();
        }
        this.credentials.add(new Credential().setCredential(credential));
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public CredentialResponse setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public CredentialResponse setNotificationId(String notificationId) {
        this.notificationId = notificationId;
        return this;
    }


    /**
     * Inner class to represent a single credential object within the credentials array.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Credential {
        @JsonProperty("credential")
        private Object credential;

        public Object getCredential() {
            return credential;
        }

        public Credential setCredential(Object credential) {
            this.credential = credential;
            return this;
        }
    }
}
