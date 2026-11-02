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
package org.keycloak.storage.ldap.mappers.membership.translation;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Translates an LDAP group name that the pattern matches as a whole into the template filled with the captures.
 */
final class NameExpression {

    private static final Pattern EMPTY = Pattern.compile("");

    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\$(\\d+)|\\$\\{([^}]*)}|(\\$\\{)|[^$]+|\\$");

    private static final String UNCLOSED_REFERENCE = "Translation template has an unclosed ${. Refer to a named capture group as ${name}.";

    private static final Pattern GROUP_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*");

    private final Pattern pattern;

    private final List<TemplatePart> template;

    private NameExpression(Pattern pattern, List<TemplatePart> template) {
        this.pattern = pattern;
        this.template = List.copyOf(template);
    }

    /**
     * A template is literal text with {@code $1} or {@code ${name}} references to the pattern's capture groups.
     */
    static Parsed<NameExpression> parse(String pattern, String template) {
        return compile(pattern).flatMap(compiled -> withTemplate(compiled, template));
    }

    /**
     * Empty when the pattern does not match the whole name, or when the filled template is blank.
     */
    Optional<String> translate(String groupName) {
        return Optional.of(pattern.matcher(groupName)).filter(Matcher::matches).map(this::fill).filter(name -> !name.isBlank());
    }

    private String fill(Matcher match) {
        return template.stream().map(part -> part.fill(match)).collect(Collectors.joining());
    }

    private static Parsed<NameExpression> withTemplate(Pattern pattern, String template) {
        return parseTemplate(pattern, template).map(parts -> new NameExpression(pattern, parts));
    }

    private static Parsed<Pattern> compile(String pattern) {
        try {
            return Parsed.valid(Pattern.compile(pattern));
        } catch (PatternSyntaxException e) {
            return Parsed.invalid("Translation pattern '" + pattern + "' is not a valid regular expression: " + e.getDescription());
        }
    }

    private static Parsed<List<TemplatePart>> parseTemplate(Pattern pattern, String template) {
        return Parsed.all(templateParts(pattern, template)).flatMap(parts -> requireCapture(template, parts));
    }

    private static List<Parsed<TemplatePart>> templateParts(Pattern pattern, String template) {
        return TEMPLATE_TOKEN.matcher(template).results().map(token -> partOf(pattern, token)).toList();
    }

    private static Parsed<List<TemplatePart>> requireCapture(String template, List<TemplatePart> parts) {
        return parts.stream().allMatch(Literal.class::isInstance) ? Parsed.invalid(noCaptureMessage(template)) : Parsed.valid(parts);
    }

    private static String noCaptureMessage(String template) {
        return "Translation template '" + template + "' refers to no capture group of the pattern, so every matching LDAP group would get the same name. Use $1 or ${name}.";
    }

    private static Parsed<TemplatePart> partOf(Pattern pattern, MatchResult token) {
        return Stream.of(Reference.values())
                .filter(reference -> token.start(reference.group) >= 0)
                .findFirst()
                .map(reference -> referenceOf(pattern, reference, token.group(reference.group)))
                .orElseGet(() -> Parsed.valid(new Literal(token.group())));
    }

    private static Parsed<TemplatePart> referenceOf(Pattern pattern, Reference reference, String text) {
        return switch (reference) {
            case NUMBERED -> numberedCapture(pattern, text);
            case NAMED -> namedCapture(pattern, text);
            case UNCLOSED -> Parsed.invalid(UNCLOSED_REFERENCE);
        };
    }

    private static Parsed<TemplatePart> numberedCapture(Pattern pattern, String digits) {
        int groupCount = pattern.matcher("").groupCount();
        return new BigInteger(digits).compareTo(BigInteger.valueOf(groupCount)) <= 0
                ? Parsed.valid(new NumberedCapture(Integer.parseInt(digits)))
                : Parsed.invalid("Translation template refers to $" + digits + ", but the pattern has " + captureGroups(groupCount) + ".");
    }

    private static String captureGroups(int count) {
        return count == 1 ? "1 capture group" : count + " capture groups";
    }

    private static Parsed<TemplatePart> namedCapture(Pattern pattern, String name) {
        return GROUP_NAME.matcher(name).matches() ? existingNamedCapture(pattern, name) : Parsed.invalid(invalidGroupNameMessage(name));
    }

    private static Parsed<TemplatePart> existingNamedCapture(Pattern pattern, String name) {
        return hasNamedGroup(pattern, name) ? Parsed.valid(new NamedCapture(name)) : Parsed.invalid(unknownGroupMessage(name));
    }

    private static String invalidGroupNameMessage(String name) {
        return "Translation template refers to ${" + name + "}, but a capture group name consists of letters and digits and starts with a letter.";
    }

    private static String unknownGroupMessage(String name) {
        return "Translation template refers to ${" + name + "}, but the pattern has no capture group named '" + name + "'.";
    }

    private static boolean hasNamedGroup(Pattern pattern, String name) {
        // Pattern.namedGroups() needs Java 20; Matcher.group(String) rejects an unknown name only while the matcher holds a match
        Matcher matched = EMPTY.matcher("");
        matched.find();
        try {
            matched.usePattern(pattern).group(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * The kinds of capture group reference in a template, by their group in {@link #TEMPLATE_TOKEN}.
     */
    private enum Reference {
        NUMBERED(1), NAMED(2), UNCLOSED(3);

        private final int group;

        Reference(int group) {
            this.group = group;
        }
    }

    sealed interface TemplatePart {

        String fill(Matcher match);
    }

    record Literal(String text) implements TemplatePart {

        @Override
        public String fill(Matcher match) {
            return text;
        }
    }

    /**
     * A group that took no part in the match fills in as empty text.
     */
    record NumberedCapture(int group) implements TemplatePart {

        @Override
        public String fill(Matcher match) {
            return Optional.ofNullable(match.group(group)).orElse("");
        }
    }

    /**
     * A group that took no part in the match fills in as empty text.
     */
    record NamedCapture(String name) implements TemplatePart {

        @Override
        public String fill(Matcher match) {
            return Optional.ofNullable(match.group(name)).orElse("");
        }
    }
}
