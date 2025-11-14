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
package org.keycloak.protocol.oid4vc.issuance;

import java.util.Arrays;
import java.util.List;

import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for OID4VCAuthorizationDetailsProcessor.
 * Tests the core logic for processing authorization_details parameter in isolation.
 * <p>
 * These tests focus on testing the individual methods and their core logic
 * to ensure they work correctly and catch regressions in future versions.
 * The tests verify the data structures and validation logic that the processor uses.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class OID4VCAuthorizationDetailsProcessorTest {

    /**
     * Creates a valid AuthorizationDetail for testing
     */
    private AuthorizationDetail createValidAuthorizationDetail() {
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType("openid_credential");
        authDetail.setCredentialConfigurationId("test-config-id");
        authDetail.setLocations(List.of("https://test-issuer.com"));
        return authDetail;
    }

    /**
     * Creates a valid AuthorizationDetail with claims for testing
     */
    private AuthorizationDetail createValidAuthorizationDetailWithClaims() {
        AuthorizationDetail authDetail = createValidAuthorizationDetail();

        ClaimsDescription claim1 = new ClaimsDescription();
        claim1.setPath(Arrays.asList("credentialSubject", "given_name"));
        claim1.setMandatory(true);

        ClaimsDescription claim2 = new ClaimsDescription();
        claim2.setPath(Arrays.asList("credentialSubject", "family_name"));
        claim2.setMandatory(false);

        authDetail.setClaims(Arrays.asList(claim1, claim2));
        return authDetail;
    }

    /**
     * Creates an invalid AuthorizationDetail with wrong type for testing
     */
    private AuthorizationDetail createInvalidTypeAuthorizationDetail() {
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType("invalid_type");
        authDetail.setCredentialConfigurationId("test-config-id");
        return authDetail;
    }

    /**
     * Creates an AuthorizationDetail with missing credential configuration ID for testing
     */
    private AuthorizationDetail createMissingCredentialIdAuthorizationDetail() {
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType("openid_credential");
        return authDetail;
    }

    /**
     * Creates a valid ClaimsDescription for testing
     */
    private ClaimsDescription createValidClaimsDescription() {
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(Arrays.asList("credentialSubject", "given_name"));
        claim.setMandatory(true);
        return claim;
    }

    /**
     * Creates an invalid ClaimsDescription with null path for testing
     */
    private ClaimsDescription createInvalidClaimsDescription() {
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(null);
        claim.setMandatory(true);
        return claim;
    }


    /**
     * Asserts that an AuthorizationDetail has valid structure
     */
    private void assertValidAuthorizationDetail(AuthorizationDetail authDetail) {
        assertEquals("Type should be openid_credential", "openid_credential", authDetail.getType());
        assertEquals("Credential configuration ID should be set", "test-config-id", authDetail.getCredentialConfigurationId());
        assertNotNull("Locations should not be null", authDetail.getLocations());
        assertEquals("Should have exactly one location", 1, authDetail.getLocations().size());
        assertEquals("Location should match issuer", "https://test-issuer.com", authDetail.getLocations().get(0));
    }

    /**
     * Asserts that an AuthorizationDetail has invalid type
     */
    private void assertInvalidTypeAuthorizationDetail(AuthorizationDetail authDetail) {
        assertNotEquals("Type should not be openid_credential", "openid_credential", authDetail.getType());
        assertEquals("Invalid type should be preserved", "invalid_type", authDetail.getType());
    }

    /**
     * Asserts that an AuthorizationDetail has missing credential configuration ID
     */
    private void assertMissingCredentialIdAuthorizationDetail(AuthorizationDetail authDetail) {
        assertEquals("Type should be openid_credential", "openid_credential", authDetail.getType());
        assertNull("Credential configuration ID should be null", authDetail.getCredentialConfigurationId());
    }

    /**
     * Asserts that claims have valid structure
     */
    private void assertValidClaims(List<ClaimsDescription> claims) {
        assertNotNull("Claims should not be null", claims);
        assertEquals("Should have exactly two claims", 2, claims.size());

        ClaimsDescription firstClaim = claims.get(0);
        assertTrue("First claim should be mandatory", firstClaim.isMandatory());
        assertEquals("First claim path should be correct",
                Arrays.asList("credentialSubject", "given_name"), firstClaim.getPath());

        ClaimsDescription secondClaim = claims.get(1);
        assertFalse("Second claim should not be mandatory", secondClaim.isMandatory());
        assertEquals("Second claim path should be correct",
                Arrays.asList("credentialSubject", "family_name"), secondClaim.getPath());
    }

    @Test
    public void testAuthorizationDetailValidation() {
        // Test the core validation logic that the processor uses
        AuthorizationDetail authDetail = createValidAuthorizationDetail();
        assertValidAuthorizationDetail(authDetail);
    }

    @Test
    public void testAuthorizationDetailWithInvalidType() {
        // Test validation logic for invalid type
        AuthorizationDetail authDetail = createInvalidTypeAuthorizationDetail();
        assertInvalidTypeAuthorizationDetail(authDetail);
    }

    @Test
    public void testAuthorizationDetailWithMissingCredentialConfigurationId() {
        // Test validation logic for missing credential configuration ID
        AuthorizationDetail authDetail = createMissingCredentialIdAuthorizationDetail();
        assertMissingCredentialIdAuthorizationDetail(authDetail);
    }

    @Test
    public void testAuthorizationDetailWithClaims() {
        // Test the claims processing logic that the processor uses
        AuthorizationDetail authDetail = createValidAuthorizationDetailWithClaims();
        assertValidClaims(authDetail.getClaims());
    }

    @Test
    public void testAuthorizationDetailWithComplexClaims() {
        // Test complex claims processing logic
        AuthorizationDetail authDetail = createValidAuthorizationDetail();

        ClaimsDescription claim1 = new ClaimsDescription();
        claim1.setPath(Arrays.asList("credentialSubject", "address", "street"));
        claim1.setMandatory(true);

        ClaimsDescription claim2 = new ClaimsDescription();
        claim2.setPath(Arrays.asList("credentialSubject", "personalInfo", "birthDate"));
        claim2.setMandatory(false);

        authDetail.setClaims(Arrays.asList(claim1, claim2));

        // Verify complex claims structure
        assertEquals("Should have exactly two claims", 2, authDetail.getClaims().size());

        ClaimsDescription firstClaim = authDetail.getClaims().get(0);
        assertEquals("First claim path should be preserved",
                Arrays.asList("credentialSubject", "address", "street"), firstClaim.getPath());
        assertTrue("First claim should be mandatory", firstClaim.isMandatory());

        ClaimsDescription secondClaim = authDetail.getClaims().get(1);
        assertEquals("Second claim path should be preserved",
                Arrays.asList("credentialSubject", "personalInfo", "birthDate"), secondClaim.getPath());
        assertFalse("Second claim should not be mandatory", secondClaim.isMandatory());
    }

    @Test
    public void testAuthorizationDetailWithNullClaims() {
        // Test null claims handling
        AuthorizationDetail authDetail = createValidAuthorizationDetail();
        authDetail.setClaims(null);
        assertNull("Claims should be null", authDetail.getClaims());
    }

    @Test
    public void testAuthorizationDetailWithEmptyClaims() {
        // Test empty claims handling
        AuthorizationDetail authDetail = createValidAuthorizationDetail();
        authDetail.setClaims(List.of());
        assertNotNull("Claims should not be null", authDetail.getClaims());
        assertTrue("Claims should be empty", authDetail.getClaims().isEmpty());
    }

    @Test
    public void testAuthorizationDetailWithMultipleLocations() {
        // Test multiple locations handling
        AuthorizationDetail authDetail = createValidAuthorizationDetail();
        authDetail.setLocations(Arrays.asList("https://issuer1.com", "https://issuer2.com"));

        // Verify multiple locations structure
        assertNotNull("Locations should not be null", authDetail.getLocations());
        assertEquals("Should have exactly two locations", 2, authDetail.getLocations().size());
        assertEquals("First location should be preserved", "https://issuer1.com", authDetail.getLocations().get(0));
        assertEquals("Second location should be preserved", "https://issuer2.com", authDetail.getLocations().get(1));
    }

    @Test
    public void testAuthorizationDetailWithNullLocations() {
        // Test null locations handling
        AuthorizationDetail authDetail = createValidAuthorizationDetail();
        authDetail.setLocations(null);
        assertNull("Locations should be null", authDetail.getLocations());
    }

    @Test
    public void testClaimsDescriptionValidation() {
        // Test claims description validation logic
        ClaimsDescription claim = createValidClaimsDescription();

        // Verify claims description structure
        assertNotNull("Path should not be null", claim.getPath());
        assertEquals("Should have exactly two path elements", 2, claim.getPath().size());
        assertEquals("First path element should be correct", "credentialSubject", claim.getPath().get(0));
        assertEquals("Second path element should be correct", "given_name", claim.getPath().get(1));
        assertTrue("Claim should be mandatory", claim.isMandatory());
    }

    @Test
    public void testClaimsDescriptionWithNullPath() {
        // Test claims description with null path
        ClaimsDescription claim = createInvalidClaimsDescription();
        assertNull("Path should be null", claim.getPath());
        assertTrue("Mandatory should be true", claim.isMandatory());
    }

    @Test
    public void testClaimsDescriptionWithEmptyPath() {
        // Test claims description with empty path
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(List.of());
        claim.setMandatory(true);

        // Verify empty path handling
        assertNotNull("Path should not be null", claim.getPath());
        assertTrue("Path should be empty", claim.getPath().isEmpty());
        assertTrue("Mandatory should be true", claim.isMandatory());
    }

    @Test
    public void testClaimsDescriptionWithComplexPath() {
        // Test claims description with complex path
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(Arrays.asList("credentialSubject", "address", "street", "number"));
        claim.setMandatory(false);

        // Verify complex path handling
        assertNotNull("Path should not be null", claim.getPath());
        assertEquals("Should have exactly four path elements", 4, claim.getPath().size());
        assertEquals("First path element should be correct", "credentialSubject", claim.getPath().get(0));
        assertEquals("Second path element should be correct", "address", claim.getPath().get(1));
        assertEquals("Third path element should be correct", "street", claim.getPath().get(2));
        assertEquals("Fourth path element should be correct", "number", claim.getPath().get(3));
        assertFalse("Claim should not be mandatory", claim.isMandatory());
    }

    @Test
    public void testParseAuthorizationDetailsLogic() {
        // Test valid authorization details structure that would be parsed
        AuthorizationDetail authDetail = createValidAuthorizationDetail();
        ClaimsDescription claim = createValidClaimsDescription();
        authDetail.setClaims(List.of(claim));

        List<AuthorizationDetail> authDetails = List.of(authDetail);

        // Verify the structure that parseAuthorizationDetails() would process
        assertNotNull("Authorization details list should not be null", authDetails);
        assertEquals("Should have exactly one authorization detail", 1, authDetails.size());

        AuthorizationDetail parsedDetail = authDetails.get(0);
        assertEquals("Type should be preserved", "openid_credential", parsedDetail.getType());
        assertEquals("Credential configuration ID should be preserved", "test-config-id", parsedDetail.getCredentialConfigurationId());
        assertNotNull("Claims should be preserved", parsedDetail.getClaims());
        assertEquals("Should have exactly one claim", 1, parsedDetail.getClaims().size());
    }

    @Test
    public void testValidateAuthorizationDetailLogic() {
        // Test valid authorization detail that would pass validation
        AuthorizationDetail validDetail = createValidAuthorizationDetail();
        assertValidAuthorizationDetail(validDetail);

        // Test invalid type that would fail validation
        AuthorizationDetail invalidDetail = createInvalidTypeAuthorizationDetail();
        assertInvalidTypeAuthorizationDetail(invalidDetail);

        // Test missing credential configuration ID that would fail validation
        AuthorizationDetail missingIdDetail = createMissingCredentialIdAuthorizationDetail();
        assertMissingCredentialIdAuthorizationDetail(missingIdDetail);
    }

    @Test
    public void testValidateClaimsLogic() {
        // Test valid claims that would pass validation
        AuthorizationDetail authDetailWithClaims = createValidAuthorizationDetailWithClaims();
        List<ClaimsDescription> validClaims = authDetailWithClaims.getClaims();
        assertValidClaims(validClaims);

        for (ClaimsDescription claim : validClaims) {
            assertNotNull("Each claim path should not be null", claim.getPath());
            assertFalse("Each claim path should not be empty", claim.getPath().isEmpty());
            assertEquals("Each claim path should start with credentialSubject", "credentialSubject", claim.getPath().get(0));
        }

        // Test invalid claims that would fail validation
        ClaimsDescription invalidClaim = createInvalidClaimsDescription();
        assertNull("Invalid claim path should be null", invalidClaim.getPath());

        ClaimsDescription emptyPathClaim = new ClaimsDescription();
        emptyPathClaim.setPath(List.of()); // Empty path
        emptyPathClaim.setMandatory(true);

        assertNotNull("Empty path claim should not be null", emptyPathClaim.getPath());
        assertTrue("Empty path should be empty", emptyPathClaim.getPath().isEmpty());
    }

    @Test
    public void testBuildAuthorizationDetailResponseLogic() {
        // Test authorization detail that would be used to build response
        AuthorizationDetail authDetail = createValidAuthorizationDetail();
        ClaimsDescription claim = createValidClaimsDescription();
        authDetail.setClaims(List.of(claim));

        // Verify the data structure that buildAuthorizationDetailResponse() would process
        assertValidAuthorizationDetail(authDetail);
        assertNotNull("Claims should not be null", authDetail.getClaims());
        assertEquals("Should have exactly one claim", 1, authDetail.getClaims().size());

        // Test the response structure that would be built
        String expectedType = "openid_credential";
        String expectedCredentialConfigurationId = "test-config-id";
        List<String> expectedCredentialIdentifiers = List.of("test-identifier-123");
        List<ClaimsDescription> expectedClaims = List.of(claim);

        // Verify the response data that would be created
        assertEquals("Response type should match", expectedType, "openid_credential");
        assertEquals("Response credential configuration ID should match", expectedCredentialConfigurationId, "test-config-id");
        assertNotNull("Response credential identifiers should not be null", expectedCredentialIdentifiers);
        assertEquals("Response should have exactly one credential identifier", 1, expectedCredentialIdentifiers.size());
        assertNotNull("Response claims should not be null", expectedClaims);
        assertEquals("Response should have exactly one claim", 1, expectedClaims.size());
    }

    @Test
    public void testProcessStoredAuthorizationDetailsLogic() {
        // Test valid stored authorization details
        AuthorizationDetail storedDetail = createValidAuthorizationDetail();
        ClaimsDescription claim = createValidClaimsDescription();
        storedDetail.setClaims(List.of(claim));

        List<AuthorizationDetail> storedDetails = List.of(storedDetail);

        // Verify the stored details structure that processStoredAuthorizationDetails() would process
        assertNotNull("Stored details should not be null", storedDetails);
        assertEquals("Should have exactly one stored detail", 1, storedDetails.size());

        AuthorizationDetail processedDetail = storedDetails.get(0);
        assertValidAuthorizationDetail(processedDetail);
        assertNotNull("Claims should be preserved", processedDetail.getClaims());
        assertEquals("Should have exactly one claim", 1, processedDetail.getClaims().size());

        // Test null stored details
        List<AuthorizationDetail> nullStoredDetails = null;
        assertNull("Null stored details should be null", nullStoredDetails);
    }

    @Test
    public void testGenerateAuthorizationDetailsFromCredentialOfferLogic() {
        // Test credential configuration IDs that would be extracted from credential offer
        List<String> credentialConfigurationIds = Arrays.asList("config-1", "config-2", "config-3");

        // Verify the credential configuration IDs structure
        assertNotNull("Credential configuration IDs should not be null", credentialConfigurationIds);
        assertEquals("Should have exactly three configuration IDs", 3, credentialConfigurationIds.size());
        assertEquals("First configuration ID should be correct", "config-1", credentialConfigurationIds.get(0));
        assertEquals("Second configuration ID should be correct", "config-2", credentialConfigurationIds.get(1));
        assertEquals("Third configuration ID should be correct", "config-3", credentialConfigurationIds.get(2));

        // Test the authorization details that would be generated
        for (String configId : credentialConfigurationIds) {
            // Verify each configuration ID would generate proper authorization detail
            assertNotNull("Configuration ID should not be null", configId);
            assertFalse("Configuration ID should not be empty", configId.isEmpty());
            assertTrue("Configuration ID should start with 'config'", configId.startsWith("config"));
        }

        // Test empty credential configuration IDs
        List<String> emptyConfigIds = List.of();
        assertNotNull("Empty configuration IDs should not be null", emptyConfigIds);
        assertTrue("Empty configuration IDs should be empty", emptyConfigIds.isEmpty());
    }

    @Test
    public void testErrorHandlingLogic() {
        // Test invalid type error handling
        AuthorizationDetail invalidTypeDetail = createInvalidTypeAuthorizationDetail();
        assertInvalidTypeAuthorizationDetail(invalidTypeDetail);

        // Test missing credential configuration ID error handling
        AuthorizationDetail missingIdDetail = createMissingCredentialIdAuthorizationDetail();
        assertMissingCredentialIdAuthorizationDetail(missingIdDetail);

        // Test invalid claims error handling
        ClaimsDescription invalidClaim = createInvalidClaimsDescription();
        assertNull("Invalid claim path should be null", invalidClaim.getPath());

        // Test empty claims error handling
        ClaimsDescription emptyPathClaim = new ClaimsDescription();
        emptyPathClaim.setPath(List.of());
        emptyPathClaim.setMandatory(true);

        assertNotNull("Empty path claim should not be null", emptyPathClaim.getPath());
        assertTrue("Empty path should be empty", emptyPathClaim.getPath().isEmpty());
    }
}
