/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.processing.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.xml.sax.SAXException;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class JAXPValidationUtilTest {

    private static final String REQUEST_VALID = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"a123\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
            "<saml:Issuer>urn:test</saml:Issuer>" +
            "</samlp:AuthnRequest>";

    private static final String REQUEST_FLAWED = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"&heh;\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
            "<saml:Issuer>urn:test</saml:Issuer>" +
            "</samlp:AuthnRequest>";

    private static final String REQUEST_FLAWED_LOCAL = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"&heh;\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
            "<saml:Issuer>urn:test</saml:Issuer>" +
            "</samlp:AuthnRequest>";

    private static final String REQUEST_INVALID = "<samlp:InvalidAuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"a123\" Version=\"2.0\" IssueInstant=\"2014-07-16T23:52:45Z\" >" +
            "<saml:Issuer>urn:test</saml:Issuer>" +
            "</samlp:AuthnRequest>";


    @Test
    public void testServerSideValidator() throws Exception {
        String preamble = "<!DOCTYPE AuthnRequest [" +
                "<!ELEMENT AuthnRequest (#PCDATA)>" +
                "<!ENTITY heh SYSTEM \"file:///etc/passwd\">" +
                "]>";

        assertInputValidation(REQUEST_VALID, Matchers.nullValue());

        assertInputValidation(REQUEST_INVALID, Matchers.notNullValue());
        assertInputValidation(preamble + REQUEST_FLAWED, Matchers.notNullValue());
        assertInputValidation(preamble + REQUEST_FLAWED_LOCAL, Matchers.notNullValue());
        assertInputValidation(preamble + "<AuthnRequest></AuthnRequest>", Matchers.notNullValue());
    }

    private void assertInputValidation(String s, Matcher<Object> matcher) {
        String validationResult = null;
        try {
            JAXPValidationUtil.validate(new ByteArrayInputStream(s.getBytes()));
        } catch (SAXException | IOException ex) {
            validationResult = ex.getMessage();
        }
//        log.debugf("Validation result: '%s' for: %s", validationResult, s);
        assertThat(s, validationResult, matcher);
    }

}
