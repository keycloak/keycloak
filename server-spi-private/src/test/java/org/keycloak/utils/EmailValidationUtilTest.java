/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.utils;

import org.junit.Assert;
import org.junit.Test;

public class EmailValidationUtilTest {

    @Test
    public void testValidEmails() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user.name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user+tag@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user_name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user-name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("123@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@subdomain.example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@example.co.uk", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("a@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@ex.com", 64));
    }

    @Test
    public void testValidEmailsWithSpecialCharacters() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user!name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user#name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user$name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user%name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user&name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user'name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user*name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user=name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user?name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user^name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user`name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user{name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user|name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user}name@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user~name@example.com", 64));
    }

    @Test
    public void testValidEmailsWithQuotes() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("\"user name\"@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("\"user@name\"@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("\"user.name\"@example.com", 64));
    }

    @Test
    public void testValidEmailsWithIPAddresses() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@[192.168.1.1]", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@[255.255.255.255]", 64));
    }

    @Test
    public void testValidEmailsWithIPv6Addresses() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@[IPv6:2001:db8::1]", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@[IPv6:2001:0db8:0000:0000:0000:0000:0000:0001]", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@[IPv6:::1]", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@[IPv6:fe80::1]", 64));
    }

    @Test
    public void testValidEmailsWithInternationalCharacters() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("test@domain.jp", 64));
    }

    @Test
    public void testInvalidEmailsNullOrEmpty() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail(null, 64));
        Assert.assertFalse(EmailValidationUtil.isValidEmail("", 64));
    }

    @Test
    public void testInvalidEmailsNoAtSign() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail("userexample.com", 64));
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user", 64));
    }

    @Test
    public void testInvalidEmailsMultipleAtSigns() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@@example.com", 64));
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@name@example.com", 64));
    }

    @Test
    public void testInvalidEmailsNoLocalPart() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail("@example.com", 64));
    }

    @Test
    public void testInvalidEmailsNoDomain() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@", 64));
    }

    @Test
    public void testInvalidEmailsTrailingDot() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@example.com.", 64));
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@example.", 64));
    }

    @Test
    public void testInvalidEmailsConsecutiveDots() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user..name@example.com", 64));
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@example..com", 64));
    }

    @Test
    public void testInvalidEmailsStartingDot() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail(".user@example.com", 64));
    }

    @Test
    public void testLocalPartLengthLimit() {
        String validLocalPart = "a".repeat(64);
        Assert.assertTrue(EmailValidationUtil.isValidEmail(validLocalPart + "@example.com", 64));

        String invalidLocalPart = "a".repeat(65);
        Assert.assertFalse(EmailValidationUtil.isValidEmail(invalidLocalPart + "@example.com", 64));
    }

    @Test
    public void testCustomLocalPartLengthLimit() {
        String localPart50 = "a".repeat(50);
        Assert.assertTrue(EmailValidationUtil.isValidEmail(localPart50 + "@example.com", 50));

        String localPart51 = "a".repeat(51);
        Assert.assertFalse(EmailValidationUtil.isValidEmail(localPart51 + "@example.com", 50));

        String localPart30 = "a".repeat(30);
        Assert.assertTrue(EmailValidationUtil.isValidEmail(localPart30 + "@example.com", 30));

        String localPart31 = "a".repeat(31);
        Assert.assertFalse(EmailValidationUtil.isValidEmail(localPart31 + "@example.com", 30));
    }

    @Test
    public void testDomainPartLengthLimit() {
        String validDomain = "a".repeat(63) + ".com";
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user@" + validDomain, 64));

        String invalidDomain = "a".repeat(250) + ".com";
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@" + invalidDomain, 64));
    }

    @Test
    public void testEmailsWithSpaces() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user name@example.com", 64));
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@example .com", 64));
        Assert.assertFalse(EmailValidationUtil.isValidEmail(" user@example.com", 64));
        Assert.assertFalse(EmailValidationUtil.isValidEmail("user@example.com ", 64));
    }

    @Test
    public void testComplexValidEmails() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("user+tag+multiple@subdomain.example.co.uk", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("first.last+tag@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("1234567890@example.com", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("_______@example.com", 64));
    }

    @Test
    public void testEmailsWithMultipleAtInQuotedLocalPart() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("\"user@domain\"@example.com", 64));
    }

    @Test
    public void testCaseInsensitivity() {
        Assert.assertTrue(EmailValidationUtil.isValidEmail("User@Example.COM", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("USER@EXAMPLE.COM", 64));
        Assert.assertTrue(EmailValidationUtil.isValidEmail("uSeR@eXaMpLe.CoM", 64));
    }

    @Test
    public void testInvalidEmailDomains() {
        Assert.assertFalse(EmailValidationUtil.isValidEmail("test@example.com`", 64));
    }
}
