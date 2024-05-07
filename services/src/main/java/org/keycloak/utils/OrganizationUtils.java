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

package org.keycloak.utils;

import jakarta.ws.rs.core.Response;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponse;

import java.util.List;
import java.util.Map;

public class OrganizationUtils {

    public static void checkForOrgRelatedGroupRep(KeycloakSession session, GroupRepresentation rep) {
        if (isOrgsEnabled(session)) {
            checkRep(rep);
        }
    }

    public static void checkForOrgRelatedGroupModel(KeycloakSession session, GroupModel model) {
        if (isOrgsEnabled(session)) {
            checkModel(model);
        }
    }

    private static boolean isOrgsEnabled(KeycloakSession session) {
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        return orgProvider != null && orgProvider.isEnabled();
    }

    private static boolean isOrganizationRelatedGroup(Object o) {
        if (o instanceof GroupRepresentation rep) {
            return attributeContains(rep.getAttributes());
        } else if (o instanceof GroupModel model) {
            return attributeContains(model.getAttributes());
        }
        return false;
    }

    private static boolean attributeContains(Map<String, List<String>> attributes) {
        return attributes != null && attributes.containsKey(OrganizationModel.ORGANIZATION_ATTRIBUTE);
    }

    private static void checkModel(GroupModel model) {
        if (isOrganizationRelatedGroup(model)) {
            throw ErrorResponse.error("Cannot manage organization related group via non Organization API.", Response.Status.FORBIDDEN);
        }
    }

    private static void checkRep(GroupRepresentation rep) {
        if (isOrganizationRelatedGroup(rep)) {
            throw ErrorResponse.error("Cannot use group attribute reserved for organizations.", Response.Status.FORBIDDEN);
        }
    }
}
