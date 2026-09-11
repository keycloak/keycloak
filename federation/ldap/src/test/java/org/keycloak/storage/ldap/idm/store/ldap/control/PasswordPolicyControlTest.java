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

import org.junit.Assert;
import org.junit.Test;

public class PasswordPolicyControlTest {

    @Test
    public void testDecodeResponseValue() {
        // SEQUENCE { error changeAfterReset(2) }
        PasswordPolicyControl control = new PasswordPolicyControl(new byte[] { 0x30, 0x03, (byte) 0x81, 0x01, 0x02 });
        Assert.assertTrue(control.changeAfterReset());
    }

    @Test
    public void testDecodeResponseValueWithWarningAndError() {
        // SEQUENCE { warning [0] { timeBeforeExpiration(5) }, error changeAfterReset(2) }
        PasswordPolicyControl control = new PasswordPolicyControl(new byte[] { 0x30, 0x08, (byte) 0xA0, 0x03, (byte) 0x80, 0x01, 0x05, (byte) 0x81, 0x01, 0x02 });
        Assert.assertTrue(control.changeAfterReset());
    }

    @Test
    public void testDecodeEmptySequence() {
        // SEQUENCE {} (no warning and no error)
        PasswordPolicyControl control = new PasswordPolicyControl(new byte[] { 0x30, 0x00 });
        Assert.assertFalse(control.changeAfterReset());
    }

    @Test
    public void testDecodeNullValue() {
        PasswordPolicyControl control = new PasswordPolicyControl(null);
        Assert.assertFalse(control.changeAfterReset());
    }

    @Test
    public void testDecodeEmptyValue() {
        PasswordPolicyControl control = new PasswordPolicyControl(new byte[] {});
        Assert.assertFalse(control.changeAfterReset());
    }

    @Test
    public void testDecodeErrors() {
        // Not a sequence.
        new PasswordPolicyControl(new byte[] { 0x31, 0x02, (byte) 0x81, 0x01, 0x02 });

        // Sequence with invalid length.
        new PasswordPolicyControl(new byte[] { 0x30, (byte) 0xFF, (byte) 0x82, 0x01, 0x02 });

        // Indefinite-length form (0x80) is not supported.
        new PasswordPolicyControl(new byte[] { 0x30, (byte) 0x80, (byte) 0x81, 0x01, 0x02, 0x00, 0x00 });

        // Sequence payload shorter than indicated.
        new PasswordPolicyControl(new byte[] { 0x30, 0x03 });

        // Sequence payload longer than indicated.
        new PasswordPolicyControl(new byte[] { 0x30, 0x03, (byte) 0x81, 0x01, 0x02, 0x00, 0x00 });

        // Invalid CHOICE tag.
        new PasswordPolicyControl(new byte[] { 0x30, 0x03, (byte) 0x82, 0x01, 0x02 });
    }

    @Test
    public void testDecodeErrorWithOversizedBERLength() {
        // 0x30 0x06 = SEQUENCE (startSequence discards the length without bounds-checking)
        // 0x81      = error [1] tag
        // 0x84 0x7F 0xFF 0xFF 0xFF = long-form length: numBytes=4, value=Integer.MAX_VALUE
        PasswordPolicyControl control = new PasswordPolicyControl(
                new byte[] { 0x30, 0x06, (byte) 0x81, (byte) 0x84, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
        Assert.assertFalse(control.changeAfterReset());
    }

    @Test
    public void testDecodeErrorWithOversizedShortFormBERLength() {
        // 0x30 0x02 = SEQUENCE, length 2
        // 0x81      = error [1] tag
        // 0x0A      = short-form length: value=10, but 0 bytes remain in the buffer
        PasswordPolicyControl control = new PasswordPolicyControl(
                new byte[] { 0x30, 0x02, (byte) 0x81, 0x0A });
        Assert.assertFalse(control.changeAfterReset());
    }

    @Test
    public void testDecodeWrappedWarningGraceDoesNotForgeError() {
        // SEQUENCE { warning [0] { graceAuthNsRemaining(2) } } — grace value must not be misread as error changeAfterReset(2).
        PasswordPolicyControl control = new PasswordPolicyControl(
                new byte[] { 0x30, 0x05, (byte) 0xA0, 0x03, (byte) 0x81, 0x01, 0x02 });
        Assert.assertFalse(control.changeAfterReset());
    }

    @Test
    public void testDecodeWrappedWarningTimeBeforeExpiration() {
        // SEQUENCE { warning [0] { timeBeforeExpiration(42) } }
        PasswordPolicyControl control = new PasswordPolicyControl(
                new byte[] { 0x30, 0x05, (byte) 0xA0, 0x03, (byte) 0x80, 0x01, 0x2A });
        Assert.assertFalse(control.changeAfterReset());
    }

    @Test
    public void testDecodeWrappedWarningDoesNotSuppressError() {
        // SEQUENCE { warning [0] { graceAuthNsRemaining(5) }, error changeAfterReset(2) }
        PasswordPolicyControl control = new PasswordPolicyControl(
                new byte[] { 0x30, 0x08, (byte) 0xA0, 0x03, (byte) 0x81, 0x01, 0x05, (byte) 0x81, 0x01, 0x02 });
        Assert.assertTrue(control.changeAfterReset());
    }

    @Test
    public void testDecodeZeroLengthErrorElement() {
        // Zero-length error [1] value — treated as a decode failure, no uncaught NumberFormatException.
        PasswordPolicyControl control = new PasswordPolicyControl(
                new byte[] { 0x30, 0x02, (byte) 0x81, 0x00 });
        Assert.assertFalse(control.changeAfterReset());
    }

 }
