/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.device;

import java.io.IOException;
import java.util.Base64;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeviceActivityManager {

    private static final String DEVICE_NOTE = "KC_DEVICE_NOTE";

    /** Returns the device information associated with the given {@code userSession}.
     * 
     * 
     * @param userSession the userSession
     * @return the device information or null if no device is attached to the user session
     */
    public static DeviceRepresentation getCurrentDevice(UserSessionModel userSession) {
        String deviceInfo = userSession.getNote(DEVICE_NOTE);

        if (deviceInfo == null) {
            return null;
        }

        try {
            return JsonSerialization.readValue(Base64.getDecoder().decode(deviceInfo), DeviceRepresentation.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attaches a device to the given {@code userSession} where the device information is obtained from the {@link HttpHeaders#USER_AGENT} in the current
     * request, if available.
     * 
     * @param userSession the user session
     * @param session the keycloak session
     */
    public static void attachDevice(UserSessionModel userSession, KeycloakSession session) {
        DeviceRepresentation current = session.getProvider(DeviceRepresentationProvider.class).deviceRepresentation();

        if (current != null) {
            try {
                userSession.setNote(DEVICE_NOTE, Base64.getEncoder().encodeToString(JsonSerialization.writeValueAsBytes(current)));
            } catch (IOException cause) {
                throw new RuntimeException(cause);
            }
        }
    }
}
