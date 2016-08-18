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

package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base class for mapping of user role mappings to an ID and Access Token claim.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
abstract class AbstractUserRoleMappingMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {

        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) {
            return token;
        }

        setClaim(token, mappingModel, userSession);
        return token;
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {

        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) {
            return token;
        }

        setClaim(token, mappingModel, userSession);
        return token;
    }


    protected abstract void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession);

    /**
     * Returns the role names extracted from the given {@code roleModels} while recursively traversing "Composite Roles".
     * <p>
     * Optionally prefixes each role name with the given {@code prefix}.
     * </p>
     *
     * @param roleModels
     * @param prefix     the prefix to apply, may be {@literal null}
     * @return
     */
    protected Set<String> flattenRoleModelToRoleNames(Set<RoleModel> roleModels, String prefix) {

        Set<String> roleNames = new LinkedHashSet<>();

        Deque<RoleModel> stack = new ArrayDeque<>(roleModels);
        while (!stack.isEmpty()) {

            RoleModel current = stack.pop();

            if (current.isComposite()) {
                for (RoleModel compositeRoleModel : current.getComposites()) {
                    stack.push(compositeRoleModel);
                }
            }

            String roleName = current.getName();

            if (prefix != null && !prefix.trim().isEmpty()) {
                roleName = prefix.trim() + roleName;
            }

            roleNames.add(roleName);
        }

        return roleNames;
    }
}
