/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.admin.v2.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/**
 * Jakarta Validation Group Sequence for client update operations including default validations.
 * <p>
 * This group sequence combines {@link UpdateClient} specific validations with
 * the {@link Default} validation group, ensuring both are executed during updates.
 * <p>
 * Example usage:
 * <pre>{@code
 * validator.validate(client, UpdateClientDefault.class);
 * }</pre>
 *
 * @see UpdateClient
 * @see CreateClientDefault
 */
@GroupSequence({UpdateClient.class, Default.class})
public interface UpdateClientDefault {
}
