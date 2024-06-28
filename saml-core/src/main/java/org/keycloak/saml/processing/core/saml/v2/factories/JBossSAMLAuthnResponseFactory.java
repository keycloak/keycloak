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
package org.keycloak.saml.processing.core.saml.v2.factories;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.EncryptedAssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.ResponseType.RTChoiceType;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.processing.core.saml.v2.holders.IssuerInfoHolder;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;

/**
 * Factory for the SAML v2 Authn Response
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 9, 2008
 */
public class JBossSAMLAuthnResponseFactory {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Create a StatusType given the status code uri
     *
     * @param statusCodeURI
     *
     * @return
     */
    public static StatusType createStatusType(String statusCodeURI) {
        StatusCodeType sct = new StatusCodeType();
        sct.setValue(URI.create(statusCodeURI));

        StatusType statusType = new StatusType();
        statusType.setStatusCode(sct);
        return statusType;
    }

    /**
     * <p>Create a <code>StatusType</code> with a top-level <code>org.picketlink.common.constants.JBossSAMLURIConstants.STATUS_RESPONDER</code>
     * and a second-level code reflecting the given <code>statusCodeURI</code>.</p>
     *
     * @param statusCodeURI The second-level code.
     *
     * @return
     */
    public static StatusType createStatusTypeForResponder(String statusCodeURI) {
        StatusCodeType topLevelCode = new StatusCodeType();

        topLevelCode.setValue(JBossSAMLURIConstants.STATUS_RESPONDER.getUri());

        StatusCodeType secondLevelCode = new StatusCodeType();

        secondLevelCode.setValue(URI.create(statusCodeURI));

        topLevelCode.setStatusCode(secondLevelCode);

        StatusType statusType = new StatusType();

        statusType.setStatusCode(topLevelCode);

        return statusType;
    }

    /**
     * Create a Response Type
     *
     * @param ID
     * @param issuerInfo
     * @param assertionType
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static ResponseType createResponseType(String ID, IssuerInfoHolder issuerInfo, AssertionType assertionType) {
        XMLGregorianCalendar issueInstant = XMLTimeUtil.getIssueInstant();
        ResponseType responseType = new ResponseType(ID, issueInstant);

        // Issuer
        NameIDType issuer = issuerInfo.getIssuer();
        responseType.setIssuer(issuer);

        // Status
        String statusCode = issuerInfo.getStatusCode();
        if (statusCode == null)
            throw logger.issuerInfoMissingStatusCodeError();

        responseType.setStatus(createStatusType(statusCode));

        responseType.addAssertion(new RTChoiceType(assertionType));
        return responseType;
    }

    /**
     * Create a Response Type
     *
     * @param ID
     * @param issuerInfo
     * @param encryptedAssertion a DOM {@link Element} that represents an encrypted assertion
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static ResponseType createResponseType(String ID, IssuerInfoHolder issuerInfo, Element encryptedAssertion) {
        ResponseType responseType = new ResponseType(ID, XMLTimeUtil.getIssueInstant());

        // Issuer
        NameIDType issuer = issuerInfo.getIssuer();
        responseType.setIssuer(issuer);

        // Status
        String statusCode = issuerInfo.getStatusCode();
        if (statusCode == null)
            throw logger.issuerInfoMissingStatusCodeError();

        responseType.setStatus(createStatusType(statusCode));

        responseType.addAssertion(new RTChoiceType(new EncryptedAssertionType(encryptedAssertion)));
        return responseType;
    }
}