package org.keycloak.test.framework.injection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.openqa.selenium.WebDriver;

public class ValueTypeAliasTest {

    @Test
    public void withAlias() {
        Assertions.assertEquals("browser", ValueTypeAlias.getAlias(WebDriver.class));
    }

    @Test
    public void withoutAlias() {
        Assertions.assertEquals("Keycloak", ValueTypeAlias.getAlias(Keycloak.class));
    }

}
