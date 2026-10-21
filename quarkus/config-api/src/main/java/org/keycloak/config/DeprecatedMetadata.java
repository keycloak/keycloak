/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DeprecatedMetadata {
    private final List<String> newOptionsKeys;
    private final String note;
    private final Set<String> deprecatedValues;

    private DeprecatedMetadata(List<String> newOptionsKeys, String note, Set<String> deprecatedValues) {
        this.newOptionsKeys = newOptionsKeys;
        this.note = note;
        this.deprecatedValues = deprecatedValues;
    }

    public static DeprecatedMetadata deprecateOption(String note, String... newOptionsKeys) {
        return new DeprecatedMetadata(Arrays.asList(newOptionsKeys), note, Set.of());
    }

    public static DeprecatedMetadata deprecateValues(String note, String... values) {
        return new DeprecatedMetadata(Collections.emptyList(), note,
                Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(values))));
    }

    public List<String> getNewOptionsKeys() {
        return newOptionsKeys;
    }

    public String getNote() {
        return note;
    }

    public Set<String> getDeprecatedValues() {
        return deprecatedValues;
    }
}
