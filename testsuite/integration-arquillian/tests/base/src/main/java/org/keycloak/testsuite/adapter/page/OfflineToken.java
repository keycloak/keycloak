package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;

import java.net.URL;

import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class OfflineToken extends AbstractShowTokensPage {

    public static final String DEPLOYMENT_NAME = "offline-client";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    public void logout() {
        log.info("Logging out, navigating to: " + getUriBuilder().path("/logout").build().toASCIIString());
        driver.navigate().to(getUriBuilder().path("/logout").build().toASCIIString());
        pause(300); // this is needed for FF for some reason
        waitUntilElement(By.tagName("body")).is().visible();
    }
}