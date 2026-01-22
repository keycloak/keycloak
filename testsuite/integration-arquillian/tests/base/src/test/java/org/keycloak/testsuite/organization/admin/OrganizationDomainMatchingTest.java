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

package org.keycloak.testsuite.organization.admin;

import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.organization.utils.Organizations;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for domain matching logic including wildcard subdomain support with excluded subdomains.
 * Patterns: *.domain for wildcards (with optional excludedSubdomains field), domain for exact matches.
 *
 * @author Keycloak Team
 */
public class OrganizationDomainMatchingTest {

    @Test
    public void testExactDomainMatch() {
        OrganizationDomainModel domain = new OrganizationDomainModel("example.com", true);
        
        // Exact match should work
        assertTrue(Organizations.domainMatches("example.com", domain));
        
        // Case insensitive
        assertTrue(Organizations.domainMatches("Example.COM", domain));
        assertTrue(Organizations.domainMatches("EXAMPLE.COM", domain));
        
        // Subdomain should NOT match without wildcard
        assertFalse(Organizations.domainMatches("sub.example.com", domain));
        assertFalse(Organizations.domainMatches("deep.sub.example.com", domain));
        
        // Different domain should not match
        assertFalse(Organizations.domainMatches("other.com", domain));
        assertFalse(Organizations.domainMatches("example.org", domain));
    }

    @Test
    public void testWildcardSubdomainMatch() {
        OrganizationDomainModel domain = new OrganizationDomainModel("*.example.com", true);
        
        // Exact match should still work
        assertTrue(Organizations.domainMatches("example.com", domain));
        assertTrue(Organizations.domainMatches("EXAMPLE.COM", domain));
        
        // Subdomain should match with wildcard
        assertTrue(Organizations.domainMatches("sub.example.com", domain));
        assertTrue(Organizations.domainMatches("SUB.EXAMPLE.COM", domain));
        
        // Deep subdomain should match
        assertTrue(Organizations.domainMatches("deep.sub.example.com", domain));
        assertTrue(Organizations.domainMatches("very.deep.sub.example.com", domain));
        
        // Different domain should still not match
        assertFalse(Organizations.domainMatches("other.com", domain));
        assertFalse(Organizations.domainMatches("example.org", domain));
        
        // Partial match should not work
        assertFalse(Organizations.domainMatches("notexample.com", domain));
        assertFalse(Organizations.domainMatches("example.com.fake", domain));
    }

    @Test
    public void testMicrosoftOnMicrosoftComScenario() {
        // Real-world scenario: Microsoft 365 tenants
        OrganizationDomainModel domain = new OrganizationDomainModel("*.onmicrosoft.com", true);
        
        // Tenant-specific domains should match
        assertTrue(Organizations.domainMatches("contoso.onmicrosoft.com", domain));
        assertTrue(Organizations.domainMatches("fabrikam.onmicrosoft.com", domain));
        assertTrue(Organizations.domainMatches("dev-contoso.onmicrosoft.com", domain));
        
        // Base domain should also match
        assertTrue(Organizations.domainMatches("onmicrosoft.com", domain));
        
        // Other Microsoft domains should not match
        assertFalse(Organizations.domainMatches("microsoft.com", domain));
        assertFalse(Organizations.domainMatches("outlook.com", domain));
    }

    @Test
    public void testUniversityDomainScenario() {
        // Real-world scenario: University with department subdomains
        OrganizationDomainModel domain = new OrganizationDomainModel("*.university.edu", true);
        
        // Department domains should match
        assertTrue(Organizations.domainMatches("cs.university.edu", domain));
        assertTrue(Organizations.domainMatches("math.university.edu", domain));
        assertTrue(Organizations.domainMatches("eng.university.edu", domain));
        
        // Nested departmental domains should match
        assertTrue(Organizations.domainMatches("ai.cs.university.edu", domain));
        assertTrue(Organizations.domainMatches("mail.cs.university.edu", domain));
        
        // Main domain should match
        assertTrue(Organizations.domainMatches("university.edu", domain));
        
        // Other universities should not match
        assertFalse(Organizations.domainMatches("otheruniversity.edu", domain));
    }

    @Test
    public void testNullAndEmptyValues() {
        OrganizationDomainModel domain = new OrganizationDomainModel("*.example.com", true);
        
        // Null email domain should not match
        assertFalse(Organizations.domainMatches(null, domain));
        
        // Empty string should not match
        assertFalse(Organizations.domainMatches("", domain));
        
        // Null organization domain should not match
        assertFalse(Organizations.domainMatches("example.com", null));
        
        // Null domain name should not match
        OrganizationDomainModel nullNameDomain = new OrganizationDomainModel(null, true);
        assertFalse(Organizations.domainMatches("example.com", nullNameDomain));
    }

    @Test
    public void testEdgeCases() {
        OrganizationDomainModel domain = new OrganizationDomainModel("*.example.com", true);
        
        // Single character subdomain
        assertTrue(Organizations.domainMatches("a.example.com", domain));
        
        // Numeric subdomain
        assertTrue(Organizations.domainMatches("123.example.com", domain));
        
        // Hyphenated subdomain
        assertTrue(Organizations.domainMatches("my-app.example.com", domain));
        
        // Mixed case with special subdomain
        assertTrue(Organizations.domainMatches("My-App-123.Example.COM", domain));
        
        // Should not match if there's extra content after
        assertFalse(Organizations.domainMatches("example.com.fake", domain));
    }

    @Test
    public void testExclusionPattern() {
        // Test wildcard with exclusions
        OrganizationDomainModel wildcard = new OrganizationDomainModel("*.example.com", true, 
                Set.of("admin.example.com"));
        
        // Should match non-excluded subdomains
        assertTrue(Organizations.domainMatches("users.example.com", wildcard));
        assertTrue(Organizations.domainMatches("portal.example.com", wildcard));
        assertTrue(Organizations.domainMatches("example.com", wildcard));
        
        // Should not match excluded subdomain
        assertFalse(Organizations.domainMatches("admin.example.com", wildcard));
        
        // Test cascading exclusion - subdomains of excluded domain are also excluded
        assertFalse(Organizations.domainMatches("api.admin.example.com", wildcard));
        assertFalse(Organizations.domainMatches("deep.sub.admin.example.com", wildcard));
        
        // Test with multiple exclusions
        OrganizationDomainModel multipleExclusions = new OrganizationDomainModel("*.university.edu", true,
                Set.of("alumni.university.edu", "test.university.edu"));
        
        assertTrue(Organizations.domainMatches("students.university.edu", multipleExclusions));
        assertFalse(Organizations.domainMatches("alumni.university.edu", multipleExclusions));
        assertFalse(Organizations.domainMatches("test.university.edu", multipleExclusions));
        assertFalse(Organizations.domainMatches("portal.alumni.university.edu", multipleExclusions)); // cascading
    }

    @Test
    public void testWildcardPatternValidation() {
        // Wildcard domains without base domain should be caught by validation
        // (This would be tested in integration tests with full validation)
        
        // Valid wildcard pattern
        OrganizationDomainModel wildcard = new OrganizationDomainModel("*.example.com", true);
        assertTrue(Organizations.domainMatches("sub.example.com", wildcard));
        
        // Exact match pattern
        OrganizationDomainModel exact = new OrganizationDomainModel("example.com", true);
        assertFalse(Organizations.domainMatches("sub.example.com", exact));
    }
}
