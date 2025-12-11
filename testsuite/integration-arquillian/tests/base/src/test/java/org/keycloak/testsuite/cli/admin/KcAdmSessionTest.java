package org.keycloak.testsuite.cli.admin;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.testsuite.cli.KcAdmExec;
import org.keycloak.testsuite.util.TempFileResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.cli.KcAdmExec.execute;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmSessionTest extends AbstractAdmCliTest {

    static TypeReference<List<ObjectNode>> LIST_OF_JSON = new TypeReference<List<ObjectNode>>() {};

    @Test
    public void test() throws IOException {

        initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(FileConfigHandler.getConfigFile())) {

            // login as admin using command option and env password
            loginAsUser(configFile.getFile(), serverUrl, "master", "admin", "admin", false);
            loginAsUser(configFile.getFile(), serverUrl, "master", "admin", "admin", true);

            // create realm
            KcAdmExec exe = execute("create realms --config '" + configFile.getName() + "' -s realm=demorealm -s enabled=true");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
            Assert.assertTrue(exe.stderrLines().get(exe.stderrLines().size() - 1).startsWith("Created "));

            // create user
            exe = execute("create users --config '" + configFile.getName() + "' -r demorealm -s username=testuser -s firstName=testuser -s lastName=testuser -s email=testuser@keycloak.org -s enabled=true -i");

            assertExitCodeAndStreamSizes(exe, 0, 1, 0);
            String userId = exe.stdoutLines().get(0);

            exe = execute("get users --config '" + configFile.getName() + "' -r demorealm -q q=username:testuser");
            assertExitCodeAndStdErrSize(exe, 0, 0);
            String result = exe.stdoutString();
            assertTrue(result.contains(userId));

            exe = execute("get users --config '" + configFile.getName() + "' -r demorealm -q q=username:non-existent");
            assertExitCodeAndStdErrSize(exe, 0, 0);
            String emptyResult = exe.stdoutString();
            assertFalse(emptyResult.contains(userId));

            // add realm admin capabilities to user
            exe = execute("add-roles --config '" + configFile.getName() + "' -r demorealm --uusername testuser --cclientid realm-management --rolename realm-admin");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // set password for the user
            exe = execute("set-password --config '" + configFile.getName() + "' -r demorealm --username testuser -p password");

            assertExitCodeAndStdErrSize(exe, 0, 0);


            // login as testuser
            loginAsUser(configFile.getFile(), serverUrl, "demorealm", "testuser", "password");


            // get realm roles
            exe = execute("get-roles --config '" + configFile.getName() + "'");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            List<ObjectNode> roles = loadJson(exe.stdout(), LIST_OF_JSON);
            assertThat("expected three realm roles available", roles.size(), equalTo(3));

            // create realm role
            exe = execute("create roles --config '" + configFile.getName() + "' -s name=testrole -s 'description=Test role' -o");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            ObjectNode role = loadJson(exe.stdout(), ObjectNode.class);
            Assert.assertEquals("testrole", role.get("name").asText());
            String roleId = role.get("id").asText();

            // get realm roles again
            exe = execute("get-roles --config '" + configFile.getName() + "'");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            roles = loadJson(exe.stdout(), LIST_OF_JSON);
            assertThat("expected four realm roles available", roles.size(), equalTo(4));

            // create client
            exe = execute("create clients --config '" + configFile.getName() + "' -s clientId=testclient -i");

            assertExitCodeAndStreamSizes(exe, 0, 1, 0);
            String idOfClient = exe.stdoutLines().get(0);


            // create client role
            exe = execute("create clients/" + idOfClient + "/roles --config '" + configFile.getName() + "' -s name=clientrole  -s 'description=Test client role'");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
            Assert.assertTrue(exe.stderrLines().get(exe.stderrLines().size() - 1).startsWith("Created "));

            // make sure client role has been created
            exe = execute("get-roles --config '" + configFile.getName() + "' --cclientid testclient");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            roles = loadJson(exe.stdout(), LIST_OF_JSON);
            assertThat("expected one role", roles.size(), equalTo(1));
            Assert.assertEquals("clientrole", roles.get(0).get("name").asText());

            // add created role to user - we are realm admin so we can add role to ourself
            exe = execute("add-roles --config '" + configFile.getName() + "' --uusername testuser --cclientid testclient --rolename clientrole");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);


            // make sure the roles have been added
            exe = execute("get-roles --config '" + configFile.getName() + "' --uusername testuser --all");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            ObjectNode node = loadJson(exe.stdout(), ObjectNode.class);
            Assert.assertNotNull(node.get("realmMappings"));

            List<String> realmMappings = StreamSupport.stream(node.get("realmMappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("default-roles-demorealm"), realmMappings);

            ObjectNode clientRoles = (ObjectNode) node.get("clientMappings");
            //List<String> fields = asSortedList(clientRoles.fieldNames());
            List<String> fields = StreamSupport.stream(clientRoles.spliterator(), false)
                    .map(o -> o.get("client").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("realm-management", "testclient"), fields);

            realmMappings = StreamSupport.stream(clientRoles.get("realm-management").get("mappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("realm-admin"), realmMappings);

            realmMappings = StreamSupport.stream(clientRoles.get("testclient").get("mappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("clientrole"), realmMappings);



            // add a realm role to the user
            exe = execute("add-roles --config '" + configFile.getName() + "' --uusername testuser --rolename testrole");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);


            // get all roles for the user again
            exe = execute("get-roles --config '" + configFile.getName() + "' --uusername testuser --all");
            assertExitCodeAndStdErrSize(exe, 0, 0);

            node = loadJson(exe.stdout(), ObjectNode.class);
            Assert.assertNotNull(node.get("realmMappings"));

            realmMappings = StreamSupport.stream(node.get("realmMappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("default-roles-demorealm", "testrole"), realmMappings);

            // create a group
            exe = execute("create groups --config '" + configFile.getName() + "' -s name=TestUsers -i");
            assertExitCodeAndStdErrSize(exe, 0, 0);
            String groupId = exe.stdoutLines().get(0);

            // create a sub-group
            exe = execute("create groups/" + groupId + "/children --config '" + configFile.getName() + "' -s name=TestPowerUsers -i");
            assertExitCodeAndStdErrSize(exe, 0, 0);
            String subGroupId = exe.stdoutLines().get(0);

            // add testuser to TestPowerUsers
            exe = execute("update users/" + userId + "/groups/" + subGroupId + " --config '" + configFile.getName()
                    + "' -s realm=demorealm -s userId=" + userId +  " -s groupId=" + subGroupId + " -n");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // delete everything
            exe = execute("delete groups/" + subGroupId + " --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            exe = execute("delete groups/" + groupId + " --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            exe = execute("delete clients/" + idOfClient + " --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            exe = execute("delete roles/testrole --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            exe = execute("delete users/" + userId + " --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // delete realm as well - using initial master realm session still saved in config file
            exe = execute("delete realms/demorealm --config '" + configFile.getName() + "' --realm master");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);
        }
    }

    @Test
    public void testCompositeRoleCreationWithHigherVolumeOfRoles() throws Exception {

        initCustomConfigFile();
        try (TempFileResource configFile = new TempFileResource(FileConfigHandler.getConfigFile())) {

            // login as admin
            loginAsUser(configFile.getFile(), serverUrl, "master", "admin", "admin");

            final String realmName = "HigherVolumeRolesRealm";

            // create realm
            KcAdmExec exe = execute(String.format("create realms --config '%s' -s realm=%s -s enabled=true", configFile.getName(), realmName));
            assertExitCodeAndStreamSizes(exe, 0, 0, 1);

            for (int i = 0; i < 20; i++) {
                exe = execute(String.format("create roles --config '%s' -r %s -s name=ROLE%d", configFile.getName(), realmName, i));
                assertExitCodeAndStdErrSize(exe, 0, 1);
            }

            exe = execute(String.format("add-roles --config '%s' -r %s --rname ROLE11 --cclientid realm-management --rolename impersonation --rolename view-users --rolename view-realm --rolename manage-users", configFile.getName(), realmName));
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

        }

    }


}
