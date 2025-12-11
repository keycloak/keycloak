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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.keycloak.models.UserModel;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.sssd.infopipe.InfoPipe;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class Sssd {

    private final DBusConnection dBusConnection;
    private final String username;
    private static final Logger logger = Logger.getLogger(Sssd.class);

    public Sssd(String username, DBusConnection dbusConnection) throws SSSDException {
        this.username = username;
        this.dBusConnection = dbusConnection;
    }

    public static String getRawAttribute(Variant variant) {
        if (variant != null && variant.getType() instanceof DBusListType) {
            List<?> value = (List) variant.getValue();
            if (!value.isEmpty()) {
                return value.iterator().next().toString();
            }
        }
        return null;
    }

    public List<String> getGroups() {
        List<String> userGroups;
        try {
            InfoPipe infoPipe = dBusConnection.getRemoteObject(InfoPipe.BUSNAME, InfoPipe.OBJECTPATH, InfoPipe.class);
            userGroups = infoPipe.getUserGroups(username);
        } catch (Exception e) {
            throw new SSSDException("Failed to retrieve user's groups from SSSD. Check if SSSD service is active.", e);
        }
        return userGroups;
    }

    public User getUser() {
        String[] attr = {"mail", "givenname", "sn", "telephoneNumber"};
        User user = null;
        try {
            InfoPipe infoPipe = dBusConnection.getRemoteObject(InfoPipe.BUSNAME, InfoPipe.OBJECTPATH, InfoPipe.class);
            user = new User(infoPipe.getUserAttributes(username, Arrays.asList(attr)));
        } catch (Exception e) {
            logger.debugf(e, "Failed to retrieve attributes for user '%s'. Check if SSSD service is active.", username);
        }
        return user;
    }

    public class User {

        private final String email;
        private final String firstName;
        private final String lastName;

        public User(Map<String, Variant> userAttributes) {
            this.email = getRawAttribute(userAttributes.get("mail"));
            this.firstName = getRawAttribute(userAttributes.get("givenname"));
            this.lastName = getRawAttribute(userAttributes.get("sn"));

        }

        public String getEmail() {
            return email;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;

            UserModel userModel = (UserModel) o;
            if (firstName != null && !firstName.equals(userModel.getFirstName())) {
                return false;
            }
            if (lastName != null && !lastName.equals(userModel.getLastName())) {
                return false;
            }
            if (email != null) {
                return email.equalsIgnoreCase(userModel.getEmail());
            }
            return userModel.getEmail() == null;
        }

        @Override
        public int hashCode() {
            int result = email != null ? email.hashCode() : 0;
            result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
            result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
            return result;
        }
    }
}
