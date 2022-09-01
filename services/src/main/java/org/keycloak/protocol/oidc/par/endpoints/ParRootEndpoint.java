/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.par.endpoints;

import javax.ws.rs.Path;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.ext.OIDCExtProvider;
import org.keycloak.protocol.oidc.ext.OIDCExtProviderFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class ParRootEndpoint implements OIDCExtProvider, OIDCExtProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "par";

    private final KeycloakSession session;
    private EventBuilder event;

    public ParRootEndpoint() {
        // for reflection
        this(null);
    }

    public ParRootEndpoint(KeycloakSession session) {
        this.session = session;
    }

    @Path("/request")
    public ParEndpoint request() {
        ParEndpoint endpoint = new ParEndpoint(session, event);

        ResteasyProviderFactory.getInstance().injectProperties(endpoint);

        return endpoint;
    }

    @Override
    public OIDCExtProvider create(KeycloakSession session) {
        return new ParRootEndpoint(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.PAR);
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public void close() {
    }

}
