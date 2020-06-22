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

package org.keycloak.events.log;

import org.keycloak.common.util.StackUtil;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.privacy.PrivacyFilterProvider;
import org.keycloak.privacy.anonymize.Anonymizer;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JBossLoggingEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private final Logger logger;
    private final Logger.Level successLevel;
    private final Logger.Level errorLevel;
    private final PrivacyFilterProvider privacyFilter;

    public JBossLoggingEventListenerProvider(KeycloakSession session, Logger logger, Logger.Level successLevel, Logger.Level errorLevel, PrivacyFilterProvider privacyFilter) {
        this.session = session;
        this.logger = logger;
        this.successLevel = successLevel;
        this.errorLevel = errorLevel;
        this.privacyFilter = privacyFilter;
    }

    @Override
    public void onEvent(Event event) {
        Logger.Level level = event.getError() != null ? errorLevel : successLevel;

        if (logger.isEnabled(level)) {
            StringBuilder sb = new StringBuilder();

            sb.append("type=");
            sb.append(event.getType());
            sb.append(", realmId=");
            sb.append(event.getRealmId());
            sb.append(", clientId=");
            sb.append(event.getClientId());
            sb.append(", userId=");
            sb.append(getUserId(event));
            sb.append(", ipAddress=");
            sb.append(getIpAddress(event));

            if (event.getError() != null) {
                sb.append(", error=");
                sb.append(event.getError());
            }

            if (event.getDetails() != null) {
                for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                    sb.append(", ");

                    String eventDetailValue = getEventDetailValue(event, e.getKey(), e.getValue());

                    sb.append(e.getKey());
                    if (eventDetailValue == null || eventDetailValue.indexOf(' ') == -1) {
                        sb.append("=");
                        sb.append(eventDetailValue);
                    } else {
                        sb.append("='");
                        sb.append(eventDetailValue);
                        sb.append("'");
                    }
                }
            }
            
            AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession(); 
            if(authSession!=null) {
                sb.append(", authSessionParentId=");
                sb.append(authSession.getParentSession().getId());
                sb.append(", authSessionTabId=");
                sb.append(authSession.getTabId());
            }
            
            if(logger.isTraceEnabled()) {
                setKeycloakContext(sb);

                if (StackUtil.isShortStackTraceEnabled()) {
                    sb.append(", stackTrace=").append(StackUtil.getShortStackTrace());
                }
            }

            logger.log(logger.isTraceEnabled() ? Logger.Level.TRACE : level, sb.toString());
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        Logger.Level level = adminEvent.getError() != null ? errorLevel : successLevel;

        if (logger.isEnabled(level)) {
            StringBuilder sb = new StringBuilder();

            sb.append("operationType=");
            sb.append(adminEvent.getOperationType());
            sb.append(", realmId=");
            sb.append(adminEvent.getAuthDetails().getRealmId());
            sb.append(", clientId=");
            sb.append(adminEvent.getAuthDetails().getClientId());
            sb.append(", userId=");
            sb.append(adminEvent.getAuthDetails().getUserId());
            sb.append(", ipAddress=");
            sb.append(adminEvent.getAuthDetails().getIpAddress());
            sb.append(", resourceType=");
            sb.append(adminEvent.getResourceTypeAsString());
            sb.append(", resourcePath=");
            sb.append(adminEvent.getResourcePath());

            if (adminEvent.getError() != null) {
                sb.append(", error=");
                sb.append(adminEvent.getError());
            }
            
            if(logger.isTraceEnabled()) {
                setKeycloakContext(sb);
            }

            logger.log(logger.isTraceEnabled() ? Logger.Level.TRACE : level, sb.toString());
        }
    }

    /**
     * Enables subclasses to filter / mask or anonymize event detail values.
     * @param event
     * @param key
     * @param value
     * @return
     */
    protected String getEventDetailValue(Event event, String key, String value) {
        return privacyFilter.filter(key, value, key, event);
    }

    /**
     * Enables subclasses to filter / mask or anonymize IP addresses.
     * @param event
     * @return
     */
    protected String getIpAddress(Event event) {
        return privacyFilter.filter(Anonymizer.IP_ADDRESS, event.getIpAddress(), Anonymizer.IP_ADDRESS, event);
    }

    /**
     * Enables subclasses to filter / mask or anonymize userIds.
     * @param event
     * @return
     */
    protected String getUserId(Event event) {
        return privacyFilter.filter(Anonymizer.USER_ID, event.getUserId(), Anonymizer.USER_ID, event);
    }

    @Override
    public void close() {
    }
    
    private void setKeycloakContext(StringBuilder sb) {
        KeycloakContext context = session.getContext();
        UriInfo uriInfo = context.getUri();
        HttpHeaders headers = context.getRequestHeaders();
        if (uriInfo != null) {
            sb.append(", requestUri=");
            sb.append(uriInfo.getRequestUri().toString());
        }

        if (headers != null) {
            sb.append(", cookies=[");
            boolean f = true;
            for (Map.Entry<String, Cookie> e : headers.getCookies().entrySet()) {
                if (f) {
                    f = false;
                } else {
                    sb.append(", ");
                }
                sb.append(e.getValue().toString());
            }
            sb.append("]");
        }
        
    }

}
