package org.keycloak.testsuite.adapter;

import org.apache.commons.io.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;

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

    protected static WebArchive samlServletDeployment(String name, Class... servletClasses) {
        return samlServletDeployment(name, "keycloak-saml.xml", servletClasses);
    }

    protected static WebArchive samlServletDeployment(String name, String adapterConfig ,Class... servletClasses) {
        String baseSAMLPath = "/adapter-test/keycloak-saml/";
        String webInfPath = baseSAMLPath + name + "/WEB-INF/";

        URL keycloakSAMLConfig = AbstractServletsAdapterTest.class.getResource(webInfPath + adapterConfig);
        URL webXML = AbstractServletsAdapterTest.class.getResource(baseSAMLPath + "web.xml");

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(keycloakSAMLConfig, "keycloak-saml.xml")
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);

        String webXMLContent;
        try {
            webXMLContent = IOUtils.toString(webXML.openStream())
                    .replace("%CONTEXT_PATH%", name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        deployment.add(new StringAsset(webXMLContent), "/WEB-INF/web.xml");

        URL keystore = AbstractServletsAdapterTest.class.getResource(webInfPath + "keystore.jks");
        if (keystore != null) {
            deployment.addAsWebInfResource(keystore, "keystore.jks");
        }

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
