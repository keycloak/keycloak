package org.keycloak.testsuite.util.cli;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class GroupCommands {

    public static class Create extends AbstractCommand {

        private String groupPrefix;
        private String realmName;

        @Override
        public String getName() {
            return "createGroups";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            groupPrefix = getArg(0);
            realmName = getArg(1);
            int first = getIntArg(2);
            int count = getIntArg(3);
            int batchCount = getIntArg(4);

            BatchTaskRunner.runInBatches(first, count, batchCount, session.getKeycloakSessionFactory(), this::createGroupsInBatch);

            log.infof("Command finished. All groups from %s to %s created", groupPrefix + first, groupPrefix
                    + (first + count - 1));
        }

        private void createGroupsInBatch(KeycloakSession session, int first, int count) {
            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                log.errorf("Unknown realm: %s", realmName);
                throw new HandledException();
            }

            int last = first + count;
            for (int counter = first; counter < last; counter++) {
                String groupName = groupPrefix + counter;
                GroupModel group = session.groups().createGroup(realm, groupName);
                group.setSingleAttribute("test-attribute", groupName + "_testAttribute");
            }
            log.infof("groups from %s to %s created", groupPrefix + first, groupPrefix + (last - 1));
        }

        @Override
        public String printUsage() {
            return super.printUsage() + " <group-prefix> <realm-name> <starting-group-offset> <total-count> <batch-size>. " +
                    "\n'total-count' refers to total count of newly created groups. 'batch-size' refers to number of created groups in each transaction. 'starting-group-offset' refers to starting group offset." +
                    "\nFor example if 'starting-group-offset' is 15 and total-count is 10 and group-prefix is 'test', it will create groups test15, test16, test17, ... , test24" +
                    "Example usage: " + super.printUsage() + " test demo 0 500 100";
        }

    }

}
