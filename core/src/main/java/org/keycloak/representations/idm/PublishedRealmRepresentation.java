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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.common.util.PemUtils;

import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PublishedRealmRepresentation {
    protected String realm;

    @JsonProperty("public_key")
    protected String publicKeyPem;

    @JsonProperty("token-service")
    protected String tokenServiceUrl;

    @JsonProperty("account-service")
    protected String accountServiceUrl;

    @JsonProperty("tokens-not-before")
    protected int notBefore;

    @JsonIgnore
    protected volatile transient PublicKey publicKey;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
        this.publicKey = null;
    }


    @JsonIgnore
    public PublicKey getPublicKey() {
        if (publicKey != null) return publicKey;
        if (publicKeyPem != null) {
            try {
                publicKey = PemUtils.decodePublicKey(publicKeyPem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return publicKey;
    }

    @JsonIgnore
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        this.publicKeyPem = PemUtils.encodeKey(publicKey);
    }

    public String getTokenServiceUrl() {
        return tokenServiceUrl;
    }

    public void setTokenServiceUrl(String tokenServiceUrl) {
        this.tokenServiceUrl = tokenServiceUrl;
    }

    public String getAccountServiceUrl() {
        return accountServiceUrl;
    }

    public void setAccountServiceUrl(String accountServiceUrl) {
        this.accountServiceUrl = accountServiceUrl;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }
}
