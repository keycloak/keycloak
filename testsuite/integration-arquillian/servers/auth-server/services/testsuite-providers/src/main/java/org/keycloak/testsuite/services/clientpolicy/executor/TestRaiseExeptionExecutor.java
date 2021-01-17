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
 */

package org.keycloak.testsuite.services.clientpolicy.executor;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public class TestRaiseExeptionExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(TestRaiseExeptionExecutor.class);

    protected final KeycloakSession session;
    protected final ComponentModel componentModel;

    public TestRaiseExeptionExecutor(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (isThrowExceptionNeeded(context.getEvent())) throw new ClientPolicyException(context.getEvent().toString(), "Exception thrown intentionally");
    }

    private boolean isThrowExceptionNeeded(ClientPolicyEvent event) {
        ClientPolicyLogger.log(logger, "Client Policy Trigger Event = " + event);
        List<String> l = componentModel.getConfig().get(TestRaiseExeptionExecutorFactory.TARGET_CP_EVENTS);
        return l != null && l.stream().anyMatch(i->i.equals(event.toString()));
    }
}
