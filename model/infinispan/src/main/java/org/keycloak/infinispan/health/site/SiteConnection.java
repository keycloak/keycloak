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

import java.util.Set;

/**
 * The local site identity and the set of remote sites currently visible in the Infinispan cluster view.
 *
 * @param localSite   The name of this site, or {@code null} if cross-site is not configured.
 * @param onlineSites The site names that are currently reachable in the cluster view.
 */
public record SiteConnection(String localSite, Set<String> onlineSites) {
}
