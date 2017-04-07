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

import javax.ws.rs.Path;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.services.resources.admin.RealmAuth;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyTypeService extends PolicyService {

    private final String type;

    PolicyTypeService(String type, ResourceServer resourceServer, AuthorizationProvider authorization, RealmAuth auth) {
        super(resourceServer, authorization, auth);
        this.type = type;
    }

    @Path("/provider")
    public Object getPolicyAdminResourceProvider() {
        PolicyProviderAdminService resource = getPolicyProviderAdminResource(type);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }
}
