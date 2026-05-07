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

package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import java.util.Map;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.mdoc.MdocCredential;
import org.keycloak.mdoc.MdocException;
import org.keycloak.mdoc.MdocIssuerSignedDocument;
import org.keycloak.mdoc.MdocValidityInfo;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;

public class MdocCredentialBody implements CredentialBody {

    private final MdocCredential credential;

    public MdocCredentialBody(String docType, Map<String, Object> claims, MdocValidityInfo validityInfo) {
        this.credential = new MdocCredential(docType, claims, validityInfo);
    }

    @Override
    public void addKeyBinding(JWK jwk) throws CredentialBuilderException {
        try {
            credential.addKeyBinding(jwk);
        } catch (MdocException e) {
            throw new CredentialBuilderException(e.getMessage(), e);
        }
    }

    public String getDocType() {
        return credential.getDocType();
    }

    public Map<String, Object> getClaims() {
        return credential.getClaims();
    }

    public MdocValidityInfo getValidityInfo() {
        return credential.getValidityInfo();
    }

    public MdocIssuerSignedDocument signAsIssuerSignedDocument(SignatureSignerContext signerContext) {
        try {
            return credential.signAsIssuerSignedDocument(signerContext);
        } catch (MdocException e) {
            throw new CredentialSignerException("Could not sign mDoc credential", e);
        }
    }
}
