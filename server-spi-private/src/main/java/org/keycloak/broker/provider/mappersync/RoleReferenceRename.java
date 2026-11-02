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

package org.keycloak.broker.provider.mappersync;

/**
 * What a rename means for one role reference.
 */
sealed interface RoleReferenceRename permits RoleReferenceRename.Unaffected, RoleReferenceRename.Renamed,
        RoleReferenceRename.Ambiguous {

    Unaffected UNAFFECTED = new Unaffected();

    /**
     * The reference does not name the renamed role or client.
     */
    record Unaffected() implements RoleReferenceRename {
    }

    /**
     * The reference named the renamed role or client, and {@code newReference} names it after the rename.
     */
    record Renamed(String newReference) implements RoleReferenceRename {
    }

    /**
     * The reference named the renamed role or client, and {@code newReference}, its only spelling after the rename,
     * names a different role.
     */
    record Ambiguous(String newReference) implements RoleReferenceRename {
    }

    /**
     * The reference to keep in the configuration: the new reference when renamed, {@code reference} otherwise.
     */
    default String applyTo(String reference) {
        return this instanceof Renamed renamed ? renamed.newReference() : reference;
    }
}
