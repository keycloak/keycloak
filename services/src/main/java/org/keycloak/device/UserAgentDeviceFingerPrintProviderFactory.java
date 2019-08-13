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
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.DeviceModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserDeviceStore;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.CookieHelper;
import ua_parser.Client;
import ua_parser.Parser;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserAgentDeviceFingerPrintProviderFactory implements DeviceFingerPrintProviderFactory {

    private static final Logger logger = Logger.getLogger(UserAgentDeviceFingerPrintProviderFactory.class);
    private static final String COOKIE_USER_DEVICE = "KC_USER_DEVICE";
    private static final int USER_AGENT_MAX_LENGTH = 512;
    private Parser parser;

    @Override
    public DeviceFingerPrintProvider create(KeycloakSession session) {
        return new DeviceFingerPrintProvider() {
            @Override
            public DeviceModel getCurrentDevice(UserSessionModel userSession,
                    UserDeviceStore deviceStore) {
                DeviceModel current = getDeviceFromUserAgent();

                if (current == null)
                    return null;

                Set<String> deviceId = CookieHelper.getCookieValue(COOKIE_USER_DEVICE);

                if (!deviceId.isEmpty()) {
                    DeviceModel storedDevice = deviceStore.getDeviceById(userSession.getUser(), deviceId.iterator().next());

                    if (storedDevice != null) {
                        if (isSameDevice(current, storedDevice, false)) {
                            if (!storedDevice.getBrowser().contains(current.getBrowser())) {
                                storedDevice.addBrowser(current.getBrowser());
                            }
                            return storedDevice;
                        }
                    }
                }

                return current;
            }

            @Override
            public void createFingerprint(UserSessionModel userSession) {
                if (getUserAgent(session.getContext()) != null) {
                    String deviceId = userSession.getNote(DeviceModel.DEVICE_ID);

                    if (deviceId != null) {
                        RealmModel realm = session.getContext().getRealm();
                        boolean secureOnly = realm.getSslRequired().isRequired(session.getContext().getConnection());
                        CookieHelper.addCookie(COOKIE_USER_DEVICE, deviceId, AuthenticationManager
                                        .getIdentityCookiePath(realm,
                                                session.getContext().getUri().getDelegate()), null, null, -1,
                                secureOnly, true);
                    }
                }
            }

            @Override
            public boolean isSameDevice(DeviceModel current, DeviceModel device) {
                return isSameDevice(current, device, true);
            }

            private boolean isSameDevice(DeviceModel current, DeviceModel device, boolean checkIp) {
                if (current.getOs().equals(device.getOs()) && current.getOsVersionOrDefault()
                        .equals(device.getOsVersionOrDefault())) {
                    if (current.getDevice().equals(device.getDevice())) {
                        if (checkIp) {
                            if (current.getIp().equals(device.getIp())) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
                return false;
            }

            private DeviceModel getDeviceFromUserAgent() {
                KeycloakContext context = session.getContext();
                String userAgent = getUserAgent(context);

                if (userAgent == null) {
                    return null;
                }

                if (userAgent.length() > USER_AGENT_MAX_LENGTH) {
                    logger.warn("Ignoring User-Agent header. Length is above the permitted: " + USER_AGENT_MAX_LENGTH);
                    return null;
                }

                DeviceModel current;

                try {
                    Client client = parser.parse(userAgent);
                    current = new DeviceModel();

                    current.setDevice(client.device.family);

                    String browserVersion = client.userAgent.major;

                    if (client.userAgent.minor != null) {
                        browserVersion += "." + client.userAgent.minor;
                    }

                    if (client.userAgent.patch != null) {
                        browserVersion += "." + client.userAgent.patch;
                    }

                    current.addBrowser(client.userAgent.family, browserVersion);
                    current.setOs(client.os.family);

                    String osVersion = client.os.major;

                    if (client.os.minor != null) {
                        osVersion += "." + client.os.minor;
                    }

                    if (client.os.patch != null) {
                        osVersion += "." + client.os.patch;
                    }

                    if (client.os.patchMinor != null) {
                        osVersion += "." + client.os.patchMinor;
                    }

                    current.setOsVersion(osVersion);
                    current.setIp(context.getConnection().getRemoteAddr());
                } catch (Exception cause) {
                    logger.error("Failed to create device info from user agent header", cause);
                    return null;
                }

                return current;
            }

            private String getUserAgent(KeycloakContext context) {
                return context.getRequestHeaders().getHeaderString("user-agent");
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public void init(Config.Scope config) {
        try {
            parser = new Parser();
        } catch (IOException cause) {
            throw new RuntimeException("Failed to create user agent parser", cause);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "user-agent-device-fingerprint";
    }
}
