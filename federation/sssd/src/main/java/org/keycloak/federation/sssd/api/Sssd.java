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

package org.keycloak.federation.sssd.api;

import cx.ath.matthew.LibraryLoader;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.sssd.infopipe.InfoPipe;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class Sssd {

    private static DBusConnection dBusConnection;

    public static void disconnect() {
        dBusConnection.disconnect();
    }

    private String username;
    private static final Logger logger = Logger.getLogger(Sssd.class);

    private Sssd() {
    }

    public Sssd(String username) {
        this.username = username;
        try {
            if (LibraryLoader.load().succeed())
                dBusConnection = DBusConnection.getConnection(DBusConnection.SYSTEM);
        } catch (DBusException e) {
            e.printStackTrace();
        }

    }

    public static String getRawAttribute(Variant variant) {
        if (variant != null) {
            Vector value = (Vector) variant.getValue();
            if (value.size() >= 1) {
                return value.get(0).toString();
            }
        }
        return null;
    }

    public Map<String, Variant> getUserAttributes() {
        String[] attr = {"mail", "givenname", "sn", "telephoneNumber"};
        Map<String, Variant> attributes = null;
        try {
            InfoPipe infoPipe = dBusConnection.getRemoteObject(InfoPipe.BUSNAME, InfoPipe.OBJECTPATH, InfoPipe.class);
            attributes = infoPipe.getUserAttributes(username, Arrays.asList(attr));
        } catch (Exception e) {
            throw new SSSDException("Failed to retrieve user's attributes. Check if SSSD service is active.");
        }

        return attributes;
    }

    public List<String> getUserGroups() {
        List<String> userGroups;
        try {
            InfoPipe infoPipe = dBusConnection.getRemoteObject(InfoPipe.BUSNAME, InfoPipe.OBJECTPATH, InfoPipe.class);
            userGroups = infoPipe.getUserGroups(username);
        } catch (Exception e) {
            throw new SSSDException("Failed to retrieve user's groups from SSSD. Check if SSSD service is active.");
        }
        return userGroups;
    }

    public static boolean isAvailable() {
        boolean sssdAvailable = false;
        try {
            if (LibraryLoader.load().succeed()) {
                DBusConnection connection = DBusConnection.getConnection(DBusConnection.SYSTEM);
                InfoPipe infoPipe = connection.getRemoteObject(InfoPipe.BUSNAME, InfoPipe.OBJECTPATH, InfoPipe.class);

                if (infoPipe.ping("PING") == null || infoPipe.ping("PING").isEmpty()) {
                    logger.debugv("SSSD is not available in your system. Federation provider will be disabled.");
                } else {
                    sssdAvailable = true;
                }
            } else {
                logger.debugv("The RPM libunix-dbus-java is not installed. SSSD Federation provider will be disabled.");
            }
        } catch (Exception e) {
            logger.debugv("SSSD is not available in your system. Federation provider will be disabled.", e);
        }
        return sssdAvailable;
    }

}
