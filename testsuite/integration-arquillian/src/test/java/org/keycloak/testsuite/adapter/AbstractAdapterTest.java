package org.keycloak.testsuite.adapter;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.page.adapter.AppServerContextRoot;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAdapterTest extends AbstractKeycloakTest {

    @Page
    protected AppServerContextRoot appServerContextRoot;

    protected String LOGIN_URL;

//    private boolean testRealmImported = false;
    @Before
    public void beforeAdapterTest() {
        LOGIN_URL = OIDCLoginProtocolService.authUrl(authServer.createUriBuilder())
                .build("demo").toString();
    }

    @Override
    public void importTestRealm() {
//        if (!testRealmImported) {

        RealmRepresentation realm = loadTestRealm();
        System.out.println("Setting redirect-uris in test realm '" + realm.getId() + "' as " + (isRelative() ? "" : "non-") + "relative");
        if (isRelative()) {
            modifyClientRedirectUris(realm, appServerContextRoot.getUrlString(), "");
        } else {
            modifyClientRedirectUris(realm, "^(/.*/\\*)", appServerContextRoot.getUrlString() + "$1");
        }
        importRealm(realm);
//            testRealmImported = true;
//        }
    }

    public abstract RealmRepresentation loadTestRealm();

    protected void modifyClientRedirectUris(RealmRepresentation realm, String regex, String replacement) {
        for (ClientRepresentation client : realm.getClients()) {
            List<String> redirectUris = client.getRedirectUris();
            if (redirectUris != null) {
                List<String> newRedirectUris = new ArrayList<>();
                for (String uri : redirectUris) {
                    newRedirectUris.add(uri.replaceAll(regex, replacement));
                }
                client.setRedirectUris(newRedirectUris);
            }
        }
    }

    @Override
    public void removeTestRealm() {
        System.out.println("Removing demo realm.");
        keycloak.realm("demo").remove();
    }

}
