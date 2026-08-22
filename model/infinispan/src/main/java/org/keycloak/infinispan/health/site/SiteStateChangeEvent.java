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

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * JFR event emitted when a cross-site state transition is attempted via {@link SiteStorage#compareAndSet}.
 */
@Name("org.keycloak.SiteStateChange")
@Label("Site State Change")
@Description("Cross-site state transition attempt")
@Category({"Keycloak", "Health"})
public class SiteStateChangeEvent extends Event {

    @Label("Expected Status")
    @Description("The status expected in the database before the transition")
    public String expectedStatus;

    @Label("New Status")
    @Description("The target status of the transition")
    public String newStatus;

    @Label("Expected Active Site")
    @Description("The active site in the expected state")
    public String expectedActiveSite;

    @Label("New Active Site")
    @Description("The active site in the new state")
    public String newActiveSite;

    @Label("Success")
    @Description("Whether the compare-and-set succeeded")
    public boolean success;

    public void set(SiteState expectedState, SiteState newState, boolean success) {
        this.expectedStatus = expectedState.status().name();
        this.newStatus = newState.status().name();
        this.expectedActiveSite = expectedState.activeSite();
        this.newActiveSite = newState.activeSite();
        this.success = success;
    }
}
