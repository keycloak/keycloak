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

package org.keycloak.services.clienttype;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.client.clienttype.ClientTypeManager;
import org.keycloak.client.clienttype.ClientTypeManagerFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.ClientTypeRepresentation;
import org.keycloak.representations.idm.ClientTypesRepresentation;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientTypeManagerFactory implements ClientTypeManagerFactory {

    private static final Logger logger = Logger.getLogger(DefaultClientTypeManagerFactory.class);

    private volatile List<ClientTypeRepresentation> globalClientTypes;

    @Override
    public ClientTypeManager create(KeycloakSession session) {
        return new DefaultClientTypeManager(session, getGlobalClientTypes(session));
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

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_TYPES);
    }

    protected List<ClientTypeRepresentation> getGlobalClientTypes(KeycloakSession session) {
        if (globalClientTypes == null) {
            synchronized (this) {
                if (globalClientTypes == null) {
                    logger.info("Loading global client types");

                    try {
                        ClientTypesRepresentation globalTypesRep  = JsonSerialization.readValue(getClass().getResourceAsStream("/keycloak-default-client-types.json"), ClientTypesRepresentation.class);
                        this.globalClientTypes = DefaultClientTypeManager.validateAndCastConfiguration(session, globalTypesRep.getRealmClientTypes(), Collections.emptyList());
                    } catch (IOException e) {
                        logger.error("Failed to deserialize global proposed client types from JSON.");
                        throw ClientTypeException.Message.CLIENT_TYPE_FAILED_TO_LOAD.exception(e);
                    }
                }
            }
        }
        return globalClientTypes;
    }

}