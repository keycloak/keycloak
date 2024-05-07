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
package org.keycloak.protocol.saml.attributeQuery.client;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemException;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.attributeQuery.SamlAttributeQueryUtils;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.SAML2AttributeQueryRequestBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.Urls;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a helper class to perform the SAML attribute query request protocol
 */
public class SAMLAttributeQueryClient {

    KeycloakSession session;
    RealmModel realm;
    String subject;
    SAMLAttributeQueryClientConfig config;
    ClientModel client;
    SamlClient samlClient;

    public SAMLAttributeQueryClient(KeycloakSession session, RealmModel realm, String subject, SAMLAttributeQueryClientConfig config){
        this.session = session;
        this.realm = realm;
        this.subject = subject;
        this.config = config;
        this.client = realm.getClientByClientId(config.getClientId());
        if (client == null){
            this.client = realm.getClientById(config.getClientId());
        }
        this.samlClient = new SamlClient(client);
    }

    /**
     * Perform the SAML attribute query request and return the received attributes
     * @return the attributes received from the attribute query request
     * @throws ProcessingException thrown when the request fails
     */
    public Map<String, String> request() throws ProcessingException {
        // send attribute request
        SAMLDocumentHolder attrResponse;
        try {
            attrResponse = sendAttributeRequest();
        } catch (VerificationException | ProcessingException ex){
            throw new ProcessingException("failed to send attribute request: " + ex.getMessage(), ex);
        }

        // verify response
        try {
            SamlAttributeQueryUtils.decryptAndVerifyResponse(session, config, samlClient, attrResponse);
        } catch (VerificationException | ProcessingException ex){
            throw new ProcessingException("failed to decrypt and verify response: " + ex.getMessage(), ex);
        }

        return parseAttributes(attrResponse);
    }

    /**
     * A helper function to send the attribute query request to the external SOAP endpoint specified in the config
     * @return The SAML attribute query response from the external IdP
     * @throws VerificationException thrown when the response is invalid
     * @throws ProcessingException thrown when the request fails
     */
    private SAMLDocumentHolder sendAttributeRequest() throws VerificationException, ProcessingException{
        // build saml attribute query request
        SAML2AttributeQueryRequestBuilder reqBuilder = new SAML2AttributeQueryRequestBuilder()
                .subject(subject, config.getSubjectFormat())
                .issuer(config.getIssuer())
                .destination(config.getSoapEndpoint());
        String expectedRequestId = reqBuilder.createAttributeQueryRequest().getID();
        Document attributeQueryDoc = reqBuilder.toDocument();

        // sign and encrypt request
        try {
            SamlAttributeQueryUtils.signAndEncryptDoc(session, config, samlClient, attributeQueryDoc);
        } catch (ProcessingException | PemException ex){
            throw new ProcessingException("failed to sign and encrypt attribute query request: " + ex.getMessage(), ex);
        }

        // send request
        SOAPMessage response;
        try {
            response = sendSOAPRequest(attributeQueryDoc);
        } catch (SOAPException ex){
            throw new ProcessingException("failed to send SOAP request: " + ex.getMessage(), ex);
        }

        // parse response
        SAMLDocumentHolder holder;
        try {
            holder = SamlAttributeQueryUtils.parseAttributeQueryResponse(response);
        } catch (ParsingException ex){
            throw new ProcessingException("failed to parse soap response: " + ex.getMessage(), ex);
        }

        if (holder == null) {
            throw new ProcessingException("soap response is empty");
        }

        SamlAttributeQueryUtils.verifyResponseTo(holder, expectedRequestId);

        return holder;
    }

    /**
     * Send the specified document to the SOAP endpoint specified in the configuration
     * @param attributeQueryRequest The attribute query request document
     * @return The SOAP response
     * @throws SOAPException thrown when the SOAP request fails
     */
    private SOAPMessage sendSOAPRequest(Document attributeQueryRequest) throws SOAPException {
        // wrap message in SOAP format
        Soap.SoapMessageBuilder builder = Soap.createMessage();
        builder.addToBody(attributeQueryRequest);

        return builder.call(config.getSoapEndpoint());
    }

    /**
     * Parse the attribute from the attribute query response
     * @param holder the attribute query response
     * @return the attributes from the response assertions
     */
    private Map<String, String> parseAttributes(SAMLDocumentHolder holder){
        ResponseType responseType = (ResponseType) holder.getSamlObject();

        // extract all attributes from assertions
        Map<String, String> receivedAttrs = new HashMap<>();
        responseType.getAssertions().forEach(rt -> rt.getAssertion().getAttributeStatements().forEach(as -> as.getAttributes().forEach(asc -> {
            AttributeType attr = asc.getAttribute();
            String v = attr.getAttributeValue().get(0) == null ? null : attr.getAttributeValue().get(0).toString();
            receivedAttrs.put(attr.getName(), v);
        })));

        return receivedAttrs;
    }
}
