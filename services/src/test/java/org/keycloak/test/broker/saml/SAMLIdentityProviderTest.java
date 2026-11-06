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
package org.keycloak.test.broker.saml;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link SAMLIdentityProviderConfig} covering configuration defaults
 * and behavior when properties are absent.
 */
public class SAMLIdentityProviderTest {

    @Test
    public void validateSignature_defaultsToTrueWhenAbsent() {
        SAMLIdentityProviderConfig config = new SAMLIdentityProviderConfig(new IdentityProviderModel());

        Assert.assertTrue(config.isValidateSignature());

        config.setValidateSignature(false);
        Assert.assertFalse(config.isValidateSignature());
    }

    @Test
    public void wantAssertionsSigned_defaultsToTrueWhenAbsent() {
        SAMLIdentityProviderConfig config = new SAMLIdentityProviderConfig(new IdentityProviderModel());

        Assert.assertTrue(config.isWantAssertionsSigned());

        config.setWantAssertionsSigned(false);
        Assert.assertFalse(config.isWantAssertionsSigned());
    }

    @Test
    public void validateSignature_respectsExplicitTrue() {
        SAMLIdentityProviderConfig config = new SAMLIdentityProviderConfig(new IdentityProviderModel());

        config.setValidateSignature(true);
        Assert.assertTrue(config.isValidateSignature());
    }

    @Test
    public void wantAssertionsSigned_respectsExplicitTrue() {
        SAMLIdentityProviderConfig config = new SAMLIdentityProviderConfig(new IdentityProviderModel());

        config.setWantAssertionsSigned(true);
        Assert.assertTrue(config.isWantAssertionsSigned());
    }
}
