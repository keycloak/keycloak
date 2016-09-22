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
import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.sssd.infopipe.InfoPipe;
import org.freedesktop.sssd.infopipe.User;
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

    public static User user() {
        return SingletonHolder.USER_OBJECT;
    }

    public static InfoPipe infopipe() {
        return SingletonHolder.INFOPIPE_OBJECT;
    }


    public static void disconnect() {
        SingletonHolder.DBUS_CONNECTION.disconnect();
    }

    private String username;
    private static final Logger logger = Logger.getLogger(Sssd.class);

    private Sssd() {
    }

    public Sssd(String username) {
        this.username = username;
    }

    private static final class SingletonHolder {
        private static InfoPipe INFOPIPE_OBJECT;
        private static User USER_OBJECT;
        private static DBusConnection DBUS_CONNECTION;

        static {
            try {
                DBUS_CONNECTION = DBusConnection.getConnection(DBusConnection.SYSTEM);
                INFOPIPE_OBJECT = DBUS_CONNECTION.getRemoteObject(InfoPipe.BUSNAME, InfoPipe.OBJECTPATH, InfoPipe.class);
                USER_OBJECT = DBUS_CONNECTION.getRemoteObject(InfoPipe.BUSNAME, User.OBJECTPATH, User.class);
            } catch (DBusException e) {
                logger.error("Failed to obtain D-Bus connection", e);
            }
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
            InfoPipe infoPipe = infopipe();
            attributes = infoPipe.getUserAttributes(username, Arrays.asList(attr));
        } catch (Exception e) {
            logger.error("Failed to retrieve user's attributes from SSSD", e);
        }

        return attributes;
    }

    public List<String> getUserGroups() {
        List<String> userGroups = null;
        try {
            InfoPipe infoPipe = Sssd.infopipe();
            userGroups = infoPipe.getUserGroups(username);
        } catch (Exception e) {
            logger.error("Failed to retrieve user's groups from SSSD", e);
        }
        return userGroups;
    }

    public static boolean isAvailable(){
        boolean sssdAvailable = false;
        try {
            if (LibraryLoader.load().succeed()) {
                DBusConnection connection = DBusConnection.getConnection(DBusConnection.SYSTEM);
                DBus dbus = connection.getRemoteObject(DBus.BUSNAME, DBus.OBJECTPATH, DBus.class);
                sssdAvailable = Arrays.asList(dbus.ListNames()).contains(InfoPipe.BUSNAME);
                if (!sssdAvailable) {
                    logger.debugv("SSSD is not available in your system. Federation provider will be disabled.");
                } else {
                    sssdAvailable = true;
                }
                connection.disconnect();
            } else {
                logger.debugv("libunix_dbus_java not found. Federation provider will be disabled.");
            }
        } catch (DBusException e) {
            logger.error("Failed to check the status of SSSD", e);
        }
        return sssdAvailable;
    }
}
