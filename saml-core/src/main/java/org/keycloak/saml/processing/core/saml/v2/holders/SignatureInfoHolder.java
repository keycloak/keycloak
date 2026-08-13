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
package org.keycloak.saml.processing.core.saml.v2.holders;

/**
 * Holds information about signature
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jul 24, 2009
 */
public class SignatureInfoHolder {

    private byte[] signatureValue;

    private String sigAlg;

    public SignatureInfoHolder(byte[] signatureValue, String sigAlg) {
        super();
        this.signatureValue = signatureValue;
        this.sigAlg = sigAlg;
    }

    public byte[] getSignatureValue() {
        return signatureValue;
    }

    public String getSigAlg() {
        return sigAlg;
    }
}