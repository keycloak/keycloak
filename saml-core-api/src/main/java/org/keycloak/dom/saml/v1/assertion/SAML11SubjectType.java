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

/**
 * <complexType name="SubjectType"> <choice> <sequence> <element ref="saml:NameIdentifier"/> <element
 * ref="saml:SubjectConfirmation" minOccurs="0"/>
 *
 * </sequence> <element ref="saml:SubjectConfirmation"/> </choice> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11SubjectType {

    public static class SAML11SubjectTypeChoice {

        protected SAML11NameIdentifierType nameID;

        protected SAML11SubjectConfirmationType subjectConfirmation;

        public SAML11SubjectTypeChoice(SAML11NameIdentifierType nameID) {
            this.nameID = nameID;
        }

        public SAML11SubjectTypeChoice(SAML11SubjectConfirmationType subConfirms) {
            this.subjectConfirmation = subConfirms;
        }

        public SAML11NameIdentifierType getNameID() {
            return nameID;
        }

        public SAML11SubjectConfirmationType getSubjectConfirmation() {
            return subjectConfirmation;
        }
    }

    protected SAML11SubjectConfirmationType subjectConfirmation;

    protected SAML11SubjectTypeChoice choice;

    public SAML11SubjectConfirmationType getSubjectConfirmation() {
        return subjectConfirmation;
    }

    public void setSubjectConfirmation(SAML11SubjectConfirmationType subjectConfirmation) {
        this.subjectConfirmation = subjectConfirmation;
    }

    public SAML11SubjectTypeChoice getChoice() {
        return choice;
    }

    public void setChoice(SAML11SubjectTypeChoice choice) {
        this.choice = choice;
    }
}