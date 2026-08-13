/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util;

import org.keycloak.common.Profile;

public class MultiSiteUtils {

    public static boolean isMultiSiteEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.MULTI_SITE);
    }

    /**
     * @return true when user sessions are stored in the database. In multi-site setup this is false when REMOTE_CACHE feature is enabled
     */
    public static boolean isPersistentSessionsEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) || (isMultiSiteEnabled() && !Profile.isFeatureEnabled(Profile.Feature.CLUSTERLESS));
    }
}
