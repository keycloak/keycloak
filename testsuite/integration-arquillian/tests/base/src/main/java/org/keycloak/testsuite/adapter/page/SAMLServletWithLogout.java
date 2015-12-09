package org.keycloak.testsuite.adapter.page;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

/**
 * @author mhajas
 */
public abstract class SAMLServletWithLogout extends AbstractPageWithInjectedUrl {

    public void logout() {
        driver.navigate().to(getUriBuilder().queryParam("GLO", "true").build().toASCIIString());
        getUriBuilder().replaceQueryParam("GLO",null);
    }
}
