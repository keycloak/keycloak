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

package org.keycloak.client.clienttype;


import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientTypesRepresentation;

/**
 * TODO:client-types javadoc
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientTypeManager extends Provider {

    // Constants for global types
    String STANDARD = "standard";
    String SERVICE_ACCOUNT = "service-account";

    // TODO:client-types javadoc
    ClientTypesRepresentation getClientTypes(RealmModel realm) throws ClientTypeException;

    // Implementation is supposed also to validate clientTypes before persisting them
    void updateClientTypes(RealmModel realm, ClientTypesRepresentation clientTypes) throws ClientTypeException;

    ClientType getClientType(RealmModel realm, String typeName)  throws ClientTypeException;

    // Create client, which delegates to the particular client type
    ClientModel augmentClient(ClientModel client) throws ClientTypeException;

    @Override
    default void close() {
    }
}