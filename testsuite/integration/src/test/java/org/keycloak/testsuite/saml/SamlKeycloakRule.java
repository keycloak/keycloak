package org.keycloak.testsuite.saml;

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

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class SamlKeycloakRule extends AbstractKeycloakRule {

    public static class SendUsernameServlet extends HttpServlet {
        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            OutputStream stream = resp.getOutputStream();
            Principal principal = req.getUserPrincipal();
            if (principal == null) {
                stream.write("null".getBytes());
                return;
            }
            String name = principal.getName();
            stream.write(name.getBytes());
        }
        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            OutputStream stream = resp.getOutputStream();
            Principal principal = req.getUserPrincipal();
            if (principal == null) {
                stream.write("null".getBytes());
                return;
            }
            String name = principal.getName();
            stream.write(name.getBytes());
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
        LoginConfig loginConfig = new LoginConfig("FORM", "Test Realm");

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
                .addServletExtension(new SPServletExtension());
        server.getServer().deploy(deploymentInfo);
    }

    public String getRealmJson() {
        return "/saml/testsaml.json";
    }


}
