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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class PermissionService extends PolicyService {

    public PermissionService(ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        super(resourceServer, authorization, auth, adminEvent);
    }

    @Path("/by-type/{policy-type}")
    @Override
    public PolicyTypeService getTypeService(@PathParam("policy-type") String type) {
        PolicyProviderFactory providerFactory = getPolicyProviderFactory(type);

        if (providerFactory != null) {
            return new PolicyTypeService(type, resourceServer, authorization, auth, adminEvent) {
                @Override
                protected List<Object> doSearch(Integer firstResult, Integer maxResult, String fields, Map<Policy.FilterOption, String[]> filters) {
                    filters.put(Policy.FilterOption.PERMISSION, new String[] {Boolean.TRUE.toString()});
                    filters.put(Policy.FilterOption.TYPE, new String[] {type});
                    return super.doSearch(firstResult, maxResult, fields, filters);
                }
            };
        }
        throw new ErrorResponseException("No policy provider found for this type.", "Invalid type", Status.NOT_FOUND);
    }

    @Path("/by-id/{policy-id}")
    @Override
    public PolicyResourceService getResourceService(@PathParam("policy-id") String policyId) {
        Policy policy = authorization.getStoreFactory().getPolicyStore().findById(resourceServer, policyId);

        return new PolicyTypeResourceService(policy, resourceServer, authorization, auth, adminEvent);
    }

    @Override
    protected List<Object> doSearch(Integer firstResult, Integer maxResult, String fields, Map<Policy.FilterOption, String[]> filters) {
        filters.put(Policy.FilterOption.PERMISSION, new String[] {Boolean.TRUE.toString()});
        return super.doSearch(firstResult, maxResult, fields, filters);
    }

    @Override
    protected AbstractPolicyRepresentation toRepresentation(Policy policy, String fields, AuthorizationProvider authorization) {
        return ModelToRepresentation.toRepresentation(policy, authorization, false, false, fields != null && fields.equals("*"));
    }
}
