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

package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;


/**
 * Builds verifiable credentials for the LDP_VC format.
 * {@see https://www.w3.org/TR/vc-data-model/}
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class LDCredentialBuilder implements CredentialBuilder {

    private static final String ID_CLAIM_KEY = "id";

    public LDCredentialBuilder() {
    }

    @Override
    public String getSupportedFormat() {
        return Format.LDP_VC;
    }

    @Override
    public LDCredentialBody buildCredentialBody(
            VerifiableCredential verifiableCredential,
            CredentialBuildConfig credentialBuildConfig
    ) throws CredentialBuilderException {
        // The default credential format is basically this format,
        // so not much is to be done.
        verifiableCredential.setIssuer(credentialBuildConfig.getCredentialIssuer());

        // Map the subject id claim to 'id'
        // We can't use claim name 'id' directly because it clashes with vc_id
        CredentialSubject subject = verifiableCredential.getCredentialSubject();
        Optional.ofNullable(subject.getClaims().remove(CLAIM_NAME_SUBJECT_ID)).ifPresent(id -> {
            subject.getClaims().put(ID_CLAIM_KEY, id);
        });

        return new LDCredentialBody(verifiableCredential);
    }
}
