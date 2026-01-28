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
package org.keycloak.authorization.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Path;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyTypeService extends PolicyService {

    private final String type;

    public PolicyTypeService(String type, ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        super(resourceServer, authorization, auth, adminEvent);
        this.type = type;
    }

    @Path("/provider")
    public Object getPolicyAdminResourceProvider() {
        PolicyProviderAdminService resource = getPolicyProviderAdminResource(type);

        if (resource == null) {
            return null;
        }

        return resource;
    }

    @Override
    protected Object doCreatePolicyResource(Policy policy) {
        return new PolicyTypeResourceService(policy, resourceServer,authorization, auth, adminEvent);
    }

    @Override
    protected AbstractPolicyRepresentation doCreateRepresentation(String payload) {
        PolicyProviderFactory provider = getPolicyProviderFactory(type);
        Class<? extends AbstractPolicyRepresentation> representationType = provider.getRepresentationType();

        if (representationType == null) {
            throw new RuntimeException("Policy provider for type [" + type + "] returned a null representation type.");
        }

        AbstractPolicyRepresentation representation;

        try {
            representation = JsonSerialization.readValue(payload, representationType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize JSON using policy provider for type [" + type + "].", e);
        }

        if (!"js".equals(type) || representation.getType() == null) {
            representation.setType(type);
        }

        return representation;
    }

    @Override
    protected AbstractPolicyRepresentation toRepresentation(Policy policy, String fields, AuthorizationProvider authorization) {
        return ModelToRepresentation.toRepresentation(policy, authorization, false, false);
    }

    @Override
    protected List<Object> doSearch(Integer firstResult, Integer maxResult, String fields, Map<Policy.FilterOption, String[]> filters) {
        filters.put(Policy.FilterOption.TYPE, new String[] {type});
        return super.doSearch(firstResult, maxResult, fields, filters);
    }
}
