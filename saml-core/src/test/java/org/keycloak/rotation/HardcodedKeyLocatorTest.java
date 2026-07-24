/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.rotation;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;

import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class HardcodedKeyLocatorTest {

    private static final String SAMPLE_CERTIFICATE_RSA_1 = "MIICmzCCAYMCBgGGc0gbfzANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjMwMjIxMDkyMDUwWhcNMzMwMjIxMDkyMjMwWjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCfc15pW/NOT3CM92q7BUB3pyTdA1h0WFG+JM2JjrNyZEbxsycYXS84QlaaEl/qT0wshIFQnv6bD1jy604V9W+7luK6Q/cOoQyRCiI70CVy4kB73sqT8Lgrfux6zWJeZ0lMO14sPq6eJLhWNBGxbGvJtUgBAdv5TIjf8yaHCV+yo4rc83T6Pd1sfTlRrURnokPD+hy+BbCEVj9350vYiyTRSvUD+e1wG1BIyZ/IA572p15rS69PP+qAuBBE8QF42bI56ZTsU+tXxwSX2nPqVbLD61tb1BFXfrHkArRiLe/Dte7xAmArynWs62ZI1q52REVWik1dzzy+VpJ7lef7vgtJAgMBAAEwDQYJKoZIhvcNAQELBQADggEBADB5DXugTWEYrw/ic/Jqz+aKXlz+QJvP5JEOVMnfKQLfHx+6760ubCwqJstA8HL6z8DWQUWWylwhfFv15nW/tgawbYLGHiq0NfB3/T6u3hswAPff9ZNvviL0L8CtPXpgPE5MzUEyPRIl/ExW/a7oNlo3rOPE6vA2xEG5h24f9xVdT5hGT5wRTm/e64ZT+umpWs2HnGjRcvdEKZhQPGfKrfdzNn1DVobbGSuy7P64lPWRJ/DxrhMwVkOyfZ+XoIGavS/yLQt01KjIrqtmUZOwHE5FRM/B58doGZn/zNpxq0tb7t9sxWIcW6wyZyieTAO7D9D84Qw8EBwKlbtsfS0oSZw=";
    private static final String SAMPLE_CERTIFICATE_RSA_2 = "MIICmzCCAYMCBgGGc0gb+jANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjMwMjIxMDkyMDUxWhcNMzMwMjIxMDkyMjMxWjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDOgZKCSPgYFaBCLrhaX4jBjgTqdYemJPLyR3gAq3GhO/KVVj5i3lOJYLPE3TdyxowxpvnqJK5zIgLv954y7cbah5wbyfdcFf/qa/RvEDAVb1c3gs+7e5uEoiAWgvARQbcduuO8U/rerlgF3eN0WLjIjcz8yncLmMvd+AhjOAqs3AmKrlEADeABTRq454gXjrD8x3bZwRvC67ZOdK32WpfIG9u58WABDYHWavQ8aetcs1uuwbNl7Tmi0heEtgBd8q2y3BJmn31NXmRobLwNuILEN8sujMKf6iaISA50gh0TCUYSbzeeQ6DrqHBlOA8azpuwka4pQyr+R22MDdrItTc3AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAKuc82PlWzQbevzd/FvbutsEX5Tdf4Nojd+jOvcP6NiDtImWojzgN+SSAKTtmCz3ToBxjJbI4UjhovjWN4e4ygEWksBw6YYYR9ZGCJ7Z3EZzyREojvZeF/H0lQqB3BgnjI38HBpRgCpZm3H6+1UoJtMOW2sU8jorG/k1qvXrx2Y3bZvj/6wixVnzjiFzagb3cIUzv9c7ZWlexaR2Bg0k4kQ5TFwyzYCE136nl8xPqoDd8Nc4fQEPI7wLYMGglmbLFlGvdz3IJ7XRparYJRm4wlznQ43GL2x2KGBu8JipgbA7+u6F84oqf3vOC/PozWXzVCn08e6gqBY3YdZcs6sA3qY=";
    private static final X509Certificate cert1;
    private static final X509Certificate cert2;

    static {
        try {
            cert1 = XMLSignatureUtil.getX509CertificateFromKeyInfoString(SAMPLE_CERTIFICATE_RSA_1);
            cert2 = XMLSignatureUtil.getX509CertificateFromKeyInfoString(SAMPLE_CERTIFICATE_RSA_2);
        } catch (ProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyLocator createLocatorWithName(X509Certificate... cert) {
        Map<String, Key> tmp = new HashMap<>();
        for (int i = 1; i <= cert.length; i++) {
            tmp.put("cert" +  i, cert[i - 1].getPublicKey());
        }
        return new HardcodedKeyLocator(tmp);
    }

    private static KeyLocator createLocatorWithoutName(X509Certificate... cert) {
        return new HardcodedKeyLocator(Arrays.stream(cert).map(X509Certificate::getPublicKey).collect(Collectors.toList()));
    }

    @Test
    public void testCertificateWithTwoCertificatesWithName() throws Exception {
        KeyLocator locator = createLocatorWithName(cert1, cert2);
        KeyInfo info = XMLSignatureUtil.createKeyInfo(null, null, cert1);
        Key found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert1.getPublicKey(), found);

        info = XMLSignatureUtil.createKeyInfo(null, null, cert2);
        found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert2.getPublicKey(), found);
    }

    @Test
    public void testKeyWithTwoCertificatesWithName() throws Exception {
        KeyLocator locator = createLocatorWithName(cert1, cert2);
        KeyInfo info = XMLSignatureUtil.createKeyInfo(null, cert1.getPublicKey(), null);
        Key found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert1.getPublicKey(), found);

        info = XMLSignatureUtil.createKeyInfo(null, cert2.getPublicKey(), null);
        found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert2.getPublicKey(), found);
    }

    @Test
    public void testKeyNameWithTwoCertificatesWithName() throws Exception {
        KeyLocator locator = createLocatorWithName(cert1, cert2);
        KeyInfo info = XMLSignatureUtil.createKeyInfo("cert1", null, null);
        Key found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert1.getPublicKey(), found);

        info = XMLSignatureUtil.createKeyInfo("cert2", null, null);
        found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert2.getPublicKey(), found);
    }

    @Test
    public void testKeyNameWithTwoCertificatesWithoutName() throws Exception {
        KeyLocator locator = createLocatorWithoutName(cert1, cert2);
        KeyInfo info = XMLSignatureUtil.createKeyInfo("cert1", null, null);
        Key found = locator.getKey(info);
        Assert.assertNull(found);

        info = XMLSignatureUtil.createKeyInfo("cert1", cert1.getPublicKey(), null);
        found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert1.getPublicKey(), found);

        info = XMLSignatureUtil.createKeyInfo("cert2", null, cert2);
        found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert2.getPublicKey(), found);
    }

    @Test
    public void testKeyNameWithOneCertificatesWithoutName() throws Exception {
        //hardcoded locator with one cert is always returned
        KeyLocator locator = createLocatorWithoutName(cert1);
        KeyInfo info = XMLSignatureUtil.createKeyInfo("cert1", null, null);
        Key found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert1.getPublicKey(), found);

        info = XMLSignatureUtil.createKeyInfo("cert1", cert1.getPublicKey(), null);
        found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert1.getPublicKey(), found);

        info = XMLSignatureUtil.createKeyInfo("cert2", null, cert2);
        found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert1.getPublicKey(), found);
    }

    @Test
    public void testDuplicateKey() throws Exception {
        KeyLocator locator = createLocatorWithoutName(cert1, cert1);
        KeyInfo info = XMLSignatureUtil.createKeyInfo(null, null, cert1);
        Key found = locator.getKey(info);
        Assert.assertNotNull(found);
        Assert.assertEquals(cert1.getPublicKey(), found);
    }
}
