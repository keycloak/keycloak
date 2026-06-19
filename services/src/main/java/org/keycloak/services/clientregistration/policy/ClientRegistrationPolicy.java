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

package org.keycloak.services.clientregistration.policy;

import java.util.Collection;
import java.util.Collections;

import org.keycloak.models.ClientModel;
import org.keycloak.provider.Provider;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientRegistrationPolicy extends Provider {

    void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException;

    void afterRegister(ClientRegistrationContext context, ClientModel clientModel);

    void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException;

    void afterUpdate(ClientRegistrationContext context, ClientModel clientModel);

    void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException;

    void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException;

    /**
     * Extra web origins this policy contributes to the CORS allow-list for responses produced
     * by the client registration endpoints. Origins are collected up-front so validation can
     * happen in a single {@code checkAllowedOrigins} call before the handler runs.
     */
    default Collection<String> getAllowedOrigins() {
        return Collections.emptyList();
    }

    @Override
    default void close() {
    }

}
