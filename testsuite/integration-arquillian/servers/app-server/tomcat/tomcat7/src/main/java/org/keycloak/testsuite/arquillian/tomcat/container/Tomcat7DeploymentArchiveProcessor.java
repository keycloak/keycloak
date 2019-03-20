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
package org.keycloak.testsuite.arquillian.tomcat.container;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils;
import org.keycloak.testsuite.utils.arquillian.tomcat.TomcatDeploymentArchiveProcessorUtils;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Set;

import static org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils.WEBXML_PATH;
import static org.keycloak.testsuite.utils.io.IOUtil.documentToString;

public class Tomcat7DeploymentArchiveProcessor extends CommonTomcatDeploymentArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        super.process(archive, testClass);
        if (DeploymentArchiveProcessorUtils.checkRunOnServerDeployment(archive)) return;

        Set<Class<?>> configClasses = TomcatDeploymentArchiveProcessorUtils.getApplicationConfigClasses(archive);

        if (!configClasses.isEmpty()) {
            // Tomcat 7 doesn't work with resteasy-servlet-initializer therefore we need to configure Tomcat the old way
            // jax-rs docs: http://docs.jboss.org/resteasy/docs/3.6.1.Final/userguide/html_single/#d4e161
            Document webXmlDoc;
            try {
                webXmlDoc = IOUtil.loadXML(
                        archive.get(WEBXML_PATH).getAsset().openStream());
            } catch (Exception ex) {
                throw new RuntimeException("Error when processing " + archive.getName(), ex);
            }

            addContextParam(webXmlDoc);
            addServlet(webXmlDoc, configClasses.iterator().next().getName());
            addServletMapping(webXmlDoc);

            archive.add(new StringAsset((documentToString(webXmlDoc))), DeploymentArchiveProcessorUtils.WEBXML_PATH);
        }
    }

    private void addServletMapping(Document doc) {
        Element servletMapping = doc.createElement("servlet-mapping");
        Element servetName = doc.createElement("servlet-name");
        Element urlPattern = doc.createElement("url-pattern");

        servetName.setTextContent("Resteasy");
        urlPattern.setTextContent("/*");

        servletMapping.appendChild(servetName);
        servletMapping.appendChild(urlPattern);
        IOUtil.appendChildInDocument(doc, "web-app", servletMapping);
    }

    private void addServlet(Document doc, String configClassName) {
        Element servlet = doc.createElement("servlet");
        Element servletName = doc.createElement("servlet-name");
        Element servletClass = doc.createElement("servlet-class");
        Element initParam = doc.createElement("init-param");
        Element paramName = doc.createElement("param-name");
        Element paramValue = doc.createElement("param-value");

        servletName.setTextContent("Resteasy");
        servletClass.setTextContent("org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher");
        paramName.setTextContent("javax.ws.rs.Application");
        paramValue.setTextContent(configClassName);

        servlet.appendChild(servletName);
        servlet.appendChild(servletClass);

        initParam.appendChild(paramName);
        initParam.appendChild(paramValue);

        servlet.appendChild(initParam);

        IOUtil.appendChildInDocument(doc, "web-app", servlet);
    }

    private void addContextParam(Document doc) {
        Element contextParam = doc.createElement("context-param");
        Element paramName = doc.createElement("param-name");
        Element paramValue = doc.createElement("param-value");

        paramName.setTextContent("resteasy.scan.resources");
        paramValue.setTextContent("true");

        contextParam.appendChild(paramName);
        contextParam.appendChild(paramValue);
        IOUtil.appendChildInDocument(doc, "web-app", contextParam);
    }
}
