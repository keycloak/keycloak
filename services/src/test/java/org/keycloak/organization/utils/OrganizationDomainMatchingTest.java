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

package org.keycloak.organization.utils;

import org.keycloak.models.OrganizationDomainModel;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for domain matching logic including wildcard subdomain support with exclusion patterns.
 * Patterns: *.domain for wildcards, -.domain for exclusions, domain for exact matches.
 *
 * @author Keycloak Team
 */
public class OrganizationDomainMatchingTest {

    @Test
    public void testExactDomainMatch() {
        OrganizationDomainModel domain = new OrganizationDomainModel("example.com", true);
        
        // Exact match should work
        assertTrue(Organizations.isSameDomain("example.com", domain));
        
        // Case insensitive
        assertTrue(Organizations.isSameDomain("Example.COM", domain));
        assertTrue(Organizations.isSameDomain("EXAMPLE.COM", domain));
        
        // Subdomain should NOT match without wildcard
        assertFalse(Organizations.isSameDomain("sub.example.com", domain));
        assertFalse(Organizations.isSameDomain("deep.sub.example.com", domain));
        
        // Different domain should not match
        assertFalse(Organizations.isSameDomain("other.com", domain));
        assertFalse(Organizations.isSameDomain("example.org", domain));
    }

    @Test
    public void testWildcardSubdomainMatch() {
        OrganizationDomainModel domain = new OrganizationDomainModel("*.example.com", true);
        
        // Exact match should still work
        assertTrue(Organizations.isSameDomain("example.com", domain));
        assertTrue(Organizations.isSameDomain("EXAMPLE.COM", domain));
        
        // Subdomain should match with wildcard
        assertTrue(Organizations.isSameDomain("sub.example.com", domain));
        assertTrue(Organizations.isSameDomain("SUB.EXAMPLE.COM", domain));
        
        // Deep subdomain should match
        assertTrue(Organizations.isSameDomain("deep.sub.example.com", domain));
        assertTrue(Organizations.isSameDomain("very.deep.sub.example.com", domain));
        
        // Different domain should still not match
        assertFalse(Organizations.isSameDomain("other.com", domain));
        assertFalse(Organizations.isSameDomain("example.org", domain));
        
        // Partial match should not work
        assertFalse(Organizations.isSameDomain("notexample.com", domain));
        assertFalse(Organizations.isSameDomain("example.com.fake", domain));
    }


    @Test
    public void testNullAndEmptyValues() {
        OrganizationDomainModel domain = new OrganizationDomainModel("*.example.com", true);
        
        // Null email domain should not match
        assertFalse(Organizations.isSameDomain(null, domain));
        
        // Empty string should not match
        assertFalse(Organizations.isSameDomain("", domain));
        
        // Null organization domain should not match
        assertFalse(Organizations.isSameDomain("example.com", (OrganizationDomainModel) null));
        
        // Null domain name should not match
        OrganizationDomainModel nullNameDomain = new OrganizationDomainModel(null, true);
        assertFalse(Organizations.isSameDomain("example.com", nullNameDomain));
    }

    @Test
    public void testEdgeCases() {
        OrganizationDomainModel domain = new OrganizationDomainModel("*.example.com", true);
        
        // Single character subdomain
        assertTrue(Organizations.isSameDomain("a.example.com", domain));
        
        // Numeric subdomain
        assertTrue(Organizations.isSameDomain("123.example.com", domain));
        
        // Hyphenated subdomain
        assertTrue(Organizations.isSameDomain("my-app.example.com", domain));
        
        // Mixed case with special subdomain
        assertTrue(Organizations.isSameDomain("My-App-123.Example.COM", domain));
        
        // Should not match if there's extra content after
        assertFalse(Organizations.isSameDomain("example.com.fake", domain));
    }
}
