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

package org.keycloak.truststore;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FileTruststoreProvider implements TruststoreProvider {

    private final HostnameVerificationPolicy policy;
    private final KeyStore truststore;
    private final Map<X500Principal, X509Certificate> rootCertificates;
    private final Map<X500Principal, X509Certificate> intermediateCertificates;

    FileTruststoreProvider(KeyStore truststore, HostnameVerificationPolicy policy, Map<X500Principal, X509Certificate> rootCertificates, Map<X500Principal, X509Certificate> intermediateCertificates) {
        this.policy = policy;
        this.truststore = truststore;
        this.rootCertificates = rootCertificates;
        this.intermediateCertificates = intermediateCertificates;
    }

    @Override
    public HostnameVerificationPolicy getPolicy() {
        return policy;
    }

    @Override
    public KeyStore getTruststore() {
        return truststore;
    }

    @Override
    public Map<X500Principal, X509Certificate> getRootCertificates() {
        return rootCertificates;
    }

    @Override
    public Map<X500Principal, X509Certificate> getIntermediateCertificates() {
        return intermediateCertificates;
    }

    @Override
    public void close() {
    }
}
