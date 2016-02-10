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

package org.keycloak.testsuite.util.cli;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserCommands {

    public static class Create extends AbstractCommand {

        @Override
        public String getName() {
            return "createUsers";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            String usernamePrefix = getArg(0);
            String password = getArg(1);
            String realmName = getArg(2);
            int first = getIntArg(3);
            int count = getIntArg(4);
            String roleNames = getArg(5);

            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                log.errorf("Unknown realm: %s", realmName);
                return;
            }

            Set<RoleModel> roles = findRoles(realm, roleNames);

            int last = first + count;
            for (int counter = first; counter < last; counter++) {
                String username = usernamePrefix + counter;
                UserModel user = session.users().addUser(realm, username);
                user.setEnabled(true);
                user.setEmail(username + "@keycloak.org");
                UserCredentialModel passwordCred = UserCredentialModel.password(password);
                user.updateCredential(passwordCred);

                for (RoleModel role : roles) {
                    user.grantRole(role);
                }
            }
            log.infof("Users from %s to %s created", usernamePrefix + first, usernamePrefix + (last - 1));
        }

        @Override
        public String printUsage() {
            return super.printUsage() + " <username-prefix> <password> <realm-name> <starting-user-offset> <count> <realm-roles-list>. \nRoles list is divided by comma (client roles not yet supported)>\n" +
                    "Example usage: " + super.printUsage() + " test test demo 0 20 user,admin";
        }

        private Set<RoleModel> findRoles(RealmModel realm, String rolesList) {
            Set<RoleModel> result = new HashSet<>();

            String[] roles = rolesList.split(",");
            for (String roleName : roles) {
                roleName = roleName.trim();
                RoleModel role;
                if (roleName.contains("/")) {
                    String[] spl = roleName.split("/");
                    ClientModel client = realm.getClientByClientId(spl[0]);
                    if (client == null) {
                        log.errorf("Client not found: %s", spl[0]);
                        throw new HandledException();
                    }
                    role = client.getRole(spl[1]);
                } else {
                    role = realm.getRole(roleName);
                }

                if (role == null) {
                    log.errorf("Role not found: %s", roleName);
                    throw new HandledException();
                }

                result.add(role);
            }

            return result;
        }

    }


    public static class Remove extends AbstractCommand {

        @Override
        public String getName() {
            return "removeUsers";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            String usernamePrefix = getArg(0);
            String realmName = getArg(1);
            int first = getIntArg(2);
            int count = getIntArg(3);

            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                log.errorf("Unknown realm: %s", realmName);
                return;
            }

            int last = first + count;
            for (int counter = first; counter < last; counter++) {
                String username = usernamePrefix + counter;
                UserModel user = session.users().getUserByUsername(username, realm);
                if (user == null) {
                    log.errorf("User '%s' not found", username);
                } else {
                    session.users().removeUser(realm, user);
                }
            }
            log.infof("Users from %s to %s removed", usernamePrefix + first, usernamePrefix + (last - 1));
        }

        @Override
        public String printUsage() {
            return super.printUsage() + " <username-prefix> <realm-name> <starting-user-offset> <count> \n" +
                    "Example usage: " + super.printUsage() + " test demo 0 20";
        }
    }


    public static class Count extends AbstractCommand {

        @Override
        public String getName() {
            return "getUsersCount";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            String realmName = getArg(0);
            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                log.errorf("Unknown realm: %s", realmName);
                return;
            }

            int usersCount = session.users().getUsersCount(realm);
            log.infof("Users count in realm %s: %d", realmName, usersCount);
        }

        @Override
        public String printUsage() {
            return super.printUsage() + " <realm-name>";
        }
    }


    public static class GetUser extends AbstractCommand {

        @Override
        public String getName() {
            return "getUser";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            String realmName = getArg(0);
            String username = getArg(1);
            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                log.errorf("Unknown realm: %s", realmName);
                return;
            }

            UserModel user = session.users().getUserByUsername(username, realm);
            if (user == null) {
                log.infof("User '%s' doesn't exist in realm '%s'", username, realmName);
            } else {
                log.infof("User: ID: '%s', username: '%s', mail: '%s'", user.getId(), user.getUsername(), user.getEmail());
            }
        }

        @Override
        public String printUsage() {
            return super.printUsage() + " <realm-name> <username>";
        }
    }
}
