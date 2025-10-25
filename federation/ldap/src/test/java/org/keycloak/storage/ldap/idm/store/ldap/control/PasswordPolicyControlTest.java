/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.ldap.idm.store.ldap.control;

import org.junit.Test;
import org.junit.Assert;

public class PasswordPolicyControlTest {

    @Test
    public void testDecodeResponseValue() {
        PasswordPolicyControl control = new PasswordPolicyControl(new byte[] { 0x30, 0x03, (byte) 0x81, 0x01, 0x02 });
        Assert.assertTrue(control.changeAfterReset());
    }

    @Test
    public void testDecodeErrors() {
        // Not a sequence.
        new PasswordPolicyControl(new byte[] { 0x31, 0x02, (byte) 0x81, 0x01, 0x02 });

        // Sequence with invalid length.
        new PasswordPolicyControl(new byte[] { 0x30, (byte) 0xFF, (byte) 0x82, 0x01, 0x02 });

        // Sequence payload shorter than indicated.
        new PasswordPolicyControl(new byte[] { 0x30, 0x03 });

        // Sequence payload longer than indicated.
        new PasswordPolicyControl(new byte[] { 0x30, 0x03, (byte) 0x81, 0x01, 0x02, 0x00, 0x00 });

        // Invalid CHOICE tag.
        new PasswordPolicyControl(new byte[] { 0x30, 0x03, (byte) 0x82, 0x01, 0x02 });
    }


 }
