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

package org.keycloak.organization.validation;

import org.keycloak.organization.validation.OrganizationsValidation.OrganizationValidationException;

import org.junit.Test;

public class OrganizationsValidationTest {

    @Test
    public void validateAliasAcceptsValidOAuthScopeCharacters() {
        // RFC 6749, Section 3.3: scope-token = 1*( %x21 / %x23-5B / %x5D-7E )
        OrganizationsValidation.validateAlias("acme");
        OrganizationsValidation.validateAlias("acme-2");
        OrganizationsValidation.validateAlias("acme_alias");
        OrganizationsValidation.validateAlias("acme&@#!/:");
        OrganizationsValidation.validateAlias("~acme.alias~");
    }

    @Test
    public void validateAliasAcceptsBlankOrNull() {
        OrganizationsValidation.validateAlias(null);
        OrganizationsValidation.validateAlias("");
    }

    @Test(expected = OrganizationValidationException.class)
    public void validateAliasRejectsSpace() {
        OrganizationsValidation.validateAlias("acme alias");
    }

    @Test(expected = OrganizationValidationException.class)
    public void validateAliasRejectsNewline() {
        OrganizationsValidation.validateAlias("acme\nalias");
    }

    @Test(expected = OrganizationValidationException.class)
    public void validateAliasRejectsBackslash() {
        OrganizationsValidation.validateAlias("acme\\alias");
    }

    @Test(expected = OrganizationValidationException.class)
    public void validateAliasRejectsDoubleQuote() {
        OrganizationsValidation.validateAlias("acme\"alias");
    }
}
