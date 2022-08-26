/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.utils.arquillian;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.adapters.servlet.KeycloakOIDCFilter;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.JsonSerialization;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.keycloak.testsuite.utils.io.IOUtil.modifyDocElementAttribute;
import static org.keycloak.testsuite.util.ServerURLs.getAppServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlasta Ramik</a>
 */
public class DeploymentArchiveProcessorUtils {

    private static final Logger log = Logger.getLogger(DeploymentArchiveProcessorUtils.class);

    private static final boolean AUTH_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));
    private static final boolean APP_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("app.server.ssl.required"));

    private static final String APP_SERVER_SCHEMA = APP_SERVER_SSL_REQUIRED ? "https" : "http";
    private static final String APP_SERVER_PORT_PROPERTY = "auth.server." + APP_SERVER_SCHEMA + ".port";
    private static final String AUTH_SERVER_REPLACED_URL = "http://localhost:8080";
    private static final String APP_SERVER_CONTAINER = System.getProperty("app.server", "");

    public static final String WEBXML_PATH = "/WEB-INF/web.xml";
    public static final String ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_TENANT1 = "/WEB-INF/classes/tenant1-keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_TENANT2 = "/WEB-INF/classes/tenant2-keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_JS = "/keycloak.json";
    public static final String SAML_ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak-saml.xml";
    public static final String JBOSS_DEPLOYMENT_XML_PATH = "/WEB-INF/jboss-deployment-structure.xml";
    public static final String SAML_ADAPTER_CONFIG_PATH_TENANT1 = "/WEB-INF/classes/tenant1-keycloak-saml.xml";
    public static final String SAML_ADAPTER_CONFIG_PATH_TENANT2 = "/WEB-INF/classes/tenant2-keycloak-saml.xml";
    public static final String TRUSTSTORE_PASSWORD = "secret";
    public static final Collection<String> SAML_CONFIGS = Arrays.asList(SAML_ADAPTER_CONFIG_PATH,
            SAML_ADAPTER_CONFIG_PATH_TENANT1, SAML_ADAPTER_CONFIG_PATH_TENANT2);

    /**
     * @return true iff archive's name equals run-on-server-classes.war
     */
    public static boolean checkRunOnServerDeployment(Archive<?> archive) {
        return archive.getName().equals("run-on-server-classes.war");
    }

    public static void modifyWebXMLForServletFilter(Archive<?> archive, TestClass testClass) {
        Document webXmlDoc;
        try {
            webXmlDoc = IOUtil.loadXML(
              archive.get(WEBXML_PATH).getAsset().openStream());
        } catch (Exception ex) {
            throw new RuntimeException("Error when processing " + archive.getName(), ex);
        }

        //We need to add filter declaration to web.xml
        log.info("Adding filter to " + testClass.getAnnotation(UseServletFilter.class).filterClass() +
                " with mapping " + testClass.getAnnotation(UseServletFilter.class).filterPattern() +
                " for " + archive.getName());

        Element filter = webXmlDoc.createElement("filter");
        Element filterName = webXmlDoc.createElement("filter-name");
        Element filterClass = webXmlDoc.createElement("filter-class");

        filterName.setTextContent(testClass.getAnnotation(UseServletFilter.class).filterName());
        filterClass.setTextContent(testClass.getAnnotation(UseServletFilter.class).filterClass());

        filter.appendChild(filterName);
        filter.appendChild(filterClass);
        IOUtil.appendChildInDocument(webXmlDoc, "web-app", filter);

        filter.appendChild(filterName);
        filter.appendChild(filterClass);

        // check if there was a resolver for OIDC and set as a filter param
        String keycloakResolverClass = getKeycloakResolverClass(webXmlDoc);
        if (keycloakResolverClass != null) {
            Element initParam = webXmlDoc.createElement("init-param");
            Element paramName = webXmlDoc.createElement("param-name");
            paramName.setTextContent("keycloak.config.resolver");
            Element paramValue = webXmlDoc.createElement("param-value");
            paramValue.setTextContent(keycloakResolverClass);
            initParam.appendChild(paramName);
            initParam.appendChild(paramValue);
            filter.appendChild(initParam);
        }

        // Limitation that all deployments of annotated class use same skipPattern. Refactor if 
        // something more flexible is needed (would require more tricky web.xml parsing though...)
        addInitParam(webXmlDoc, filter, KeycloakOIDCFilter.SKIP_PATTERN_PARAM, testClass.getAnnotation(UseServletFilter.class).skipPattern());
        addInitParam(webXmlDoc, filter, KeycloakOIDCFilter.ID_MAPPER_PARAM, testClass.getAnnotation(UseServletFilter.class).idMapper());


        IOUtil.appendChildInDocument(webXmlDoc, "web-app", filter);

        Element filterMapping = webXmlDoc.createElement("filter-mapping");

        Element urlPattern = webXmlDoc.createElement("url-pattern");

        filterName = webXmlDoc.createElement("filter-name");

        filterName.setTextContent(testClass.getAnnotation(UseServletFilter.class).filterName());
        urlPattern.setTextContent(IOUtil.getElementTextContent(webXmlDoc, "web-app/security-constraint/web-resource-collection/url-pattern"));

        filterMapping.appendChild(filterName);
        filterMapping.appendChild(urlPattern);

        if (!testClass.getAnnotation(UseServletFilter.class).dispatcherType().isEmpty()) {
            Element dispatcher = webXmlDoc.createElement("dispatcher");
            dispatcher.setTextContent(testClass.getAnnotation(UseServletFilter.class).dispatcherType());
            filterMapping.appendChild(dispatcher);
        }
        IOUtil.appendChildInDocument(webXmlDoc, "web-app", filterMapping);

        //finally we need to remove all keycloak related configuration from web.xml
        IOUtil.removeElementsFromDoc(webXmlDoc, "web-app", "security-constraint");
        IOUtil.removeElementsFromDoc(webXmlDoc, "web-app", "login-config");
        IOUtil.removeElementsFromDoc(webXmlDoc, "web-app", "security-role");

        archive.add(new StringAsset((IOUtil.documentToString(webXmlDoc))), WEBXML_PATH);
    }

    private static void addInitParam(Document webXmlDoc, Element filter, String initParamName, String initParamValue) {
        // Limitation that all deployments of annotated class use same skipPattern. Refactor if something more flexible is needed (would require more tricky web.xml parsing though...)
        if (initParamValue != null && !initParamValue.isEmpty()) {
            Element initParam = webXmlDoc.createElement("init-param");

            Element paramName = webXmlDoc.createElement("param-name");
            paramName.setTextContent(initParamName);

            Element paramValue = webXmlDoc.createElement("param-value");
            paramValue.setTextContent(initParamValue);

            initParam.appendChild(paramName);
            initParam.appendChild(paramValue);

            filter.appendChild(initParam);
        }
    }

    public static String getKeycloakResolverClass(Document doc) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//web-app/context-param[param-name='keycloak.config.resolver']/param-value/text()");
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if (nodes != null && nodes.getLength() > 0) {
                return nodes.item(0).getNodeValue();
            }
        } catch(DOMException e) {
            throw new IllegalStateException(e);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    public static void addFilterDependencies(Archive<?> archive, TestClass testClass) {
        log.info("Adding filter dependencies to " + archive.getName());

        String dependency = testClass.getAnnotation(UseServletFilter.class).filterDependency();
        ((WebArchive) archive).addAsLibraries(KeycloakDependenciesResolver.resolveDependencies((dependency + ":" + System.getProperty("project.version"))));

        Document jbossXmlDoc = IOUtil.loadXML(archive.get(JBOSS_DEPLOYMENT_XML_PATH).getAsset().openStream());
        IOUtil.removeNodeByAttributeValue(jbossXmlDoc, "dependencies", "module", "name", "org.keycloak.keycloak-saml-core");
        IOUtil.removeNodeByAttributeValue(jbossXmlDoc, "dependencies", "module", "name", "org.keycloak.keycloak-adapter-spi");

        archive.add(new StringAsset((IOUtil.documentToString(jbossXmlDoc))), JBOSS_DEPLOYMENT_XML_PATH);
    }

    public static void modifyOIDCAdapterConfig(Archive<?> archive, String adapterConfigPath) {
        try {
            AdapterConfig adapterConfig = IOUtil.loadJson(archive.get(adapterConfigPath)
                    .getAsset().openStream(), AdapterConfig.class);

            adapterConfig.setAuthServerUrl(getAuthServerUrl());

            if (APP_SERVER_SSL_REQUIRED) {
                adapterConfig.setSslRequired("all");
            }

            if (AUTH_SERVER_SSL_REQUIRED) {
                String trustStorePathInDeployment = "keycloak.truststore";
                if (adapterConfigPath.contains("WEB-INF")) {
                    // This is a Java adapter, we can use classpath
                    trustStorePathInDeployment = "classpath:keycloak.truststore";
                }
                adapterConfig.setTruststore(trustStorePathInDeployment);
                adapterConfig.setTruststorePassword(TRUSTSTORE_PASSWORD);

                String truststoreUrl = System.getProperty("dependency.keystore.root", "") + "/keycloak.truststore";
                File truststore = new File(truststoreUrl);

                if (!truststore.exists()) {
                    truststore = new File(DeploymentArchiveProcessorUtils.class.getResource("/keystore/keycloak.truststore").getFile());
                }

                ((WebArchive) archive).addAsResource(truststore);

                log.debugf("Adding Truststore to the deployment, path %s, password %s, adapter path %s", truststore.getAbsolutePath(), TRUSTSTORE_PASSWORD, trustStorePathInDeployment);
            }

            archive.add(new StringAsset(JsonSerialization.writeValueAsPrettyString(adapterConfig)),
                            adapterConfigPath);
        } catch (IOException ex) {
            log.error("Cannot serialize adapter config to JSON.", ex);
        }
    }

    public static void modifySAMLAdapterConfig(Archive<?> archive, String adapterConfigPath) {
        Document doc = IOUtil.loadXML(archive.get(adapterConfigPath).getAsset().openStream());

        modifySAMLDocument(doc);

        archive.add(new StringAsset(IOUtil.documentToString(doc)), adapterConfigPath);

        String truststoreUrl = System.getProperty("dependency.keystore.root", "") + "/keycloak.truststore";
        File truststore = new File(truststoreUrl);

        if (!truststore.exists()) {
            truststore = new File(DeploymentArchiveProcessorUtils.class.getResource("/keystore/keycloak.truststore").getFile());
        }

        ((WebArchive) archive).addAsResource(truststore);
    }

    public static void modifySAMLDocument(Document doc) {
        modifyDocElementAttribute(doc, "SingleSignOnService", "bindingUrl", AUTH_SERVER_REPLACED_URL, getAuthServerContextRoot());
        modifyDocElementAttribute(doc, "SingleLogoutService", "postBindingUrl", AUTH_SERVER_REPLACED_URL, getAuthServerContextRoot());
        modifyDocElementAttribute(doc, "SingleLogoutService", "redirectBindingUrl", AUTH_SERVER_REPLACED_URL, getAuthServerContextRoot());

        modifyDocElementAttribute(doc, "SingleSignOnService", "assertionConsumerServiceUrl", AUTH_SERVER_REPLACED_URL, getAppServerContextRoot());
        modifyDocElementAttribute(doc, "SP", "logoutPage", AUTH_SERVER_REPLACED_URL, getAppServerContextRoot());
    }

    private static String getAuthServerUrl() {
        String scheme = AUTH_SERVER_SSL_REQUIRED ? "https" : "http";
        String host = System.getProperty("auth.server.host", "localhost");
        String port = AUTH_SERVER_SSL_REQUIRED ? System.getProperty("auth.server.https.port", "8443") :
                System.getProperty("auth.server.http.port", "8180");

        return String.format("%s://%s:%s/auth", scheme, host, port);
    }
}
