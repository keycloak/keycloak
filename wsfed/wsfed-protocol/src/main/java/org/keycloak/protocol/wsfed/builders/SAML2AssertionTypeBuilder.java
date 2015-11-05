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

import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.util.GregorianCalendar;

public class SAML2AssertionTypeBuilder {
    protected static final Logger logger = Logger.getLogger(SAML2AssertionTypeBuilder.class);

    protected String requestID;
    protected String issuer;
    protected String requestIssuer;
    protected int subjectExpiration;
    protected int assertionExpiration;
    protected String nameId;
    protected String nameIdFormat;

    public SAML2AssertionTypeBuilder issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    /**
     * Length of time in seconds the subject can be confirmed
     * See SAML core specification 2.4.1.2 NotOnOrAfter
     *
     * @param subjectExpiration Number of seconds the subject should be valid
     * @return
     */
    public SAML2AssertionTypeBuilder subjectExpiration(int subjectExpiration) {
        this.subjectExpiration = subjectExpiration;
        return this;
    }

    /**
     * Length of time in seconds the assertion is valid for
     * See SAML core specification 2.5.1.2 NotOnOrAfter
     *
     * @param assertionExpiration Number of seconds the assertion should be valid
     * @return
     */
    public SAML2AssertionTypeBuilder assertionExpiration(int assertionExpiration) {
        this.assertionExpiration = assertionExpiration;
        return this;
    }

    public SAML2AssertionTypeBuilder requestID(String requestID) {
        this.requestID =requestID;
        return this;
    }

    public SAML2AssertionTypeBuilder requestIssuer(String requestIssuer) {
        this.requestIssuer =requestIssuer;
        return this;
    }

    public SAML2AssertionTypeBuilder nameIdentifier(String nameIdFormat, String nameId) {
        this.nameIdFormat = nameIdFormat;
        this.nameId = nameId;
        return this;
    }

    public AssertionType buildModel() throws ConfigurationException, ProcessingException, DatatypeConfigurationException {
        String id = IDGenerator.create("ID_");
        AssertionType assertion = AssertionUtil.createAssertion(id, getNameIDType(issuer, null));

        //Add subjectconfirmation
        assertion.setSubject(getSubjectType());

        //Add request issuer as the audience restriction
        AudienceRestrictionType audience = new AudienceRestrictionType();
        audience.addAudience(URI.create(requestIssuer));
        assertion.setConditions(new ConditionsType());
        assertion.getConditions().setNotBefore(getXMLGregorianCalendarNow());
        assertion.getConditions().addCondition(audience);

        //Update Conditions NotOnOrAfter
        if(assertionExpiration > 0) {
            ConditionsType conditions = assertion.getConditions();
            conditions.setNotOnOrAfter(XMLTimeUtil.add(conditions.getNotBefore(), assertionExpiration * 1000));
        }

        //Update SubjectConfirmationData NotOnOrAfter
        if(subjectExpiration > 0) {
            SubjectConfirmationType sct = assertion.getSubject().getConfirmation().get(0);
            sct.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");
            SubjectConfirmationDataType subjectConfirmationData = new SubjectConfirmationDataType();
            sct.setSubjectConfirmationData(subjectConfirmationData);
            subjectConfirmationData.setNotBefore(assertion.getConditions().getNotBefore());
            subjectConfirmationData.setNotOnOrAfter(XMLTimeUtil.add(assertion.getConditions().getNotBefore(), subjectExpiration * 1000));
        }

        return assertion;
    }

    public XMLGregorianCalendar getXMLGregorianCalendarNow() throws DatatypeConfigurationException
    {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar now =
                datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        return now;
    }

    protected NameIDType getNameIDType(String responseIssuer, String nameIdFormat) {
        NameIDType nameIDType = new NameIDType();
        nameIDType.setValue(responseIssuer);

        if(nameIdFormat != null) {
            nameIDType.setFormat(URI.create(nameIdFormat));
        }

        return nameIDType;
    }

    protected SubjectType getSubjectType() {
        SubjectType subject = new SubjectType();
        NameIDType nameIDType = getNameIDType(nameId, nameIdFormat);

        SubjectType.STSubType st = new SubjectType.STSubType();
        st.addBaseID(nameIDType);
        subject.setSubType(st);

        SubjectConfirmationType sct = new SubjectConfirmationType();
        sct.setNameID(nameIDType);
        subject.addConfirmation(sct);

        return subject;
    }
}
