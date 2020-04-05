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
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.openqa.selenium.By;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import org.jboss.shrinkwrap.api.asset.UrlAsset;

import org.junit.Assert;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

public abstract class AbstractServletsAdapterTest extends AbstractAdapterTest {

    protected static WebArchive servletDeploymentMultiTenant(String name, Class... servletClasses) {
        WebArchive servletDeployment = servletDeployment(name, null, servletClasses);

        String webInfPath = "/adapter-test/" + name + "/WEB-INF/";
        String config1 = "tenant1-keycloak.json";
        String config2 = "tenant2-keycloak.json";

        URL config1Url = AbstractServletsAdapterTest.class.getResource(webInfPath + config1);
        Assert.assertNotNull("config1Url should be in " + webInfPath + config1, config1Url);
        URL config2Url = AbstractServletsAdapterTest.class.getResource(webInfPath + config2);
        Assert.assertNotNull("config2Url should be in " + webInfPath + config2, config2Url);

        servletDeployment
                .add(new UrlAsset(config1Url), "/WEB-INF/classes/" + config1)
                .add(new UrlAsset(config2Url), "/WEB-INF/classes/" + config2);

        // In this scenario DeploymentArchiveProcessorUtils can not act automatically since the adapter configurations
        // are not stored in typical places. We need to modify them manually.
        DeploymentArchiveProcessorUtils.modifyOIDCAdapterConfig(servletDeployment, "/WEB-INF/classes/" + config1);
        DeploymentArchiveProcessorUtils.modifyOIDCAdapterConfig(servletDeployment, "/WEB-INF/classes/" + config2);

        return servletDeployment;
    }

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
        return samlServletDeployment(name, webXMLPath, null, servletClasses);
    }

    public static WebArchive samlServletDeployment(String name, String webXMLPath, Integer clockSkewSec, Class... servletClasses) {
        return samlServletDeployment(name, name, webXMLPath, clockSkewSec, servletClasses);
    }

    public static WebArchive samlServletDeployment(String name, String customArchiveName, String webXMLPath, Integer clockSkewSec, Class... servletClasses) {
        String baseSAMLPath = "/adapter-test/keycloak-saml/";
        String webInfPath = baseSAMLPath + name + "/WEB-INF/";

        URL keycloakSAMLConfig = AbstractServletsAdapterTest.class.getResource(webInfPath + "keycloak-saml.xml");
        Assert.assertNotNull("keycloak-saml.xml should be in " + webInfPath, keycloakSAMLConfig);

        URL webXML = AbstractServletsAdapterTest.class.getResource(baseSAMLPath + webXMLPath);
        Assert.assertNotNull("web.xml should be in " + baseSAMLPath + webXMLPath, keycloakSAMLConfig);

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, customArchiveName + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);

        // if a role-mappings.properties file exist in WEB-INF, include it in the deployment.
        URL roleMappingsConfig = AbstractServletsAdapterTest.class.getResource(webInfPath + "role-mappings.properties");
        if(roleMappingsConfig != null) {
            deployment.addAsWebInfResource(roleMappingsConfig, "role-mappings.properties");
        }

        String webXMLContent;
        try {
            webXMLContent = IOUtils.toString(webXML.openStream(), Charset.forName("UTF-8"))
                    .replace("%CONTEXT_PATH%", name);

            if (clockSkewSec != null) {
                String keycloakSamlXMLContent = IOUtils.toString(keycloakSAMLConfig.openStream(), Charset.forName("UTF-8"))
                    .replace("%CLOCK_SKEW%", "${allowed.clock.skew:" + String.valueOf(clockSkewSec) + "}");
                deployment.addAsWebInfResource(new StringAsset(keycloakSamlXMLContent), "keycloak-saml.xml");
            } else {
                deployment.addAsWebInfResource(keycloakSAMLConfig, "keycloak-saml.xml");
            }

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

    public static WebArchive samlServletDeploymentMultiTenant(String name, String webXMLPath, 
            String config1, String config2,
            String keystore1, String keystore2, Class... servletClasses) {
        String baseSAMLPath = "/adapter-test/keycloak-saml/";
        String webInfPath = baseSAMLPath + name + "/WEB-INF/";

        URL webXML = AbstractServletsAdapterTest.class.getResource(baseSAMLPath + webXMLPath);
        Assert.assertNotNull("web.xml should be in " + baseSAMLPath + webXMLPath, webXML);

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);

        String webXMLContent;
        try {
            webXMLContent = IOUtils.toString(webXML.openStream(), Charset.forName("UTF-8"))
                    .replace("%CONTEXT_PATH%", name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        deployment.add(new StringAsset(webXMLContent), "/WEB-INF/web.xml");

        // add the xml for each tenant in classes
        URL config1Url = AbstractServletsAdapterTest.class.getResource(webInfPath + config1);
        Assert.assertNotNull("config1Url should be in " + webInfPath + config1, config1Url);
        deployment.add(new UrlAsset(config1Url), "/WEB-INF/classes/" + config1);
        URL config2Url = AbstractServletsAdapterTest.class.getResource(webInfPath + config2);
        Assert.assertNotNull("config2Url should be in " + webInfPath + config2, config2Url);
        deployment.add(new UrlAsset(config2Url), "/WEB-INF/classes/" + config2);
        
        // add the keystores for each tenant in classes
        URL keystore1Url = AbstractServletsAdapterTest.class.getResource(webInfPath + keystore1);
        Assert.assertNotNull("keystore1Url should be in " + webInfPath + keystore1, keystore1Url);
        deployment.add(new UrlAsset(keystore1Url), "/WEB-INF/classes/" + keystore1);
        URL keystore2Url = AbstractServletsAdapterTest.class.getResource(webInfPath + keystore2);
        Assert.assertNotNull("keystore2Url should be in " + webInfPath + keystore2, keystore2Url);
        deployment.add(new UrlAsset(keystore2Url), "/WEB-INF/classes/" + keystore2);

        addContextXml(deployment, name);

        return deployment;
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/demorealm.json"));
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

            DroneUtils.getCurrentDriver().navigate().to(timeOffsetUri);
            waitForPageToLoad();
            String pageSource = DroneUtils.getCurrentDriver().getPageSource();
            System.out.println(pageSource);
        }
    }

}
