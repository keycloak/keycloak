/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.representations.info;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.KeystoreUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CryptoInfoRepresentation {

    private String cryptoProvider;
    private List<String> supportedKeystoreTypes;

    private List<String> clientSignatureSymmetricAlgorithms;

    private List<String> clientSignatureAsymmetricAlgorithms;

    public static CryptoInfoRepresentation create(List<String> clientSignatureSymmetricAlgorithms, List<String> clientSignatureAsymmetricAlgorithms) {
        CryptoInfoRepresentation info = new CryptoInfoRepresentation();

        CryptoProvider cryptoProvider = CryptoIntegration.getProvider();
        info.cryptoProvider = cryptoProvider.getClass().getSimpleName();
        info.supportedKeystoreTypes = CryptoIntegration.getProvider().getSupportedKeyStoreTypes()
                .map(KeystoreUtil.KeystoreFormat::toString)
                .collect(Collectors.toList());
        info.clientSignatureSymmetricAlgorithms = clientSignatureSymmetricAlgorithms;
        info.clientSignatureAsymmetricAlgorithms = clientSignatureAsymmetricAlgorithms;

        return info;
    }

    public String getCryptoProvider() {
        return cryptoProvider;
    }

    public void setCryptoProvider(String cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public List<String> getSupportedKeystoreTypes() {
        return supportedKeystoreTypes;
    }

    public void setSupportedKeystoreTypes(List<String> supportedKeystoreTypes) {
        this.supportedKeystoreTypes = supportedKeystoreTypes;
    }

    public List<String> getClientSignatureSymmetricAlgorithms() {
        return clientSignatureSymmetricAlgorithms;
    }

    public void setClientSignatureSymmetricAlgorithms(List<String> clientSignatureSymmetricAlgorithms) {
        this.clientSignatureSymmetricAlgorithms = clientSignatureSymmetricAlgorithms;
    }

    public List<String> getClientSignatureAsymmetricAlgorithms() {
        return clientSignatureAsymmetricAlgorithms;
    }

    public void setClientSignatureAsymmetricAlgorithms(List<String> clientSignatureAsymmetricAlgorithms) {
        this.clientSignatureAsymmetricAlgorithms = clientSignatureAsymmetricAlgorithms;
    }
}
