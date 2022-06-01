/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.authorization;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum HotRodLogic {
    /**
     * Defines that this policy follows a positive logic. In other words, the final decision is the policy outcome.
     */
    @ProtoEnumValue(number = 0)
    POSITIVE,

    /**
     * Defines that this policy uses a logical negation. In other words, the final decision would be a negative of the policy outcome.
     */
    @ProtoEnumValue(number = 1)
    NEGATIVE,
}
