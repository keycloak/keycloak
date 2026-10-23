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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Extracts the set of field (or alias) names a configured GraphQL query selects at a given Response Path,
 * without executing the query. GraphQL cannot return a field that wasn't requested, so the selection set in
 * the query text is both a reliable and an exhaustive description of what the response at that path can
 * contain — used by {@link OID4VCGraphQLClaimMapper} to reject reserved claim names and declare issuer
 * metadata before ever calling the external endpoint, instead of only finding out at issuance time.
 *
 * <p>Deliberately minimal: this only navigates selection sets (field names, aliases, and skipping over
 * arguments/variable-definitions as opaque parenthesized blocks) to answer "what does the response at this
 * path look like". It does not interpret argument values, directives, or fragments — a query using any of
 * those, or anything else outside this grammar, is reported as unparseable ({@link Optional#empty()}) rather
 * than guessed at. Callers fall back to runtime-only checks when that happens.
 */
final class GraphQLSelectionSetParser {

    private GraphQLSelectionSetParser() {
    }

    /**
     * The three things a caller might need to know about the response at a configured Response Path, without
     * running the query: {@code UNRESOLVABLE} (the query or path doesn't parse under this class's minimal
     * grammar — callers can't tell anything, so fall back to runtime-only checks), {@code SCALAR} (the path
     * resolves to a field with no nested selection set — a Scalar/Array response, where Claim Name applies),
     * or {@code OBJECT} (the field has a nested selection set — {@link #fieldNames} are its immediate
     * children's output keys). These are deliberately distinct: an {@code UNRESOLVABLE} query might still turn
     * out to be an Object response at runtime, so a caller declaring metadata must not treat it the same as a
     * confirmed {@code SCALAR}.
     */
    record Resolution(Kind kind, List<String> fieldNames) {
        enum Kind { UNRESOLVABLE, SCALAR, OBJECT }

        static final Resolution UNRESOLVABLE = new Resolution(Kind.UNRESOLVABLE, null);
        static final Resolution SCALAR = new Resolution(Kind.SCALAR, null);

        static Resolution object(List<String> fieldNames) {
            return new Resolution(Kind.OBJECT, fieldNames);
        }
    }

    static Resolution resolve(String query, String responsePath) {
        if (query == null) {
            return Resolution.UNRESOLVABLE;
        }
        var trimmed = query.stripLeading();

        // Enforce single operation: document must start with exactly one 'query' keyword followed by
        // optional name and optional variable definitions, then selection set. Multiple operations,
        // or fragments/directives before the operation, make the query UNRESOLVABLE.
        if (!trimmed.toLowerCase().startsWith("query")) {
            return Resolution.UNRESOLVABLE;
        }

        // Skip past 'query' keyword and any operation name to find variable definitions or selection set.
        // Variable definitions look like ($var: Type, $var2: Type = {default: "value"}). The opening
        // brace of a default value like {default: "value"} is NOT the selection set — skip the entire
        // (...) block first before looking for the selection set's opening brace.
        var i = 5; // after "query"
        i = skipTrivia(trimmed, i);

        // Skip optional operation name
        if (i < trimmed.length() && isNameStart(trimmed.charAt(i))) {
            i = scanName(trimmed, i);
            i = skipTrivia(trimmed, i);
        }

        // Skip optional variable definitions: ($var: Type, ...)
        if (i < trimmed.length() && trimmed.charAt(i) == '(') {
            i = skipBalanced(trimmed, i, '(', ')');
            if (i < 0) {
                return Resolution.UNRESOLVABLE;
            }
            i = skipTrivia(trimmed, i);
        }

        // Now find the selection set opening brace
        if (i >= trimmed.length() || trimmed.charAt(i) != '{') {
            return Resolution.UNRESOLVABLE;
        }
        var selectionSetStart = i;

        // Skip past the operation's selection set to check for additional operations
        var afterSelectionSet = skipBalanced(trimmed, selectionSetStart, '{', '}');
        if (afterSelectionSet < 0) {
            return Resolution.UNRESOLVABLE;
        }

        // Check if there's another operation definition after this one. Multiple operations in a single
        // document require an operationName parameter, which this mapper doesn't support. Scan for another
        // 'query' keyword that's not inside a string or comment.
        if (containsOperationKeyword(trimmed, afterSelectionSet)) {
            return Resolution.UNRESOLVABLE;
        }

        // Parse and validate response path segments, trimming and rejecting blank segments
        var segments = new String[0];
        if (responsePath != null && !responsePath.isBlank()) {
            var rawSegments = responsePath.split("\\.");
            var validSegments = new ArrayList<String>();
            for (var seg : rawSegments) {
                var trimmedSeg = seg.trim();
                if (trimmedSeg.isEmpty()) {
                    // Whitespace-only segment like "  " passes split but fails at runtime navigation
                    return Resolution.UNRESOLVABLE;
                }
                validSegments.add(trimmedSeg);
            }
            segments = validSegments.toArray(new String[0]);
        }

        for (var idx = 0; idx < segments.length; idx++) {
            // Check if this segment appears multiple times in the current selection set. If it does,
            // we can't accurately determine its fields without merging all occurrences (which GraphQL
            // does at runtime), so reject as UNRESOLVABLE. Also reject if the scan encountered an
            // unsupported construct (null return = scan failure).
            var multipleOccurrences = fieldAppearsMultipleTimes(trimmed, selectionSetStart, segments[idx]);
            if (multipleOccurrences == null || multipleOccurrences) {
                return Resolution.UNRESOLVABLE;
            }
            var field = findField(trimmed, selectionSetStart, segments[idx]);
            if (field == null) {
                return Resolution.UNRESOLVABLE;
            }
            var isLastSegment = idx == segments.length - 1;
            if (field.nestedSelectionSetStart() < 0) {
                // A leaf field: a Scalar/Array response if this was the last segment, otherwise there's
                // nothing to descend into for the remaining segments — a genuine path/query mismatch.
                return isLastSegment ? Resolution.SCALAR : Resolution.UNRESOLVABLE;
            }
            selectionSetStart = field.nestedSelectionSetStart();
            if (isLastSegment) {
                return collectFieldNames(trimmed, selectionSetStart).<Resolution>map(Resolution::object)
                        .orElse(Resolution.UNRESOLVABLE);
            }
        }

        // No Response Path segments: the virtual root is the operation's own top-level selection set (same
        // as how navigating the runtime response with an empty/null path returns the 'data' object as-is).
        return collectFieldNames(trimmed, selectionSetStart).<Resolution>map(Resolution::object)
                .orElse(Resolution.UNRESOLVABLE);
    }

    /**
     * Convenience for callers that only care whether the response is an Object (and what its fields are),
     * treating an unparseable query the same as a confirmed Scalar/Array response. See {@link #resolve} for
     * callers (like metadata declaration) that need to tell those two apart.
     */
    static Optional<List<String>> resolveObjectFieldNames(String query, String responsePath) {
        var resolution = resolve(query, responsePath);
        return resolution.kind() == Resolution.Kind.OBJECT ? Optional.of(resolution.fieldNames()) : Optional.empty();
    }

    private record Field(String outputKey, int nestedSelectionSetStart) {
    }

    /**
     * Checks whether a field with the given output key appears more than once in the selection set.
     * GraphQL merges repeated fields at runtime, but we can't accurately determine the merged fields
     * without parsing all occurrences, so callers should reject such queries as UNRESOLVABLE.
     *
     * @return null if the selection set contains unsupported constructs (scan failed), true if the
     *         field appears multiple times, false if it appears at most once
     */
    private static Boolean fieldAppearsMultipleTimes(String s, int selectionSetStart, String targetOutputKey) {
        var count = new int[1];
        var ok = iterateSelectionSet(s, selectionSetStart, field -> {
            if (field.outputKey().equals(targetOutputKey)) {
                count[0]++;
                if (count[0] > 1) {
                    return true; // Stop early, found duplicate
                }
            }
            return false;
        });
        // If scan failed (encountered unsupported construct like inline fragment), propagate failure
        if (!ok) {
            return null;
        }
        return count[0] > 1;
    }

    /**
     * Finds the immediate child field of the selection set starting at {@code s.charAt(selectionSetStart) ==
     * '{'} whose output key (alias, or name if unaliased) equals {@code targetOutputKey}. Returns {@code
     * null} if not found, or if the selection set can't be parsed under this class's minimal grammar.
     *
     * <p>When the same field appears multiple times (e.g., {@code booking { flightNumber } booking { route }}),
     * GraphQL merges their selection sets at runtime. This method stops at the first occurrence; callers that
     * need all fields from a merged result should use {@link #collectFieldNames} or reject repeated fields.
     */
    private static Field findField(String s, int selectionSetStart, String targetOutputKey) {
        var result = new Field[1];
        var ok = iterateSelectionSet(s, selectionSetStart, field -> {
            if (field.outputKey().equals(targetOutputKey)) {
                result[0] = field;
                return true;
            }
            return false;
        });
        return ok ? result[0] : null;
    }

    private static Optional<List<String>> collectFieldNames(String s, int selectionSetStart) {
        // Collect field names, checking for repeated selections of the same field. GraphQL allows this
        // (e.g., `booking { flightNumber } booking { route }`) and merges the selection sets at runtime,
        // but accurately declaring the merged result would require parsing and merging all occurrences.
        // Instead, reject queries with repeated field selections as UNRESOLVABLE so metadata stays accurate.
        var names = new LinkedHashSet<String>();
        var seenCount = new int[1];
        var ok = iterateSelectionSet(s, selectionSetStart, field -> {
            seenCount[0]++;
            if (!names.add(field.outputKey())) {
                // Same field name appeared twice — reject to avoid incomplete metadata
                return true; // Signal failure by stopping early
            }
            return false;
        });
        // If we stopped early due to a duplicate, report UNRESOLVABLE
        if (ok && names.size() < seenCount[0]) {
            return Optional.empty();
        }
        return ok ? Optional.of(new ArrayList<>(names)) : Optional.empty();
    }

    private interface FieldVisitor {
        /** @return true to stop iterating (the field being looked for was found). */
        boolean visit(Field field);
    }

    /**
     * Iterates the immediate field children of the selection set starting at {@code s.charAt(start) == '{'},
     * skipping over (not descending into) each field's own arguments and nested selection set.
     *
     * @return true if the selection set was fully parsed (whether or not the visitor stopped early); false
     *         if it uses a construct outside this class's minimal grammar (e.g. a fragment spread or
     *         directive) or is malformed.
     */
    private static boolean iterateSelectionSet(String s, int start, FieldVisitor visitor) {
        var len = s.length();
        var i = skipTrivia(s, start + 1);
        while (i < len) {
            var c = s.charAt(i);
            if (c == '}') {
                return true;
            }
            if (!isNameStart(c)) {
                return false;
            }

            var nameEnd = scanName(s, i);
            var firstName = s.substring(i, nameEnd);
            i = skipTrivia(s, nameEnd);

            var outputKey = firstName;
            if (i < len && s.charAt(i) == ':') {
                i = skipTrivia(s, i + 1);
                if (i >= len || !isNameStart(s.charAt(i))) {
                    return false;
                }
                var fieldNameEnd = scanName(s, i);
                i = skipTrivia(s, fieldNameEnd);
            }

            if (i < len && s.charAt(i) == '(') {
                i = skipBalanced(s, i, '(', ')');
                if (i < 0) {
                    return false;
                }
                i = skipTrivia(s, i);
            }

            // Reject directives (@include, @skip, etc.) before checking for nested selection sets.
            // A directive makes the field's presence conditional, so we can't statically determine
            // what the response will contain — report UNRESOLVABLE instead.
            if (i < len && s.charAt(i) == '@') {
                return false;
            }

            var nestedStart = -1;
            if (i < len && s.charAt(i) == '{') {
                nestedStart = i;
                i = skipBalanced(s, i, '{', '}');
                if (i < 0) {
                    return false;
                }
                i = skipTrivia(s, i);
            }

            if (visitor.visit(new Field(outputKey, nestedStart))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scans from {@code start} onwards for another operation keyword (query, mutation, subscription),
     * skipping over strings and comments. Returns true if found, indicating multiple operations.
     */
    private static boolean containsOperationKeyword(String s, int start) {
        var i = start;
        var len = s.length();
        while (i < len) {
            // Skip comments
            if (s.charAt(i) == '#') {
                while (i < len && s.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            // Skip block strings
            if (s.startsWith("\"\"\"", i)) {
                var end = s.indexOf("\"\"\"", i + 3);
                if (end < 0) return false;
                i = end + 3;
                continue;
            }
            // Skip strings
            if (s.charAt(i) == '"') {
                i++;
                while (i < len && s.charAt(i) != '"' && s.charAt(i) != '\n') {
                    if (s.charAt(i) == '\\') i++;
                    i++;
                }
                if (i < len && s.charAt(i) == '"') i++;
                continue;
            }
            // Check for operation keywords at word boundaries
            if (isNameStart(s.charAt(i))) {
                var wordEnd = scanName(s, i);
                var word = s.substring(i, wordEnd).toLowerCase();
                if (word.equals("query") || word.equals("mutation") || word.equals("subscription")) {
                    return true;
                }
                i = wordEnd;
                continue;
            }
            i++;
        }
        return false;
    }

    private static boolean isNameStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static int scanName(String s, int start) {
        var i = start + 1;
        while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) {
            i++;
        }
        return i;
    }

    /** Skips whitespace, commas, and '#'-to-end-of-line comments. */
    private static int skipTrivia(String s, int i) {
        var len = s.length();
        while (i < len) {
            var c = s.charAt(i);
            if (Character.isWhitespace(c) || c == ',') {
                i++;
            } else if (c == '#') {
                while (i < len && s.charAt(i) != '\n') {
                    i++;
                }
            } else {
                break;
            }
        }
        return i;
    }

    /**
     * Skips from {@code s.charAt(openIndex) == open} to just past its matching {@code close}, treating
     * string, block-string, and comment contents as opaque so a brace/paren inside any of those isn't
     * counted toward balance tracking.
     *
     * @return the index just past the matching closer, or -1 if unterminated.
     */
    private static int skipBalanced(String s, int openIndex, char open, char close) {
        var len = s.length();
        var depth = 0;
        var i = openIndex;
        while (i < len) {
            // Skip comments before checking anything else, so braces in comments don't affect balance
            if (s.charAt(i) == '#') {
                while (i < len && s.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (s.startsWith("\"\"\"", i)) {
                var end = s.indexOf("\"\"\"", i + 3);
                if (end < 0) {
                    return -1;
                }
                i = end + 3;
                continue;
            }
            var c = s.charAt(i);
            if (c == '"') {
                var j = i + 1;
                while (j < len && s.charAt(j) != '"' && s.charAt(j) != '\n') {
                    if (s.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                if (j >= len || s.charAt(j) != '"') {
                    return -1;
                }
                i = j + 1;
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
            i++;
        }
        return -1;
    }
}
