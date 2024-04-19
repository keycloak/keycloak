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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTypeRepresentation;

import java.util.Map;

/**
 * TODO:client-types javadocs
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientType {

    String getName();

    // Augment client type at runtime
    // Can be property name (like "standardFlow" or "rootUrl") or attributeName (like "pkceEnabled")
    boolean isApplicable(String optionName);

    // Return if option is configurable by clientType or not...
    boolean isReadOnly(String optionName);

    // Return the value of particular option (if it can be provided by clientType) or return null if this option is not provided by client type
    <T> T getDefaultValue(String optionName, Class<T> optionType);


    Map<String, ClientTypeRepresentation.PropertyConfig> getConfiguration();

    // Augment at the client type
    // Augment particular client on creation of client  (TODO:client-types Should it be clientModel or clientRepresentation? Or something else?)
    void onCreate(ClientRepresentation newClient) throws ClientTypeException;

    // Augment particular client on update of client (TODO:client-types Should it be clientModel or clientRepresentation? Or something else?)
    void onUpdate(ClientModel currentClient, ClientRepresentation clientToUpdate) throws ClientTypeException;
}