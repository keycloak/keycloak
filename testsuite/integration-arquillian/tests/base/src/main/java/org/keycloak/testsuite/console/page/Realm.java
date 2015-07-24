package org.keycloak.testsuite.console.page;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.openqa.selenium.WebElement;

/**
 *
 * @author tkyjovsk
 */
public class Realm extends RealmsRoot {

    public static final String CONSOLE_REALM = "consoleRealm";
    
    public static final String MASTER = "master";
    public static final String DEMO = "demo";
    public static final String TEST = "test";

    public Realm() {
        setUriParameter(CONSOLE_REALM, MASTER);
    }

    public Realm setConsoleRealm(String realm) {
        setUriParameter(CONSOLE_REALM, realm);
        return this;
    }

    public String getConsoleRealm() {
        return getUriParameter(CONSOLE_REALM).toString();
    }

    @Override
    public String getFragment() {
        return super.getFragment() + "/{" + CONSOLE_REALM + "}";
    }

    @FindByJQuery("a:contains('Users')")
    private WebElement usersLink;

    public void clickUsers() {
        usersLink.click();
    }

    public String getAuthRoot() {
        URI uri = buildUri();
        return uri.getScheme() + "://" + uri.getAuthority() + "/auth";
    }

    public URI getOIDCLoginUrl() {
        return OIDCLoginProtocolService.authUrl(UriBuilder.fromPath(getAuthRoot()))
                .build(getConsoleRealm());
    }

}
