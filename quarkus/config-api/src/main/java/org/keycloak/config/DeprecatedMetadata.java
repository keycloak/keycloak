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

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DeprecatedMetadata {
    private final Set<String> newOptionsKeys;
    private final String note;

    public DeprecatedMetadata() {
        newOptionsKeys = Collections.emptySet();
        note = null;
    }

    public DeprecatedMetadata(Set<String> newOptionsKeys, String note) {
        this.newOptionsKeys = newOptionsKeys == null ? Collections.emptySet() : Collections.unmodifiableSet(newOptionsKeys);
        this.note = note;
    }

    public Set<String> getNewOptionsKeys() {
        return newOptionsKeys;
    }

    public String getNote() {
        return note;
    }
}
