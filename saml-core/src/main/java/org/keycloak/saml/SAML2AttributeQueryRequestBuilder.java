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
package org.keycloak.saml;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.AttributeQueryType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.w3c.dom.Document;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Helper class to build attribute query request documents
 */
public class SAML2AttributeQueryRequestBuilder implements SamlProtocolExtensionsAwareBuilder<SAML2AttributeQueryRequestBuilder> {

    private final AttributeQueryType attributeQueryType;
    protected final List<NodeGenerator> extensions = new LinkedList<>();

    public SAML2AttributeQueryRequestBuilder() {
        this.attributeQueryType = new AttributeQueryType(IDGenerator.create("ID_"), XMLTimeUtil.getIssueInstant());
    }

    /**
     * Add extensions to the request
     * @param extension The extension to add
     * @return this
     */
    @Override
    public SAML2AttributeQueryRequestBuilder addExtension(NodeGenerator extension) {
        this.extensions.add(extension);
        return this;
    }

    /**
     * Set the target destination of the request
     * @param destination The target destination
     * @return this
     */
    public SAML2AttributeQueryRequestBuilder destination(String destination){
        this.attributeQueryType.setDestination(URI.create(destination));
        return this;
    }

    /**
     * Set the subject on the request
     * @param subject The subject to set in the request
     * @return this
     */
    public SAML2AttributeQueryRequestBuilder subject(String subject){
        return subject(subject, null);
    }

    /**
     * Set the subject on the request with a given format
     * @param subject The subject to set in the request
     * @param format The NAMEID format
     * @return this
     */
    public SAML2AttributeQueryRequestBuilder subject(String subject, String format) {
        String sanitizedSubject = subject != null ? subject.trim() : null;
        if (sanitizedSubject != null && !sanitizedSubject.isEmpty()) {
            this.attributeQueryType.setSubject(createSubject(sanitizedSubject, format));
        }
        return this;
    }

    /**
     * Set the issuer on the request
     * @param issuer The issuer of the request
     * @return this
     */
    public SAML2AttributeQueryRequestBuilder issuer(String issuer){
        this.attributeQueryType.setIssuer(SAML2NameIDBuilder.value(issuer).build());
        return this;
    }

    /**
     * Helper function to generate the SubjectType object
     * @param value The subject of the attribute query request
     * @param format The nameid format to use in the subject
     * @return The SubjectType object
     */
    private SubjectType createSubject(String value, String format) {
        NameIDType nameId = new NameIDType();
        nameId.setValue(value);
        if (format == null){
            nameId.setFormat(null);
        } else {
            nameId.setFormat(URI.create(format));
        }
        SubjectType subject = new SubjectType();
        SubjectType.STSubType subType = new SubjectType.STSubType();
        subType.addBaseID(nameId);
        subject.setSubType(subType);
        return subject;
    }

    /**
     * Create the attribute query request document
     * @return The attribute query request document
     */
    public Document toDocument() {
        AttributeQueryType attributeQueryType = createAttributeQueryRequest();

        try {
            return new SAML2Request().convert(attributeQueryType);
        } catch (Exception e) {
            throw new RuntimeException("Could not convert " + attributeQueryType + " to a document.", e);
        }
    }

    /**
     * Create the Java representation of an attribute query request
     * @return The java representation of the attribute query request
     */
    public AttributeQueryType createAttributeQueryRequest() {
        AttributeQueryType res = this.attributeQueryType;

        if (! this.extensions.isEmpty()) {
            ExtensionsType extensionsType = new ExtensionsType();
            for (NodeGenerator extension : this.extensions) {
                extensionsType.addExtension(extension);
            }
            res.setExtensions(extensionsType);
        }

        return res;
    }
}