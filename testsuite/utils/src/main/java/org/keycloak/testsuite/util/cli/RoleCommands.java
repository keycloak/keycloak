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
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleCommands {

    public static class CreateRoles extends AbstractCommand {

        private String rolePrefix;
        private String roleContainer;

        @Override
        public String getName() {
            return "createRoles";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            rolePrefix = getArg(0);
            roleContainer = getArg(1);
            int first = getIntArg(2);
            int count = getIntArg(3);
            int batchCount = getIntArg(4);

            BatchTaskRunner.runInBatches(first, count, batchCount, session.getKeycloakSessionFactory(), (KeycloakSession bathcSession, int firstInThisIteration, int countInThisIteration) -> {
                createRolesInBatch(session, roleContainer, rolePrefix, firstInThisIteration, countInThisIteration);
            });

            log.infof("Command finished. All roles from %s to %s created", rolePrefix + first, rolePrefix + (first + count - 1));
        }

        private void createRolesInBatch(KeycloakSession session, String roleContainer, String rolePrefix, int first, int count) {
            RoleContainerModel container = getRoleContainer(session, roleContainer);

            int last = first + count;
            for (int counter = first; counter < last; counter++) {
                String roleName = rolePrefix + counter;
                RoleModel role = container.addRole(roleName);
            }
            log.infof("Roles from %s to %s created", rolePrefix + first, rolePrefix + (last - 1));
        }

        private RoleContainerModel getRoleContainer(KeycloakSession session, String roleContainer) {
            String[] parts = roleContainer.split("/");
            String realmName = parts[0];

            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                log.errorf("Unknown realm: %s", realmName);
                throw new HandledException();
            }

            if (parts.length == 1) {
                return realm;
            } else {
                String clientId = parts[1];
                ClientModel client = session.realms().getClientByClientId(clientId, realm);
                if (client == null) {
                    log.errorf("Unknown client: %s", clientId);
                    throw new HandledException();
                }

                return client;
            }
        }

        @Override
        public String printUsage() {
            return super.printUsage() + " <role-prefix> <role-container> <starting-role-offset> <total-count> <batch-size> . " +
                    "\n'total-count' refers to total count of newly created roles. 'batch-size' refers to number of created roles in each transaction. 'starting-role-offset' refers to starting role offset." +
                    "\nFor example if 'starting-role-offset' is 15 and total-count is 10 and role-prefix is 'test', it will create roles test15, test16, test17, ... , test24" +
                    "\n'role-container' is either realm (then use just realmName like 'demo' or client (then use realm/clientId like 'demo/my-client' .\n" +
                    "Example usage: " + super.printUsage() + " test demo 0 500 100";
        }

    }
}
