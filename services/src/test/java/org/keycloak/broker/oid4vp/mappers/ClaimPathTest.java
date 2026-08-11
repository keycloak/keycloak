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

package org.keycloak.broker.oid4vp.mappers;

import java.util.List;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ClaimPathTest {

    @Test
    public void parsesFieldSteps() {
        List<ClaimPath.Step> steps = ClaimPath.parse("address.locality").steps();

        assertEquals(List.of(fieldStep("address"), fieldStep("locality")), steps);
    }

    @Test
    public void parsesSelectorSteps() {
        assertEquals(List.of(fieldStep("degrees"), allElementsStep(), fieldStep("type")),
                ClaimPath.parse("degrees[].type").steps());
        assertEquals(List.of(fieldStep("degrees"), firstElementStep(), fieldStep("type")),
                ClaimPath.parse("degrees[0].type").steps());
        assertEquals(List.of(fieldStep("matrix"), allElementsStep(), allElementsStep()),
                ClaimPath.parse("matrix[][]").steps());
    }

    @Test
    public void parsesEscapedDotAsLiteralFieldName() {
        assertEquals(List.of(fieldStep("org.example")), ClaimPath.parse("org\\.example").steps());
    }

    @Test
    public void rejectsMalformedPaths() {
        assertNull(ClaimPath.parse(null));
        assertNull(ClaimPath.parse(""));
        assertNull(ClaimPath.parse("[0]"));
        assertNull(ClaimPath.parse("degrees[x]"));
        assertNull(ClaimPath.parse("degrees[1]"));
        assertNull(ClaimPath.parse("degrees[2]"));
        assertNull(ClaimPath.parse("degrees[10]"));
        assertNull(ClaimPath.parse("degrees[-1]"));
        assertNull(ClaimPath.parse("degrees[00]"));
        assertNull(ClaimPath.parse("degrees[*]"));
        assertNull(ClaimPath.parse("degrees["));
        assertNull(ClaimPath.parse("degrees[]x"));
        assertNull(ClaimPath.parse("degrees[]]"));
        assertNull(ClaimPath.parse("degrees[[]]"));
        assertNull(ClaimPath.parse("address."));
        assertNull(ClaimPath.parse("degrees[]."));
        assertNull(ClaimPath.parse("org\\."));
        assertNull(ClaimPath.parse("org\\\\."));
    }

    @Test
    public void gibberishNeverThrowsAndSelectsNothing() throws Exception {
        List<String> gibberish = List.of(
                ".", "..", "a..b", "*", "***", "\\", "a\\", "§$%&!?", "🔥💥",
                "[[[]]]", "]][[", "a[b]c[d]", "[*]", "*[*]*", "a]b[c", " ");
        for (String path : gibberish) {
            ClaimPath parsed = ClaimPath.parse(path);
            if (parsed != null) {
                assertTrue("Gibberish '" + path + "' must select nothing",
                        parsed.select(claims()).isEmpty());
            }
        }
    }

    @Test
    public void stepsAreImmutable() {
        List<ClaimPath.Step> steps = ClaimPath.parse("email").steps();

        assertThrows(UnsupportedOperationException.class, () -> steps.add(fieldStep("more")));
    }

    @Test
    public void selectsScalarAndNestedClaims() throws Exception {
        assertEquals("alice@email.cz", selectSingle("email").textValue());
        assertEquals("London", selectSingle("address.locality").textValue());
    }

    @Test
    public void selectsWholeArrayAsOneMatch() throws Exception {
        JsonNode match = selectSingle("nationalities");

        assertTrue(match.isArray());
        assertEquals(2, match.size());
    }

    @Test
    public void allElementsSelectorFansOutAndSkipsNullValues() throws Exception {
        List<JsonNode> matches = ClaimPath.parse("degrees[].type").select(claims());

        assertEquals(List.of("BSc", "MSc"), matches.stream().map(JsonNode::textValue).toList());
    }

    @Test
    public void firstElementSelectorTakesTheFirstPresentedElement() throws Exception {
        assertEquals("DE", selectSingle("nationalities[0]").textValue());
        assertEquals("BSc", selectSingle("degrees[0].type").textValue());
        assertTrue(ClaimPath.parse("none[0]").select(claims()).isEmpty());
    }

    @Test
    public void deadEndsSelectNothing() throws Exception {
        assertTrue(ClaimPath.parse("missing").select(claims()).isEmpty());
        assertTrue(ClaimPath.parse("empty").select(claims()).isEmpty());
        assertTrue(ClaimPath.parse("email.nested").select(claims()).isEmpty());
        assertTrue(ClaimPath.parse("email[]").select(claims()).isEmpty());
        assertTrue(ClaimPath.parse("address[]").select(claims()).isEmpty());
        assertTrue(ClaimPath.parse("email").select(null).isEmpty());
    }

    private static JsonNode selectSingle(String path) throws Exception {
        List<JsonNode> matches = ClaimPath.parse(path).select(claims());
        assertEquals("Expected exactly one match for " + path, 1, matches.size());
        return matches.get(0);
    }

    private static ClaimPath.Step fieldStep(String name) {
        return new ClaimPath.Step(name, ClaimPath.Step.Kind.FIELD);
    }

    private static ClaimPath.Step allElementsStep() {
        return new ClaimPath.Step(null, ClaimPath.Step.Kind.ALL_ELEMENTS);
    }

    private static ClaimPath.Step firstElementStep() {
        return new ClaimPath.Step(null, ClaimPath.Step.Kind.FIRST_ELEMENT);
    }

    private static JsonNode claims() throws Exception {
        return JsonSerialization.readValue("""
                {
                  "email": "alice@email.cz",
                  "empty": null,
                  "address": {"locality": "London"},
                  "nationalities": ["DE", "CZ"],
                  "degrees": [{"type": "BSc"}, {"type": "MSc"}, {"type": null}],
                  "none": [],
                  "org.example": "literal"
                }""", JsonNode.class);
    }
}
