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

package org.keycloak.saml;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.OneTimeUseType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.SAML2NameIDBuilder;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.holders.IDPInfoHolder;
import org.keycloak.saml.processing.core.saml.v2.holders.IssuerInfoHolder;
import org.keycloak.saml.processing.core.saml.v2.holders.SPInfoHolder;
import org.keycloak.saml.processing.core.saml.v2.util.StatementUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Document;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.saml.common.util.StringUtil.isNotNull;

/**
 * <p> Handles for dealing with SAML2 Authentication </p>
 * <p/>
 * Configuration Options:
 *
 * @author bburke@redhat.com
*/
public class SAML2LoginResponseBuilder implements SamlProtocolExtensionsAwareBuilder<SAML2LoginResponseBuilder> {
    protected static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    protected String destination;
    protected NameIDType issuer;
    protected int subjectExpiration;
    protected int assertionExpiration;
    protected int sessionExpiration;
    protected String nameId;
    protected String nameIdFormat;
    protected boolean multiValuedRoles;
    protected boolean disableAuthnStatement;
    protected String requestID;
    protected String authMethod;
    protected String requestIssuer;
    protected String sessionIndex;
    protected final List<NodeGenerator> extensions = new LinkedList<>();
    protected boolean includeOneTimeUseCondition;

    public SAML2LoginResponseBuilder sessionIndex(String sessionIndex) {
        this.sessionIndex = sessionIndex;
        return this;
    }

    public SAML2LoginResponseBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2LoginResponseBuilder issuer(NameIDType issuer) {
        this.issuer = issuer;
        return this;
    }

    public SAML2LoginResponseBuilder issuer(String issuer) {
        return issuer(SAML2NameIDBuilder.value(issuer).build());
    }

    /**
     * Length of time in seconds the subject can be confirmed
     * See SAML core specification 2.4.1.2 NotOnOrAfter
     *
     * @param subjectExpiration Number of seconds the subject should be valid
     * @return
     */
    public SAML2LoginResponseBuilder subjectExpiration(int subjectExpiration) {
        this.subjectExpiration = subjectExpiration;
        return this;
    }

    /**
     * Length of time in seconds the idp session will be valid
     * See SAML core specification 2.7.2 SessionNotOnOrAfter
     *
     * @param sessionExpiration Number of seconds the session should be valid
     * @return
     */
    public SAML2LoginResponseBuilder sessionExpiration(int sessionExpiration) {
        this.sessionExpiration = sessionExpiration;
        return this;
    }

    /**
     * Length of time in seconds the assertion is valid for
     * See SAML core specification 2.5.1.2 NotOnOrAfter
     *
     * @param assertionExpiration Number of seconds the assertion should be valid
     * @return
     */
    public SAML2LoginResponseBuilder assertionExpiration(int assertionExpiration) {
        this.assertionExpiration = assertionExpiration;
        return this;
    }

    public SAML2LoginResponseBuilder requestID(String requestID) {
        this.requestID = requestID;
        return this;
    }

    public SAML2LoginResponseBuilder requestIssuer(String requestIssuer) {
        this.requestIssuer = requestIssuer;
        return this;
    }

    public SAML2LoginResponseBuilder authMethod(String authMethod) {
        this.authMethod = authMethod;
        return this;
    }

    public SAML2LoginResponseBuilder nameIdentifier(String nameIdFormat, String nameId) {
        this.nameIdFormat = nameIdFormat;
        this.nameId = nameId;
        return this;
    }

    public SAML2LoginResponseBuilder multiValuedRoles(boolean multiValuedRoles) {
        this.multiValuedRoles = multiValuedRoles;
        return this;
    }

    public SAML2LoginResponseBuilder disableAuthnStatement(boolean disableAuthnStatement) {
        this.disableAuthnStatement = disableAuthnStatement;
        return this;
    }

