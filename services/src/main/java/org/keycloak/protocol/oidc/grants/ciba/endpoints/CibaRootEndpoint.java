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
 *
 */
package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import jakarta.ws.rs.Path;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.ext.OIDCExtProvider;
import org.keycloak.protocol.oidc.ext.OIDCExtProviderFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CibaRootEndpoint implements OIDCExtProvider, OIDCExtProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "ciba";

    private final KeycloakSession session;
    private EventBuilder event;

    public CibaRootEndpoint() {
        // for reflection
        this(null);
    }

    public CibaRootEndpoint(KeycloakSession session) {
        this.session = session;
    }

    /**
     * The backchannel authentication endpoint used by consumption devices to obtain authorization from end-users.
     *
     * @return
     */
    @Path("/auth")
    public BackchannelAuthenticationEndpoint authorize() {
        return new BackchannelAuthenticationEndpoint(session, event);
    }

    /**
     * The callback endpoint used by authentication devices to notify Keycloak about the end-user authentication status.
     *
     * @return
     */
    @Path("/auth/callback")
    public BackchannelAuthenticationCallbackEndpoint authenticate() {
        return new BackchannelAuthenticationCallbackEndpoint(session, event);
    }

    @Override
    public OIDCExtProvider create(KeycloakSession session) {
        return new CibaRootEndpoint(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CIBA);
    }

}
