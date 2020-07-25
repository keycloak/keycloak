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
package org.keycloak.saml.processing.core.saml.v2.writers;

import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResolveType;
import org.keycloak.dom.saml.v2.protocol.AttributeQueryType;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import java.net.URI;
import java.util.List;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;

/**
 * Writes a SAML2 Request Type to Stream
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 2, 2010
 */
public class SAMLRequestWriter extends BaseWriter {

    public SAMLRequestWriter(XMLStreamWriter writer) {
        super(writer);
    }

    /**
     * Write a {@code AuthnRequestType } to stream
     *
     * @param request
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public void write(AuthnRequestType request) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.AUTHN_REQUEST.get(), PROTOCOL_NSURI.get());
        StaxUtil.writeNameSpace(writer, PROTOCOL_PREFIX, PROTOCOL_NSURI.get());
        StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());
        StaxUtil.writeDefaultNameSpace(writer, ASSERTION_NSURI.get());

        // Attributes
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ID.get(), request.getID());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.VERSION.get(), request.getVersion());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISSUE_INSTANT.get(), request.getIssueInstant().toString());

        URI destination = request.getDestination();
        if (destination != null)
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.DESTINATION.get(), destination.toASCIIString());

        String consent = request.getConsent();
        if (StringUtil.isNotNull(consent))
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.CONSENT.get(), consent);

        URI assertionURL = request.getAssertionConsumerServiceURL();
        if (assertionURL != null)
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE_URL.get(),
                    assertionURL.toASCIIString());

        Boolean forceAuthn = request.isForceAuthn();
        if (forceAuthn != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.FORCE_AUTHN.get(), forceAuthn.toString());
        }

        Boolean isPassive = request.isIsPassive();
        // The AuthnRequest IsPassive attribute is optional and if omitted its default value is false. 
        // Some IdPs refuse requests if the IsPassive attribute is present and set to false, so to 
        // maximize compatibility we emit it only if it is set to true
        if (isPassive != null && isPassive == true) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.IS_PASSIVE.get(), isPassive.toString());
        }

        URI protocolBinding = request.getProtocolBinding();
        if (protocolBinding != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.PROTOCOL_BINDING.get(), protocolBinding.toString());
        }

        Integer assertionIndex = request.getAssertionConsumerServiceIndex();
        if (assertionIndex != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE_INDEX.get(),
                    assertionIndex.toString());
        }

        Integer attrIndex = request.getAttributeConsumingServiceIndex();
        if (attrIndex != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.ATTRIBUTE_CONSUMING_SERVICE_INDEX.get(), attrIndex.toString());
        }
        String providerName = request.getProviderName();
        if (StringUtil.isNotNull(providerName)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.PROVIDER_NAME.get(), providerName);
        }

        NameIDType issuer = request.getIssuer();
        if (issuer != null) {
            write(issuer, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.ISSUER.get(), ASSERTION_PREFIX), false);
        }

        SubjectType subject = request.getSubject();
        if (subject != null) {
            write(subject);
        }

        Element sig = request.getSignature();
        if (sig != null) {
            StaxUtil.writeDOMElement(writer, sig);
        }

        ExtensionsType extensions = request.getExtensions();
        if (extensions != null && ! extensions.getAny().isEmpty()) {
            write(extensions);
        }

        NameIDPolicyType nameIDPolicy = request.getNameIDPolicy();
        if (nameIDPolicy != null) {
            write(nameIDPolicy);
        }

        RequestedAuthnContextType requestedAuthnContext = request.getRequestedAuthnContext();
        if (requestedAuthnContext != null) {
            write(requestedAuthnContext);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    /**
     * Write a {@code LogoutRequestType} to stream
     *
     * @param logOutRequest
     *
     * @throws ProcessingException
     */
    public void write(LogoutRequestType logOutRequest) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.LOGOUT_REQUEST.get(), PROTOCOL_NSURI.get());

        StaxUtil.writeNameSpace(writer, PROTOCOL_PREFIX, PROTOCOL_NSURI.get());
        StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());
        StaxUtil.writeDefaultNameSpace(writer, ASSERTION_NSURI.get());

        // Attributes
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ID.get(), logOutRequest.getID());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.VERSION.get(), logOutRequest.getVersion());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISSUE_INSTANT.get(), logOutRequest.getIssueInstant().toString());

        URI destination = logOutRequest.getDestination();
        if (destination != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.DESTINATION.get(), destination.toASCIIString());
        }

        String consent = logOutRequest.getConsent();
        if (StringUtil.isNotNull(consent))
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.CONSENT.get(), consent);

        NameIDType issuer = logOutRequest.getIssuer();
        write(issuer, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.ISSUER.get(), ASSERTION_PREFIX));

        Element signature = logOutRequest.getSignature();
        if (signature != null) {
            StaxUtil.writeDOMElement(writer, signature);
        }

        ExtensionsType extensions = logOutRequest.getExtensions();
        if (extensions != null && ! extensions.getAny().isEmpty()) {
            write(extensions);
        }

        NameIDType nameID = logOutRequest.getNameID();
        if (nameID != null) {
            write(nameID, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.NAMEID.get(), ASSERTION_PREFIX));
        }

        List<String> sessionIndexes = logOutRequest.getSessionIndex();

        for (String sessionIndex : sessionIndexes) {
            StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.SESSION_INDEX.get(), PROTOCOL_NSURI.get());

            StaxUtil.writeCharacters(writer, sessionIndex);

            StaxUtil.writeEndElement(writer);
            StaxUtil.flush(writer);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    /**
     * Write a {@code NameIDPolicyType} to stream
     *
     * @param nameIDPolicy
     *
     * @throws ProcessingException
     */
    public void write(NameIDPolicyType nameIDPolicy) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.NAMEID_POLICY.get(), PROTOCOL_NSURI.get());

        URI format = nameIDPolicy.getFormat();
        if (format != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.FORMAT.get(), format.toASCIIString());
        }

        String spNameQualifier = nameIDPolicy.getSPNameQualifier();
        if (StringUtil.isNotNull(spNameQualifier)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.SP_NAME_QUALIFIER.get(), spNameQualifier);
        }

        Boolean allowCreate = nameIDPolicy.isAllowCreate();
        // The NameID AllowCreate attribute must not be used when using the transient NameID format.
        if (allowCreate != null && (format == null || !NAMEID_FORMAT_TRANSIENT.get().equals(format.toASCIIString()))) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.ALLOW_CREATE.get(), allowCreate.toString());
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    /**
     * Write a {@code RequestedAuthnContextType} to stream
     *
     * @param requestedAuthnContextType
     *
     * @throws ProcessingException
     */
    public void write(RequestedAuthnContextType requestedAuthnContextType) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.REQUESTED_AUTHN_CONTEXT.get(), PROTOCOL_NSURI.get());

        AuthnContextComparisonType comparison = requestedAuthnContextType.getComparison();

        if (comparison != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.COMPARISON.get(), comparison.value());
        }

        List<String> authnContextClassRef = requestedAuthnContextType.getAuthnContextClassRef();

        if (authnContextClassRef != null && !authnContextClassRef.isEmpty()) {
            for (String classRef : authnContextClassRef) {
                StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHN_CONTEXT_CLASS_REF.get(), ASSERTION_NSURI.get());
                StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());
                StaxUtil.writeCharacters(writer, classRef);
                StaxUtil.writeEndElement(writer);
            }
        }

        List<String> authnContextDeclRef = requestedAuthnContextType.getAuthnContextDeclRef();

        if (authnContextDeclRef != null && !authnContextDeclRef.isEmpty()) {
            for (String declRef : authnContextDeclRef) {
                StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHN_CONTEXT_DECL_REF.get(), ASSERTION_NSURI.get());
                StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());
                StaxUtil.writeCharacters(writer, declRef);
                StaxUtil.writeEndElement(writer);
            }
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(ArtifactResolveType request) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.ARTIFACT_RESOLVE.get(), PROTOCOL_NSURI.get());
        StaxUtil.writeNameSpace(writer, PROTOCOL_PREFIX, PROTOCOL_NSURI.get());
        StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());
        StaxUtil.writeDefaultNameSpace(writer, ASSERTION_NSURI.get());

        // Attributes
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ID.get(), request.getID());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.VERSION.get(), request.getVersion());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISSUE_INSTANT.get(), request.getIssueInstant().toString());

        URI destination = request.getDestination();
        if (destination != null)
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.DESTINATION.get(), destination.toASCIIString());

        String consent = request.getConsent();
        if (StringUtil.isNotNull(consent))
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.CONSENT.get(), consent);

        NameIDType issuer = request.getIssuer();
        if (issuer != null) {
            write(issuer, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.ISSUER.get(), ASSERTION_PREFIX));
        }
        Element sig = request.getSignature();
        if (sig != null) {
            StaxUtil.writeDOMElement(writer, sig);
        }
        ExtensionsType extensions = request.getExtensions();
        if (extensions != null && ! extensions.getAny().isEmpty()) {
            write(extensions);
        }

        String artifact = request.getArtifact();
        if (StringUtil.isNotNull(artifact)) {
            StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.ARTIFACT.get(), PROTOCOL_NSURI.get());
            StaxUtil.writeCharacters(writer, artifact);
            StaxUtil.writeEndElement(writer);
        }
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(AttributeQueryType request) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.ATTRIBUTE_QUERY.get(), PROTOCOL_NSURI.get());
        StaxUtil.writeNameSpace(writer, PROTOCOL_PREFIX, PROTOCOL_NSURI.get());
        StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());
        StaxUtil.writeDefaultNameSpace(writer, ASSERTION_NSURI.get());

        // Attributes
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ID.get(), request.getID());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.VERSION.get(), request.getVersion());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISSUE_INSTANT.get(), request.getIssueInstant().toString());

        URI destination = request.getDestination();
        if (destination != null)
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.DESTINATION.get(), destination.toASCIIString());

        String consent = request.getConsent();
        if (StringUtil.isNotNull(consent))
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.CONSENT.get(), consent);

        NameIDType issuer = request.getIssuer();
        if (issuer != null) {
            write(issuer, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.ISSUER.get(), ASSERTION_PREFIX));
        }
        Element sig = request.getSignature();
        if (sig != null) {
            StaxUtil.writeDOMElement(writer, sig);
        }
        ExtensionsType extensions = request.getExtensions();
        if (extensions != null && ! extensions.getAny().isEmpty()) {
            write(extensions);
        }
        SubjectType subject = request.getSubject();
        if (subject != null) {
            write(subject);
        }
        List<AttributeType> attributes = request.getAttribute();
        for (AttributeType attr : attributes) {
            write(attr);
        }
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

}