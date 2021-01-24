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

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public class TestRaiseExeptionExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(TestRaiseExeptionExecutor.class);
    private static final String LOGMSG_PREFIX = "CLIENT-POLICY";
    private String logMsgPrefix() {
        return LOGMSG_PREFIX + "@" + session.hashCode() + " :: EXECUTOR";
    }

    protected final KeycloakSession session;

    public TestRaiseExeptionExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Object config) {
        // no setup configuration item
    }

    @Override
    public String getProviderId() {
        return TestRaiseExeptionExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (isThrowExceptionNeeded(context.getEvent())) throw new ClientPolicyException(context.getEvent().toString(), "Exception thrown intentionally");
    }

    private boolean isThrowExceptionNeeded(ClientPolicyEvent event) {
        ClientPolicyLogger.logv(logger, "{0} Client Policy Trigger Event = {1}", logMsgPrefix(), event);
        switch (event) {
            case REGISTERED:
            case UPDATED:
            //case VIEW:
            case UNREGISTER:
                return true;
            default :
                return false;
        }

    }
}