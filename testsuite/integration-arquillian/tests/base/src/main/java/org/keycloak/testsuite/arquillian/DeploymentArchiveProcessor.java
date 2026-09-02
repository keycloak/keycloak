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

package org.keycloak.testsuite.arquillian;

import java.io.File;
import java.io.IOException;

import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenFormatStage;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.w3c.dom.Document;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.isRelative;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.isWASAppServer;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.isWLSAppServer;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.utils.io.IOUtil.documentToString;
import static org.keycloak.testsuite.utils.io.IOUtil.loadJson;
import static org.keycloak.testsuite.utils.io.IOUtil.loadXML;
import static org.keycloak.testsuite.utils.io.IOUtil.modifyDocElementAttribute;


/**
 * @author tkyjovsk
 */
@Deprecated
public class DeploymentArchiveProcessor implements ApplicationArchiveProcessor {

    protected final Logger log = Logger.getLogger(DeploymentArchiveProcessor.class);

    private static final boolean AUTH_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));
    private static final boolean APP_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("app.server.ssl.required"));

    public static final String WEBXML_PATH = "/WEB-INF/web.xml";
    public static final String ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_TENANT1 = "/WEB-INF/classes/tenant1-keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_TENANT2 = "/WEB-INF/classes/tenant2-keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_JS = "/keycloak.json";
    public static final String SAML_ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak-saml.xml";
    public static final String JBOSS_DEPLOYMENT_XML_PATH = "/WEB-INF/jboss-deployment-structure.xml";
    public static final String SAML_ADAPTER_CONFIG_PATH_TENANT1 = "/WEB-INF/classes/tenant1-keycloak-saml.xml";
    public static final String SAML_ADAPTER_CONFIG_PATH_TENANT2 = "/WEB-INF/classes/tenant2-keycloak-saml.xml";

    @Inject
    @ClassScoped
    private InstanceProducer<TestContext> testContextProducer;

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        // Ignore run on server classes
        if (archive.getName().equals("run-on-server-classes.war")) {
            return;
        }

        // Ignore archives modifed in specific DeploymentArchiveProcessors, see e.g. 
        // org.keycloak.testsuite.arquillian.wildfly.container.WildflyDeploymentArchiveProcessor
        if (isWLSAppServer() || isWASAppServer()) {

            log.info("Processing archive " + archive.getName());
            modifyAdapterConfigs(archive, testClass);
            modifyWebXml(archive, testClass);

            MavenResolverSystem resolver = Maven.resolver();
            MavenFormatStage dependencies = resolver
                    .loadPomFromFile("pom.xml")
                    .importTestDependencies()
                    .resolve("org.apache.httpcomponents:httpclient")
                    .withTransitivity();

            ((WebArchive) archive)
                    .addAsLibraries(dependencies.asFile())
                    .addClass(org.keycloak.testsuite.arquillian.annotation.AppServerContainer.class);
        }
    }

    protected void modifyAdapterConfigs(Archive<?> archive, TestClass testClass) {
        boolean relative = isRelative();
        modifyAdapterConfig(archive, ADAPTER_CONFIG_PATH, relative);
        modifyAdapterConfig(archive, ADAPTER_CONFIG_PATH_TENANT1, relative);
        modifyAdapterConfig(archive, ADAPTER_CONFIG_PATH_TENANT2, relative);
        modifyAdapterConfig(archive, ADAPTER_CONFIG_PATH_JS, relative);
        modifyAdapterConfig(archive, SAML_ADAPTER_CONFIG_PATH, relative);
        modifyAdapterConfig(archive, SAML_ADAPTER_CONFIG_PATH_TENANT1, relative);
        modifyAdapterConfig(archive, SAML_ADAPTER_CONFIG_PATH_TENANT2, relative);
    }

    protected void modifyAdapterConfig(Archive<?> archive, String adapterConfigPath, boolean relative) {
        if (archive.contains(adapterConfigPath)) {
            log.info("Modifying adapter config " + adapterConfigPath + " in " + archive.getName());
            if (adapterConfigPath.endsWith(".xml")) { // SAML adapter config
                log.info("Modifying saml adapter config in " + archive.getName());

                Document doc = loadXML(archive.get(adapterConfigPath).getAsset().openStream());
                if (AUTH_SERVER_SSL_REQUIRED) {
                    modifyDocElementAttribute(doc, "SingleSignOnService", "bindingUrl", "8080", System.getProperty("auth.server.https.port"));
                    modifyDocElementAttribute(doc, "SingleSignOnService", "bindingUrl", "http", "https");
                    modifyDocElementAttribute(doc, "SingleSignOnService", "assertionConsumerServiceUrl", "8080", System.getProperty("app.server.https.port"));
                    modifyDocElementAttribute(doc, "SingleSignOnService", "assertionConsumerServiceUrl", "http", "https");
                    modifyDocElementAttribute(doc, "SingleLogoutService", "postBindingUrl", "8080", System.getProperty("auth.server.https.port"));
                    modifyDocElementAttribute(doc, "SingleLogoutService", "postBindingUrl", "http", "https");
                    modifyDocElementAttribute(doc, "SingleLogoutService", "redirectBindingUrl", "8080", System.getProperty("auth.server.https.port"));
                    modifyDocElementAttribute(doc, "SingleLogoutService", "redirectBindingUrl", "http", "https");
                    modifyDocElementAttribute(doc, "SP", "logoutPage", "8080", System.getProperty("app.server.https.port"));
                    modifyDocElementAttribute(doc, "SP", "logoutPage", "http", "https");
                } else {
                    modifyDocElementAttribute(doc, "SingleSignOnService", "bindingUrl", "8080", System.getProperty("auth.server.http.port"));
                    modifyDocElementAttribute(doc, "SingleSignOnService", "assertionConsumerServiceUrl", "8080", System.getProperty("app.server.http.port"));
                    modifyDocElementAttribute(doc, "SingleLogoutService", "postBindingUrl", "8080", System.getProperty("auth.server.http.port"));
                    modifyDocElementAttribute(doc, "SingleLogoutService", "redirectBindingUrl", "8080", System.getProperty("auth.server.http.port"));
                    modifyDocElementAttribute(doc, "SP", "logoutPage", "8080", System.getProperty("app.server.http.port"));
                }

                archive.add(new StringAsset(IOUtil.documentToString(doc)), adapterConfigPath);

                ((WebArchive) archive).addAsResource(new File(DeploymentArchiveProcessor.class.getResource("/keystore/keycloak.truststore").getFile()));

                // For running SAML tests it is necessary to have few dependencies on app-server side.
                // Few of them are not in adapter zip so we need to add them manually here
            } else { // OIDC adapter config
                try {
                    AdapterConfig adapterConfig = loadJson(archive.get(adapterConfigPath)
                            .getAsset().openStream(), AdapterConfig.class);

                    adapterConfig.setAuthServerUrl(getAuthServerContextRoot() + "/auth");

                    if (APP_SERVER_SSL_REQUIRED) {
                        adapterConfig.setSslRequired("all");
                    }

                    archive.add(new StringAsset(JsonSerialization.writeValueAsPrettyString(adapterConfig)),
                            adapterConfigPath);

                } catch (IOException ex) {
                    log.error("Cannot serialize adapter config to JSON.", ex);
                }
            }
        }
    }

    protected void modifyWebXml(Archive<?> archive, TestClass testClass) {
        if (!archive.contains(WEBXML_PATH)) return;

        Document webXmlDoc;
        try {
            webXmlDoc = loadXML(
              archive.get(WEBXML_PATH).getAsset().openStream());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Error when processing " + archive.getName(), ex);
        }

        archive.add(new StringAsset((documentToString(webXmlDoc))), WEBXML_PATH);
    }
}
