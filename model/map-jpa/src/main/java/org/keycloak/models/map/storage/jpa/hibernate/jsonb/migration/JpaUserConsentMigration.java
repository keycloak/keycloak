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
package org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Migration functions for user consents.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaUserConsentMigration {

    public static final List<Function<ObjectNode, ObjectNode>> MIGRATORS = Arrays.asList(
            o -> o // no migration yet
    );
}
