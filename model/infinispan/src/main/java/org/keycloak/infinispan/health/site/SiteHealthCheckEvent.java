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

import java.util.Map;
import java.util.stream.Collectors;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * JFR event emitted during each cross-site health check, capturing the site statuses, cluster view, and current
 * state from the database.
 */
@Name("org.keycloak.SiteHealthCheck")
@Label("Site Health Check")
@Description("Periodic cross-site health check")
@Category({"Keycloak", "Health"})
public class SiteHealthCheckEvent extends Event {

    @Label("Site Statuses")
    @Description("Backup site statuses in key=value format")
    public String sites;

    @Label("Local Site")
    @Description("The local site name")
    public String localSite;

    @Label("Online Sites")
    @Description("Sites visible in the cluster view")
    public String onlineSites;

    @Label("Status")
    @Description("Current site state status from the database")
    public String status;

    @Label("Active Site")
    @Description("The site currently owning the state transition")
    public String activeSite;

    public void set(Map<String, String> sites, SiteConnection connection, SiteState siteState) {
        this.sites = sites.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
        this.localSite = connection.localSite();
        this.onlineSites = String.join(", ", connection.onlineSites());
        this.status = siteState.status().name();
        this.activeSite = siteState.activeSite();
    }
}
