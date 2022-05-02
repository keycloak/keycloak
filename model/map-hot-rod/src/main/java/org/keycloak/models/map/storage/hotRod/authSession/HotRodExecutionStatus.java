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

package org.keycloak.models.map.storage.hotRod.authSession;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum HotRodExecutionStatus {

    @ProtoEnumValue(number = 0)
    FAILED,

    @ProtoEnumValue(number = 1)
    SUCCESS,

    @ProtoEnumValue(number = 2)
    SETUP_REQUIRED,

    @ProtoEnumValue(number = 3)
    ATTEMPTED,

    @ProtoEnumValue(number = 4)
    SKIPPED,

    @ProtoEnumValue(number = 5)
    CHALLENGED,

    @ProtoEnumValue(number = 6)
    EVALUATED_TRUE,

    @ProtoEnumValue(number = 7)
    EVALUATED_FALSE
}
