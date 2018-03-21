/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.cli;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.console.ConsoleUsernamePasswordAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.TotpUtils;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test that clients can override auth flows
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class KcinitTest extends AbstractTestRealmKeycloakTest {

    public static final String KCINIT_CLIENT = "kcinit";
    public static final String APP = "app";
    public static final String UNAUTHORIZED_APP = "unauthorized_app";
    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class)
                .addPackages(true, "org.keycloak.testsuite");
    }


    @Before
    public void setupFlows() {
        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction("terms_and_conditions");
        rep.setEnabled(true);
        adminClient.realm("test").flows().updateRequiredAction("terms_and_conditions", rep);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            ClientModel client = session.realms().getClientByClientId("kcinit", realm);
            if (client != null) {
                return;
            }

            ClientModel kcinit = realm.addClient(KCINIT_CLIENT);
            kcinit.setSecret("password");
            kcinit.setEnabled(true);
            kcinit.addRedirectUri("urn:ietf:wg:oauth:2.0:oob");
            kcinit.setPublicClient(false);

            ClientModel app = realm.addClient(APP);
            app.setSecret("password");
            app.setEnabled(true);
            app.setPublicClient(false);

            ClientModel unauthorizedApp = realm.addClient(UNAUTHORIZED_APP);
            unauthorizedApp.setSecret("password");
            unauthorizedApp.setEnabled(true);
            unauthorizedApp.setPublicClient(false);

            // permission for client to client exchange to "target" client
            AdminPermissionManagement management = AdminPermissions.management(session, realm);
            management.clients().setPermissionsEnabled(app, true);
            ClientPolicyRepresentation clientRep = new ClientPolicyRepresentation();
            clientRep.setName("to");
            clientRep.addClient(kcinit.getId());
            ResourceServer server = management.realmResourceServer();
            Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(clientRep, server);
            management.clients().exchangeToPermission(app).addAssociatedPolicy(clientPolicy);
            PasswordPolicy policy = realm.getPasswordPolicy();
            policy = PasswordPolicy.parse(session, "hashIterations(1)");
            realm.setPasswordPolicy(policy);

            UserModel user = session.users().addUser(realm, "bburke");
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password("password"));
            user.setEnabled(true);
            user.setEmail("p@p.com");
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
            user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
            user.addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
            user.addRequiredAction(TermsAndConditions.PROVIDER_ID);
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);

            user = session.users().addUser(realm, "wburke");
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password("password"));
            user.setEnabled(true);
            user = session.users().addUser(realm, "tbrady");
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password("password"));
            user.setEnabled(true);
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        });
    }

    //@Test
    public void testDemo() throws Exception {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            Map<String, String> smtp = new HashMap<>();
            smtp.put("host", "smtp.gmail.com");
            smtp.put("port", "465");
            smtp.put("fromDisplayName", "Keycloak SSO");
            smtp.put("from", "****");
            smtp.put("replyToDisplayName", "Keycloak no-reply");
            smtp.put("replyTo", "reply-to@keycloak.org");
            smtp.put("ssl", "true");
            smtp.put("auth", "true");
            smtp.put("user", "*****");
            smtp.put("password", "****");
            realm.setSmtpConfig(smtp);

        });

        Thread.sleep(100000000);
    }

    @Test
    public void testBadCommand() throws Exception {
        KcinitExec exe = KcinitExec.execute("covfefe");
        Assert.assertEquals(1, exe.exitCode());
        Assert.assertEquals("stderr first line", "Error: unknown command \"covfefe\" for \"kcinit\"", exe.stderrLines().get(0));
    }

    //@Test
    public void testInstall() throws Exception {
        KcinitExec exe = KcinitExec.execute("uninstall");
        Assert.assertEquals(0, exe.exitCode());

        exe = KcinitExec.newBuilder()
                .argsLine("install")
                .executeAsync();
        //System.out.println(exe.stderrString());
        //exe.waitForStderr("(y/n):");
        //exe.sendLine("n");
        exe.waitForStderr("Authentication server URL [http://localhost:8080/auth]:");
        exe.sendLine(OAuthClient.AUTH_SERVER_ROOT);
        //System.out.println(exe.stderrString());
        exe.waitForStderr("Name of realm [master]:");
        exe.sendLine("test");
        //System.out.println(exe.stderrString());
        exe.waitForStderr("client id [kcinit]:");
        exe.sendLine("");
        //System.out.println(exe.stderrString());
        exe.waitForStderr("Client secret [none]:");
        exe.sendLine("password");
        //System.out.println(exe.stderrString());
        exe.waitCompletion();
        Assert.assertEquals(0, exe.exitCode());
    }

    @Test
    public void testBasic() throws Exception {
        testInstall();
        // login
        //System.out.println("login....");
        KcinitExec exe = KcinitExec.newBuilder()
                .argsLine("login")
                .executeAsync();
        //System.out.println(exe.stderrString());
        exe.waitForStderr("Username:");
        exe.sendLine("wburke");
        //System.out.println(exe.stderrString());
        exe.waitForStderr("Password:");
        exe.sendLine("password");
        //System.out.println(exe.stderrString());
        exe.waitForStderr("Login successful");
        exe.waitCompletion();
        Assert.assertEquals(0, exe.exitCode());
        Assert.assertEquals(0, exe.stdoutLines().size());

        exe = KcinitExec.execute("token");
        Assert.assertEquals(0, exe.exitCode());
        Assert.assertEquals(1, exe.stdoutLines().size());
        String token = exe.stdoutLines().get(0).trim();
        //System.out.println("token: " + token);
        String introspect = oauth.introspectAccessTokenWithClientCredential("kcinit", "password", token);
        Map json = JsonSerialization.readValue(introspect, Map.class);
        Assert.assertTrue(json.containsKey("active"));
        Assert.assertTrue((Boolean)json.get("active"));
        //System.out.println("introspect");
        //System.out.println(introspect);

        exe = KcinitExec.execute("token app");
        Assert.assertEquals(0, exe.exitCode());
        Assert.assertEquals(1, exe.stdoutLines().size());
        String appToken = exe.stdoutLines().get(0).trim();
        Assert.assertFalse(appToken.equals(token));
        //System.out.println("token: " + token);
        introspect = oauth.introspectAccessTokenWithClientCredential("kcinit", "password", appToken);
        json = JsonSerialization.readValue(introspect, Map.class);
        Assert.assertTrue(json.containsKey("active"));
        Assert.assertTrue((Boolean)json.get("active"));


        exe = KcinitExec.execute("token badapp");
        Assert.assertEquals(1, exe.exitCode());
        Assert.assertEquals(0, exe.stdoutLines().size());
        Assert.assertEquals(1, exe.stderrLines().size());
        Assert.assertTrue(exe.stderrLines().get(0), exe.stderrLines().get(0).contains("failed to exchange token: invalid_client Audience not found"));

        exe = KcinitExec.execute("logout");
        Assert.assertEquals(0, exe.exitCode());

        introspect = oauth.introspectAccessTokenWithClientCredential("kcinit", "password", token);
        json = JsonSerialization.readValue(introspect, Map.class);
        Assert.assertTrue(json.containsKey("active"));
        Assert.assertFalse((Boolean)json.get("active"));



    }

    @Test
    public void testTerms() throws Exception {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("wburke", realm);
            user.addRequiredAction(TermsAndConditions.PROVIDER_ID);
        });

        testInstall();

        KcinitExec exe = KcinitExec.newBuilder()
                .argsLine("login")
                .executeAsync();
        exe.waitForStderr("Username:");
        exe.sendLine("wburke");
        exe.waitForStderr("Password:");
        exe.sendLine("password");
        exe.waitForStderr("Accept Terms? [y/n]:");
        exe.sendLine("y");
        exe.waitForStderr("Login successful");
        exe.waitCompletion();
        Assert.assertEquals(0, exe.exitCode());
        Assert.assertEquals(0, exe.stdoutLines().size());
    }


    @Test
    public void testUpdateProfile() throws Exception {
        // expects that updateProfile is a passthrough
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("wburke", realm);
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
        });

        try {
            testInstall();

            //Thread.sleep(100000000);

            KcinitExec exe = KcinitExec.newBuilder()
                    .argsLine("login")
                    .executeAsync();
            try {
                exe.waitForStderr("Username:");
                exe.sendLine("wburke");
                exe.waitForStderr("Password:");
                exe.sendLine("password");

                exe.waitForStderr("Login successful");
                exe.waitCompletion();
                Assert.assertEquals(0, exe.exitCode());
                Assert.assertEquals(0, exe.stdoutLines().size());
            } catch (Exception ex) {
                System.out.println(exe.stderrString());
                throw ex;
            }
        } finally {

            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername("wburke", realm);
                user.removeRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
            });
        }
    }


    @Test
    public void testUpdatePassword() throws Exception {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("wburke", realm);
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        });

        try {
            testInstall();

            KcinitExec exe = KcinitExec.newBuilder()
                    .argsLine("login")
                    .executeAsync();
            exe.waitForStderr("Username:");
            exe.sendLine("wburke");
            exe.waitForStderr("Password:");
            exe.sendLine("password");
            exe.waitForStderr("New Password:");
            exe.sendLine("pw");
            exe.waitForStderr("Confirm Password:");
            exe.sendLine("pw");
            exe.waitForStderr("Login successful");
            exe.waitCompletion();
            Assert.assertEquals(0, exe.exitCode());
            Assert.assertEquals(0, exe.stdoutLines().size());

            exe = KcinitExec.newBuilder()
                    .argsLine("login -f")
                    .executeAsync();
            exe.waitForStderr("Username:");
            exe.sendLine("wburke");
            exe.waitForStderr("Password:");
            exe.sendLine("pw");
            exe.waitForStderr("Login successful");
            exe.waitCompletion();
            Assert.assertEquals(0, exe.exitCode());
            Assert.assertEquals(0, exe.stdoutLines().size());

            exe = KcinitExec.execute("logout");
            Assert.assertEquals(0, exe.exitCode());
        } finally {

            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername("wburke", realm);
                session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password("password"));
            });
        }

    }

    protected TimeBasedOTP totp = new TimeBasedOTP();


    @Test
    public void testConfigureTOTP() throws Exception {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("wburke", realm);
            user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        });

        try {

            testInstall();

            KcinitExec exe = KcinitExec.newBuilder()
                    .argsLine("login")
                    .executeAsync();
            exe.waitForStderr("Username:");
            exe.sendLine("wburke");
            exe.waitForStderr("Password:");
            exe.sendLine("password");
            exe.waitForStderr("One Time Password:");

            Pattern p = Pattern.compile("Open the application and enter the key\\s+(.+)\\s+Use the following configuration values");
            //Pattern p = Pattern.compile("Open the application and enter the key");

            String stderr = exe.stderrString();
            //System.out.println("***************");
            //System.out.println(stderr);
            //System.out.println("***************");
            Matcher m = p.matcher(stderr);
            Assert.assertTrue(m.find());
            String otpSecret = m.group(1).trim();

            //System.out.println("***************");
            //System.out.println(otpSecret);
            //System.out.println("***************");

            otpSecret = TotpUtils.decode(otpSecret);
            String code = totp.generateTOTP(otpSecret);
            //System.out.println("***************");
            //System.out.println("code: " + code);
            //System.out.println("***************");
            exe.sendLine(code);
            Thread.sleep(100);
            //stderr = exe.stderrString();
            //System.out.println("***************");
            //System.out.println(stderr);
            //System.out.println("***************");
            exe.waitForStderr("Login successful");
            exe.waitCompletion();
            Assert.assertEquals(0, exe.exitCode());
            Assert.assertEquals(0, exe.stdoutLines().size());


            exe = KcinitExec.execute("logout");
            Assert.assertEquals(0, exe.exitCode());

            exe = KcinitExec.newBuilder()
                    .argsLine("login")
                    .executeAsync();
            exe.waitForStderr("Username:");
            exe.sendLine("wburke");
            exe.waitForStderr("Password:");
            exe.sendLine("password");
            exe.waitForStderr("One Time Password:");
            exe.sendLine(totp.generateTOTP(otpSecret));
            exe.waitForStderr("Login successful");
            exe.waitCompletion();

            exe = KcinitExec.execute("logout");
            Assert.assertEquals(0, exe.exitCode());
        } finally {
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername("wburke", realm);
                session.userCredentialManager().disableCredentialType(realm, user, CredentialModel.OTP);
            });
        }


    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Test
    public void testVerifyEmail() throws Exception {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("test-user@localhost", realm);
            user.addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
        });

        testInstall();

        KcinitExec exe = KcinitExec.newBuilder()
                .argsLine("login")
                .executeAsync();
        exe.waitForStderr("Username:");
        exe.sendLine("test-user@localhost");
        exe.waitForStderr("Password:");
        exe.sendLine("password");
        exe.waitForStderr("Email Code:");

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String text = MailUtils.getBody(message).getText();
        Assert.assertTrue(text.contains("Please verify your email address by entering in the following code."));
        String code = text.substring("Please verify your email address by entering in the following code.".length()).trim();

        exe.sendLine(code);

        exe.waitForStderr("Login successful");
        exe.waitCompletion();
        Assert.assertEquals(0, exe.exitCode());
        Assert.assertEquals(0, exe.stdoutLines().size());


        exe = KcinitExec.execute("logout");
        Assert.assertEquals(0, exe.exitCode());
    }




}
