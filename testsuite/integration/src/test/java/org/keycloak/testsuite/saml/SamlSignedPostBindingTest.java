package org.keycloak.testsuite.saml;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlSignedPostBindingTest {

    @ClassRule
    public static SamlKeycloakRule keycloakRule = new SamlKeycloakRule() {
        @Override
        public void initWars() {
             ClassLoader classLoader = SamlSignedPostBindingTest.class.getClassLoader();

            initializeSamlSecuredWar("/saml/signed-post", "/sales-post-sig",  "post-sig.war", classLoader);
            initializeSamlSecuredWar("/saml/bad-client-signed-post", "/bad-client-sales-post-sig",  "bad-client-post-sig.war", classLoader);
            initializeSamlSecuredWar("/saml/bad-realm-signed-post", "/bad-realm-sales-post-sig",  "bad-realm-post-sig.war", classLoader);

        }

        @Override
        public String getRealmJson() {
            return "/saml/testsaml.json";
        }
    };

    @Rule
    public WebRule webRule = new WebRule(this);
    @WebResource
    protected WebDriver driver;
    @WebResource
    protected LoginPage loginPage;

    @Test
    @Ignore
    public void runit() throws Exception {
        Thread.sleep(10000000);
    }


    @Test
    public void testSignedLoginLogout() {
        driver.navigate().to("http://localhost:8081/sales-post-sig/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-post-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to("http://localhost:8081/sales-post-sig?GLO=true");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");

    }
    @Test
    public void testBadClientSignature() {
        driver.navigate().to("http://localhost:8081/bad-client-sales-post-sig/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        Assert.assertEquals(driver.getTitle(), "We're sorry...");

    }

    @Test
    public void testBadRealmSignature() {
        driver.navigate().to("http://localhost:8081/bad-realm-sales-post-sig/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/bad-realm-sales-post-sig/");
        Assert.assertTrue(driver.getPageSource().contains("null"));
    }


}
