package org.keycloak.testsuite.keycloaksaml;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.WebResourceCollection;
import org.junit.Assert;
import org.keycloak.adapters.saml.undertow.SamlServletExtension;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.picketlink.identity.federation.bindings.wildfly.sp.SPServletExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.Principal;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class SamlKeycloakRule extends AbstractKeycloakRule {

    public static class SendUsernameServlet extends HttpServlet {

        public static Principal sentPrincipal;
        public static List<String> checkRoles;

        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            if (checkRoles != null) {
                for (String role : checkRoles) {
                    System.out.println("check role: " + role);
                    Assert.assertTrue(req.isUserInRole(role));
                }

            }
            resp.setContentType("text/plain");
            OutputStream stream = resp.getOutputStream();
            Principal principal = req.getUserPrincipal();
            stream.write("request-path: ".getBytes());
            stream.write(req.getPathInfo().getBytes());
            stream.write("\n".getBytes());
            stream.write("principal=".getBytes());
            if (principal == null) {
                stream.write("null".getBytes());
                return;
            }
            String name = principal.getName();
            stream.write(name.getBytes());
            sentPrincipal = principal;

        }
        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            if (checkRoles != null) {
                for (String role : checkRoles) {
                    Assert.assertTrue(req.isUserInRole(role));
                }

            }
            resp.setContentType("text/plain");
            OutputStream stream = resp.getOutputStream();
            Principal principal = req.getUserPrincipal();
            stream.write("request-path: ".getBytes());
            stream.write(req.getPathInfo().getBytes());
            stream.write("\n".getBytes());
            stream.write("principal=".getBytes());
            if (principal == null) {
                stream.write("null".getBytes());
                return;
            }
            String name = principal.getName();
            stream.write(name.getBytes());
            sentPrincipal = principal;
        }
    }

    public static class TestResourceManager implements ResourceManager {

        private final String basePath;

        public TestResourceManager(String basePath){
            this.basePath = basePath;
        }

        @Override
        public Resource getResource(String path) throws IOException {
            String temp = path;
            String fullPath = basePath + temp;
            URL url = getClass().getResource(fullPath);
            if (url == null) {
                System.out.println("url is null: " + fullPath);
            }
            return new URLResource(url, url.openConnection(), path);
        }

        @Override
        public boolean isResourceChangeListenerSupported() {
            throw new RuntimeException();
        }

        @Override
        public void registerResourceChangeListener(ResourceChangeListener listener) {
            throw new RuntimeException();
        }

        @Override
        public void removeResourceChangeListener(ResourceChangeListener listener) {
            throw new RuntimeException();
        }

        @Override
        public void close() throws IOException {
            throw new RuntimeException();
        }
    }

    public static class TestIdentityManager implements IdentityManager {
        @Override
        public Account verify(Account account) {
            return account;
        }

        @Override
        public Account verify(String userName, Credential credential) {
            throw new RuntimeException("WTF");
        }

        @Override
        public Account verify(Credential credential) {
            throw new RuntimeException();
        }
    }

    @Override
    protected void setupKeycloak() {
        String realmJson = getRealmJson();
        server.importRealm(getClass().getResourceAsStream(realmJson));
        initWars();
    }

    public abstract void initWars();

    public void initializeSamlSecuredWar(String warResourcePath, String contextPath, String warDeploymentName, ClassLoader classLoader) {

        ServletInfo regularServletInfo = new ServletInfo("servlet", SendUsernameServlet.class)
                .addMapping("/*");

        SecurityConstraint constraint = new SecurityConstraint();
        WebResourceCollection collection = new WebResourceCollection();
        collection.addUrlPattern("/*");
        constraint.addWebResourceCollection(collection);
        constraint.addRoleAllowed("manager");
        constraint.addRoleAllowed("el-jefe");
        LoginConfig loginConfig = new LoginConfig("KEYCLOAK-SAML", "Test Realm");

        ResourceManager resourceManager = new TestResourceManager(warResourcePath);

        DeploymentInfo deploymentInfo = new DeploymentInfo()
                .setClassLoader(classLoader)
                .setIdentityManager(new TestIdentityManager())
                .setContextPath(contextPath)
                .setDeploymentName(warDeploymentName)
                .setLoginConfig(loginConfig)
                .setResourceManager(resourceManager)
                .addServlets(regularServletInfo)
                .addSecurityConstraint(constraint)
                .addServletExtension(new SamlServletExtension());
        server.getServer().deploy(deploymentInfo);
    }

    public String getRealmJson() {
        return "/keycloak-saml/testsaml.json";
    }


}
