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

/**
 * Holds all information required to build a uri to a credentials offer.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialOfferURI {
    private String issuer;
    private String nonce;

    public String getIssuer() {
        return issuer;
    }

    public CredentialOfferURI setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public CredentialOfferURI setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }
}