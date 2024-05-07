/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml.attributeQuery;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AttributeQueryType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.SubjectQueryAbstractType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.protocol.saml.attributeQuery.client.SAMLAttributeQueryClient;
import org.keycloak.protocol.saml.attributeQuery.client.SAMLAttributeQueryClientConfig;
import org.keycloak.protocol.saml.attributeQuery.server.SamlAttributeQueryServerConfig;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.saml.v2.util.SubjectUtil;
import org.keycloak.saml.validators.ConditionsValidator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * @author Ben Cresitello-Dittmar
 * Common utilities for processing SAML2 attribute query requests and responses
 */
public class SamlAttributeQueryUtils {
    private static final Logger logger = Logger.getLogger(SAMLAttributeQueryClient.class);

    // PARSERS

    /**
     * Parse the SAML2 object into an attribute query request object
     * @return The parsed attribute query request
     * @throws ParsingException thrown if the SAML2 object does not represent a valid attribute query request
     */
    public static AttributeQueryType parseAttributeQueryRequest(SAML2Object req) throws ParsingException {
        if (!(req instanceof AttributeQueryType)) {
            throw new ParsingException("failed to parse request as attribute query");
        }

        return (AttributeQueryType) req;
    }

    /**
     * Parse the SOAP message into a SAML2 response object
     * @param response The SOAP message to parse
     * @return the parsed document
     * @throws ParsingException thrown if the SOAP message does not represent a valid SAML2 response
     */
    public static SAMLDocumentHolder parseAttributeQueryResponse(SOAPMessage response) throws ParsingException {
        Document resDoc;
        SOAPBody soapBody;

        if (response == null) {
            throw new ParsingException("attribute query response was null");
        }
        try {
            soapBody = response.getSOAPBody();
            resDoc = soapBody.extractContentAsDocument();
        } catch (SOAPException ex) {
            throw new ParsingException("failed to extract SOAP message body: " + ex.getMessage(), ex);
        }

        SAMLDocumentHolder holder;
        try {
            holder = SAMLRequestParser.parseResponseDocument(DocumentUtil.getDocumentAsString(resDoc).getBytes());
        } catch (ProcessingException | ConfigurationException ex){
            throw new ParsingException("failed to parse document as SAML: " + ex.getMessage(), ex);
        }
        return holder;
    }

    // HELPERS

    /**
     * Sign and encrypt the provided document using the provided SAML client and configuration
     * @param session the keycloak session the signature is taking place in
     * @param config the attribute query config
     * @param samlClient the SAML client to use for signing the document
     * @param doc the document to sign
     * @throws ProcessingException thrown if the document cannot be signed
     */
    public static void signAndEncryptDoc(KeycloakSession session, BaseSamlAttributeQueryConfig config, SamlClient samlClient, Document doc) throws ProcessingException {
        PublicKey idpEncryptionKey;
        PrivateKey clientSigningPrivateKey;
        X509Certificate clientSigningCert;
        PublicKey clientSigningPublicKey;

        JaxrsSAML2BindingBuilder bindingBuilder = new JaxrsSAML2BindingBuilder(session);

        // parse signing keys
        if (config.isSignDocument() || config.isSignAssertion()){
            try {
                clientSigningPrivateKey = PemUtils.decodePrivateKey(samlClient.getClientSigningPrivateKey());
                clientSigningCert = PemUtils.decodeCertificate(samlClient.getClientSigningCertificate());
                clientSigningPublicKey = SamlProtocolUtils.getPublicKey(samlClient.getClientSigningCertificate());
            } catch (PemException | VerificationException ex){
                throw new ProcessingException("invalid signing certificate: " + ex.getMessage(), ex);
            }

            // set signing configuration
            String canonicalization = samlClient.getCanonicalizationMethod();
            if (canonicalization != null) {
                bindingBuilder.canonicalizationMethod(canonicalization);
            }
            bindingBuilder.signatureAlgorithm(samlClient.getSignatureAlgorithm()).signWith(null, clientSigningPrivateKey, clientSigningPublicKey, clientSigningCert);
        }

        // parse encryption keys
        if ((config.isEncryptSubject() || config.isEncryptAssertion())){
            try {
                idpEncryptionKey = SamlProtocolUtils.getPublicKey(config.getIdpEncryptionCert());
            } catch (VerificationException ex){
                throw new ProcessingException("invalid encryption certificate");
            }

            // set encryption configuration
            bindingBuilder.encryptWith(idpEncryptionKey);
        }

        if (config.isSignAssertion()){
            bindingBuilder.signAssertion(doc);
        }

        if (config.isEncryptSubject()){
            bindingBuilder.encryptSubject();
            bindingBuilder.encryptSubject(doc);
        }

        if (config.isEncryptAssertion()){
            bindingBuilder.encrypt();
            bindingBuilder.encryptDocument(doc);
        }

        if (config.isSignDocument()){
            bindingBuilder.signDocument(doc);
        }
    }

