/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.models;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialModelBackwardsCompatibilityTest {

    @Test
    public void testCredentialModelLegacyGetterSetters() {
        CredentialModel credential = new CredentialModel();

        // Assert null can be read
        Assert.assertNull(credential.getValue());
        Assert.assertNull(credential.getDevice());
        Assert.assertNull(credential.getAlgorithm());
        Assert.assertNull(credential.getSalt());
        Assert.assertEquals(0, credential.getCounter());
        Assert.assertEquals(0, credential.getHashIterations());
        Assert.assertEquals(0, credential.getDigits());
        Assert.assertEquals(0, credential.getPeriod());

        credential.setValue("foo");
        credential.setDevice("foo-device");
        credential.setAlgorithm("foo-algorithm");
        credential.setSalt(new byte[] { 1, 2, 3});
        credential.setCounter(15);
        credential.setHashIterations(20);
        credential.setDigits(25);
        credential.setPeriod(30);

        Assert.assertEquals("foo", credential.getValue());
        Assert.assertEquals("foo-device", credential.getDevice());
        Assert.assertTrue(Arrays.equals(new byte[] { 1, 2, 3 }, credential.getSalt()));
        Assert.assertEquals(15, credential.getCounter());
        Assert.assertEquals(20, credential.getHashIterations());
        Assert.assertEquals(25, credential.getDigits());
        Assert.assertEquals(30, credential.getPeriod());

        // Set null to some values
        credential.setValue(null);
        credential.setSalt(null);
        credential.setAlgorithm(null);

        Assert.assertNull(credential.getValue());
        Assert.assertNull(credential.getAlgorithm());
        Assert.assertNull(credential.getSalt());
        Assert.assertEquals("foo-device", credential.getDevice());
    }

    @Test
    public void testCredentialModelConfigMap() {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.add("key1", "val11");
        map.add("key1", "val12");
        map.add("key2", "val21");

        CredentialModel credential = new CredentialModel();
        Assert.assertNull(credential.getConfig());
        credential.setConfig(map);

        MultivaluedHashMap<String, String> loadedMap = credential.getConfig();
        Assert.assertEquals(map, loadedMap);
    }

    @Test
    public void testCredentialModelOTP() {
        CredentialModel otp = OTPCredentialModel.createTOTP("456123", 6, 30, "someAlg");

        Assert.assertEquals("456123", otp.getValue());
        Assert.assertEquals(6, otp.getDigits());
        Assert.assertEquals(30, otp.getPeriod());
        Assert.assertEquals("someAlg", otp.getAlgorithm());

        // Change something and assert it is changed
        otp.setValue("789789");
        Assert.assertEquals("789789", otp.getValue());

        // Test clone
        OTPCredentialModel cloned = OTPCredentialModel.createFromCredentialModel(otp);
        Assert.assertEquals("789789", cloned.getOTPSecretData().getValue());
        Assert.assertEquals(6, cloned.getOTPCredentialData().getDigits());
        Assert.assertEquals("someAlg", cloned.getOTPCredentialData().getAlgorithm());
    }


    @Test
    public void testCredentialModelPassword() {
        byte[] salt = { 1, 2, 3 };
        CredentialModel password = PasswordCredentialModel.createFromValues("foo", salt, 1000, "pass");

        Assert.assertEquals("pass", password.getValue());
        Assert.assertTrue(Arrays.equals(salt, password.getSalt()));
        Assert.assertEquals(1000, password.getHashIterations());
        Assert.assertEquals("foo", password.getAlgorithm());

        // Change something and assert it is changed
        password.setValue("789789");
        Assert.assertEquals("789789", password.getValue());

        // Test clone
        PasswordCredentialModel cloned = PasswordCredentialModel.createFromCredentialModel(password);
        Assert.assertEquals("789789", cloned.getPasswordSecretData().getValue());
        Assert.assertEquals(1000, cloned.getPasswordCredentialData().getHashIterations());
        Assert.assertEquals(1000, cloned.getPasswordCredentialData().getHashIterations());
        Assert.assertEquals("foo", cloned.getPasswordCredentialData().getAlgorithm());

    }
}
