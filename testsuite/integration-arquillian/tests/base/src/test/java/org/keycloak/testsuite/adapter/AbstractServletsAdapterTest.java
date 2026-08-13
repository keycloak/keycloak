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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils;
import org.keycloak.testsuite.utils.io.IOUtil;

import org.apache.commons.io.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractServletsAdapterTest extends AbstractAdapterTest {

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
        Assertions.assertNotNull(keycloakSAMLConfig, "keycloak-saml.xml should be in " + webInfPath);

        URL webXML = AbstractServletsAdapterTest.class.getResource(baseSAMLPath + webXMLPath);
        Assertions.assertNotNull(keycloakSAMLConfig, "web.xml should be in " + baseSAMLPath + webXMLPath);

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, customArchiveName + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);
        addSameSiteUndertowHandlers(deployment);

        // if a role-mappings.properties file exist in WEB-INF, include it in the deployment.
        URL roleMappingsConfig = AbstractServletsAdapterTest.class.getResource(webInfPath + "role-mappings.properties");
        if(roleMappingsConfig != null) {
            deployment.addAsWebInfResource(roleMappingsConfig, "role-mappings.properties");
        }

        String webXMLContent;
        try {
            webXMLContent = IOUtils.toString(webXML.openStream(), StandardCharsets.UTF_8)
                    .replace("%CONTEXT_PATH%", name);

            if (clockSkewSec != null) {
                String keycloakSamlXMLContent = IOUtils.toString(keycloakSAMLConfig.openStream(), StandardCharsets.UTF_8)
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

        if (AppServerTestEnricher.isJBossJakartaAppServer()) {
            DeploymentArchiveProcessorUtils.useJakartaEEServletClass(deployment, "/WEB-INF/web.xml");
        }

        return deployment;
    }

    public static WebArchive samlServletDeploymentMultiTenant(String name, String webXMLPath,
            String config1, String config2,
            String keystore1, String keystore2, Class... servletClasses) {
        String baseSAMLPath = "/adapter-test/keycloak-saml/";
        String webInfPath = baseSAMLPath + name + "/WEB-INF/";

        URL webXML = AbstractServletsAdapterTest.class.getResource(baseSAMLPath + webXMLPath);
        Assertions.assertNotNull(webXML, "web.xml should be in " + baseSAMLPath + webXMLPath);

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);
        addSameSiteUndertowHandlers(deployment);

        String webXMLContent;
        try {
            webXMLContent = IOUtils.toString(webXML.openStream(), StandardCharsets.UTF_8)
                    .replace("%CONTEXT_PATH%", name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        deployment.add(new StringAsset(webXMLContent), "/WEB-INF/web.xml");

        // add the xml for each tenant in classes
        URL config1Url = AbstractServletsAdapterTest.class.getResource(webInfPath + config1);
        Assertions.assertNotNull(config1Url, "config1Url should be in " + webInfPath + config1);
        deployment.add(new UrlAsset(config1Url), "/WEB-INF/classes/" + config1);
        URL config2Url = AbstractServletsAdapterTest.class.getResource(webInfPath + config2);
        Assertions.assertNotNull(config2Url, "config2Url should be in " + webInfPath + config2);
        deployment.add(new UrlAsset(config2Url), "/WEB-INF/classes/" + config2);

        // add the keystores for each tenant in classes
        URL keystore1Url = AbstractServletsAdapterTest.class.getResource(webInfPath + keystore1);
        Assertions.assertNotNull(keystore1Url, "keystore1Url should be in " + webInfPath + keystore1);
        deployment.add(new UrlAsset(keystore1Url), "/WEB-INF/classes/" + keystore1);
        URL keystore2Url = AbstractServletsAdapterTest.class.getResource(webInfPath + keystore2);
        Assertions.assertNotNull(keystore2Url, "keystore2Url should be in " + webInfPath + keystore2);
        deployment.add(new UrlAsset(keystore2Url), "/WEB-INF/classes/" + keystore2);

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
        timeOffSet.set(timeOffset);

        for (String servletUri : servletUris) {
            setAdapterServletTimeOffset(timeOffset, servletUri);
        }
    }

    protected void setAdapterServletTimeOffset(int timeOffset, String servletUri) {
        String timeOffsetUri = UriBuilder.fromUri(servletUri)
                .queryParam(AdapterActionsFilter.TIME_OFFSET_PARAM, timeOffset)
                .build().toString();

        DroneUtils.getCurrentDriver().navigate().to(timeOffsetUri);
        waitForPageToLoad();
        String pageSource = DroneUtils.getCurrentDriver().getPageSource();
        assertThat(pageSource, containsString("Offset set successfully"));
    }

}
