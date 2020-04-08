/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.ciba.channel;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.ciba.endpoints.request.BackchannelAuthenticationRequest;
import org.keycloak.provider.Provider;

/**
 * Provides the interface for requesting the authentication(AuthN) and authorization(AuthZ) by an authentication device (AD) to the external entity via Authentication Channel.
 * This interface is for Client Initiated Backchannel Authentication(CIBA).
 *
 */
public interface AuthenticationChannelProvider extends Provider {

    /**
     * Request the authentication(AuthN) and authorization(AuthZ) by an authentication device (AD) to the external entity via Authentication Channel.
     * @param client the client as Consumption Device (CD)
     * @param request the representation of Authentication Request received on Backchannel Authentication Endpoint
     * @param expiresIn the duration in second for the active AuthN and Authz by AD
     * @param authResultId identifies the result of AuthN and Authz by AD
     * @param userSessionIdWillBeCreated the id for UserSessionModel that will be created after completing AuthN and Authz by AD
     */
    void requestAuthentication(ClientModel client, BackchannelAuthenticationRequest request, int expiresIn, String authResultId, String userSessionIdWillBeCreated);

}
