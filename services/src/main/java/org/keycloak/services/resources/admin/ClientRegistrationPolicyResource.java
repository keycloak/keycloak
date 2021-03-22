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

package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyFactory;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @resource Client Registration Policy
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationPolicyResource {

    private final AdminPermissionEvaluator auth;
    private final RealmModel realm;
    private final AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClientRegistrationPolicyResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_INITIAL_ACCESS_MODEL);

    }


    /**
     * Base path for retrieve providers with the configProperties properly filled
     *
     * @return
     */
    @Path("providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<ComponentTypeRepresentation> getProviders() {
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(ClientRegistrationPolicy.class);

        return providerFactories.stream().map((ProviderFactory factory) -> {

            ClientRegistrationPolicyFactory clientRegFactory = (ClientRegistrationPolicyFactory) factory;
            List<ProviderConfigProperty> configProps = clientRegFactory.getConfigProperties(session);

            ComponentTypeRepresentation rep = new ComponentTypeRepresentation();
            rep.setId(clientRegFactory.getId());
            rep.setHelpText(clientRegFactory.getHelpText());
            rep.setProperties(ModelToRepresentation.toRepresentation(configProps));
            return rep;

        }).collect(Collectors.toList());
    }



}
