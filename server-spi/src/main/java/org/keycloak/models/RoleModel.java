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

    enum Type {
        REALM,
        CLIENT,
        ORGANIZATION
    }

    interface RoleNameChangeEvent extends ProviderEvent {
        RealmModel getRealm();
        String getNewName();
        String getPreviousName();

        /**
         * @return the client ID for a client role, or {@code null} for a realm role
         * @deprecated consumers should resolve richer role context through supported model APIs
         */
        @Deprecated
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

    /**
     * Returns the scope that owns this role.
     *
     * @return the role type
     */
    default Type getType() {
        RoleContainerModel container = getContainer();
        if (container instanceof ClientModel) {
            return Type.CLIENT;
        }
        if (container instanceof OrganizationModel) {
            return Type.ORGANIZATION;
        }
        return Type.REALM;
    }

    /**
     * Returns whether this role is of the given {@code type}.
     *
     * @param type the role type
     * @return {@code true} if this role is of the given type
     */
    default boolean isType(Type type) {
        return getType() == type;
    }

    /**
     * Returns whether this role is a client role.
     *
     * @return {@code true} if this role is a client role
     * @deprecated use {@link #isType(Type)} with {@link Type#CLIENT}
     */
    @Deprecated
    default boolean isClientRole() {
        return isType(Type.CLIENT);
    }

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
