/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.adapter;

import org.apache.commons.io.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
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
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);

        URL keystore = AbstractServletsAdapterTest.class.getResource(webInfPath + "keystore.jks");
        if (keystore != null) {
            deployment.addAsWebInfResource(keystore, "classes/keystore.jks");
        }

        if (keycloakJSON != null) {
            deployment.addAsWebInfResource(keycloakJSON, "keycloak.json");
        }

        addContextXml(deployment, name);

        return deployment;
    }

    public static WebArchive samlServletDeployment(String name, Class... servletClasses) {
        return samlServletDeployment(name, "web.xml", servletClasses);
    }

    public static WebArchive samlServletDeployment(String name, String webXMLPath, Class... servletClasses) {
        String baseSAMLPath = "/adapter-test/keycloak-saml/";
        String webInfPath = baseSAMLPath + name + "/WEB-INF/";

        URL keycloakSAMLConfig = AbstractServletsAdapterTest.class.getResource(webInfPath + "keycloak-saml.xml");
        Assert.assertNotNull("keycloak-saml.xml should be in " + webInfPath, keycloakSAMLConfig);

        URL webXML = AbstractServletsAdapterTest.class.getResource(baseSAMLPath + webXMLPath);
        Assert.assertNotNull("web.xml should be in " + baseSAMLPath + webXMLPath, keycloakSAMLConfig);

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

    protected void setAdapterAndServerTimeOffset(int timeOffset, String... servletUris) {
        setTimeOffset(timeOffset);

        for (String servletUri : servletUris) {
            String timeOffsetUri = UriBuilder.fromUri(servletUri)
                    .queryParam(AdapterActionsFilter.TIME_OFFSET_PARAM, timeOffset)
                    .build().toString();

            driver.navigate().to(timeOffsetUri);
            WaitUtils.waitUntilElement(By.tagName("body")).is().visible();
        }
    }

}
