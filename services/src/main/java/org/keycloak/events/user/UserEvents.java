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

package org.keycloak.events.user;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UserEvents {
    private static final Logger logger = Logger.getLogger(UserEvents.class);

    private UserEvents() {
    }

    public static UserEventContext createByAdmin(KeycloakSession session, UserRepresentation rep) {
        return new UserEventContext(session, UserEventType.CREATE_BY_ADMIN, rep);
    }

    public static UserEventContext createSelfRegister(KeycloakSession session, String username, String email) {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(username);
        rep.setEmail(email);

        return new UserEventContext(session, UserEventType.CREATE_SELF_REGISTER, rep);
    }

    public static class UserEventContext {
        private final KeycloakSession session;
        private final UserEvent event;
        private boolean preEventFired = false;
        private boolean postEventFired = false;
        private final Map<Class<? extends UserEventListener>, UserEventListener.ApprovalResult> approvalResults = new HashMap<>();

        private UserEventContext(KeycloakSession session, UserEvent event) {
            this.session = session;
            this.event = event;
        }

        private UserEventContext(KeycloakSession session, UserEventType eventType, UserRepresentation rep) {
            UserEvent event = UserEvent.builder(KeycloakModelUtils.generateId(), eventType)
                    .setRepresentation(rep)
                    .build();
            this.session = session;
            this.event = event;
        }

        public UserEventContext firePreEvent() {
            if (!Profile.isFeatureEnabled(Profile.Feature.USER_EVENT_SPI)) {
                logger.debug("Feature USER_EVENT_SPI disabled, not firing any pre-events");
                return this;
            }

            if (preEventFired) throw new IllegalStateException("Pre-event was already fired");

            Set<UserEventListener> listeners = session.getAllProviders(UserEventListener.class);
            for (UserEventListener listener : listeners) {
                approvalResults.put(listener.getClass(), listener.handlePreEvent(event));
            }

            // TODO throw exceptions if a listener doesn't approve

            preEventFired = true;

            return this;
        }

        public UserEventContext firePostEvent() {
            if (!Profile.isFeatureEnabled(Profile.Feature.USER_EVENT_SPI)) {
                logger.debug("Feature USER_EVENT_SPI disabled, not firing any post-events");
                return this;
            }

            if (postEventFired) throw new IllegalStateException("Post-event was already fired");

            Set<UserEventListener> listeners = session.getAllProviders(UserEventListener.class);
            for (UserEventListener listener : listeners) {
                listener.handlePostEvent(event);
            }

            postEventFired = true;

            return this;
        }

        public UserEvent getEvent() {
            return event;
        }

        public Map<Class<? extends UserEventListener>, UserEventListener.ApprovalResult> getApprovalResults() {
            if (!preEventFired) throw new IllegalStateException("Pre-event was not yet fired");
            return Collections.unmodifiableMap(approvalResults);
        }
    }
}
