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
 */

package org.keycloak.events.log;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for {@link GdprAwareJBossLoggingEventListenerProvider}
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public class GdprAwareJBossLoggingEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "gdpr-jboss-logging";

    private static final Logger LOGGER = Logger.getLogger("org.keycloak.events");

    private Logger.Level successLevel;

    private Logger.Level errorLevel;

    private Anonymizer anonymizer;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new GdprAwareJBossLoggingEventListenerProvider(session, LOGGER, successLevel, errorLevel, anonymizer);
    }

    @Override
    public void init(Config.Scope config) {
        successLevel = Logger.Level.valueOf(config.get("success-level", "debug").toUpperCase());
        errorLevel = Logger.Level.valueOf(config.get("error-level", "warn").toUpperCase());
        // TODO introduce dedicated SPI for anonymization
        anonymizer = createAnonymizer();
    }

    /**
     * Allows subclasses to create custom anonymizers
     *
     * @return
     */
    protected Anonymizer.Default createAnonymizer() {
        return new Anonymizer.Default();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public String getId() {
        return ID;
    }

}