    /**
     * Get the keycloak user the provided subject is referencing. If a subjectAttribute is configured then the user will
     * be looked up by the specified attribute, otherwise, the user will be looked up by username.
     * @param session
     * @param config
     * @param subject
     * @return
     */
    public static UserModel getUserFromSubject(KeycloakSession session, BaseSamlAttributeQueryConfig config, String subject){
        UserModel user;
        if (config.getSubjectAttribute() == null) {
            user = session.users().getUserByUsername(session.getContext().getRealm(), subject);
        } else {
            user = session.users().searchForUserByUserAttributeStream(session.getContext().getRealm(), config.getSubjectAttribute(), subject).findFirst().orElse(null);
        }

        return user;
    }

    /**
     * Get the SAML2 attribute query URL for the current realm
     * @param session the keycloak session this request is taking place in
     * @return the attribute query URL
     */
    public static String getAttributeQueryUrl(KeycloakSession session){
        final String realmName = session.getContext().getRealm().getName();
        final URI baseUri = session.getContext().getUri().getBaseUri();
        return Urls.samlAttributeEndpoint(baseUri, realmName).toString();
    }

    /**
     * Decrypt the assertion in the provided document using the given client and configuration
     * @param config the attribute query configuration
     * @param samlClient the client containing the decryption keys
     * @param holder the document to decrypt
     * @return the decrypted element
     * @throws VerificationException thrown if encryption is required in the config but the document is not encrypted
     * @throws ProcessingException thrown if the document failed to decrypt
     */
    public static Element decryptAssertion(BaseSamlAttributeQueryConfig config, SamlClient samlClient, SAMLDocumentHolder holder) throws ProcessingException{
        Element assertionElement;
        ResponseType responseType = (ResponseType) holder.getSamlObject();

        // decrypt assertion
        try {
            boolean isEncrypted = AssertionUtil.isAssertionEncrypted(responseType);
            if (config.isRequireEncryption() && !isEncrypted){
                throw new ProcessingException("encryption was required but SAML assertion is not encrypted.");
            }

            if (isEncrypted) {
                assertionElement = AssertionUtil.decryptAssertion(responseType, PemUtils.decodePrivateKey(samlClient.getClientEncryptingPrivateKey()));
            } else {
                assertionElement = DocumentUtil.getElement(holder.getSamlDocument(), new QName(JBossSAMLConstants.ASSERTION.get()));
            }
        } catch (ParsingException | ConfigurationException | ProcessingException ex) {
            throw new ProcessingException("failed to decrypt request: " + ex.getMessage());
        }

        return assertionElement;
    }

    /**
     * Decrypt the provided subject using the given client and configuration
     * @param config the attribute query configuration
     * @param samlClient the client containing the decryption keys
     * @param subjectQueryType the subject to decrypt
     * @throws VerificationException thrown if encryption is required in the config but the document is not encrypted
     * @throws ProcessingException thrown if the document failed to decrypt
     */
    public static void decryptSubject(SamlAttributeQueryServerConfig config, SamlClient samlClient, SubjectQueryAbstractType subjectQueryType) throws ProcessingException {
        if (config.isRequireEncryption() && !SubjectUtil.isSubjectEncrypted(subjectQueryType)){
            throw new ProcessingException("encryption required but subject is not encrypted");
        }

        if (SubjectUtil.isSubjectEncrypted(subjectQueryType)) {
            try {
                PrivateKey privateKey = PemUtils.decodePrivateKey(samlClient.getClientEncryptingPrivateKey());
                SubjectUtil.decryptSubject(subjectQueryType, privateKey);
            } catch (ProcessingException | ParsingException | ConfigurationException ex) {
                logger.errorf("failed to decrypt subject: %s", ex);
                throw new ProcessingException("failed to decrypt subject");
            }
        }
    }

    // VERIFIERS

