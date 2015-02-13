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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.AuthenticationResponse;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.protocol.saml.SAML2AuthnRequestBuilder;
import org.keycloak.protocol.saml.SAML2NameIDPolicyBuilder;
import org.keycloak.util.CollectionUtil;
import org.keycloak.util.MultivaluedHashMap;
import org.picketlink.common.constants.JBossSAMLConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.common.util.StaxParserUtil;
import org.picketlink.identity.federation.api.saml.v2.request.SAML2Request;
import org.picketlink.identity.federation.api.saml.v2.response.SAML2Response;
import org.picketlink.identity.federation.api.saml.v2.sig.SAML2Signature;
import org.picketlink.identity.federation.core.parsers.saml.SAMLParser;
import org.picketlink.identity.federation.core.saml.v2.common.SAMLDocumentHolder;
import org.picketlink.identity.federation.core.util.JAXPValidationUtil;
import org.picketlink.identity.federation.core.util.XMLEncryptionUtil;
import org.picketlink.identity.federation.core.util.XMLSignatureUtil;
import org.picketlink.identity.federation.saml.v2.assertion.*;
import org.picketlink.identity.federation.saml.v2.assertion.SubjectType.STSubType;
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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
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
    private static final Logger logger = Logger.getLogger(SAMLIdentityProvider.class);

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
            String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();

            if (nameIDPolicyFormat == null) {
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }

            String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

            if (getConfig().isPostBindingResponse()) {
                protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
            }

            SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
                    .assertionConsumerUrl(request.getRedirectUri())
                    .destination(destinationUrl)
                    .issuer(issuerURL)
                    .forceAuthn(getConfig().isForceAuthn())
                    .protocolBinding(protocolBinding)
                    .nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat))
                    .relayState(request.getState());

            if (getConfig().isWantAuthnRequestsSigned()) {
                PrivateKey privateKey = request.getRealm().getPrivateKey();
                PublicKey publicKey = request.getRealm().getPublicKey();

                if (privateKey == null) {
                    logger.error("Identity Provider [" + getConfig().getName() + "] wants a signed authentication request. But the Realm [" + request.getRealm().getName() + "] does not have a private key.");
                    throw new RuntimeException("Identity Provider [" + getConfig().getName() + "] wants a signed authentication request. But the Realm [" + request.getRealm().getName() + "] does not have a private key.");
                }

                if (publicKey == null) {
                    logger.error("Identity Provider [" + getConfig().getName() + "] wants a signed authentication request. But the Realm [" + request.getRealm().getName() + "] does not have a public key.");
                    throw new RuntimeException("Identity Provider [" + getConfig().getName() + "] wants a signed authentication request. But the Realm [" + request.getRealm().getName() + "] does not have a public key.");
                }

                KeyPair keypair = new KeyPair(publicKey, privateKey);

                authnRequestBuilder.signWith(keypair);
                authnRequestBuilder.signDocument();
            }

            if (getConfig().isPostBindingAuthnRequest()) {
                return AuthenticationResponse.fromResponse(authnRequestBuilder.postBinding().request());
            } else {
                return AuthenticationResponse.fromResponse(authnRequestBuilder.redirectBinding().request());
            }
        } catch (Exception e) {
            logger.error("Could not create authentication request", e);
            throw new RuntimeException("Could not create authentication request.", e);
        }
    }

    @Override
    public String getRelayState(AuthenticationRequest request) {
        return getRequestParameter(request, RELAY_STATE_PARAMETER);
    }

    @Override
    public AuthenticationResponse handleResponse(AuthenticationRequest request) {
        try {
            AssertionType assertion = getAssertion(request);
            SubjectType subject = assertion.getSubject();
            STSubType subType = subject.getSubType();
            NameIDType subjectNameID = (NameIDType) subType.getBaseID();
            FederatedIdentity identity = new FederatedIdentity(subjectNameID.getValue());

            identity.setUsername(subjectNameID.getValue());
            identity.setClaims(getClaims(assertion));

            identity.setEmail(identity.getClaims().getFirst("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));

            String givenname = identity.getClaims().getFirst("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname");
            String surname = identity.getClaims().getFirst("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname");
            identity.setName(String.format("%s %s", givenname, surname));

            if (subjectNameID.getFormat() != null && subjectNameID.getFormat().toString().equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
                identity.setEmail(subjectNameID.getValue());
            }

            return AuthenticationResponse.end(identity);
        } catch (Exception e) {
            logger.error("Could not process response from SAML identity provider.", e);
            throw new RuntimeException("Could not process response from SAML identity provider.", e);
        }
    }

    private MultivaluedHashMap<String, String> getClaims(AssertionType assertion) {
        MultivaluedHashMap<String, String> claims = new MultivaluedHashMap<String, String>();;

        AttributeStatementType attributes = (AttributeStatementType)CollectionUtils.find(assertion.getStatements(), new Predicate<StatementAbstractType>() {
            @Override
            public boolean evaluate(StatementAbstractType object) {
                return object instanceof AttributeStatementType;
            }
        });

        if(attributes != null) {
            for (AttributeStatementType.ASTChoiceType attr : attributes.getAttributes()) {
                for (Object value : attr.getAttribute().getAttributeValue()) {
                    claims.add(attr.getAttribute().getName(), value.toString());
                }
            }
        }

        //Add our claims
        claims.add("http://schemas.software.dell.com/DellIdentityBroker/claims/authenticatingIdp", getConfig().getId());

        return claims;
    }

    private AssertionType getAssertion(AuthenticationRequest request) throws Exception {
        String samlResponse = getRequestParameter(request, SAML_RESPONSE_PARAMETER);

        if (samlResponse == null) {
            throw new RuntimeException("No response from SAML identity provider.");
        }

        SAML2Request saml2Request = new SAML2Request();
        ResponseType responseType;

        if (getConfig().isPostBindingResponse()) {
            responseType = (ResponseType) saml2Request
                    .getSAML2ObjectFromStream(PostBindingUtil.base64DecodeAsStream(URLDecoder.decode(samlResponse, "UTF-8")));
        } else {
            responseType = (ResponseType) saml2Request
                    .getSAML2ObjectFromStream(RedirectBindingUtil.base64DeflateDecode((samlResponse)));
        }

        validateStatusResponse(responseType);
        validateSignature(saml2Request);

        List<RTChoiceType> assertions = responseType.getAssertions();

        if (assertions.isEmpty()) {
            logger.error("No assertion from response.");
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
            X509Certificate certificate = XMLSignatureUtil.getX509CertificateFromKeyInfoString(getConfig().getSigningCertificate().replaceAll("\\s", ""));
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

            logger.error("Authentication failed with code [" + statusCode.getValue() + " and detail [" + detailMessage.toString() + ".");
            throw new RuntimeException("Authentication failed with code [" + statusCode.getValue() + " and detail [" + detailMessage.toString() + ".");
        }
    }

    private ResponseType decryptAssertion(ResponseType responseType, PrivateKey privateKey) throws ProcessingException {
        SAML2Response saml2Response = new SAML2Response();

        try {
            Document doc = saml2Response.convert(responseType);
            Element enc = DocumentUtil.getElement(doc, new QName(JBossSAMLConstants.ENCRYPTED_ASSERTION.get()));

            if (enc == null) {
                logger.error("No encrypted assertion found.");
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
            logger.error("Could not decrypt assertion.", e);
            throw new RuntimeException("Could not decrypt assertion.", e);
        }
    }

    private String getRequestParameter(AuthenticationRequest request, String parameterName) {
        MultivaluedMap<String, String> requestParameters;

        if (getConfig().isPostBindingResponse()) {
            HttpRequest httpRequest = request.getHttpRequest();
            requestParameters = httpRequest.getFormParameters();
        } else {
            UriInfo uriInfo = request.getUriInfo();
            requestParameters = uriInfo.getQueryParameters();
        }

        return requestParameters.getFirst(parameterName);
    }
}
