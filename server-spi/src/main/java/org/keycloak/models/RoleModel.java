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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.provider.ProviderEvent;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleModel {

    interface RoleNameChangeEvent extends ProviderEvent {
        RealmModel getRealm();
        String getNewName();
        String getPreviousName();

        /**
         * @return the Client ID of the client, for a client role; {@code null}, for a realm role
         */
        String getClientId();
        KeycloakSession getKeycloakSession();
    }

    interface RoleEvent extends ProviderEvent {
        RealmModel getRealm();
        RoleModel getRole();
        KeycloakSession getKeycloakSession();
    }

    interface RoleGrantedEvent extends RoleModel.RoleEvent {
        static void fire(RoleModel role, UserModel user, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new RoleModel.RoleGrantedEvent() {
                @Override
                public RealmModel getRealm() {
                    return session.getContext().getRealm();
                }

                @Override
                public RoleModel getRole() {
                    return role;
                }

                @Override
                public UserModel getUser() {
                    return user;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }

        UserModel getUser();
    }

    interface RoleRevokedEvent extends RoleModel.RoleEvent {
        static void fire(RoleModel role, UserModel user, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new RoleModel.RoleRevokedEvent() {
                @Override
                public RealmModel getRealm() {
                    return session.getContext().getRealm();
                }

                @Override
                public RoleModel getRole() {
                    return role;
                }

                @Override
                public UserModel getUser() {
                    return user;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }

        UserModel getUser();
    }

    String getName();

    String getDescription();

    void setDescription(String description);

    String getId();

    void setName(String name);

    boolean isComposite();

    void addCompositeRole(RoleModel role);

    void removeCompositeRole(RoleModel role);

    /**
     * Returns all composite roles as a stream.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    default Stream<RoleModel> getCompositesStream() {
        return getCompositesStream(null, null, null);
    }

    /**
     * Returns a paginated stream of composite roles of {@code this} role that contain given string in its name.
     *
     * @param search Case-insensitive search string
     * @param first Index of the first result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return A stream of requested roles ordered by the role name
     */
    Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max);

    boolean isClientRole();

    String getContainerId();

    RoleContainerModel getContainer();

    boolean hasRole(RoleModel role);

    void setSingleAttribute(String name, String value);

    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);

    default String getFirstAttribute(String name) {
        return getAttributeStream(name).findFirst().orElse(null);
    }

    /**
     * Returns all role's attributes that match the given name as a stream.
     * @param name {@code String} Name of an attribute to be used as a filter.
     * @return Stream of {@code String}. Never returns {@code null}.
     */
    Stream<String> getAttributeStream(String name);

    Map<String, List<String>> getAttributes();
}
