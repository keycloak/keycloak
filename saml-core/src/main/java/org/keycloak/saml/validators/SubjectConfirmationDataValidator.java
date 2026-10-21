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
import org.keycloak.saml.common.util.StringUtil;
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
        // Per the SAML 2.0 Web Browser SSO Profile, a Bearer SubjectConfirmation MUST carry a
        // SubjectConfirmationData element with a bounded validity window and a Recipient; both
        // are the anti-replay/anti-redirection properties the Bearer method relies on, so a
        // missing element must be treated as invalid rather than skipped.
        if (subjectConfirmationData == null) {
            logger.tracef("Response Validation Error: assertion '%s' is missing the mandatory Bearer SubjectConfirmationData element", assertionId);
            return false;
        }

        if (!validateExpiration()) {
            return false;
        }

        if (!validateRecipient()) {
            return false;
        }

        return validateInResponseTo();
    }

    private boolean validateExpiration() {
        if (subjectConfirmationData.getNotOnOrAfter() == null) {
            logger.tracef("Response Validation Error: SubjectConfirmationData of assertion '%s' is missing the mandatory NotOnOrAfter attribute", assertionId);
            return false;
        }

        return ConditionsValidator.validateExpiration(assertionId, subjectConfirmationData.getNotBefore(), subjectConfirmationData.getNotOnOrAfter(), now, clockSkewInMillis);
    }

    private boolean validateRecipient() {
        if (allowedRecipients.isEmpty()) {
            // caller did not supply any recipient to check against, so the check cannot be performed
            return true;
        }

        if (!StringUtil.isNotNull(subjectConfirmationData.getRecipient())) {
            logger.tracef("Response Validation Error: SubjectConfirmationData of assertion '%s' is missing the mandatory Recipient attribute", assertionId);
            return false;
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
        if (inResponseTo == null) {
            // caller is not expecting a specific request id (e.g. IdP-initiated flow), so the check cannot be performed
            return true;
        }

        if (!StringUtil.isNotNull(subjectConfirmationData.getInResponseTo())) {
            logger.tracef("Response Validation Error: SubjectConfirmationData of assertion '%s' is missing the expected InResponseTo '%s'",
                    assertionId, inResponseTo);
            return false;
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
