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

package org.keycloak.infinispan.health.site;

/**
 * The cross-site state machine statuses.
 * <p>
 * The normal lifecycle is: {@code HEALTHY → SUSPECTING → UNHEALTHY → RECOVERING → HEALTHY}.
 */
public enum Status {
    /**
     * All sites are reachable and replication is functioning normally.
     */
    HEALTHY(0),
    /**
     * A site has detected that one or more remote sites are unreachable. A one-round delay is applied before
     * transitioning to {@link #UNHEALTHY} to avoid reacting to transient failures.
     */
    SUSPECTING(1),
    /**
     * Sites have come back online after a failure. The active site verifies connectivity before transitioning back
     * to {@link #HEALTHY}.
     */
    RECOVERING(3),
    /**
     * One or more remote sites are confirmed unreachable. The active site has disconnected replication and is
     * serving traffic alone.
     */
    UNHEALTHY(2);

    private final int value;

    Status(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
