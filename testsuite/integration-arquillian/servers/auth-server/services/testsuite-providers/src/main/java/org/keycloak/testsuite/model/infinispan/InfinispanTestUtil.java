/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.model.infinispan;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;

import static org.keycloak.connections.infinispan.InfinispanUtil.setTimeServiceToKeycloakTime;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanTestUtil {

    protected static final Logger logger = Logger.getLogger(InfinispanTestUtil.class);

    private static Runnable origTimeService = null;

    /**
     * Set Keycloak test TimeService to infinispan cacheManager. This will cause that infinispan will be aware of Keycloak Time offset, which is useful
     * for testing that infinispan entries are expired after moving Keycloak time forward with {@link org.keycloak.common.util.Time#setOffset} .
     */
    public static void setTestingTimeService(KeycloakSession session) {
        // Testing timeService already set. This shouldn't happen if this utility is properly used
        if (origTimeService != null) {
            throw new IllegalStateException("Calling setTestingTimeService when testing TimeService was already set");
        }

        logger.info("Will set KeycloakIspnTimeService to the infinispan cacheManager");

        InfinispanConnectionProvider ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
        EmbeddedCacheManager cacheManager = ispnProvider.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).getCacheManager();
        origTimeService = setTimeServiceToKeycloakTime(cacheManager);
    }

    public static void revertTimeService() {
        // Testing timeService not set. This shouldn't happen if this utility is properly used
        if (origTimeService == null) {
            throw new IllegalStateException("Calling revertTimeService when testing TimeService was not set");
        }

        origTimeService.run();
        origTimeService = null;
    }
}
