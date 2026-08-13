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

package org.keycloak.authentication;

import java.util.Map;

import org.keycloak.models.ClientModel;

/**
 * Encapsulates information about the execution in ClientAuthenticationFlow
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientAuthenticationFlowContext extends AbstractAuthenticationFlowContext {

    /**
     * Current client attached to this flow.  It can return null if no client has been identified yet
     *
     * @return
     */
    ClientModel getClient();

    /**
     * Attach a specific client to this flow.
     *
     * @param client
     */
    void setClient(ClientModel client);

    /**
     * Return the map where the authenticators can put some additional state related to authenticated client and the context how was
     * client authenticated (ie. attributes from client certificate etc). Map is writable, so you can add/remove items from it as needed.
     *
     * After successful authentication will be those state data put into UserSession notes. This allows you to configure
     * UserSessionNote protocol mapper for your client, which will allow to map those state data into the access token available in the application
     *
     * @return
     */
    Map<String, String> getClientAuthAttributes();

    /**
     * Provides a mechanism for sharing computed state across multiple authenticators. Returns state of the given type.
     * If not already set the supplier is used to initialise the state.
     *
     * @param type the class type of the state
     * @param supplier a supplier that can create the computed state if not already set
     * @return the current state
     * @param <T> the type of the state
     */
    <T> T getState(Class<T> type, ClientAuthenticationFlowContextSupplier<T> supplier) throws Exception;

}
