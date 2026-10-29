/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.client.policies;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.tests.providers.client.policies.TrackEventsClientPolicyExecutor;

public class TrackEventsSnapshot implements FetchOnServer {

    @Override
    public Object run(KeycloakSession session) {
        return TrackEventsClientPolicyExecutor.getInstance().snapshotAndClear().toArray(ClientPolicyEvent[]::new);
    }
}
