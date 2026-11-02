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

package org.keycloak.broker.provider;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * An identity provider mapper that can grant roles named in its configuration beyond {@link ConfigConstants#ROLE}.
 */
public interface RoleGrantingIdentityProviderMapper extends IdentityProviderMapper {

    /**
     * The provider of {@code mapper}, when that provider can grant roles beyond {@link ConfigConstants#ROLE}.
     */
    static Optional<RoleGrantingIdentityProviderMapper> of(KeycloakSessionFactory sessionFactory, IdentityProviderMapperModel mapper) {
        return Optional.ofNullable(mapper.getIdentityProviderMapper())
                .map(mapperType -> sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapperType))
                .filter(RoleGrantingIdentityProviderMapper.class::isInstance)
                .map(RoleGrantingIdentityProviderMapper.class::cast);
    }

    /**
     * The role references, in the format of
     * {@link org.keycloak.models.utils.KeycloakModelUtils#getRoleFromString(org.keycloak.models.KeycloakSession, org.keycloak.models.RealmModel, String)},
     * that a mapper with this configuration may grant beyond {@link ConfigConstants#ROLE}; empty when the configuration
     * names none or cannot be read.
     */
    Set<String> getGrantableRoleReferences(Map<String, String> config);

    /**
     * The configuration with {@code rewriteReference} applied to each role reference beyond
     * {@link ConfigConstants#ROLE}; the given configuration when no reference changes or the configuration cannot be
     * read. The given configuration is not modified.
     */
    Map<String, String> withRewrittenRoleReferences(Map<String, String> config, UnaryOperator<String> rewriteReference);
}
