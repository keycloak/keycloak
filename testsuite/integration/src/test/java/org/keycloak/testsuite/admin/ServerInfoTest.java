package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.Version;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @Test
    public void testServerInfo() {
        ServerInfoRepresentation info = keycloak.serverInfo().getInfo();

        Assert.assertNotNull(info);
        Assert.assertNotNull(info.getProviders());
        Assert.assertNotNull(info.getThemes());
        Assert.assertNotNull(info.getEnums());

        Assert.assertNotNull(info.getMemoryInfo());
        Assert.assertNotNull(info.getSystemInfo());

        Assert.assertEquals(Version.VERSION, info.getSystemInfo().getVersion());
        Assert.assertNotNull(info.getSystemInfo().getServerTime());
        Assert.assertNotNull(info.getSystemInfo().getUptime());
    }

}
