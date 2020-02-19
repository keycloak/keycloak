package org.keycloak.testsuite.theme;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.util.Locale;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class ThemeResourceProviderTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void getTheme() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Assert.assertNotNull(theme.getTemplate("test.ftl"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void getResourceAsStream() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Assert.assertNotNull(theme.getResourceAsStream("test.js"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void getMessages() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Assert.assertNotNull(theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-8818"));
                Assert.assertNotEquals("Full name (Theme-resources)", theme.getMessages("messages", Locale.ENGLISH).get("fullName"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    /**
     * See KEYCLOAK-12926
     */
    @Test
    public void getMessagesLocaleResolving() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Assert.assertEquals("Test en_US_variant", theme.getMessages("messages", new Locale("en", "US", "variant")).get("test.keycloak-12926"));
                Assert.assertEquals("Test en_US", theme.getMessages("messages", new Locale("en", "US")).get("test.keycloak-12926"));
                Assert.assertEquals("Test en", theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-12926"));
                Assert.assertEquals("Test en_US", theme.getMessages("messages", new Locale("en", "US")).get("test.keycloak-12926"));
                Assert.assertEquals("Test en", theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-12926"));

                Assert.assertEquals("only de_AT_variant", theme.getMessages("messages", new Locale("de", "AT", "variant")).get("test.keycloak-12926-resolving1"));
                Assert.assertNull(theme.getMessages("messages", new Locale("de", "AT")).get("test.keycloak-12926-resolving1"));

                Assert.assertEquals("only de_AT", theme.getMessages("messages", new Locale("de", "AT", "variant")).get("test.keycloak-12926-resolving2"));
                Assert.assertNull(theme.getMessages("messages", new Locale("de")).get("test.keycloak-12926-resolving2"));

                Assert.assertEquals("only de", theme.getMessages("messages", new Locale("de", "AT", "variant")).get("test.keycloak-12926-only_de"));
                Assert.assertNull(theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-12926-only_de"));

                Assert.assertEquals("fallback en", theme.getMessages("messages", new Locale("de", "AT", "variant")).get("test.keycloak-12926-resolving3"));
                Assert.assertEquals("fallback en", theme.getMessages("messages", new Locale("de", "AT")).get("test.keycloak-12926-resolving3"));
                Assert.assertEquals("fallback en", theme.getMessages("messages", new Locale("de")).get("test.keycloak-12926-resolving3"));
                Assert.assertNull(theme.getMessages("messages", Locale.ENGLISH).get("fallback en"));

            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }
}
