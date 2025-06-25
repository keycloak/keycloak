package org.keycloak.services.resources;

import org.junit.Assert;
import org.junit.Test;

public class WelcomeResourceTest {

    @Test
    public void testIfCreatesTemporaryAdminOnNoConfig() {
        //given
        class WelcomeResourceForTest extends WelcomeResource {

            @Override
            protected String getThemeProperty(String propertyName, String defaultValue) {
                return defaultValue;
            }
        }

        //when
        boolean isTemporaryAdmin = (new WelcomeResourceForTest()).isAdminUserTemporary();

        //then
        Assert.assertTrue(isTemporaryAdmin);
    }

    @Test
    public void testIfCreatesTemporaryAdminOnThemeProperty() {
        //given
        class WelcomeResourceForTest extends WelcomeResource {

            @Override
            protected String getThemeProperty(String propertyName, String defaultValue) {
                return "false";
            }
        }

        //when
        boolean isTemporaryAdmin = (new WelcomeResourceForTest()).isAdminUserTemporary();

        //then
        Assert.assertFalse(isTemporaryAdmin);
    }

    @Test
    public void testIfCreatesTemporaryAdminOnEnvironmentVariable() {
        //given
        class WelcomeResourceForTest extends WelcomeResource {

            @Override
            protected String getThemeProperty(String propertyName, String defaultValue) {
                return "true";
            }
        }

        //when
        System.setProperty("KC_BOOTSTRAP_ADMIN_IS_TEMPORARY", "false");
        boolean isTemporaryAdmin = (new WelcomeResourceForTest()).isAdminUserTemporary();
        System.clearProperty("KC_BOOTSTRAP_ADMIN_IS_TEMPORARY");

        //then
        Assert.assertTrue("Theme override takes priority", isTemporaryAdmin);
    }

    @Test
    public void testIfCreatesTemporaryAdminOnEnvironmentVariable2() {
        //given
        class WelcomeResourceForTest extends WelcomeResource {

            @Override
            protected String getThemeProperty(String propertyName, String defaultValue) {
                return "false";
            }
        }

        //when
        System.setProperty("KC_BOOTSTRAP_ADMIN_IS_TEMPORARY", "true");
        boolean isTemporaryAdmin = (new WelcomeResourceForTest()).isAdminUserTemporary();
        System.clearProperty("KC_BOOTSTRAP_ADMIN_IS_TEMPORARY");

        //then
        Assert.assertFalse("Theme override takes priority", isTemporaryAdmin);
    }

}
