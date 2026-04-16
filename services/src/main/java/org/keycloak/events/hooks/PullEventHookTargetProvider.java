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

package org.keycloak.events.hooks;

import java.util.List;

public class PullEventHookTargetProvider implements EventHookTargetProvider {

    @Override
    public EventHookDeliveryResult deliver(EventHookTargetModel target, EventHookMessageModel message) {
        return waitingResult(target);
    }

    @Override
    public EventHookDeliveryResult deliverBatch(EventHookTargetModel target, List<EventHookMessageModel> messages) {
        return waitingResult(target);
    }

    @Override
    public void close() {
    }

    private EventHookDeliveryResult waitingResult(EventHookTargetModel target) {
        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setWaiting(true);
        result.setRetryable(false);
        result.setStatusCode("PULL_WAITING");
        result.setDetails("Waiting for consumption through " + PullEventHookTargetProviderFactory.consumePath(target));
        result.setDurationMillis(0);
        return result;
    }
}
