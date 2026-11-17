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

package org.keycloak.services.clientregistration;

import java.util.Set;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationService {

    private final EventBuilder event;

    private final KeycloakSession session;

    private static final Set<String> DEPRECATED_PROVIDER_IDS = Set.of("default", "install", "saml2-entity-descriptor");

    public ClientRegistrationService(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
    }

    @Path("{provider}")
    public Object provider(@PathParam("provider") String providerId) {
        checkSsl();

        if (DEPRECATED_PROVIDER_IDS.contains(providerId)) {
            ServicesLogger.LOGGER.deprecatedClientRegistrationProvider(providerId);
        }

        ClientRegistrationProvider provider = session.getProvider(ClientRegistrationProvider.class, providerId);

        if (provider == null) {
            throw new NotFoundException("Client registration provider not found");
        }

        provider.setEvent(event);
        provider.setAuth(new ClientRegistrationAuth(session, provider, event, providerId));
        return provider;
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https")) {
            if (session.getContext().getRealm().getSslRequired().isRequired(session.getContext().getConnection())) {
                throw new ErrorResponseException("invalid_request", "HTTPS required", Response.Status.FORBIDDEN);
            }
        }
    }

}
