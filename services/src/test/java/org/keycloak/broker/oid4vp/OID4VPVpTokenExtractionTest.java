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

package org.keycloak.broker.oid4vp;

import org.keycloak.common.VerificationException;

import org.junit.Assert;
import org.junit.Test;

public class OID4VPVpTokenExtractionTest {

    private static final String CREDENTIAL = "eyJhbGciOiJFUzI1NiJ9.eyJ2Y3QiOiJ4In0.sig~WyJzIiwibiIsInYiXQ~";

    @Test
    public void unwrapsBareCompactSdJwt() throws Exception {
        Assert.assertEquals(CREDENTIAL, OID4VPIdentityProviderEndpoint.extractCredential(CREDENTIAL));
    }

    @Test
    public void unwrapsDcqlObject() throws Exception {
        Assert.assertEquals(CREDENTIAL,
                OID4VPIdentityProviderEndpoint.extractCredential("{\"identity\":\"" + CREDENTIAL + "\"}"));
    }

    @Test
    public void unwrapsDcqlObjectWithArrayValue() throws Exception {
        Assert.assertEquals(CREDENTIAL,
                OID4VPIdentityProviderEndpoint.extractCredential("{\"identity\":[\"" + CREDENTIAL + "\"]}"));
    }

    @Test
    public void unwrapsTopLevelArray() throws Exception {
        Assert.assertEquals(CREDENTIAL,
                OID4VPIdentityProviderEndpoint.extractCredential("[\"" + CREDENTIAL + "\"]"));
    }

    @Test(expected = VerificationException.class)
    public void rejectsJsonWithoutPresentation() throws Exception {
        OID4VPIdentityProviderEndpoint.extractCredential("{}");
    }

    @Test(expected = VerificationException.class)
    public void rejectsNonTextualPresentation() throws Exception {
        OID4VPIdentityProviderEndpoint.extractCredential("{\"identity\":123}");
    }

    @Test(expected = VerificationException.class)
    public void rejectsMultipleCredentialsInObject() throws Exception {
        OID4VPIdentityProviderEndpoint.extractCredential(
                "{\"identity\":\"" + CREDENTIAL + "\",\"other\":\"" + CREDENTIAL + "\"}");
    }

    @Test(expected = VerificationException.class)
    public void rejectsMultipleCredentialsInArray() throws Exception {
        OID4VPIdentityProviderEndpoint.extractCredential(
                "[\"" + CREDENTIAL + "\",\"" + CREDENTIAL + "\"]");
    }
}
