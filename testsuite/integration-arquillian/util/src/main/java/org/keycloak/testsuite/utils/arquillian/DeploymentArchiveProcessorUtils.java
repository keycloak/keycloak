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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.keycloak.testsuite.util.ServerURLs.getAppServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.utils.io.IOUtil.modifyDocElementAttribute;
import static org.keycloak.testsuite.utils.io.IOUtil.modifyDocElementValue;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlasta Ramik</a>
 */
public class DeploymentArchiveProcessorUtils {

    private static final Logger log = Logger.getLogger(DeploymentArchiveProcessorUtils.class);

    private static final boolean AUTH_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));
    private static final boolean APP_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("app.server.ssl.required"));

    private static final String AUTH_SERVER_REPLACED_URL = "http://localhost:8080";

    public static final String WEBXML_PATH = "/WEB-INF/web.xml";
    public static final String ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak.json";
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

    public static void useJakartaEEServletClass(Archive<?> archive, String adapterConfigPath) {
        final String SERVLET_TAG = "servlet";
        final String SERVLET_CLASS_TAG = "servlet-class";
        final String JAVAX_APPLICATION = "javax.ws.rs.core.Application";
        final String JAKARTA_APPLICATION = "jakarta.ws.rs.core.Application";

        final Asset configAsset = Optional.ofNullable(archive.get(adapterConfigPath))
                .map(Node::getAsset)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find '%s' config path", adapterConfigPath)));

        try (InputStream configStream = configAsset.openStream()) {
            final Document doc = IOUtil.loadXML(configStream);
            final NodeList servletNodeList = doc.getElementsByTagName(SERVLET_TAG);

            if (servletNodeList.getLength() == 1) {
                final int servletClassCount = doc.getElementsByTagName(SERVLET_CLASS_TAG).getLength();

                if (servletClassCount == 0) {
                    final Element servletClassElement = doc.createElement(SERVLET_CLASS_TAG);
                    servletClassElement.setTextContent(JAKARTA_APPLICATION);
                    servletNodeList.item(0).appendChild(servletClassElement);
                    log.infof("Appending '%s' tag with Jakarta application class to '%s'\n", SERVLET_CLASS_TAG, archive.getName());
                } else if (servletClassCount == 1) {
                    modifyDocElementValue(doc, SERVLET_CLASS_TAG, JAVAX_APPLICATION, JAKARTA_APPLICATION);
                    log.infof("Modifying 'servlet-class' tag to use Jakarta application class in '%s'\n", SERVLET_CLASS_TAG, archive.getName());
                } else {
                    log.error(String.format("Invalid count of '%s' tags for '%s'\n", SERVLET_CLASS_TAG, archive.getName()));
                    return;
                }
                archive.add(new StringAsset(IOUtil.documentToString(doc)), adapterConfigPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getAuthServerUrl() {
        String scheme = AUTH_SERVER_SSL_REQUIRED ? "https" : "http";
        String host = System.getProperty("auth.server.host", "localhost");
        String port = AUTH_SERVER_SSL_REQUIRED ? System.getProperty("auth.server.https.port", "8443") :
                System.getProperty("auth.server.http.port", "8180");

        return String.format("%s://%s:%s/auth", scheme, host, port);
    }
}