    /**
     * Decrypt and verify the provided attribute query response
     * @param session the keycloak session this response is being processed in
     * @param config the attribute query configuration
     * @param samlClient the client containing the decryption keys
     * @param holder the attribute query response to decrypt and verify
     * @throws VerificationException thrown if the response is not valid
     * @throws ProcessingException thrown if the document fails to decrypt or the configuration requires a signature or encryption but they are not present
     */
    public static void decryptAndVerifyResponse(KeycloakSession session, SAMLAttributeQueryClientConfig config, SamlClient samlClient, SAMLDocumentHolder holder) throws VerificationException, ProcessingException{
        ResponseType responseType = (ResponseType) holder.getSamlObject();

        // verify document
        SamlAttributeQueryUtils.verifyIssuer(config, responseType);
        SamlAttributeQueryUtils.verifySuccess(responseType);
        SamlAttributeQueryUtils.verifyAssertionExists(responseType);
        if (config.isRequireDocumentSignature()){
            SamlAttributeQueryUtils.verifyDocumentSignature(config, holder.getSamlDocument());
        }

        Element assertionElement = SamlAttributeQueryUtils.decryptAssertion(config, samlClient, holder);

        // verify assertion signature
        if (config.isRequireAssertionSignature() && !AssertionUtil.isSignatureValid(assertionElement, new HardcodedKeyLocator(SamlProtocolUtils.getPublicKey(config.getIdpSigningCert())))) {
            throw new VerificationException("invalid assertion signature");
        }

        // verify assertion contents
        AssertionType assertionType = responseType.getAssertions().get(0).getAssertion();
        SamlAttributeQueryUtils.verifyAssertion(assertionType, config.getIssuer());
        SamlAttributeQueryUtils.verifyIssuer(config, assertionType);

    }

    /**
     * Decrypt and verify the provided attribute query request
     * @param session the keycloak session this request is being processed in
     * @param config the attribute query configuration
     * @param holder the attribute query request to decrypt and verify
     * @param attributeQueryType  the attribute query request to decrypt and verify
     * @throws VerificationException thrown if the request is not valid
     * @throws ProcessingException thrown if the document fails to decrypt or the configuration requires a signature or encryption but they are not present
     */
    public static void decryptAndVerifyRequest(KeycloakSession session, SamlAttributeQueryServerConfig config, SAMLDocumentHolder holder, AttributeQueryType attributeQueryType) throws VerificationException, ProcessingException {
        if (config.isRequireDocumentSignature()){
            SamlAttributeQueryUtils.verifyDocumentSignature(config, holder.getSamlDocument());
        }

        SamlAttributeQueryUtils.verifyIssuer(config, attributeQueryType);
        SamlAttributeQueryUtils.verifyDestination(session, attributeQueryType);
        SamlClient client = SamlAttributeQueryUtils.verifyClient(session.getContext().getRealm(), config);
        SamlAttributeQueryUtils.decryptSubject(config, client, attributeQueryType);
        SamlAttributeQueryUtils.verifySubjectExists(attributeQueryType);
    }

    /**
     * Verify the signature on the document using the certificate in the provided configuration
     * @param doc the signed attribute query document
     * @throws VerificationException thrown if the signature is invalid
     */
    public static void verifyDocumentSignature(BaseSamlAttributeQueryConfig config, Document doc) throws VerificationException{
        try {
            SamlProtocolUtils.verifyDocumentSignature(doc, new HardcodedKeyLocator(SamlProtocolUtils.getPublicKey(config.getIdpSigningCert())));
        } catch (VerificationException ex){
            throw new VerificationException(String.format("failed to verify message signature: %s", ex.getMessage()));
        }
    }

    /**
     * Verify the attribute query response includes an assertion
     * @param rt the attribute query response
     * @throws VerificationException thrown if no assertions are found in the response
     */
    public static void verifyAssertionExists(ResponseType rt) throws VerificationException{
        if (rt.getAssertions() == null || rt.getAssertions().isEmpty()) {
            throw new VerificationException("no assertions found in response");
        }
    }

    /**
     * Verify the subject contains a NameID value
     * @param subjectQueryType the subject
     * @throws VerificationException thrown if the NameID doesn't exist or is empty
     */
    public static void verifySubjectExists(SubjectQueryAbstractType subjectQueryType) throws VerificationException{
        NameIDType nt = (NameIDType)subjectQueryType.getSubject().getSubType().getBaseID();

        if (nt == null || nt.getValue() == null || nt.getValue().isEmpty()){
            throw new VerificationException("subject cannot be empty");
        }
    }

    /**
     * Verify the issuer of the response matches the expected issuer from the configuration
     * @param config the configuration
     * @param rt the attribute query response
     * @throws VerificationException thrown if the issuer is not valid
     */
    public static void verifyIssuer(SAMLAttributeQueryClientConfig config, ResponseType rt) throws VerificationException{
        String issuer = rt.getIssuer().getValue();
        String expected = config.getExpectedIssuer();
        if (!issuer.equals(expected)){
            throw new VerificationException(String.format("response issuer is not valid. Got %s but expected %s", issuer, expected));
        }
    }



