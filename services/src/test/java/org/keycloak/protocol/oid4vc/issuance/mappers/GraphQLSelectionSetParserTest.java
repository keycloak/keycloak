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

package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraphQLSelectionSetParserTest {

    @Test
    public void testExampleQueryResolvesFieldNames() {
        var query = "query($id: String!) { booking(ticketRef: $id) { ticketRef flightNumber route "
                + "departureDateTime passengerName } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "booking");
        assertTrue(result.isPresent());
        assertEquals(List.of("ticketRef", "flightNumber", "route", "departureDateTime", "passengerName"), result.get());
    }

    @Test
    public void testMultiSegmentResponsePath() {
        var query = "query { typedData { count active } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "typedData");
        assertTrue(result.isPresent());
        assertEquals(List.of("count", "active"), result.get());
    }

    @Test
    public void testEmptyResponsePathUsesTopLevelSelectionSet() {
        var query = "query { roles }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, null);
        assertTrue(result.isPresent());
        assertEquals(List.of("roles"), result.get());
    }

    @Test
    public void testBlankResponsePathUsesTopLevelSelectionSet() {
        var query = "query { roles }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "");
        assertTrue(result.isPresent());
        assertEquals(List.of("roles"), result.get());
    }

    @Test
    public void testScalarFieldHasNoSelectionSet() {
        // "count" has no nested selection set, so it's a scalar/array leaf — Claim Name applies, not spread.
        var query = "query { typedData { count } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "typedData.count");
        assertFalse(result.isPresent());
    }

    @Test
    public void testAliasIsUsedAsOutputKey() {
        var query = "query { flightNum: flightNumber }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, null);
        assertTrue(result.isPresent());
        assertEquals(List.of("flightNum"), result.get());
    }

    @Test
    public void testAliasOnNavigatedField() {
        var query = "query { b: booking(ticketRef: $id) { flightNumber } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "b");
        assertTrue(result.isPresent());
        assertEquals(List.of("flightNumber"), result.get());
    }

    @Test
    public void testArgumentsAreSkipped() {
        var query = "query($id: String!) { booking(ticketRef: $id, extra: \"ignored\") { flightNumber } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "booking");
        assertTrue(result.isPresent());
        assertEquals(List.of("flightNumber"), result.get());
    }

    @Test
    public void testBracesInsideStringArgumentDoNotConfuseBalancing() {
        var query = "query { booking(ref: \"a{b}c)\") { flightNumber } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "booking");
        assertTrue(result.isPresent());
        assertEquals(List.of("flightNumber"), result.get());
    }

    @Test
    public void testCommentsAreSkipped() {
        var query = "query {\n  # a comment\n  booking { flightNumber }\n}";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "booking");
        assertTrue(result.isPresent());
        assertEquals(List.of("flightNumber"), result.get());
    }

    @Test
    public void testFragmentSpreadIsUnparseable() {
        var query = "query { booking { ...BookingFields } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "booking");
        assertFalse(result.isPresent());
    }

    @Test
    public void testUnknownResponsePathSegmentIsUnresolvable() {
        var query = "query { booking { flightNumber } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "booking.nonExistent");
        assertFalse(result.isPresent());
    }

    @Test
    public void testMalformedQueryIsUnparseable() {
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames("query { booking { ", "booking");
        assertFalse(result.isPresent());
    }

    @Test
    public void testNullQueryIsUnparseable() {
        assertFalse(GraphQLSelectionSetParser.resolveObjectFieldNames(null, "booking").isPresent());
    }

    // resolve() distinguishes SCALAR (confirmed, Claim Name is accurate) from UNRESOLVABLE (unknown, a caller
    // declaring metadata must not guess) — resolveObjectFieldNames() collapses both to empty, which is fine
    // for its callers (the reserved-claim check), but metadata declaration needs the distinction.

    @Test
    public void testResolveDistinguishesScalarFromUnresolvable() {
        var scalarQuery = "query { typedData { count } }";
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.SCALAR,
                GraphQLSelectionSetParser.resolve(scalarQuery, "typedData.count").kind());

        var fragmentQuery = "query { booking { ...BookingFields } }";
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE,
                GraphQLSelectionSetParser.resolve(fragmentQuery, "booking").kind());
    }

    @Test
    public void testResolveObjectKindCarriesFieldNames() {
        var query = "query { booking { flightNumber route } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.OBJECT, resolution.kind());
        assertEquals(List.of("flightNumber", "route"), resolution.fieldNames());
    }

    // Copilot issue #1: Variable default object mistaken for selection set
    @Test
    public void testVariableDefinitionWithDefaultObjectValue() {
        var query = "query($filter: InputType = {name: \"default\"}) { booking { flightNumber } }";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "booking");
        assertTrue(result.isPresent());
        assertEquals(List.of("flightNumber"), result.get());
    }

    // Copilot issue #2: Duplicate metadata paths from repeated GraphQL fields
    // Updated per Copilot feedback: reject repeated fields instead of deduplicating
    @Test
    public void testRepeatedFieldNamesAreUnresolvable() {
        var query = "query { user { name } user { email } user { name } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, null);
        // Repeated "user" field means we can't accurately determine metadata without merging,
        // so reject as UNRESOLVABLE
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    // Copilot issue #3: Comments with braces break selection-set parsing
    @Test
    public void testCommentWithBracesDoesNotBreakParsing() {
        var query = "query {\n  # This { is a } comment\n  booking { flightNumber }\n}";
        var result = GraphQLSelectionSetParser.resolveObjectFieldNames(query, "booking");
        assertTrue(result.isPresent());
        assertEquals(List.of("flightNumber"), result.get());
    }

    // Copilot issue #5: Whitespace response path passes validation but fails at runtime
    @Test
    public void testWhitespaceOnlyResponsePathSegmentIsUnresolvable() {
        var query = "query { booking { flightNumber } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking.  ");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    @Test
    public void testResponsePathWithBlankSegmentIsUnresolvable() {
        var query = "query { a { b { c } } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "a..b");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    // Copilot issue #6: Multiple query operations accepted without operationName
    @Test
    public void testMultipleOperationsAreUnresolvable() {
        var query = "query First { booking { flightNumber } } query Second { user { name } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    @Test
    public void testMutationDocumentIsUnresolvable() {
        var query = "mutation { updateBooking(id: 1) { flightNumber } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "updateBooking");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    @Test
    public void testDocumentWithFragmentDefinitionBeforeQueryIsUnresolvable() {
        var query = "fragment Fields on Booking { flightNumber } query { booking { ...Fields } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    // Copilot remaining issue #1: Reject directives before response-field classification
    @Test
    public void testDirectiveOnResponsePathFieldIsUnresolvable() {
        var query = "query { booking @include(if: true) { flightNumber } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    @Test
    public void testDirectiveOnNestedFieldIsUnresolvable() {
        var query = "query { booking { flightNumber @skip(if: false) } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    // Copilot remaining issue #2: Multiple query operations with names
    @Test
    public void testMultipleNamedOperationsAreUnresolvable() {
        var query = "query GetBooking { booking { flightNumber } } query GetUser { user { name } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking");
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    @Test
    public void testMultipleShortNamedOperationsAreUnresolvable() {
        // Exact case from Copilot comment
        var query = "query A { roles } query B { roles }";
        var resolution = GraphQLSelectionSetParser.resolve(query, null);
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    // Copilot remaining issue #3: Repeated parent selections omit fields from metadata
    @Test
    public void testRepeatedFieldSelectionsAreUnresolvable() {
        var query = "query { booking { flightNumber } booking { route } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, null);
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    @Test
    public void testRepeatedResponsePathFieldIsUnresolvable() {
        // Exact case from Copilot: navigating to "booking" when it appears twice with different selections
        var query = "query { booking { flightNumber } booking { route } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking");
        // Should be UNRESOLVABLE because we can't tell which fields booking actually has without merging
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }

    // Copilot final issue: Propagate scan failures to prevent incomplete metadata
    @Test
    public void testInlineFragmentCausesScanFailure() {
        // Exact case from Copilot: inline fragment "... on Query" is unsupported
        var query = "query { booking { flightNumber } ... on Query { booking { route } } }";
        var resolution = GraphQLSelectionSetParser.resolve(query, "booking");
        // Should be UNRESOLVABLE because the inline fragment can't be parsed
        assertEquals(GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE, resolution.kind());
    }
}
