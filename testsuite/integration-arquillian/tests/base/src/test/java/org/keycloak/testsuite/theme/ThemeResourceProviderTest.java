package org.keycloak.testsuite.theme;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;

import java.io.IOException;
import java.util.Locale;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class ThemeResourceProviderTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void getTheme() {
        testingClient.server().run(session -> {
            try {
                ThemeProvider extending = session.getProvider(ThemeProvider.class, "extending");
                Theme theme = extending.getTheme("base", Theme.Type.LOGIN);
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
                ThemeProvider extending = session.getProvider(ThemeProvider.class, "extending");
                Theme theme = extending.getTheme("base", Theme.Type.LOGIN);
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
                ThemeProvider extending = session.getProvider(ThemeProvider.class, "extending");
                Theme theme = extending.getTheme("base", Theme.Type.LOGIN);
                Assert.assertNotNull(theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-8818"));
                Assert.assertNotEquals("Full name (Theme-resources)", theme.getMessages("messages", Locale.ENGLISH).get("fullName"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }
}
