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

import org.keycloak.dom.saml.v2.assertion.*;
import org.keycloak.dom.saml.v2.protocol.*;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.w3c.dom.Document;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Helper class to build attribute query response documents
 */
public class SAML2AttributeQueryResponseBuilder implements SamlProtocolExtensionsAwareBuilder<SAML2AttributeQueryResponseBuilder> {

    protected static final Logger logger = Logger.getLogger(SAML2AttributeQueryResponseBuilder.class);

    protected List<NodeGenerator> extensions = new LinkedList<>();
    private StatusType statusType;
    private NameIDType issuerType;
    private SubjectType subjectType;
    private String inResponseToValue;
    private long tokenLifespanSeconds;
    private AudienceRestrictionType audienceRestrictionType;
    private List<AttributeStatementType.ASTChoiceType> attributeTypeList;

    /**
     * Add extensions to the response
     * @param extension The extension to add
     * @return this
     */
    @Override
    public SAML2AttributeQueryResponseBuilder addExtension(NodeGenerator extension) {
        this.extensions.add(extension);
        return this;
    }

    /**
     * The status code to set in the response document
     * @param status The status of the response
     * @return this
     */
    public SAML2AttributeQueryResponseBuilder status(URI status){
        this.statusType = new StatusType();
        StatusCodeType sct = new StatusCodeType();
        sct.setValue(status);
        this.statusType.setStatusCode(sct);
        return this;
    }

    /**
     * Set the issuer of the response
     * @param issuer The issuer of the response document
     * @return this
     */
    public SAML2AttributeQueryResponseBuilder issuer(String issuer){
        this.issuerType = SAML2NameIDBuilder.value(issuer).build();
        return this;
    }

    /**
     * Set the subject of the response document
     * @param subject The subject of the response
     * @return this
     */
    public SAML2AttributeQueryResponseBuilder subject(String subject){
        NameIDType nameId = SAML2NameIDBuilder.value(subject).build();
        this.subjectType = new SubjectType();
        SubjectType.STSubType subType = new SubjectType.STSubType();
        subType.addBaseID(nameId);
        this.subjectType.setSubType(subType);
        return this;
    }

    /**
     * Set the inResponseTo value for the response
     * @param id The ID of the request that this response corresponds to
     * @return this
     */
    public SAML2AttributeQueryResponseBuilder inResponseTo(String id){
        this.inResponseToValue = id;
        return this;
    }

    /**
     * Set the lifespan of the generated response
     * @param lifespan The lifespan
     * @return this
     */
    public SAML2AttributeQueryResponseBuilder tokenLifespan(long lifespan){
        this.tokenLifespanSeconds = lifespan;
        return this;
    }

    /**
     * Set the audience that this response should be restricted to
     * @param audience The audience this response is instended for
     * @return this
     */
    public SAML2AttributeQueryResponseBuilder audienceRestriction(String audience){
        this.audienceRestrictionType = new AudienceRestrictionType();
        this.audienceRestrictionType.addAudience(URI.create(audience));
        return this;
    }

    /**
     * Set the attributes to include in the AttributeStatement in the response
     * @param attributes The attributes to include in the response
     * @return this
     */
    public SAML2AttributeQueryResponseBuilder attributes(Map<String, List<String>> attributes){
        this.attributeTypeList = attributes.entrySet().stream().map(entry -> {
            AttributeType attrType = new AttributeType(entry.getKey());
            entry.getValue().stream().forEach(v -> attrType.addAttributeValue(v));
            return new AttributeStatementType.ASTChoiceType(attrType);
        }).collect(Collectors.toList());
        return this;
    }

    /**
     * Convert the response representation to a SAML document
     * @return The SAML Document
     * @throws ParsingException
     * @throws ConfigurationException
     * @throws ProcessingException
     */
    public Document toDocument() throws ParsingException, ConfigurationException, ProcessingException {
        ResponseType responseType = createAttributeQueryResponse();
        return new SAML2Request().convert(responseType);
    }

    /**
     * Create the Java response representation
     * @return The response representation object
     * @throws ProcessingException
     */
    public ResponseType createAttributeQueryResponse() throws ProcessingException {
        try {
            // create the status object
            StatusResponseType srt = new StatusResponseType(IDGenerator.create("ID_"), XMLTimeUtil.getIssueInstant());
            srt.setStatus(assertNotNull(this.statusType, "status"));

            // create the response type object
            ResponseType rt = new ResponseType(srt);
            if (! this.extensions.isEmpty()) {
                ExtensionsType extensionsType = new ExtensionsType();
                for (NodeGenerator extension : this.extensions) {
                    extensionsType.addExtension(extension);
                }
                rt.setExtensions(extensionsType);
            }

            // set response values
            rt.setIssuer(assertNotNull(this.issuerType, "issuer"));
            rt.setInResponseTo(assertNotNull(this.inResponseToValue, "in response to value"));

            // generate the assertion and set the attribute statement
            AssertionType at = new AssertionType(IDGenerator.create("ID_"), XMLTimeUtil.getIssueInstant());

            AttributeStatementType ast = new AttributeStatementType();
            ast.addAttributes(assertNotNull(this.attributeTypeList, "attributes"));

            at.addStatement(ast);
            at.setIssuer(assertNotNull(this.issuerType, "issuer"));
            at.setSubject(assertNotNull(this.subjectType, "subject"));

            // add any addition conditions such as lifespan
            ConditionsType conditions = new ConditionsType();
            conditions.setNotBefore(XMLTimeUtil.getIssueInstant());
            conditions.setNotOnOrAfter(XMLTimeUtil.add(conditions.getNotBefore(), assertNotNull(this.tokenLifespanSeconds, "token lifespan") * 1000L));
            if (this.audienceRestrictionType != null) {
                conditions.addCondition(this.audienceRestrictionType);
            }

            at.setConditions(conditions);
            rt.addAssertion(new ResponseType.RTChoiceType(at));

            return rt;
        } catch (ProcessingException ex){
            logger.errorf("failed to build attribute query response: %s", ex.getMessage());
            throw new ProcessingException(ex);
        }
    }

    private <T> T assertNotNull(T obj, String msg) throws ProcessingException {
        if (obj == null){
            throw new ProcessingException(String.format("attribute query response %s must be set", msg));
        }

        return obj;
    }
}