/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.privacy.anonymize;

import org.keycloak.events.Event;
import org.keycloak.privacy.PrivacyFilterProvider;
import org.keycloak.privacy.PrivacyTypeHints;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A {@link PrivacyFilterProvider} that uses a configured {@link Anonymizer} to obfuscate given input strings.
 * Type-hints can be used to adjust the anonymization process, see {@link PrivacyTypeHints}.
 * <p>
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public class AnonymizingPrivacyFilterProvider implements PrivacyFilterProvider {

    /**
     * Holds a set of type hints that should be considered during anonymization.
     */
    private final Set<String> typeHints;

    /**
     * Holds aliases for mapping new type hints to known type hints.
     */
    private final Map<String, String> typeAliases;

    /**
     * Holds a type hint that should be used if no type hint is provided.
     */
    private final String fallbackTypeHint;

    /**
     * Holds the {@link Anonymizer} to anonymize the given input.
     */
    private final Anonymizer anonymizer;

    /**
     * @param typeHints        set of type hints that should be considered during anonymization
     * @param typeAliases      aliases for mapping new type hints to known type hints
     * @param fallbackTypeHint type hint that should be used if no type hint is provided
     * @param anonymizer       {@link Anonymizer} to anonymize the given input
     */
    public AnonymizingPrivacyFilterProvider(Set<String> typeHints, Map<String, String> typeAliases, String fallbackTypeHint, Anonymizer anonymizer) {
        this.typeHints = Collections.unmodifiableSet(typeHints);
        this.typeAliases = Collections.unmodifiableMap(typeAliases);
        this.fallbackTypeHint = fallbackTypeHint;
        this.anonymizer = anonymizer;
    }

    /**
     * Anonymizes the given input by applying the configured {@link Anonymizer} according to the given type-hint.
     * <p>
     * Resolves the type-hint to use based on the given input and the provided type-hint, see {@link PrivacyTypeHints}.
     *
     * @param input
     * @param typeHint
     * @param userEvent the keycloak event, may be null.
     * @return
     */
    @Override
    public String filter(String input, String typeHint, Event userEvent) {

        String resolvedTypeHint = resolveTypeHint(input, typeHint);

        if (input == null || resolvedTypeHint == null || resolvedTypeHint.isEmpty() || input.isEmpty()) {
            return input;
        }

        if (PrivacyTypeHints.PLAIN.equals(resolvedTypeHint)) {
            return input;
        }

        if (!typeHints.contains(resolvedTypeHint)) {
            return input;
        }

        return anonymize(input, resolvedTypeHint, userEvent);
    }

    protected String anonymize(String input, String typeHint, Event userEvent) {
        return anonymizer.anonymize(input, typeHint);
    }

    protected String resolveTypeHint(String input, String typeHint) {

        if (input == null) {
            return null;
        }

        if (typeHint == null) {
            typeHint = fallbackTypeHint;
        }

        String typeAlias = typeAliases.get(typeHint);
        if (typeAlias != null) {
            typeHint = typeAlias;
        }

        return typeHint;
    }

    public Set<String> getTypeHints() {
        return typeHints;
    }

    public Map<String, String> getTypeAliases() {
        return typeAliases;
    }

    public String getFallbackTypeHint() {
        return fallbackTypeHint;
    }

    public Anonymizer getAnonymizer() {
        return anonymizer;
    }
}
