package org.keycloak.testsuite.adapter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.TestRealms;
import org.keycloak.testsuite.arquillian.ContainersTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.page.adapter.AppServerContextRoot;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer
public abstract class AbstractAdapterTest extends AbstractKeycloakTest {

    @Page
    protected AppServerContextRoot appServerContextRoot;

    protected String LOGIN_URL;

    public static final String JBOSS_DEPLOYMENT_STRUCTURE_XML = "jboss-deployment-structure.xml";
    public static final URL jbossDeploymentStructure = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + JBOSS_DEPLOYMENT_STRUCTURE_XML);

    public AbstractAdapterTest() {
        System.out.println("AbstractAdapterTest - App server: " + appServerContextRoot.getUrlString());
    }

    @Before
    public void beforeAdapterTest() {
        LOGIN_URL = OIDCLoginProtocolService.authUrl(authServer.createUriBuilder())
                .build("demo").toString();
    }

    @Override
    public TestRealms loadTestRealms() {
        TestRealms adapterTestRealms = loadAdapterTestRealms();
        for (RealmRepresentation realm : adapterTestRealms.values()) {
            System.out.println("Setting redirect-uris in test realm '" + realm.getRealm() + "' as " + (isRelative() ? "" : "non-") + "relative");
            if (isRelative()) {
                modifyClientRedirectUris(realm, appServerContextRoot.getUrlString(), "");
            } else {
                modifyClientRedirectUris(realm, "^(/.*/\\*)", appServerContextRoot.getUrlString() + "$1");
            }
        }
        return adapterTestRealms;
    }

    public abstract TestRealms loadAdapterTestRealms();

    public boolean isRelative() {
        return ContainersTestEnricher.isRelative(this.getClass());
    }

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

}
