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
package org.keycloak.dom.saml.v1.assertion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.dom.saml.common.CommonAssertionType;

import org.w3c.dom.Element;

/**
 * <complexType name="AssertionType"> <sequence> <element ref="saml:Conditions" minOccurs="0"/> <element
 * ref="saml:Advice"
 * minOccurs="0"/> <choice maxOccurs="unbounded"> <element ref="saml:Statement"/> <element
 * ref="saml:SubjectStatement"/>
 * <element ref="saml:AuthenticationStatement"/> <element ref="saml:AuthorizationDecisionStatement"/> <element
 * ref="saml:AttributeStatement"/> </choice>
 *
 * <element ref="ds:Signature" minOccurs="0"/> </sequence> <attribute name="MajorVersion" type="integer"
 * use="required"/>
 * <attribute name="MinorVersion" type="integer" use="required"/> <attribute name="AssertionID" type="ID"
 * use="required"/>
 * <attribute name="Issuer" type="string" use="required"/> <attribute name="IssueInstant" type="dateTime"
 * use="required"/>
 * </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 21, 2011
 */
public class SAML11AssertionType extends CommonAssertionType {

    protected int majorVersion = 1;

    protected int minorVersion = 1;

    protected SAML11ConditionsType conditions;

    protected SAML11AdviceType advice;

    protected List<SAML11StatementAbstractType> statements = new ArrayList<>();

    protected Element signature;

    protected String issuer;

    public SAML11AssertionType(String iD, XMLGregorianCalendar issueInstant) {
        super(iD, issueInstant);
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void add(SAML11StatementAbstractType statement) {
        this.statements.add(statement);
    }

    public void addAllStatements(List<SAML11StatementAbstractType> statement) {
        this.statements.addAll(statement);
    }

    public boolean remove(SAML11StatementAbstractType statement) {
        return this.statements.remove(statement);
    }

    public List<SAML11StatementAbstractType> getStatements() {
        return Collections.unmodifiableList(statements);
    }

    public SAML11ConditionsType getConditions() {
        return conditions;
    }

    public void setConditions(SAML11ConditionsType conditions) {
        this.conditions = conditions;
    }

    public SAML11AdviceType getAdvice() {
        return advice;
    }

    public void setAdvice(SAML11AdviceType advice) {
        this.advice = advice;
    }

    public Element getSignature() {
        return signature;
    }

    public void setSignature(Element signature) {
        this.signature = signature;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}