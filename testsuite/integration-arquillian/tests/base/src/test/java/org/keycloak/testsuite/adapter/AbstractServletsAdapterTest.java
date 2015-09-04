package org.keycloak.testsuite.adapter;

import java.net.URL;
import java.util.List;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.util.IOUtil.*;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;

public abstract class AbstractServletsAdapterTest extends AbstractAdapterTest {

    protected static WebArchive servletDeployment(String name, Class... servletClasses) {
        return servletDeployment(name, "keycloak.json", servletClasses);
    }

    protected static WebArchive servletDeployment(String name, String adapterConfig, Class... servletClasses) {
        String webInfPath = "/adapter-test/" + name + "/WEB-INF/";

        URL keycloakJSON = AbstractServletsAdapterTest.class.getResource(webInfPath + adapterConfig);
        URL webXML = AbstractServletsAdapterTest.class.getResource(webInfPath + "web.xml");

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(webXML, "web.xml")
                .addAsWebInfResource(keycloakJSON, "keycloak.json")
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);

        addContextXml(deployment, name);

        return deployment;
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/demorealm.json"));
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(DEMO);
    }

}
