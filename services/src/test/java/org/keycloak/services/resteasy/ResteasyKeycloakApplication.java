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

package org.keycloak.services.resteasy;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.common.Profile;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.error.KcUnrecognizedPropertyExceptionHandler;
import org.keycloak.services.error.KeycloakErrorHandler;
import org.keycloak.services.error.KeycloakMismatchedInputExceptionHandler;
import org.keycloak.services.filters.InvalidQueryParameterFilter;
import org.keycloak.services.filters.KeycloakSecurityHeadersFilter;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.LoadBalancerResource;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.ServerMetadataResource;
import org.keycloak.services.resources.ThemeResource;
import org.keycloak.services.resources.WelcomeResource;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.util.ObjectMapperResolver;

public class ResteasyKeycloakApplication extends KeycloakApplication {

    protected Set<Object> singletons = new HashSet<>();
    protected Set<Class<?>> classes = new HashSet<>();

    public ResteasyKeycloakApplication() {
        classes.add(RealmsResource.class);
        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_API)) {
            classes.add(AdminRoot.class);
        }
        classes.add(ThemeResource.class);
        classes.add(InvalidQueryParameterFilter.class);
        classes.add(KeycloakSecurityHeadersFilter.class);
        classes.add(KeycloakErrorHandler.class);
        classes.add(KcUnrecognizedPropertyExceptionHandler.class);
        classes.add(KeycloakMismatchedInputExceptionHandler.class);

        singletons.add(new ObjectMapperResolver());
        classes.add(WelcomeResource.class);
        classes.add(ServerMetadataResource.class);

        if (MultiSiteUtils.isMultiSiteEnabled()) {
            // If we are running in multi-site mode, we need to add a resource which to expose
            // an endpoint for the load balancer to gather information whether this site should receive requests or not.
            classes.add(LoadBalancerResource.class);
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    @Override
    protected KeycloakSessionFactory createSessionFactory() {
        ResteasyKeycloakSessionFactory factory = new ResteasyKeycloakSessionFactory();
        factory.init();
        return factory;
    }

    @Override
    protected void createTemporaryAdmin(KeycloakSession session) {
        // do nothing
    }

}
