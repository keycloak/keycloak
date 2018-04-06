package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.LoginPage;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class ThemeSelectorTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void clientOverride() {
        loginPage.open();
        assertEquals("keycloak", detectTheme());

        ClientRepresentation rep = testRealm().clients().findByClientId("test-app").get(0);
        rep.getAttributes().put("login_theme", "base");
        testRealm().clients().get(rep.getId()).update(rep);

        loginPage.open();
        assertEquals("base", detectTheme());

        rep.getAttributes().put("login_theme", "");
        testRealm().clients().get(rep.getId()).update(rep);
    }

    @Test
    public void clientTemplateOverride() {
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("loginTheme");
        templateRep.setAttributes(new HashMap<>());
        templateRep.getAttributes().put("login_theme", "base");

        String templateId = ApiUtil.getCreatedId(testRealm().clientTemplates().create(templateRep));

        loginPage.open();
        assertEquals("keycloak", detectTheme());

        ClientRepresentation rep = testRealm().clients().findByClientId("test-app").get(0);
        rep.setClientTemplate("loginTheme");
        testRealm().clients().get(rep.getId()).update(rep);

        loginPage.open();
        assertEquals("base", detectTheme());

        rep.setClientTemplate("NONE");
        testRealm().clients().get(rep.getId()).update(rep);

        testRealm().clientTemplates().get(templateId).remove();
    }

    private String detectTheme() {
        if(driver.getPageSource().contains("/login/keycloak/css/login.css")) {
            return "keycloak";
        } else {
            return "base";
        }
    }

}
