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

import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientTypeRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientTypeProvider extends Provider {

    // Return client types for the model returned
    ClientType getClientType(ClientTypeRepresentation clientTypeRep, ClientType parent);

    // TODO:client-types type-safety here. The returned clientType should have correctly casted client type configuration
    // Used when creating/updating clientType. The JSON configuration is validated to be checked if it matches the good format for client type
    ClientTypeRepresentation checkClientTypeConfig(ClientTypeRepresentation clientType) throws ClientTypeException;

    @Override
    default void close() {
    }

}
