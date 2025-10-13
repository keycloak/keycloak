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

package org.keycloak.models;

import org.keycloak.provider.ProviderEvent;

import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleContainerModel {

    interface RoleRemovedEvent extends ProviderEvent {
        RoleModel getRole();
        KeycloakSession getKeycloakSession();
    }

    String getId();

    RoleModel getRole(String name);

    RoleModel addRole(String name);

    RoleModel addRole(String id, String name);

    boolean removeRole(RoleModel role);

    /**
     * Returns available roles as a stream.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    Stream<RoleModel> getRolesStream();

    /**
     * Returns available roles as a stream.
     * @param firstResult {@code Integer} Index of the first desired role. Ignored if negative or {@code null}.
     * @param maxResults {@code Integer} Maximum number of returned roles. Ignored if negative or {@code null}.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults);

    /**
     * Searches roles by the given name. Returns all roles that match the given filter.
     * @param search {@code String} Name of the role to be used as a filter.
     * @param first {@code Integer} Index of the first desired role. Ignored if negative or {@code null}.
     * @param max {@code Integer} Maximum number of returned roles. Ignored if negative or {@code null}.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max);

}
