package org.keycloak.testsuite.theme;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.theme.Theme;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:vincent.letarouilly@gmail.com">Vincent Letarouilly</a>
 */
public class DefaultThemeManagerTest extends AbstractKeycloakTest {

    private static final String THEME_NAME = "environment-agnostic";

    @Before
    public void setUp() {
        testingClient.server().run(session -> {
            System.setProperty("existing_system_property", "Keycloak is awesome");
            session.theme().clearCache();
        });
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    // KEYCLOAK-6698
    @Test
    public void systemPropertiesSubstitutionInThemeProperties() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme(THEME_NAME, Theme.Type.LOGIN);
                Assert.assertEquals("getTheme(...) returns default theme when no matching theme found, but we need " + THEME_NAME + " theme deployed.",THEME_NAME, theme.getName());
                Assert.assertEquals("Keycloak is awesome", theme.getProperties().getProperty("system.property.found"));
                Assert.assertEquals("${missing_system_property}", theme.getProperties().getProperty("system.property.missing"));
                Assert.assertEquals("defaultValue", theme.getProperties().getProperty("system.property.missing.with.default"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void verifyLocaleNames() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Properties messages = theme.getMessages(Locale.ENGLISH);
                assertThat(messages.getProperty("locale_de"), Matchers.containsString("German"));
                assertThat(messages.getProperty("locale_zh-TW"), Matchers.containsString("Chinese (Traditional)"));
                assertThat(messages.getProperty("locale_zh-CN"), Matchers.containsString("Chinese (Simplified)"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    // KEYCLOAK-6698
    @Test
    public void environmentVariablesSubstitutionInThemeProperties() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme(THEME_NAME, Theme.Type.LOGIN);
                Assert.assertEquals("getTheme(...) returns default theme when no matching theme found, but we need " + THEME_NAME + " theme deployed.",THEME_NAME, theme.getName());
                Assert.assertEquals("${env.MISSING_ENVIRONMENT_VARIABLE}", theme.getProperties().getProperty("env.missing"));
                Assert.assertEquals("defaultValue", theme.getProperties().getProperty("env.missingWithDefault"));
                if (System.getenv().containsKey("HOMEPATH")) {
                    // Windows
                    Assert.assertEquals(System.getenv().get("HOMEPATH"), theme.getProperties().getProperty("env.windowsHome"));
                } else if (System.getenv().containsKey("HOME")) {
                    // Unix
                    Assert.assertEquals(System.getenv().get("HOME"), theme.getProperties().getProperty("env.unixHome"));
                } else {
                    Assert.fail("No default env variable found, can't verify");
                }
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }
}
