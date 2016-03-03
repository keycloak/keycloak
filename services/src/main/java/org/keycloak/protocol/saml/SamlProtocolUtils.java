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

package org.keycloak.protocol.saml;

import org.keycloak.common.VerificationException;
import org.keycloak.models.ClientModel;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.common.util.PemUtils;
import org.w3c.dom.Document;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolUtils {


    public static void verifyDocumentSignature(ClientModel client, Document document) throws VerificationException {
        SamlClient samlClient = new SamlClient(client);
        if (!samlClient.requiresClientSignature()) {
            return;
        }
        PublicKey publicKey = getSignatureValidationKey(client);
        verifyDocumentSignature(document, publicKey);
    }

    public static void verifyDocumentSignature(Document document, PublicKey publicKey) throws VerificationException {
        SAML2Signature saml2Signature = new SAML2Signature();
        try {
            if (!saml2Signature.validate(document, publicKey)) {
                throw new VerificationException("Invalid signature on document");
            }
        } catch (ProcessingException e) {
            throw new VerificationException("Error validating signature", e);
        }
    }

    public static PublicKey getSignatureValidationKey(ClientModel client) throws VerificationException {
        return getPublicKey(new SamlClient(client).getClientSigningCertificate());
    }

    public static PublicKey getEncryptionValidationKey(ClientModel client) throws VerificationException {
        return getPublicKey(client, SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE);
    }

    public static PublicKey getPublicKey(ClientModel client, String attribute) throws VerificationException {
        String certPem = client.getAttribute(attribute);
        return getPublicKey(certPem);
    }

    private static PublicKey getPublicKey(String certPem) throws VerificationException {
        if (certPem == null) throw new VerificationException("Client does not have a public key.");
        Certificate cert = null;
        try {
            cert = PemUtils.decodeCertificate(certPem);
        } catch (Exception e) {
            throw new VerificationException("Could not decode cert", e);
        }
        return cert.getPublicKey();
    }

    public static void verifyRedirectSignature(PublicKey publicKey, UriInfo uriInformation, String paramKey) throws VerificationException {
        MultivaluedMap<String, String> encodedParams = uriInformation.getQueryParameters(false);
        String request = encodedParams.getFirst(paramKey);
        String algorithm = encodedParams.getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
        String signature = encodedParams.getFirst(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY);
        String decodedAlgorithm = uriInformation.getQueryParameters(true).getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);

        if (request == null) throw new VerificationException("SAM was null");
        if (algorithm == null) throw new VerificationException("SigAlg was null");
        if (signature == null) throw new VerificationException("Signature was null");

        // Shibboleth doesn't sign the document for redirect binding.
        // todo maybe a flag?


        UriBuilder builder = UriBuilder.fromPath("/")
                .queryParam(paramKey, request);
        if (encodedParams.containsKey(GeneralConstants.RELAY_STATE)) {
            builder.queryParam(GeneralConstants.RELAY_STATE, encodedParams.getFirst(GeneralConstants.RELAY_STATE));
        }
        builder.queryParam(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY, algorithm);
        String rawQuery = builder.build().getRawQuery();

        try {
            byte[] decodedSignature = RedirectBindingUtil.urlBase64Decode(signature);

            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getFromXmlMethod(decodedAlgorithm);
            Signature validator = signatureAlgorithm.createSignature(); // todo plugin signature alg
            validator.initVerify(publicKey);
            validator.update(rawQuery.getBytes("UTF-8"));
            if (!validator.verify(decodedSignature)) {
                throw new VerificationException("Invalid query param signature");
            }
        } catch (Exception e) {
            throw new VerificationException(e);
        }
    }


}
