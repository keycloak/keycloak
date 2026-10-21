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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.hamcrest.MatcherAssert.assertThat;

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
    public void testFreshValidator() throws Exception {
        assertThat(JAXPValidationUtil.validator(), Matchers.not(Matchers.sameInstance(JAXPValidationUtil.validator())));
    }

    @Test
    public void testConcurrentValidation() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting for concurrent validation");
                    }
                    for (int j = 0; j < 20; j++) {
                        JAXPValidationUtil.validate(new ByteArrayInputStream(REQUEST_VALID.getBytes(StandardCharsets.UTF_8)));
                        assertInputValidation(REQUEST_INVALID, Matchers.notNullValue());
                    }
                    return null;
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS), Matchers.is(true));
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(60, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS), Matchers.is(true));
        }
    }

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
