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

package org.keycloak.services.clientpolicy;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.ClientProfileRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientPolicyManagerFactory implements ClientPolicyManagerFactory {

    private static final Logger logger = Logger.getLogger(DefaultClientPolicyManagerFactory.class);

    // Global (builtin) profiles are loaded on booting keycloak at once.
    // therefore, their representations are kept and remain unchanged.
    // these are shared among all realms.
    private volatile List<ClientProfileRepresentation> globalClientProfiles;

    @Override
    public ClientPolicyManager create(KeycloakSession session) {
        return new DefaultClientPolicyManager(session, () -> getGlobalClientProfiles(session));
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "default";
    }

    /**
     * When this method is called, assumption is that CLIENT_POLICIES feature is enabled
     */
    protected List<ClientProfileRepresentation> getGlobalClientProfiles(KeycloakSession session) {
        if (globalClientProfiles == null) {
            synchronized (this) {
                if (globalClientProfiles == null) {
                    logger.trace("LOAD GLOBAL CLIENT PROFILES ON KEYCLOAK");

                    // load builtin profiles from keycloak-services
                    try {
                        this.globalClientProfiles = ClientPoliciesUtil.getValidatedGlobalClientProfilesRepresentation(session, getClass().getResourceAsStream("/keycloak-default-client-profiles.json"));
                    } catch (ClientPolicyException cpe) {
                        logger.warnv("LOAD GLOBAL PROFILES ON KEYCLOAK FAILED :: error = {0}, error detail = {1}", cpe.getError(), cpe.getErrorDetail());
                        throw new IllegalStateException(cpe);
                    }
                }
            }
        }
        return globalClientProfiles;
    }
}
