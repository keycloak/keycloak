package org.keycloak.testsuite.adapter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.Before;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
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
    public static final String TOMCAT_CONTEXT_XML = "context.xml";
    public static final URL tomcatContext = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + TOMCAT_CONTEXT_XML);

    @Before
    public void beforeAdapterTest() {
        LOGIN_URL = OIDCLoginProtocolService.authUrl(authServer.createUriBuilder())
                .build("demo").toString();
    }

    @Override
    public void loadTestRealmsInto(List<RealmRepresentation> testRealms) {
        loadAdapterTestRealmsInto(testRealms);
        for (RealmRepresentation realm : testRealms) {
            System.out.println("Setting redirect-uris in test realm '" + realm.getRealm() + "' as " + (isRelative() ? "" : "non-") + "relative");
            if (isRelative()) {
                modifyClientRedirectUris(realm, appServerContextRoot.getUrlString(), "");
                modifyClientUrls(realm, appServerContextRoot.getUrlString(), "");
            } else {
                modifyClientRedirectUris(realm, "^(/.*/\\*)", appServerContextRoot.getUrlString() + "$1");
                modifyClientUrls(realm, "^(/.*)", appServerContextRoot.getUrlString() + "$1");
            }
        }
    }

    public abstract void loadAdapterTestRealmsInto(List<RealmRepresentation> testRealms);

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

    protected void modifyClientUrls(RealmRepresentation realm, String regex, String replacement) {
        for (ClientRepresentation client : realm.getClients()) {
            String baseUrl = client.getBaseUrl();
            if (baseUrl != null) {
                client.setBaseUrl(baseUrl.replaceAll(regex, replacement));
            }
            String adminUrl = client.getAdminUrl();
            if (adminUrl != null) {
                client.setAdminUrl(adminUrl.replaceAll(regex, replacement));
            }
        }
    }

    public static void addContextXml(Archive archive, String contextPath) {
        try {
            String contextXmlContent = IOUtils.toString(tomcatContext.openStream())
                    .replace("%CONTEXT_PATH%", contextPath);
            archive.add(new StringAsset(contextXmlContent), "/META-INF/context.xml");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
