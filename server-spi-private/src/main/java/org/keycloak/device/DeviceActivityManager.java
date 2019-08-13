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

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.models.DeviceModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserDeviceStore;
import org.keycloak.models.UserSessionModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeviceActivityManager {

    private static final Logger logger = Logger.getLogger(DeviceActivityManager.class);
    private static final String DEVICE_UPDATED_FLAG = "DEVICE_UPDATED";

    public static void createOrUpdateDevice(UserSessionModel userSession, KeycloakSession session) {
        if (!Profile.isFeatureEnabled(Profile.Feature.DEVICE_ACTIVITY)) {
            return;
        }

        if (session.getAttributeOrDefault(DEVICE_UPDATED_FLAG, true)) {
            UserDeviceStore deviceProvider = (UserDeviceStore) session.users();

            for (DeviceFingerPrintProvider provider : session.getAllProviders(DeviceFingerPrintProvider.class)) {
                DeviceModel current = provider.getCurrentDevice(userSession, deviceProvider);

                // the first provider to resolve a device wins, we can make this configurable on a per-realm or per-client basis
                if (current != null) {
                    DeviceModel device = getUserDevice(userSession, current, deviceProvider, provider);

                    if (device == null) {
                        registerDevice(userSession, current, deviceProvider, session);
                    } else {
                        updateDevice(userSession, device, session);
                    }

                    session.setAttribute(DEVICE_UPDATED_FLAG, false);
                    return;
                }
            }
        }
    }

    private static DeviceModel getUserDevice(UserSessionModel userSession, DeviceModel current,
            UserDeviceStore deviceProvider, DeviceFingerPrintProvider fingerPrintProvider) {
        // the the provider returned a stored device, use it
        if (current != null && current.getId() != null) {
            return current;
        }

        DeviceModel attached = getAttachedDevice(userSession, current, deviceProvider, fingerPrintProvider);

        // if a device is already attached to the session, use it
        if (attached != null) {
            return attached;
        }

        List<DeviceModel> stored = deviceProvider.getDevices(userSession.getUser());

        // as a fallback, asks for the DeviceFingerPrintProvider if any stored device is the same as the current one
        return stored.stream().filter(model -> fingerPrintProvider.isSameDevice(current, model)).findFirst().orElse(null);
    }

    private static DeviceModel getAttachedDevice(UserSessionModel userSession, DeviceModel current,
            UserDeviceStore deviceProvider,
            DeviceFingerPrintProvider fingerPrintProvider) {
        String attached = userSession.getNote(DeviceModel.DEVICE_ID);

        if (attached != null) {
            DeviceModel device = deviceProvider.getDeviceById(userSession.getUser(), attached);

            if (device != null) {
                if (!fingerPrintProvider.isSameDevice(current, device)) {
                    logger.warnf("Fingerprint of device attached to session [%s] differs from current device [%s]", device,
                            current);
                }
                return device;
            }
        }

        return null;
    }

    private static void registerDevice(UserSessionModel userSession, DeviceModel current, UserDeviceStore deviceProvider,
            KeycloakSession session) {
        current.setIp(session.getContext().getConnection().getRemoteAddr());
        current.setLastAccess(userSession.getLastSessionRefresh());
        deviceProvider.addDevice(userSession.getRealm(), userSession.getUser(), current);
        if (userSession.getNote(DeviceModel.DEVICE_ID) == null) {
            userSession.setNote(DeviceModel.DEVICE_ID, current.getId());
        }
    }

    private static void updateDevice(UserSessionModel userSession, DeviceModel device, KeycloakSession session) {
        String currentIp = session.getContext().getConnection().getRemoteAddr();

        if (!device.getIp().equals(currentIp)) {
            device.setIp(currentIp);
        }

        int lastSessionRefresh = userSession.getLastSessionRefresh();

        if (device.getLastAccess() != lastSessionRefresh) {
            device.setLastAccess(lastSessionRefresh);
        }
    }

    public static void createFingerprint(UserSessionModel userSession, KeycloakSession keycloakSession) {
        for (DeviceFingerPrintProvider provider : keycloakSession.getAllProviders(DeviceFingerPrintProvider.class)) {
            // try to generate a server-side fingerprint so that the provider can identify devices on subsequent requests
            provider.createFingerprint(userSession);
        }
    }
}
