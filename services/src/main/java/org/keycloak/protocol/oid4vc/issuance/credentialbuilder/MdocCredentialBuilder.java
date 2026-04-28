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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.VCFormat;
import org.keycloak.mdoc.MdocValidityInfo;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

public class MdocCredentialBuilder implements CredentialBuilder {

    @Override
    public String getSupportedFormat() {
        return VCFormat.MSO_MDOC;
    }

    @Override
    public MdocCredentialBody buildCredentialBody(VerifiableCredential verifiableCredential,
                                                  CredentialBuildConfig credentialBuildConfig) throws CredentialBuilderException {
        // OID4VCI 1.0 Appendix A.2.2 calls the mDoc credential type identifier "doctype". Internally it shares
        // CredentialBuildConfig.credentialType with SD-JWT VC's "vct" because both fields identify the credential type.
        String docType = credentialBuildConfig.getCredentialType();
        if (docType == null || docType.isBlank()) {
            throw new CredentialBuilderException("mDoc issuance requires vc.verifiable_credential_type");
        }

        Instant issuanceInstant = verifiableCredential.getIssuanceDate();
        if (issuanceInstant == null) {
            throw new CredentialBuilderException("mDoc issuance requires issuanceDate");
        }

        Instant expirationInstant = verifiableCredential.getExpirationDate();
        if (expirationInstant == null) {
            throw new CredentialBuilderException("mDoc issuance requires expirationDate");
        }

        MdocValidityInfo validityInfo = MdocValidityInfo.issuedAt(issuanceInstant, expirationInstant);

        Map<String, Object> claims = new LinkedHashMap<>(verifiableCredential.getCredentialSubject().getClaims());
        return new MdocCredentialBody(docType, claims, validityInfo);
    }

    @Override
    public void contributeToMetadata(SupportedCredentialConfiguration credentialConfig, CredentialScopeModel credentialScope) {
        credentialConfig.setDocType(CredentialBuildConfig.resolveCredentialType(credentialScope));
    }
}
