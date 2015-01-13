/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.saml;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.AuthenticationResponse;
import org.keycloak.broker.provider.FederatedIdentity;
import org.picketlink.common.constants.JBossSAMLConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.common.util.StaxParserUtil;
import org.picketlink.identity.federation.api.saml.v2.request.SAML2Request;
import org.picketlink.identity.federation.api.saml.v2.response.SAML2Response;
import org.picketlink.identity.federation.api.saml.v2.sig.SAML2Signature;
import org.picketlink.identity.federation.core.parsers.saml.SAMLParser;
import org.picketlink.identity.federation.core.saml.v2.common.IDGenerator;
import org.picketlink.identity.federation.core.saml.v2.common.SAMLDocumentHolder;
import org.picketlink.identity.federation.core.util.JAXPValidationUtil;
import org.picketlink.identity.federation.core.util.XMLEncryptionUtil;
import org.picketlink.identity.federation.core.util.XMLSignatureUtil;
import org.picketlink.identity.federation.saml.v2.assertion.AssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.EncryptedAssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.NameIDType;
import org.picketlink.identity.federation.saml.v2.assertion.SubjectType;
import org.picketlink.identity.federation.saml.v2.assertion.SubjectType.STSubType;
import org.picketlink.identity.federation.saml.v2.protocol.AuthnRequestType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType.RTChoiceType;
import org.picketlink.identity.federation.saml.v2.protocol.StatusCodeType;
import org.picketlink.identity.federation.saml.v2.protocol.StatusDetailType;
import org.picketlink.identity.federation.saml.v2.protocol.StatusType;
import org.picketlink.identity.federation.web.util.PostBindingUtil;
import org.picketlink.identity.federation.web.util.RedirectBindingUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URLDecoder;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProvider extends AbstractIdentityProvider<SAMLIdentityProviderConfig> {

    private static final String SAML_REQUEST_PARAMETER = "SAMLRequest";
    private static final String SAML_RESPONSE_PARAMETER = "SAMLResponse";
    private static final String RELAY_STATE_PARAMETER = "RelayState";

    private SAML2Signature saml2Signature = new SAML2Signature();

    public SAMLIdentityProvider(SAMLIdentityProviderConfig config) {
        super(config);
    }

    @Override
    public AuthenticationResponse handleRequest(AuthenticationRequest request) {
        try {
            UriInfo uriInfo = request.getUriInfo();
            String issuerURL = UriBuilder.fromUri(uriInfo.getBaseUri()).build().toString();
            String destinationUrl = getConfig().getSingleSignOnServiceUrl();
            SAML2Request samlRequest = new SAML2Request();
            String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();

            if (nameIDPolicyFormat == null) {
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }

            samlRequest.setNameIDFormat(nameIDPolicyFormat);

            AuthnRequestType authn = samlRequest
                    .createAuthnRequestType(IDGenerator.create("ID_"), request.getRedirectUri(), destinationUrl, issuerURL);

            authn.setProtocolBinding(URI.create(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get()));
            authn.setForceAuthn(getConfig().isForceAuthn());

            Document authnDoc = samlRequest.convert(authn);

            if (getConfig().isWantAuthnRequestsSigned()) {
                PrivateKey privateKey = request.getRealm().getPrivateKey();
                PublicKey publicKey = request.getRealm().getPublicKey();

                if (privateKey == null) {
                    throw new RuntimeException("Identity Provider [" + getConfig().getName() + "] wants a signed authentication request. But the Realm [" + request.getRealm().getName() + "] does not have a private key.");
                }

                if (publicKey == null) {
                    throw new RuntimeException("Identity Provider [" + getConfig().getName() + "] wants a signed authentication request. But the Realm [" + request.getRealm().getName() + "] does not have a public key.");
                }

                KeyPair keypair = new KeyPair(publicKey, privateKey);

                this.saml2Signature.signSAMLDocument(authnDoc, keypair);
            }

            byte[] responseBytes = DocumentUtil.getDocumentAsString(authnDoc).getBytes("UTF-8");
            String urlEncodedResponse = RedirectBindingUtil.deflateBase64URLEncode(responseBytes);
            URI redirectUri = UriBuilder.fromPath(destinationUrl)
                    .queryParam(SAML_REQUEST_PARAMETER, urlEncodedResponse)
                    .queryParam(RELAY_STATE_PARAMETER, request.getState()).build();

            return AuthenticationResponse.temporaryRedirect(redirectUri);
        } catch (Exception e) {
            throw new RuntimeException("Could not create authentication request.", e);
        }
    }

    @Override
    public String getRelayState(AuthenticationRequest request) {
        HttpRequest httpRequest = request.getHttpRequest();
        return httpRequest.getFormParameters().getFirst(RELAY_STATE_PARAMETER);
    }

    @Override
    public AuthenticationResponse handleResponse(AuthenticationRequest request) {
        HttpRequest httpRequest = request.getHttpRequest();
        String samlResponse = httpRequest.getFormParameters().getFirst(SAML_RESPONSE_PARAMETER);

        if (samlResponse == null) {
            throw new RuntimeException("No response from SAML identity provider.");
        }

        try {
            SAML2Request saml2Request = new SAML2Request();
            ResponseType responseType = (ResponseType) saml2Request
                    .getSAML2ObjectFromStream(PostBindingUtil.base64DecodeAsStream(URLDecoder.decode(samlResponse, "UTF-8")));
            AssertionType assertion = getAssertion(request, saml2Request, responseType);

            SubjectType subject = assertion.getSubject();
            STSubType subType = subject.getSubType();
            NameIDType subjectNameID = (NameIDType) subType.getBaseID();

            FederatedIdentity user = new FederatedIdentity(subjectNameID.getValue());

            user.setUsername(subjectNameID.getValue());

            if (subjectNameID.getFormat().toString().equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
                user.setEmail(subjectNameID.getValue());
            }

            return AuthenticationResponse.end(user);
        } catch (Exception e) {
            throw new RuntimeException("Could not process response from SAML identity provider.", e);
        }
    }

    private AssertionType getAssertion(AuthenticationRequest request, SAML2Request saml2Request, ResponseType responseType) throws ProcessingException {
        validateStatusResponse(responseType);
        validateSignature(saml2Request);

        List<RTChoiceType> assertions = responseType.getAssertions();

        if (assertions.isEmpty()) {
            throw new RuntimeException("No assertion from response.");
        }

        RTChoiceType rtChoiceType = assertions.get(0);
        EncryptedAssertionType encryptedAssertion = rtChoiceType.getEncryptedAssertion();

        if (encryptedAssertion != null) {
            decryptAssertion(responseType, request.getRealm().getPrivateKey());

        }

        return responseType.getAssertions().get(0).getAssertion();
    }

    private void validateSignature(SAML2Request saml2Request) throws ProcessingException {
        if (getConfig().isValidateSignature()) {
            X509Certificate certificate = XMLSignatureUtil.getX509CertificateFromKeyInfoString(getConfig().getSigningPublicKey().replaceAll("\\s", ""));
            SAMLDocumentHolder samlDocumentHolder = saml2Request.getSamlDocumentHolder();
            Document samlDocument = samlDocumentHolder.getSamlDocument();

            this.saml2Signature.validate(samlDocument, certificate.getPublicKey());
        }
    }

    private void validateStatusResponse(ResponseType responseType) {
        StatusType status = responseType.getStatus();
        StatusCodeType statusCode = status.getStatusCode();

        if (!JBossSAMLURIConstants.STATUS_SUCCESS.get().equals(statusCode.getValue().toString())) {
            StatusDetailType statusDetailType = status.getStatusDetail();
            StringBuilder detailMessage = new StringBuilder();

            if (statusDetailType != null) {
                for (Object statusDetail : statusDetailType.getAny()) {
                    detailMessage.append(statusDetail);
                }
            } else {
                detailMessage.append("none");
            }

            throw new RuntimeException("Authentication failed with code [" + statusCode.getValue() + " and detail [" + detailMessage.toString() + ".");
        }
    }

    private ResponseType decryptAssertion(ResponseType responseType, PrivateKey privateKey) throws ProcessingException {
        SAML2Response saml2Response = new SAML2Response();

        try {
            Document doc = saml2Response.convert(responseType);
            Element enc = DocumentUtil.getElement(doc, new QName(JBossSAMLConstants.ENCRYPTED_ASSERTION.get()));

            if (enc == null) {
                throw new RuntimeException("No encrypted assertion found.");
            }

            String oldID = enc.getAttribute(JBossSAMLConstants.ID.get());
            Document newDoc = DocumentUtil.createDocument();
            Node importedNode = newDoc.importNode(enc, true);
            newDoc.appendChild(importedNode);

            Element decryptedDocumentElement = XMLEncryptionUtil.decryptElementInDocument(newDoc, privateKey);
            SAMLParser parser = new SAMLParser();

            JAXPValidationUtil.checkSchemaValidation(decryptedDocumentElement);
            AssertionType assertion = (AssertionType) parser.parse(StaxParserUtil.getXMLEventReader(DocumentUtil
                    .getNodeAsStream(decryptedDocumentElement)));

            responseType.replaceAssertion(oldID, new RTChoiceType(assertion));

            return responseType;
        } catch (Exception e) {
            throw new RuntimeException("Could not decrypt assertion.", e);
        }
    }

}
