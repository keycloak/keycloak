/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.idm.authorization;

import java.util.Map;
import java.util.Objects;

import org.keycloak.util.EnumWithStableIndex;

/**
 * The decision strategy dictates how the policies associated with a given policy are evaluated and how a final decision
 * is obtained.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public enum Logic implements EnumWithStableIndex {

    /**
     * Defines that this policy follows a positive logic. In other words, the final decision is the policy outcome.
     */
    POSITIVE(0),

    /**
     * Defines that this policy uses a logical negation. In other words, the final decision would be a negative of the policy outcome.
     */
    NEGATIVE(1);

    private final int stableIndex;
    private static final Map<Integer, Logic> BY_ID = EnumWithStableIndex.getReverseIndex(values());

    private Logic(int stableIndex) {
        Objects.requireNonNull(stableIndex);
        this.stableIndex = stableIndex;
    }

    @Override
    public int getStableIndex() {
        return stableIndex;
    }

    public static Logic valueOfInteger(Integer id) {
        return id == null ? null : BY_ID.get(id);
    }
}
