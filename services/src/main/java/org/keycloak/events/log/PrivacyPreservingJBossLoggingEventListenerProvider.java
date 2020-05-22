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
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;

/**
 * A {@link JBossLoggingEventListenerProvider} which has support to anonymize personal identifiable information (PII) values via an {@link Anonymizer}.
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public class PrivacyPreservingJBossLoggingEventListenerProvider extends JBossLoggingEventListenerProvider {

    private final Anonymizer anonymizer;

    public PrivacyPreservingJBossLoggingEventListenerProvider(KeycloakSession session, Logger logger, Logger.Level successLevel, Logger.Level errorLevel, Anonymizer anonymizer) {
        super(session, logger, successLevel, errorLevel);
        this.anonymizer = anonymizer;
    }

    @Override
    protected String getUserId(Event event) {
        return anonymizer.anonymize(Anonymizer.USER_ID, super.getUserId(event));
    }

    @Override
    protected String getIpAddress(Event event) {
        return anonymizer.anonymize(Anonymizer.IP_ADDRESS, super.getIpAddress(event));
    }

    @Override
    protected String getEventDetailValue(Event event, String key, String value) {
        return anonymizer.anonymize(key, super.getEventDetailValue(event, key, value));
    }
}
