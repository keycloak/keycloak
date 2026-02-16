/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.validators;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.jboss.logging.Logger;

/**
 *
 * @author rmartinc
 */
public class SubjectConfirmationDataValidator {

    private static final Logger logger = Logger.getLogger(SubjectConfirmationDataValidator.class);
    private final SubjectConfirmationDataType subjectConfirmationData;
    private final String inResponseTo;
    private final int clockSkewInMillis;
    private final String assertionId;
    private final XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();
    private final Set<String> allowedRecipients;
    private final DestinationValidator destinationValidator;

    private SubjectConfirmationDataValidator(String assertionId, SubjectConfirmationDataType subjectConfirmationData, String inResponseTo,
            int clockSkewInMillis, Set<String> allowedRecipients, DestinationValidator destinationValidator) {
        this.assertionId = assertionId;
        this.subjectConfirmationData = subjectConfirmationData;
        this.inResponseTo = inResponseTo;
        this.clockSkewInMillis = clockSkewInMillis;
        this.allowedRecipients = allowedRecipients;
        this.destinationValidator = destinationValidator;
    }

    public static class Builder {

        private final String assertionId;
        private final SubjectConfirmationDataType subjectConfirmationData;
        private final DestinationValidator destinationValidator;
        private int clockSkewInMillis = 0;
        private final Set<String> allowedRecipients = new HashSet<>();
        private String inResponseTo;

        public Builder(String assertionId, SubjectConfirmationDataType subjectConfirmationData, DestinationValidator destinationValidator) {
            this.assertionId = assertionId;
            this.subjectConfirmationData = subjectConfirmationData;
            this.destinationValidator = destinationValidator;
        }

        public Builder inResponseTo(String inResponseTo) {
            this.inResponseTo = inResponseTo;
            return this;
        }

        public Builder clockSkewInMillis(int clockSkewInMillis) {
            this.clockSkewInMillis = clockSkewInMillis;
            return this;
        }

        public Builder allowedRecipient(String... allowedRecipients) {
            this.allowedRecipients.addAll(Arrays.asList(allowedRecipients));
            return this;
        }

        public SubjectConfirmationDataValidator build() {
            return new SubjectConfirmationDataValidator(assertionId, subjectConfirmationData, inResponseTo, clockSkewInMillis, allowedRecipients, destinationValidator);
        }
    }

    public boolean isValid() {
        if (subjectConfirmationData == null) {
            return true;
        }

        if (!ConditionsValidator.validateExpiration(assertionId, subjectConfirmationData.getNotBefore(), subjectConfirmationData.getNotOnOrAfter(), now, clockSkewInMillis)) {
            return false;
        }

        if (!validateRecipient()) {
            return false;
        }

        return validateInResponseTo();
    }

    private boolean validateRecipient() {
        if (subjectConfirmationData.getRecipient() == null || allowedRecipients.isEmpty()) {
            return true;
        }

        for (String allowedRecipient : allowedRecipients) {
            if (destinationValidator.validate(subjectConfirmationData.getRecipient(), allowedRecipient)) {
                return true;
            }
        }

        logger.tracef("Response Validation Error: SubjectConfirmationData invalid recipient '%s', not in %s",
                subjectConfirmationData.getRecipient(), allowedRecipients);
        return false;
    }

    private boolean validateInResponseTo() {
        if (inResponseTo == null || subjectConfirmationData.getInResponseTo() == null) {
            return true;
        }

        // Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo does not match request ID
        if (subjectConfirmationData.getInResponseTo().equals(inResponseTo)) {
            return true;
        }

        logger.tracef("Response Validation Error: received SubjectConfirmationData InResponseTo '%s' does not match the expected request ID '%s'",
                subjectConfirmationData.getInResponseTo(), inResponseTo);
        return false;
    }
}
