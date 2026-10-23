/*
 * Copyright 2026 Andrzej Świętek
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
package org.keycloak.broker.oidc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.OAuth2Constants;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Static parameters an identity provider adds to the authorization code and refresh token requests it sends to the
 * token endpoint of the external provider, parsed from the {@link OAuth2IdentityProviderConfig#TOKEN_REQUEST_PARAMETERS}
 * setting by {@link #parse(String, Set)}.
 */
public final class TokenRequestParameters {

    public static final Set<String> RESERVED_NAMES = Set.of(
            OAuth2Constants.CODE,
            OAuth2Constants.REDIRECT_URI,
            OAuth2Constants.GRANT_TYPE,
            OAuth2Constants.CODE_VERIFIER,
            OAuth2Constants.REFRESH_TOKEN,
            OAuth2Constants.CLIENT_ID,
            OAuth2Constants.CLIENT_SECRET,
            OAuth2Constants.CLIENT_ASSERTION,
            OAuth2Constants.CLIENT_ASSERTION_TYPE);

    private static final String MAP_ENTRY_KEY = "key";
    private static final String MAP_ENTRY_VALUE = "value";

    private static final ObjectReader STRICT_READER = JsonSerialization.mapper.reader()
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private static final TokenRequestParameters NONE = new TokenRequestParameters(List.of(), List.of());

    private static final TokenRequestParameters MALFORMED = new TokenRequestParameters(List.of(), List.of(malformed()));

    private final List<Parameter> parameters;
    private final List<Problem> problems;

    private TokenRequestParameters(List<Parameter> parameters, List<Problem> problems) {
        this.parameters = List.copyOf(parameters);
        this.problems = List.copyOf(problems);
    }

    private record Parameter(String name, String value) {
    }

    /**
     * @param message describes a configured entry that is not sent; names in it have whitespace replaced by spaces,
     *                quotes escaped and control characters and ANSI escape sequences removed
     */
    public record Problem(String message) {
    }

    private record NameRule(Predicate<String> violated, Function<String, Problem> problem) {
    }

    private record Checked(Parameter parameter, Optional<Problem> problem) {
        boolean accepted() {
            return problem.isEmpty();
        }
    }

    /**
     * @param stored the setting in the {@link org.keycloak.provider.ProviderConfigProperty#MAP_TYPE} format; empty
     *               means no parameters, any other value that is not a JSON list of text name/value pairs is a
     *               problem; names and values are kept as they are
     * @param providerReservedNames names the identity provider sets itself, reserved in addition to
     *                              {@link #RESERVED_NAMES}
     */
    public static TokenRequestParameters parse(String stored, Set<String> providerReservedNames) {
        return stored.isEmpty() ? NONE : parseStored(stored, reservedNames(providerReservedNames));
    }

    /**
     * @return the configured entries that are not sent, ordered by name
     */
    public List<Problem> problems() {
        return problems;
    }

    /**
     * Adds the configured entries that have no problem to the form body of {@code request}, ordered by name.
     */
    public void addTo(SimpleHttpRequest request) {
        parameters.forEach(parameter -> request.param(parameter.name(), parameter.value()));
    }

    private static Predicate<String> reservedNames(Set<String> providerReservedNames) {
        return name -> RESERVED_NAMES.contains(name) || providerReservedNames.contains(name);
    }

    private static TokenRequestParameters parseStored(String stored, Predicate<String> reserved) {
        return deserialize(stored)
                .map(valuesByName -> classify(valuesByName, reserved))
                .orElse(MALFORMED);
    }

    private static Optional<SortedMap<String, List<String>>> deserialize(String stored) {
        return readTree(stored)
                .filter(JsonNode::isArray)
                .map(TokenRequestParameters::elementsOf)
                .filter(TokenRequestParameters::allTextPairs)
                .map(TokenRequestParameters::valuesByName);
    }