    public SAML2LoginResponseBuilder includeOneTimeUseCondition(boolean includeOneTimeUseCondition) {
        this.includeOneTimeUseCondition = includeOneTimeUseCondition;
        return this;
    }

    @Override
    public SAML2LoginResponseBuilder addExtension(NodeGenerator extension) {
        this.extensions.add(extension);
        return this;
    }

    public Document buildDocument(ResponseType responseType) throws ConfigurationException, ProcessingException {
        Document samlResponseDocument = null;

        try {
            SAML2Response docGen = new SAML2Response();
            samlResponseDocument = docGen.convert(responseType);

            if (logger.isTraceEnabled()) {
                logger.trace("SAML Response Document: " + DocumentUtil.asString(samlResponseDocument));
            }
        } catch (Exception e) {
            throw logger.samlAssertionMarshallError(e);
        }

        return samlResponseDocument;
    }

    public ResponseType buildModel() throws ConfigurationException, ProcessingException {
        ResponseType responseType = null;

        SAML2Response saml2Response = new SAML2Response();

        // Create a response type
        String id = IDGenerator.create("ID_");

        IssuerInfoHolder issuerHolder = new IssuerInfoHolder(issuer);
        issuerHolder.setStatusCode(JBossSAMLURIConstants.STATUS_SUCCESS.get());

        IDPInfoHolder idp = new IDPInfoHolder();
        idp.setNameIDFormatValue(nameId);
        idp.setNameIDFormat(nameIdFormat);

        SPInfoHolder sp = new SPInfoHolder();
        sp.setResponseDestinationURI(destination);
        sp.setRequestID(requestID);
        sp.setIssuer(requestIssuer);
        responseType = saml2Response.createResponseType(id, sp, idp, issuerHolder);

        AssertionType assertion = responseType.getAssertions().get(0).getAssertion();

        //Add request issuer as the audience restriction
        AudienceRestrictionType audience = new AudienceRestrictionType();
        audience.addAudience(URI.create(requestIssuer));
        assertion.getConditions().addCondition(audience);

        //Update Conditions NotOnOrAfter
        if(assertionExpiration > 0) {
            ConditionsType conditions = assertion.getConditions();
            conditions.setNotOnOrAfter(XMLTimeUtil.add(conditions.getNotBefore(), assertionExpiration * 1000L));
        }

        //Update SubjectConfirmationData NotOnOrAfter
        if(subjectExpiration > 0) {
            SubjectConfirmationDataType subjectConfirmationData = assertion.getSubject().getConfirmation().get(0).getSubjectConfirmationData();
            subjectConfirmationData.setNotOnOrAfter(XMLTimeUtil.add(assertion.getConditions().getNotBefore(), subjectExpiration * 1000L));
        }

        // Create an AuthnStatementType
        if (!disableAuthnStatement) {
            String authContextRef = JBossSAMLURIConstants.AC_UNSPECIFIED.get();
            if (isNotNull(authMethod))
                authContextRef = authMethod;

            AuthnStatementType authnStatement = StatementUtil.createAuthnStatement(XMLTimeUtil.getIssueInstant(),
                    authContextRef);

            if (sessionExpiration > 0)
                authnStatement.setSessionNotOnOrAfter(XMLTimeUtil.add(authnStatement.getAuthnInstant(), sessionExpiration * 1000L));

            if (sessionIndex != null) authnStatement.setSessionIndex(sessionIndex);
            else authnStatement.setSessionIndex(assertion.getID());

            assertion.addStatement(authnStatement);
        }

        if (includeOneTimeUseCondition) {
            assertion.getConditions().addCondition(new OneTimeUseType());
        }

        if (!this.extensions.isEmpty()) {
            ExtensionsType extensionsType = new ExtensionsType();
            for (NodeGenerator extension : this.extensions) {
                extensionsType.addExtension(extension);
            }
            responseType.setExtensions(extensionsType);
        }

        return responseType;
    }

}
