/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite;

import io.undertow.Undertow;
import org.apache.catalina.startup.Tomcat;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.proxy.ProxyServerBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testutils.KeycloakServer;
import org.openqa.selenium.WebDriver;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.regex.Matcher;

@Ignore
public class ProxyTest {
    static String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri("http://localhost:8081/auth"))
            .queryParam(OAuth2Constants.REDIRECT_URI, "http://localhost:8080/customer-portal").build("demo").toString();

    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            RealmRepresentation representation = KeycloakServer.loadJson(getClass().getResourceAsStream("/demorealm.json"), RealmRepresentation.class);
            RealmModel realm = manager.importRealm(representation);
       }
    };

    public static class SendUsernameServlet extends HttpServlet {
        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            String requestURI = req.getRequestURI();
            resp.setContentType("text/plain");
            OutputStream stream = resp.getOutputStream();
            stream.write(req.getRequestURL().toString().getBytes());
            stream.write("\n".getBytes());
            Integer count = (Integer)req.getSession().getAttribute("counter");
            if (count == null) count = new Integer(0);
            req.getSession().setAttribute("counter", new Integer(count.intValue() + 1));
            stream.write(("count:"+count).getBytes());

            Enumeration<String> headers = req.getHeaderNames();
            while (headers.hasMoreElements()) {
                String name = headers.nextElement();
                System.out.println(name +": " + req.getHeader(name));
            }

            if (requestURI.contains("/bearer")) {
                Client client = ClientBuilder.newClient();

                try {
                    String appBase = "http://localhost:8080/customer-portal";
                    WebTarget target = client.target(appBase + "/call-bearer");

                    Response response = null;
                    response = target.request()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer CRAP")
                            .get();
                    Assert.assertEquals(401, response.getStatus());
                    response.close();
                    response = target.request()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + req.getHeader("KEYCLOAK_ACCESS_TOKEN"))
                            .get();
                    Assert.assertEquals(200, response.getStatus());
                    String data = response.readEntity(String.class);
                    response.close();
                    stream.write(data.getBytes());
                } finally {
                    client.close();
                }

            } else if (requestURI.contains("/call-bearer")) {
                stream.write("bearer called".getBytes());
            }
        }
        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            doGet(req, resp);
        }
    }
    public static class SendError extends HttpServlet {
        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            OutputStream stream = resp.getOutputStream();
            stream.write("access error".getBytes());
        }
        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            doGet(req, resp);
        }
    }

    static Tomcat tomcat = null;

    public static void initTomcat() throws Exception {
        URL dir = ProxyTest.class.getResource("/tomcat-test/webapp/WEB-INF/web.xml");
        File webappDir = new File(dir.getFile()).getParentFile().getParentFile();
        tomcat = new Tomcat();
        String baseDir = getBaseDirectory();
        tomcat.setBaseDir(baseDir);
        tomcat.setPort(8082);

        tomcat.addWebapp("/customer-portal", webappDir.toString());
        System.out.println("configuring app with basedir: " + webappDir.toString());

        tomcat.start();
        //tomcat.getServer().await();
    }

    public static void shutdownTomcat() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }

    static Undertow proxyServer = null;

    @BeforeClass
    public static void initProxy() throws Exception {
        initTomcat();
        InputStream is = ProxyTest.class.getResourceAsStream("/proxy-config.json");
        proxyServer = ProxyServerBuilder.build(is);
        proxyServer.start();

    }

    @AfterClass
    public static void shutdownProxy() throws Exception {
        shutdownTomcat();
        if (proxyServer != null) proxyServer.stop();
    }


    @Rule
    public WebRule webRule = new WebRule(this);
    @WebResource
    protected WebDriver driver;
    @WebResource
    protected LoginPage loginPage;

    public static final String LOGIN_URL = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri("http://localhost:8081/auth")).build("demo").toString();

    @Test
    public void testHttp() throws Exception {
        String baseUrl = "http://localhost:8080";
        testit(baseUrl);


    }

    @Test
    public void testHttps() throws Exception {
        String baseUrl = "https://localhost:8443";
        testit(baseUrl);


    }

    public void testit(String baseUrl) {
        driver.navigate().to(baseUrl + "/customer-portal/users");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        String loginPageSource = driver.getPageSource();
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), baseUrl + "/customer-portal/users");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("customer-portal/users"));
        Assert.assertTrue(pageSource.contains("count:0"));
        driver.navigate().to(baseUrl + "/customer-portal/users");
        Assert.assertEquals(driver.getCurrentUrl(), baseUrl + "/customer-portal/users");
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("customer-portal/users"));
        Assert.assertTrue(pageSource.contains("count:1")); // test http session

        driver.navigate().to(baseUrl + "/customer-portal/bearer");
        pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("bearer called"));


        driver.navigate().to(baseUrl + "/customer-portal/users/deny");
        Assert.assertEquals(driver.getCurrentUrl(), baseUrl + "/customer-portal/users/deny");
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("access error"));

        driver.navigate().to(baseUrl + "/customer-portal/admins");
        Assert.assertEquals(driver.getCurrentUrl(), baseUrl + "/customer-portal/admins");
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("access error"));


        // test logout

        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri("http://localhost:8081/auth"))
                .queryParam(OAuth2Constants.REDIRECT_URI, baseUrl + "/customer-portal/users").build("demo").toString();
        driver.navigate().to(logoutUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(baseUrl + "/customer-portal/users");
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));

        // test unsecured page
        driver.navigate().to(baseUrl + "/customer-portal") ;
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("customer-portal"));
        driver.navigate().to(baseUrl + "/customer-portal/users/permit") ;
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("customer-portal/users/permit"));
    }

    private static String getBaseDirectory() {
        String dirPath = null;
        String relativeDirPath = "testsuite" + File.separator + "proxy" + File.separator + "target";

        if (System.getProperties().containsKey("maven.home")) {
            dirPath = System.getProperty("user.dir").replaceFirst("testsuite.proxy.*", Matcher.quoteReplacement(relativeDirPath));
        } else {
            for (String c : System.getProperty("java.class.path").split(File.pathSeparator)) {
                if (c.contains(File.separator + "testsuite" + File.separator + "proxy")) {
                    dirPath = c.replaceFirst("testsuite.proxy.*", Matcher.quoteReplacement(relativeDirPath));
                    break;
                }
            }
        }

        String absolutePath = new File(dirPath).getAbsolutePath();
        return absolutePath;
    }




}
