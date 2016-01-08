package org.keycloak.testsuite.adapter.page;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author mhajas
 */
public abstract class SAMLServletWithLogout extends AbstractPageWithInjectedUrl {

    public void logout() {
        driver.navigate().to(getUriBuilder().queryParam("GLO", "true").build().toASCIIString());
        getUriBuilder().replaceQueryParam("GLO", new Object());
        pause(300);
    }
}
