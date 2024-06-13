/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;


/**
 * <p>Abstract saml request context for any SAML request received. The context
 * will have the type object received, the client model and binding type
 * the client used to connect.</p>
 *
 * @author rmartinc
 * @param <T> The saml request type
 */
public abstract class AbstractSamlRequestContext<T> implements ClientPolicyContext {

    protected final T request;
    protected final ClientModel client;
    protected final String protocolBinding;

    public AbstractSamlRequestContext(final T request, final ClientModel client, final String protocolBinding) {
        this.request = request;
        this.client = client;
        this.protocolBinding = protocolBinding;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract ClientPolicyEvent getEvent();

    /**
     * Getter for the SAML request received.
     * @return The SAML request type
     */
    public T getRequest() {
        return request;
    }

    /**
     * Getter for the client model doing the request.
     * @return The client model
     */
    public ClientModel getClient() {
        return client;
    }

    /**
     * Getter for the protocol binding type that is processing the request.
     * @return The keycloak protocol binding type.
     */
    public String getProtocolBinding() {
        return protocolBinding;
    }
}
