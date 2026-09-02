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

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of the cross-site state stored in the database, consisting of the current {@link Status}, the
 * site that owns the ongoing state transition ({@code activeSite}), and a random {@code revision} used for
 * compare-and-set identity.
 */
public record SiteState(Status status, String activeSite, UUID revision) {

    private static final SiteState HEALTHY = new SiteState(Status.HEALTHY, null, null);

    public SiteState {
        Objects.requireNonNull(status);
    }

    // Helpers
    public static SiteState healthy() {
        return HEALTHY;
    }

    public static SiteState suspecting(String activeSite) {
        return new  SiteState(Status.SUSPECTING, activeSite, UUID.randomUUID());
    }

    public static SiteState recovering(String activeSite) {
        return new  SiteState(Status.RECOVERING, activeSite, UUID.randomUUID());
    }

    public static SiteState unhealthy(String activeSite) {
        return new  SiteState(Status.UNHEALTHY, activeSite, null);
    }
}
