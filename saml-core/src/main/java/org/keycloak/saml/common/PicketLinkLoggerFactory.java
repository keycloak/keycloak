/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.saml.common;

/**
 * <p> Factory class to create {@link PicketLinkLogger} instances. </p> <p> The logger instances are created based on
 * the following patterns: <br/> <ul> <li>Tries to load a class with the same full qualified name of {@link
 * PicketLinkLogger} plus the "Impl" suffix;</li> <li>If no class is found fallback to the {@link
 * DefaultPicketLinkLogger} as the default logger implementation.</li> </ul> </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public final class PicketLinkLoggerFactory {

    private static PicketLinkLogger LOGGER;

    static {
        try {
            LOGGER = (PicketLinkLogger) Class.forName(PicketLinkLogger.class.getName() + "Impl").newInstance();
        } catch (Exception e) {
            // if no implementation is found uses the default implementation.
            LOGGER = new DefaultPicketLinkLogger();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.usingLoggerImplementation(LOGGER.getClass().getName());
        }
    }

    /**
     * <p>Returns a {@link PicketLinkLogger} instance.</p>
     *
     * @return
     */
    public static PicketLinkLogger getLogger() {
        return LOGGER;
    }

}
