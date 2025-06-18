/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.federation.sssd.impl;

import org.jboss.logging.Logger;

/**
 * <p>Class to detect if SSSD is available in the system. As keycloak uses
 * the native java implementation (jdk >= 16 needed) the default implementation
 * for previous versions always returns false.</p>
 *
 * @author rmartinc
 */
public class AvailabilityChecker {

    private static final Logger logger = Logger.getLogger(AvailabilityChecker.class);

    /**
     * Returns if the SSSD is available in the system.
     * @return true if SSSD is available, null otherwise
     */
    public static boolean isAvailable() {
        logger.debug("SSSD is not available for this version of java (jdk >= 16 needed). Federation provider will be disabled.");
        return false;
    }
}
