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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.Key;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.keycloak.models.ClientModel;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLResponseWriter;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.w3c.dom.Document;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;

import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.w3c.dom.Element;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolUtils {

    /**
     * Verifies a signature of the given SAML document using settings for the given client.
     * Throws an exception if the client signature is expected to be present as per the client
     * settings and it is invalid, otherwise returns back to the caller.
     *
     * @param client
     * @param document
     * @throws VerificationException
     */
    public static void verifyDocumentSignature(ClientModel client, Document document) throws VerificationException {
        PublicKey publicKey = getSignatureValidationKey(client);
        verifyDocumentSignature(document, new HardcodedKeyLocator(publicKey));
    }

    /**
     * Verifies a signature of the given SAML document using keys obtained from the given key locator.
     * Throws an exception if the client signature is invalid, otherwise returns back to the caller.
     *
     * @param document
     * @param keyLocator
     * @throws VerificationException
     */
    public static void verifyDocumentSignature(Document document, KeyLocator keyLocator) throws VerificationException {
        SAML2Signature saml2Signature = new SAML2Signature();
        try {
            if (!saml2Signature.validate(document, keyLocator)) {
                throw new VerificationException("Invalid signature on document");
            }
        } catch (ProcessingException e) {
            throw new VerificationException("Error validating signature", e);
        }
    }

    /**
     * Returns public part of SAML signing key from the client settings.
     * @param client
     * @return Public key for signature validation.
     * @throws VerificationException
     */
    public static PublicKey getSignatureValidationKey(ClientModel client) throws VerificationException {
        return getPublicKey(new SamlClient(client).getClientSigningCertificate());
    }

    /**
     * Returns public part of SAML encryption key from the client settings.
     * @param client
     * @return Public key for encryption.
     * @throws VerificationException
     */
    public static PublicKey getEncryptionKey(ClientModel client) throws VerificationException {
        return getPublicKey(client, SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE);
    }

    public static PublicKey getPublicKey(ClientModel client, String attribute) throws VerificationException {
        String certPem = client.getAttribute(attribute);
        return getPublicKey(certPem);
    }

    private static PublicKey getPublicKey(String certPem) throws VerificationException {
        if (certPem == null) throw new VerificationException("Client does not have a public key.");
        X509Certificate cert = null;
        try {
            cert = PemUtils.decodeCertificate(certPem);
            cert.checkValidity();
        } catch (CertificateException ex) {
            throw new VerificationException("Certificate is not valid.");
        } catch (Exception e) {
            throw new VerificationException("Could not decode cert", e);
        }
        return cert.getPublicKey();
    }

    public static void verifyRedirectSignature(SAMLDocumentHolder documentHolder, KeyLocator locator, UriInfo uriInformation, String paramKey) throws VerificationException {
        MultivaluedMap<String, String> encodedParams = uriInformation.getQueryParameters(false);
        verifyRedirectSignature(documentHolder, locator, encodedParams, paramKey);
    }

    public static void verifyRedirectSignature(SAMLDocumentHolder documentHolder, KeyLocator locator, MultivaluedMap<String, String> encodedParams, String paramKey) throws VerificationException {
        String request = encodedParams.getFirst(paramKey);
        String algorithm = encodedParams.getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
        String signature = encodedParams.getFirst(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY);
        String relayState = encodedParams.getFirst(GeneralConstants.RELAY_STATE);

        if (request == null) throw new VerificationException("SAM was null");
        if (algorithm == null) throw new VerificationException("SigAlg was null");
        if (signature == null) throw new VerificationException("Signature was null");

        String keyId = getMessageSigningKeyId(documentHolder.getSamlObject());

        // Shibboleth doesn't sign the document for redirect binding.
        // todo maybe a flag?

        StringBuilder rawQueryBuilder = new StringBuilder().append(paramKey).append("=").append(request);
        if (encodedParams.containsKey(GeneralConstants.RELAY_STATE)) {
            rawQueryBuilder.append("&" + GeneralConstants.RELAY_STATE + "=").append(relayState);
        }
        rawQueryBuilder.append("&" + GeneralConstants.SAML_SIG_ALG_REQUEST_KEY + "=").append(algorithm);
        String rawQuery = rawQueryBuilder.toString();

        try {
            byte[] decodedSignature = RedirectBindingUtil.urlBase64Decode(signature);

            String decodedAlgorithm = RedirectBindingUtil.urlDecode(encodedParams.getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY));
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getFromXmlMethod(decodedAlgorithm);
            Signature validator = signatureAlgorithm.createSignature(); // todo plugin signature alg
            Key key = locator.getKey(keyId);
            if (key instanceof PublicKey) {
                validator.initVerify((PublicKey) key);
                validator.update(rawQuery.getBytes("UTF-8"));
            } else {
                throw new VerificationException("Invalid key locator for signature verification");
            }
            if (!validator.verify(decodedSignature)) {
                throw new VerificationException("Invalid query param signature");
            }
        } catch (Exception e) {
            throw new VerificationException(e);
        }
    }

    private static String getMessageSigningKeyId(SAML2Object doc) {
        final ExtensionsType extensions;
        if (doc instanceof RequestAbstractType) {
            extensions = ((RequestAbstractType) doc).getExtensions();
        } else if (doc instanceof StatusResponseType) {
            extensions = ((StatusResponseType) doc).getExtensions();
        } else {
            return null;
        }

        if (extensions == null) {
            return null;
        }

        for (Object ext : extensions.getAny()) {
            if (! (ext instanceof Element)) {
                continue;
            }

            String res = KeycloakKeySamlExtensionGenerator.getMessageSigningKeyIdFromElement((Element) ext);

            if (res != null) {
                return res;
            }
        }

        return null;
    }

    /**
     * Takes a saml object (an object that will be part of resulting ArtifactResponse), and inserts it as the body of 
     * an ArtifactResponse. The ArtifactResponse is returned as ArtifactResponseType
     *
     * @param samlObject a Saml object
     * @param issuer issuer of the resulting ArtifactResponse, should be the same as issuer of the samlObject
     * @param statusCode status code of the resulting response
     * @return An ArtifactResponse containing the saml object.
     */
    public static ArtifactResponseType buildArtifactResponse(SAML2Object samlObject, NameIDType issuer, URI statusCode) throws ConfigurationException, ProcessingException {
        ArtifactResponseType artifactResponse = new ArtifactResponseType(IDGenerator.create("ID_"),
                XMLTimeUtil.getIssueInstant());

        // Status
        StatusType statusType = new StatusType();
        StatusCodeType statusCodeType = new StatusCodeType();
        statusCodeType.setValue(statusCode);
        statusType.setStatusCode(statusCodeType);

        artifactResponse.setStatus(statusType);
        artifactResponse.setIssuer(issuer);
        artifactResponse.setAny(samlObject);

        return artifactResponse;
    }

    /**
     * Takes a saml object (an object that will be part of resulting ArtifactResponse), and inserts it as the body of 
     * an ArtifactResponse. The ArtifactResponse is returned as ArtifactResponseType
     * 
     * @param samlObject a Saml object
     * @param issuer issuer of the resulting ArtifactResponse, should be the same as issuer of the samlObject
     * @return An ArtifactResponse containing the saml object.
     */
    public static ArtifactResponseType buildArtifactResponse(SAML2Object samlObject, NameIDType issuer) throws ConfigurationException, ProcessingException {
        return buildArtifactResponse(samlObject, issuer, JBossSAMLURIConstants.STATUS_SUCCESS.getUri());
    }

    /**
     * Takes a saml document and inserts it as a body of ArtifactResponseType
     * @param document the document
     * @return An ArtifactResponse containing the saml document.
     */
    public static ArtifactResponseType buildArtifactResponse(Document document) throws ParsingException, ProcessingException, ConfigurationException {
        SAML2Object samlObject = SAML2Request.getSAML2ObjectFromDocument(document).getSamlObject();

        if (samlObject instanceof StatusResponseType) {
            return buildArtifactResponse(samlObject, ((StatusResponseType)samlObject).getIssuer());
        } else if (samlObject instanceof RequestAbstractType) {
            return buildArtifactResponse(samlObject, ((RequestAbstractType)samlObject).getIssuer());
        }
        
        throw new ProcessingException("SAMLObject was not StatusResponseType or LogoutRequestType");
    }

    /**
     * Convert a SAML2 ArtifactResponse into a Document
     * @param responseType an artifactResponse
     *
     * @return an artifact response converted to a Document
     *
     * @throws ParsingException
     * @throws ConfigurationException
     * @throws ProcessingException
     */
    public static Document convert(ArtifactResponseType responseType) throws ProcessingException, ConfigurationException,
            ParsingException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SAMLResponseWriter writer = new SAMLResponseWriter(StaxUtil.getXMLStreamWriter(bos));
        writer.write(responseType);
        return DocumentUtil.getDocument(new ByteArrayInputStream(bos.toByteArray()));
    }
}
