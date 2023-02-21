/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage;

import org.keycloak.credential.CredentialInput;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.user.MapCredentialValidationOutput;

/**
 * A map store transaction that can authenticate the credentials provided by a user.
 *
 * @author Alexander Schwartz
 */
public interface MapKeycloakTransactionWithAuth<V extends AbstractEntity, M> extends MapKeycloakTransaction<V, M> {

    /**
     * Authenticate a user with the provided input credentials. Use this, for example, for Kerberos SPNEGO
     * authentication, where the user will be determined at the end of the interaction with the client.
     * @param realm realm against which to authenticate against
     * @param input information provided by the user
     * @return Information on how to continue the conversion with the client, or a terminal result. For a successful
     * authentication, will also contain information about the user.
     */
    MapCredentialValidationOutput<V> authenticate(RealmModel realm, CredentialInput input);
}
