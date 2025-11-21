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

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

import org.jboss.logging.Logger;

public class TestRaiseExceptionExecutor implements ClientPolicyExecutorProvider<TestRaiseExceptionExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(TestRaiseExceptionExecutor.class);

    protected final KeycloakSession session;
    private Configuration configuration;

    public TestRaiseExceptionExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(TestRaiseExceptionExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        protected List<ClientPolicyEvent> events;

        public List<ClientPolicyEvent> getEvents() {
            return events;
        }

        public void setEvents(List<ClientPolicyEvent> events) {
            this.events = events;
        }
    }

    @Override
    public String getProviderId() {
        return TestRaiseExceptionExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (isThrowExceptionNeeded(context.getEvent())) throw new ClientPolicyException(context.getEvent().toString(), "Exception thrown intentionally");
    }

    private boolean isThrowExceptionNeeded(ClientPolicyEvent event) {
        logger.tracev("Client Policy Trigger Event = {0}",  event);
        if (configuration != null && configuration.getEvents() != null && !configuration.getEvents().isEmpty()) {
            return configuration.getEvents().contains(event);
        }
        return false;
    }
}