/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.wsfed.builders;

import org.keycloak.protocol.wsfed.writers.WSTrustResponseWriter;
import org.keycloak.wsfed.common.builders.WSFedResponseBuilder;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.Base64;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.picketlink.identity.federation.core.wstrust.wrappers.Lifetime;
import org.picketlink.identity.federation.core.wstrust.wrappers.RequestSecurityTokenResponse;
import org.picketlink.identity.federation.core.wstrust.wrappers.RequestSecurityTokenResponseCollection;
import org.picketlink.identity.federation.ws.addressing.AttributedURIType;
import org.picketlink.identity.federation.ws.addressing.EndpointReferenceType;
import org.picketlink.identity.federation.ws.policy.AppliesTo;
import org.picketlink.identity.federation.ws.trust.RequestedReferenceType;
import org.picketlink.identity.federation.ws.trust.RequestedSecurityTokenType;
import org.picketlink.identity.federation.ws.wss.secext.BinarySecurityTokenType;
import org.picketlink.identity.federation.ws.wss.secext.KeyIdentifierType;
import org.picketlink.identity.federation.ws.wss.secext.SecurityTokenReferenceType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class RequestSecurityTokenResponseBuilder extends WSFedResponseBuilder {
    protected String requestIssuer;
    protected int tokenExpiration;
    protected AssertionType samlToken;
    protected String jwt;

    protected SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RSA_SHA256;
    protected KeyPair signingKeyPair;
    protected X509Certificate signingCertificate;
    protected String canonicalizationMethodType = CanonicalizationMethod.EXCLUSIVE;

    public RequestSecurityTokenResponseBuilder() {
        setMethod(HttpMethod.POST);
    }

    public String getRequestIssuer() {
        return requestIssuer;
    }

    public RequestSecurityTokenResponseBuilder setRequestIssuer(String requestIssuer) {
        this.requestIssuer = requestIssuer;
        return this;
    }

    public int getTokenExpiration() {
        return tokenExpiration;
    }

    public RequestSecurityTokenResponseBuilder setTokenExpiration(int tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
        return this;
    }

    public AssertionType getSamlToken() {
        return samlToken;
    }

    public RequestSecurityTokenResponseBuilder setSamlToken(AssertionType samlToken) {
        this.samlToken = samlToken;
        return this;
    }

    public String getJwt() {
        return jwt;
    }

    public RequestSecurityTokenResponseBuilder setJwt(String jwt) {
        this.jwt = jwt;
        return this;
    }

    public KeyPair getSigningKeyPair() {
        return signingKeyPair;
    }

    public RequestSecurityTokenResponseBuilder setSigningKeyPair(KeyPair signingKeyPair) {
        this.signingKeyPair = signingKeyPair;
        return this;
    }

    public X509Certificate getSigningCertificate() {
        return signingCertificate;
    }

    public RequestSecurityTokenResponseBuilder setSigningCertificate(X509Certificate signingCertificate) {
        this.signingCertificate = signingCertificate;
        return this;
    }

    public String getCanonicalizationMethodType() {
        return canonicalizationMethodType;
    }

    public RequestSecurityTokenResponseBuilder setCanonicalizationMethodType(String canonicalizationMethodType) {
        this.canonicalizationMethodType = canonicalizationMethodType;
        return this;
    }

    @Override
    public RequestSecurityTokenResponseBuilder setDestination(String destination) {
        super.setDestination(destination);
        return this;
    }

    @Override
    public RequestSecurityTokenResponseBuilder setAction(String action) {
        super.setAction(action);
        return this;
    }

    @Override
    public RequestSecurityTokenResponseBuilder setRealm(String realm) {
        super.setRealm(realm);
        return this;
    }

    @Override
    public RequestSecurityTokenResponseBuilder setContext(String context) {
        super.setContext(context);
        return this;
    }


    public Response buildResponse() throws ProcessingException, org.picketlink.common.exceptions.ProcessingException, ConfigurationException, IOException {
        return buildResponse(getStringValue());
    }

    public RequestSecurityTokenResponse build() throws ConfigurationException, ProcessingException {
        RequestSecurityTokenResponse response = new RequestSecurityTokenResponse();
        response.setContext(context);

        XMLGregorianCalendar issueInstance = XMLTimeUtil.getIssueInstant();
        response.setLifetime(new Lifetime(issueInstance.toGregorianCalendar(), XMLTimeUtil.add(issueInstance, tokenExpiration * 1000).toGregorianCalendar()));
        response.setAppliesTo(new AppliesTo());
        EndpointReferenceType ert = new EndpointReferenceType();
        ert.setAddress(new AttributedURIType());
        ert.getAddress().setValue(requestIssuer);
        response.getAppliesTo().addAny(ert);
        response.setRequestedSecurityToken(new RequestedSecurityTokenType());

        response.setRequestType(URI.create("http://schemas.xmlsoap.org/ws/2005/02/trust/Issue"));

        if(samlToken != null) {
            //Sign token
            Document doc = AssertionUtil.asDocument(samlToken);
            doc = signAssertion(doc);

            response.getRequestedSecurityToken().add(doc.getDocumentElement());

            response.setRequestedUnattachedReference(new RequestedReferenceType());
            response.getRequestedUnattachedReference().setSecurityTokenReference(new SecurityTokenReferenceType());
            KeyIdentifierType ki = new KeyIdentifierType();
            ki.setValue(IDGenerator.create("ID_"));
            ki.setValueType("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID");
            response.getRequestedUnattachedReference().getSecurityTokenReference().addAny(ki);

            response.setTokenType(URI.create("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0"));
        }
        else if(jwt != null) {
            BinarySecurityTokenType bstt = new BinarySecurityTokenType();
            bstt.setValue(Base64.encodeBytes(jwt.getBytes()));
            bstt.setId(IDGenerator.create("ID_"));
            bstt.setValueType("urn:ietf:params:oauth:token-type:jwt");
            bstt.setEncodingType("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");

            response.getRequestedSecurityToken().add(bstt);
            response.setTokenType(URI.create("urn:ietf:params:oauth:token-type:jwt"));
        }
        else {
            throw new ConfigurationException("SAML or JWT must be set.");
        }

        return response;
    }

    protected Document signAssertion(Document samlDocument) throws ProcessingException {
        Element originalAssertionElement = samlDocument.getDocumentElement(); //org.keycloak.saml.common.util.DocumentUtil.getChildElement(samlDocument.getDocumentElement(), new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()));

        if (originalAssertionElement == null) return samlDocument;
        Node clonedAssertionElement = originalAssertionElement.cloneNode(true);
        Document temporaryDocument;

        try {
            temporaryDocument = org.keycloak.saml.common.util.DocumentUtil.createDocument();
        } catch (ConfigurationException e) {
            throw new ProcessingException(e);
        }

        temporaryDocument.adoptNode(clonedAssertionElement);
        temporaryDocument.appendChild(clonedAssertionElement);

        signDocument(temporaryDocument);

        return temporaryDocument;
    }

    protected void signDocument(Document samlDocument) throws ProcessingException {
        String signatureMethod = signatureAlgorithm.getXmlSignatureMethod();
        String signatureDigestMethod = signatureAlgorithm.getXmlSignatureDigestMethod();
        SAML2Signature samlSignature = new SAML2Signature();

        if (signatureMethod != null) {
            samlSignature.setSignatureMethod(signatureMethod);
        }

        if (signatureDigestMethod != null) {
            samlSignature.setDigestMethod(signatureDigestMethod);
        }

        Node nextSibling = samlSignature.getNextSiblingOfIssuer(samlDocument);

        samlSignature.setNextSibling(nextSibling);

        if (signingCertificate != null) {
            samlSignature.setX509Certificate(signingCertificate);
        }

        samlSignature.signSAMLDocument(samlDocument, signingKeyPair, canonicalizationMethodType);
    }

    public String getStringValue() throws ConfigurationException, ProcessingException, org.picketlink.common.exceptions.ProcessingException {
        return getStringValue(build());
    }

    public static String getStringValue(RequestSecurityTokenResponse response) throws ProcessingException, org.picketlink.common.exceptions.ProcessingException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WSTrustResponseWriter writer = new WSTrustResponseWriter(bos);
        RequestSecurityTokenResponseCollection coll = new RequestSecurityTokenResponseCollection();
        coll.addRequestSecurityTokenResponse(response);
        writer.write(coll);
        return new String(bos.toByteArray());
    }
}
