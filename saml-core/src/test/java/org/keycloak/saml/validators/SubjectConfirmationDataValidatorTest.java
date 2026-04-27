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

import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class SubjectConfirmationDataValidatorTest {

    @Test
    public void testNull() {
        DestinationValidator destVal = DestinationValidator.forProtocolMap(null);
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", null, destVal).build().isValid());
    }

    @Test
    public void testNotBefore() {
        SubjectConfirmationDataType scd = new SubjectConfirmationDataTypeBuilder().notBefore(1000).build();
        DestinationValidator destVal = DestinationValidator.forProtocolMap(null);
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).clockSkewInMillis(2000).build().isValid());
        Assert.assertFalse(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).clockSkewInMillis(0).build().isValid());
    }

    @Test
    public void testNotOnOrBefore() {
        SubjectConfirmationDataType scd = new SubjectConfirmationDataTypeBuilder().notOnOrAfter(-1000).build();
        DestinationValidator destVal = DestinationValidator.forProtocolMap(null);
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).clockSkewInMillis(2000).build().isValid());
        Assert.assertFalse(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).clockSkewInMillis(0).build().isValid());
    }

    @Test
    public void tesBothDates() {
        SubjectConfirmationDataType scd = new SubjectConfirmationDataTypeBuilder().notBefore(-1000).notOnOrAfter(1000).build();
        DestinationValidator destVal = DestinationValidator.forProtocolMap(null);
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).build().isValid());
        scd = new SubjectConfirmationDataTypeBuilder().notBefore(1000).notOnOrAfter(-1000).build();
        Assert.assertFalse(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).build().isValid());
    }

    @Test
    public void testInResponseTo() {
        SubjectConfirmationDataType scd = new SubjectConfirmationDataTypeBuilder().inResponseTo("id-123").build();
        DestinationValidator destVal = DestinationValidator.forProtocolMap(null);
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).inResponseTo("id-123").build().isValid());
        Assert.assertFalse(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).inResponseTo("id-567").build().isValid());
        scd = new SubjectConfirmationDataTypeBuilder().recipient("https://keycloak.org").notOnOrAfter(1000).build();
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).inResponseTo("id-123").build().isValid());
    }

    @Test
    public void testRecipient() {
        SubjectConfirmationDataType scd = new SubjectConfirmationDataTypeBuilder().recipient("https://keycloak.org").build();
        DestinationValidator destVal = DestinationValidator.forProtocolMap(null);
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).allowedRecipient("https://keycloak.org").build().isValid());
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).allowedRecipient("https://keycloak.com", "https://keycloak.org").build().isValid());
        Assert.assertFalse(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).allowedRecipient("https://keycloak.com").build().isValid());
        scd = new SubjectConfirmationDataTypeBuilder().inResponseTo("id-123").notOnOrAfter(1000).build();
        Assert.assertTrue(new SubjectConfirmationDataValidator.Builder("id", scd, destVal).allowedRecipient("https://keycloak.org").build().isValid());
    }

    private static class SubjectConfirmationDataTypeBuilder {
        private final SubjectConfirmationDataType scd;

        public SubjectConfirmationDataTypeBuilder() {
            scd = new SubjectConfirmationDataType();
        }

        public SubjectConfirmationDataTypeBuilder notBefore(long offsetMillis) {
            XMLGregorianCalendar value = XMLTimeUtil.getIssueInstant();
            value = XMLTimeUtil.add(value, offsetMillis);
            scd.setNotBefore(value);
            return this;
        }

        public SubjectConfirmationDataTypeBuilder notOnOrAfter(long offsetMillis) {
            XMLGregorianCalendar value = XMLTimeUtil.getIssueInstant();
            value = XMLTimeUtil.add(value, offsetMillis);
            scd.setNotOnOrAfter(value);
            return this;
        }

        public SubjectConfirmationDataTypeBuilder inResponseTo(String id) {
            scd.setInResponseTo(id);
            return this;
        }

        public SubjectConfirmationDataTypeBuilder recipient(String recipient) {
            scd.setRecipient(recipient);
            return this;
        }

        public SubjectConfirmationDataType build() {
            return scd;
        }
    }

}