    private static Optional<JsonNode> readTree(String stored) {
        try {
            return Optional.of(STRICT_READER.readTree(stored));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static List<JsonNode> elementsOf(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).toList();
    }

    private static boolean allTextPairs(List<JsonNode> elements) {
        return elements.stream().allMatch(TokenRequestParameters::isTextPair);
    }

    private static boolean isTextPair(JsonNode element) {
        return element.path(MAP_ENTRY_KEY).isTextual() && element.path(MAP_ENTRY_VALUE).isTextual();
    }

    private static SortedMap<String, List<String>> valuesByName(List<JsonNode> textPairs) {
        return textPairs.stream()
                .collect(Collectors.groupingBy(TokenRequestParameters::nameOf, TreeMap::new,
                        Collectors.mapping(TokenRequestParameters::valueOf, Collectors.toUnmodifiableList())));
    }

    private static String nameOf(JsonNode textPair) {
        return textPair.path(MAP_ENTRY_KEY).textValue();
    }

    private static String valueOf(JsonNode textPair) {
        return textPair.path(MAP_ENTRY_VALUE).textValue();
    }

    private static TokenRequestParameters classify(SortedMap<String, List<String>> valuesByName, Predicate<String> reserved) {
        List<NameRule> rules = nameRules(reserved, duplicatedNames(valuesByName));
        List<Checked> checked = checkedEntries(valuesByName, rules);
        return new TokenRequestParameters(acceptedParameters(checked), problemsOf(checked));
    }

    private static Set<String> duplicatedNames(SortedMap<String, List<String>> valuesByName) {
        return valuesByName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<NameRule> nameRules(Predicate<String> reserved, Set<String> duplicated) {
        return List.of(
                new NameRule(TokenRequestParameters::isBlank, name -> blank()),
                new NameRule(TokenRequestParameters::hasSurroundingWhitespace, TokenRequestParameters::surroundingWhitespace),
                new NameRule(reserved, TokenRequestParameters::reserved),
                new NameRule(duplicated::contains, TokenRequestParameters::duplicate));
    }

    private static List<Checked> checkedEntries(SortedMap<String, List<String>> valuesByName, List<NameRule> rules) {
        return valuesByName.entrySet().stream()
                .map(entry -> checked(entry, rules))
                .toList();
    }

    private static Checked checked(Map.Entry<String, List<String>> entry, List<NameRule> rules) {
        return new Checked(new Parameter(entry.getKey(), entry.getValue().get(0)), problemOf(entry.getKey(), rules));
    }

    private static Optional<Problem> problemOf(String name, List<NameRule> rules) {
        return rules.stream()
                .filter(rule -> rule.violated().test(name))
                .map(rule -> rule.problem().apply(name))
                .findFirst();
    }

    private static List<Parameter> acceptedParameters(List<Checked> checked) {
        return checked.stream()
                .filter(Checked::accepted)
                .map(Checked::parameter)
                .toList();
    }

    private static List<Problem> problemsOf(List<Checked> checked) {
        return checked.stream()
                .map(Checked::problem)
                .flatMap(Optional::stream)
                .toList();
    }

    private static boolean isBlank(String name) {
        return name.codePoints().allMatch(TokenRequestParameters::isSpace);
    }

    private static boolean hasSurroundingWhitespace(String name) {
        return !name.isEmpty() && (isSpace(name.codePointAt(0)) || isSpace(name.codePointBefore(name.length())));
    }

    /**
     * {@link Character#isWhitespace(int)} excludes no-break spaces, {@link Character#isSpaceChar(int)} excludes tabs
     * and line breaks.
     */
    private static boolean isSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    private static Problem malformed() {
        return new Problem("the value is not a list of name/value pairs with text names and values");
    }

    private static Problem blank() {
        return new Problem("a parameter has a blank name");
    }

    private static Problem surroundingWhitespace(String name) {
        return new Problem(quoted(name) + " has leading or trailing whitespace");
    }

    private static Problem reserved(String name) {
        return new Problem(quoted(name) + " is set by Keycloak and cannot be configured");
    }

    private static Problem duplicate(String name) {
        return new Problem(quoted(name) + " is configured more than once");
    }

    /**
     * Problem messages reach the server log, so line breaks and other whitespace become spaces, quotes are escaped and
     * remaining control characters are removed.
     */
    private static String quoted(String name) {
        return "'" + StringUtil.removeControlCharacters(StringUtil.sanitizeSpacesAndQuotes(name, '\'')) + "'";
    }
}
