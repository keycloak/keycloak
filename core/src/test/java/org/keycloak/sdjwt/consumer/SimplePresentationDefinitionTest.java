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

package org.keycloak.sdjwt.consumer;

import org.keycloak.common.VerificationException;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.sdjwt.TestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class SimplePresentationDefinitionTest {

    private final ObjectMapper mapper = SdJwtUtils.mapper;

    @Test
    public void testCheckIfSatisfiedBy() throws VerificationException, JsonProcessingException {
        SimplePresentationDefinition definition = SimplePresentationDefinition.builder()
                .addClaimRequirement("vct", ".*identity_credential.*")
                .addClaimRequirement("given_name", "\"John\"")
                .addClaimRequirement("cat", "123")
                .addClaimRequirement("addr", ".*\"(Douala|Berlin)\".*")
                .addClaimRequirement("colors", "\\[\"red\",.*")
                .build();

        definition.checkIfSatisfiedBy(exampleDisclosedPayload());
    }

    @Test
    public void testCheckIfSatisfiedBy_shouldFailOnRequiredFieldMissing() {
        SimplePresentationDefinition definition = SimplePresentationDefinition.builder()
                .addClaimRequirement("family_name", ".*")
                .build();

        VerificationException exception = assertThrows(VerificationException.class,
                () -> definition.checkIfSatisfiedBy(exampleDisclosedPayload()));

        assertEquals("A required field was not presented: `family_name`", exception.getMessage());
    }

    @Test
    public void testCheckIfSatisfiedBy_shouldFailOnNonMatchingPattern() {
        SimplePresentationDefinition definition = SimplePresentationDefinition.builder()
                .addClaimRequirement("vct", ".*diploma.*")
                .build();

        VerificationException exception = assertThrows(VerificationException.class,
                () -> definition.checkIfSatisfiedBy(exampleDisclosedPayload()));

        assertThat(exception.getMessage(), startsWith("Pattern matching failed for required field"));
    }

    private JsonNode exampleDisclosedPayload() throws JsonProcessingException {
        String content = TestUtils.readFileAsString(getClass(),
                "sdjwt/s7.4-sample-disclosed-issuer-payload.json");
        return mapper.readTree(content);
    }
}
