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

package org.keycloak.testsuite.utils.undertow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ServletSessionConfig;
import io.undertow.servlet.api.WebResourceCollection;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple web.xml parser just to handle our test deployments
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class SimpleWebXmlParser {

    private static final Logger log = Logger.getLogger(SimpleWebXmlParser.class);

    void parseWebXml(Document webXml, DeploymentInfo di) {
        try {
            DocumentWrapper document = new DocumentWrapper(webXml);

            if (di.getServlets().get("ResteasyServlet") == null) {

                // SERVLETS
                Map<String, String> servletMappings = new HashMap<>();
                List<ElementWrapper> sm = document.getElementsByTagName("servlet-mapping");
                for (ElementWrapper mapping : sm) {
                    String servletName = mapping.getElementByTagName("servlet-name").getText();
                    String path = mapping.getElementByTagName("url-pattern").getText();
                    servletMappings.put(servletName, path);
                }

                List<ElementWrapper> servlets = document.getElementsByTagName("servlet");
                for (ElementWrapper servlet : servlets) {
                    String servletName = servlet.getElementByTagName("servlet-name").getText();
                    ElementWrapper servletClassEw = servlet.getElementByTagName("servlet-class");
                    String servletClass = servletClassEw == null ? servletName : servletClassEw.getText();
                    ElementWrapper loadOnStartupEw = servlet.getElementByTagName("load-on-startup");
                    Integer loadOnStartup = loadOnStartupEw == null ? null : Integer.valueOf(loadOnStartupEw.getText());

                    Class<? extends Servlet> servletClazz = (Class<? extends Servlet>) Class.forName(servletClass, false, di.getClassLoader());
                    ServletInfo undertowServlet = new ServletInfo(servletName, servletClazz);

                    if (servletMappings.containsKey(servletName)) {
                        undertowServlet.addMapping(servletMappings.get(servletName));
                        undertowServlet.setLoadOnStartup(loadOnStartup);
                        di.addServlet(undertowServlet);
                    } else {
                        log.warnf("Missing servlet-mapping for '%s'", servletName);
                    }
                }
            }

            // FILTERS
            Map<String, String> filterMappings = new HashMap<>();
            List<ElementWrapper> fm = document.getElementsByTagName("filter-mapping");
            for (ElementWrapper mapping : fm) {
                String filterName = mapping.getElementByTagName("filter-name").getText();
                String path = mapping.getElementByTagName("url-pattern").getText();
                filterMappings.put(filterName, path);
            }

            List<ElementWrapper> filters = document.getElementsByTagName("filter");
            for (ElementWrapper filter : filters) {
                String filterName = filter.getElementByTagName("filter-name").getText();
                String filterClass = filter.getElementByTagName("filter-class").getText();

                Class<? extends Filter> filterClazz = (Class<? extends Filter>) Class.forName(filterClass, false, di.getClassLoader());
                FilterInfo undertowFilter = new FilterInfo(filterName, filterClazz);

                List<ElementWrapper> initParams = filter.getElementsByTagName("init-param");
                for (ElementWrapper initParam : initParams) {
                    String paramName = initParam.getElementByTagName("param-name").getText();
                    String paramValue = initParam.getElementByTagName("param-value").getText();
                    undertowFilter.addInitParam(paramName, paramValue);
                }

                di.addFilter(undertowFilter);

                if (filterMappings.containsKey(filterName)) {
                    di.addFilterUrlMapping(filterName, filterMappings.get(filterName), DispatcherType.REQUEST);
                } else {
                    log.warnf("Missing filter-mapping for '%s'", filterName);
                }
            }

            // CONTEXT PARAMS
            List<ElementWrapper> contextParams = document.getElementsByTagName("context-param");
            for (ElementWrapper param : contextParams) {
                String paramName = param.getElementByTagName("param-name").getText();
                String paramValue = param.getElementByTagName("param-value").getText();
                di.addInitParameter(paramName, paramValue);
            }


            // ROLES
            List<ElementWrapper> securityRoles = document.getElementsByTagName("security-role");
            for (ElementWrapper sr : securityRoles) {
                String roleName = sr.getElementByTagName("role-name").getText();
                di.addSecurityRole(roleName);
            }


            // SECURITY CONSTRAINTS
            List<ElementWrapper> secConstraints = document.getElementsByTagName("security-constraint");
            for (ElementWrapper constraint : secConstraints) {
                String urlPattern = constraint.getElementByTagName("web-resource-collection")
                        .getElementByTagName("url-pattern")
                        .getText();

                ElementWrapper authCsnt = constraint.getElementByTagName("auth-constraint");
                String roleName = authCsnt==null ? null : authCsnt
                        .getElementByTagName("role-name")
                        .getText();

                SecurityConstraint undertowConstraint = new SecurityConstraint();
                WebResourceCollection collection = new WebResourceCollection();
                collection.addUrlPattern(urlPattern);
                undertowConstraint.addWebResourceCollection(collection);

                if (roleName != null) {
                    undertowConstraint.addRoleAllowed(roleName);
                } else {
                    undertowConstraint.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.PERMIT);
                }
                di.addSecurityConstraint(undertowConstraint);
            }

            // LOGIN CONFIG
            ElementWrapper loginCfg = document.getElementByTagName("login-config");
            if (loginCfg != null) {
                String mech = loginCfg.getElementByTagName("auth-method").getText();
                String realmName = loginCfg.getElementByTagName("realm-name").getText();

                ElementWrapper form = loginCfg.getElementByTagName("form-login-config");
                if (form != null) {
                    String loginPage = form.getElementByTagName("form-login-page").getText();
                    String errorPage = form.getElementByTagName("form-error-page").getText();
                    di.setLoginConfig(new LoginConfig(mech, realmName, loginPage, errorPage));
                } else {
                    di.setLoginConfig(new LoginConfig(realmName).addFirstAuthMethod(mech));
                }
            }

            // COOKIE CONFIG
            ElementWrapper sessionCfg = document.getElementByTagName("session-config");
            if (sessionCfg != null) {
                ElementWrapper cookieConfig = sessionCfg.getElementByTagName("cookie-config");
                String cookieName = cookieConfig.getElementByTagName("name").getText();

                ServletSessionConfig cfg = new ServletSessionConfig();
                if (cookieConfig.getElementByTagName("http-only") != null) {
                    cfg.setHttpOnly(Boolean.parseBoolean(cookieConfig.getElementByTagName("http-only").getText()));
                }
                cfg.setName(cookieName);
                di.setServletSessionConfig(cfg);
            }
            
            // ERROR PAGES
            List<ElementWrapper> errorPages = document.getElementsByTagName("error-page");
            for (ElementWrapper errorPageWrapper : errorPages) {
                String location = errorPageWrapper.getElementByTagName("location").getText();

                ErrorPage errorPage;
                if (errorPageWrapper.getElementByTagName("error-code") != null) {
                    errorPage = new ErrorPage(location, Integer.parseInt(errorPageWrapper.getElementByTagName("error-code").getText()));
                } else {
                    errorPage = new ErrorPage(location);
                }

                di.addErrorPage(errorPage);
            }

        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        } catch (NullPointerException npe) {
            throw new RuntimeException("Error parsing web.xml of " + di.getDeploymentName(), npe);
        }
    }


    private static abstract class XmlWrapper {


        abstract List<ElementWrapper> getElementsByTagName(String tagName);


        abstract ElementWrapper getElementByTagName(String tagName);


        List<ElementWrapper> getElementsFromNodeList(NodeList nl) {
            List<ElementWrapper> result = new LinkedList<>();

            for (int i=0; i<nl.getLength() ; i++) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    result.add(new ElementWrapper((Element) node));
                }
            }

            return result;
        }


        ElementWrapper getElementFromNodeList(NodeList nl) {
            if (nl.getLength() > 0) {
                return new ElementWrapper((Element) nl.item(0));
            } else {
                return null;
            }
        }

    }


    private static class ElementWrapper extends XmlWrapper {

        private final Element element;

        public ElementWrapper(Element element) {
            this.element = element;
        }

        @Override
        public List<ElementWrapper> getElementsByTagName(String tagName) {
            NodeList nl = element.getElementsByTagName(tagName);
            return getElementsFromNodeList(nl);
        }

        @Override
        public ElementWrapper getElementByTagName(String tagName) {
            NodeList nl = element.getElementsByTagName(tagName);
            return getElementFromNodeList(nl);
        }

        public String getText() {
            return this.element.getTextContent();
        }
    }


    private static class DocumentWrapper extends XmlWrapper {

        private final Document document;

        public DocumentWrapper(Document document) {
            this.document = document;
        }


        @Override
        List<ElementWrapper> getElementsByTagName(String tagName) {
            NodeList nl = document.getElementsByTagName(tagName);
            return getElementsFromNodeList(nl);
        }


        @Override
        ElementWrapper getElementByTagName(String tagName) {
            NodeList nl = document.getElementsByTagName(tagName);
            return getElementFromNodeList(nl);
        }
    }

}
