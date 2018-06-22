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
package org.keycloak.crypto;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class KeyWrapper {

    private String providerId;
    private long providerPriority;
    private String kid;
    private Set<String> algorithms;
    private String type;
    private KeyUse use;
    private KeyStatus status;
    private SecretKey secretKey;
    private Key signKey;
    private Key verifyKey;
    private X509Certificate certificate;

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

    public Set<String> getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(String... algorithms) {
        this.algorithms = new HashSet<>();
        for (String a : algorithms) {
            this.algorithms.add(a);
        }
    }

    public void setAlgorithms(Set<String> algorithms) {
        this.algorithms = algorithms;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public KeyUse getUse() {
        return use;
    }

    public void setUse(KeyUse use) {
        this.use = use;
    }

    public KeyStatus getStatus() {
        return status;
    }

    public void setStatus(KeyStatus status) {
        this.status = status;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public Key getSignKey() {
        return signKey;
    }

    public void setSignKey(Key signKey) {
        this.signKey = signKey;
    }

    public Key getVerifyKey() {
        return verifyKey;
    }

    public void setVerifyKey(Key verifyKey) {
        this.verifyKey = verifyKey;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }
}