    /**
     * Verify the assertion issuer matches the expected issuer from the config
     * @param config the configuration
     * @param at the assertion
     * @throws VerificationException thrown if the issuer is invalid
     */
    public static void verifyIssuer(SAMLAttributeQueryClientConfig config, AssertionType at) throws VerificationException{
        String issuer = at.getIssuer().getValue();
        String expected = config.getExpectedIssuer();
        if (!issuer.equals(expected)){
            throw new VerificationException(String.format("assertion issuer is not valid. Got %s but expected %s", issuer, expected));
        }
    }

    /**
     * Verify the attribute query response issuer matches the expected issuer from the config
     * @param config the configuration
     * @param req the attribute query response
     * @throws VerificationException thrown if the issuer is invalid
     */
    public static void verifyIssuer(BaseSamlAttributeQueryConfig config, AttributeQueryType req) throws VerificationException {
        // verify issuer
        if (!req.getIssuer().getValue().equals(config.getExpectedIssuer())){
            throw new VerificationException(String.format("invalid issuer got %s but expected %s", req.getIssuer().getValue(), config.getExpectedIssuer()));
        }
    }

    /**
     * Verify the conditions specified in the assertion. This includes audience verification.
     * @param at The assertion to validate
     * @param expectedAudience The audience to expect in the assertion
     * @throws VerificationException thrown if the assertion audience is invalid
     */
    public static void verifyAssertion(AssertionType at, String expectedAudience) throws VerificationException{
        // create validator
        ConditionsValidator.Builder cvb = new ConditionsValidator.Builder(at.getID(), at.getConditions(), DestinationValidator.forProtocolMap(null)).addAllowedAudience(URI.create(expectedAudience));

        // validate assertion
        if (!cvb.build().isValid()) {
            throw new VerificationException("assertion is invalid");
        }
    }

    /**
     * Verify the destination of the request is keycloak's attribute query endpoint
     * @param session the keycloak session
     * @param req the attribute query request
     * @throws VerificationException thrown if the destination is invalid
     */
    public static void verifyDestination(KeycloakSession session, RequestAbstractType req) throws VerificationException {
        if (!req.getDestination().toString().equals(SamlAttributeQueryUtils.getAttributeQueryUrl(session))){
            throw new VerificationException(String.format("invalid destination got %s but expected %s", req.getDestination().toString(), SamlAttributeQueryUtils.getAttributeQueryUrl(session)));
        }
    }

    /**
     * Verify the provided document is in response to the provided request ID
     * @param holder the attribute query response
     * @param expectedRequestId the ID of the request the response is in response to
     * @throws VerificationException thrown if the response to ID is invalid
     */
    public static void verifyResponseTo(SAMLDocumentHolder holder, String expectedRequestId) throws VerificationException{
        ResponseType responseType = (ResponseType) holder.getSamlObject();
        if (responseType.getInResponseTo() == null || !responseType.getInResponseTo().equals(expectedRequestId)){
            throw new VerificationException(String.format("expected response to SAML request %s but got %s", expectedRequestId, responseType.getInResponseTo()));
        }
    }

    /**
     * Verify the provided attribute query response's status is success
     * @param rt the attribute query response
     * @throws VerificationException thrown if the status is not success
     */
    public static void verifySuccess(ResponseType rt) throws VerificationException{
        if (rt == null
                || rt.getStatus() == null
                || rt.getStatus().getStatusCode() == null
                || rt.getStatus().getStatusCode().getValue() == null
                || !Objects.equals(rt.getStatus().getStatusCode().getValue().toString(), JBossSAMLURIConstants.STATUS_SUCCESS.get())) {
            String statusMessage = (rt == null || rt.getStatus() == null || rt.getStatus().getStatusMessage() == null) ? Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR : rt.getStatus().getStatusMessage();
            throw new VerificationException(String.format("attribute request failed: %s", statusMessage));
        }
    }

    /**
     * Verify the client in the provided configuration exists in the given realm
     * @param realm the keycloak realm
     * @param config the configuration
     * @return the SAML client
     * @throws VerificationException thrown if the client could not be found
     */
    public static SamlClient verifyClient(RealmModel realm, BaseSamlAttributeQueryConfig config) throws VerificationException{
        ClientModel client = realm.getClientByClientId(config.getClientId());
        if (client == null){
            logger.errorf("client %s could not be found", config.getClientId());
            throw new VerificationException("client not found");
        }

        return new SamlClient(client);
    }
}
