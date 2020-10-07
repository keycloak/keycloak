package org.keycloak.testsuite.utils.arquillian.tomcat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ClassAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils;
import org.w3c.dom.Document;

import javax.ws.rs.ApplicationPath;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.testsuite.utils.io.IOUtil.documentToString;
import static org.keycloak.testsuite.utils.io.IOUtil.loadXML;
import static org.keycloak.testsuite.utils.io.IOUtil.modifyDocElementValue;
import static org.keycloak.testsuite.utils.io.IOUtil.removeElementFromDoc;

public class TomcatDeploymentArchiveProcessorUtils {

    private static final String WAR_CLASSPATH = "/WEB-INF/classes/";
    private static final String CONTEXT_PATH = "/META-INF/context.xml";
    private static final String OIDC_VALVE_CLASS = "org.keycloak.adapters.tomcat.KeycloakAuthenticatorValve";
    private static final String SAML_VALVE_CLASS = "org.keycloak.adapters.saml.tomcat.SamlAuthenticatorValve";
    private static final Logger LOG = Logger.getLogger(DeploymentArchiveProcessorUtils.class);


    /**
     * Tomcat doesn't load files (e. g. secure-portal keystore) from webarchive classpath
     * we need to copy it to common classpath /catalina_home/lib
     * @param archive
     */
    public static void copyWarClasspathFilesToCommonTomcatClasspath(Archive<?> archive) {
        Stream<Node> contentOfArchiveClasspath = archive.getContent(archivePath ->
                archivePath.get().startsWith(WAR_CLASSPATH)).values().stream() // get all nodes in WAR classpath
                .filter(node -> StringUtils.countMatches(node.toString(), "/")
                        == StringUtils.countMatches(WAR_CLASSPATH, "/")  // get only files not directories
                        && node.toString().contains("."));


        String catalinaHome = System.getProperty("app.server.home");
        contentOfArchiveClasspath.forEach(
                (Node node) -> {
                    Path p = Paths.get(node.toString());
                    File outputFile = new File(catalinaHome + "/lib/" + p.getFileName().toString());
                    if (!outputFile.exists()) {
                        try {
                            Files.copy(node.getAsset().openStream(), outputFile.toPath());
                        } catch (IOException e) {
                            throw new RuntimeException("Couldn't copy classpath files from deployed war to common classpath of tomcat", e);
                        }
                    }
                }
        );
    }

    public static void replaceKEYCLOAKMethodWithBASIC(Archive<?> archive) {
        if (!archive.contains(DeploymentArchiveProcessorUtils.WEBXML_PATH)) return;

        try {
            Document webXmlDoc = loadXML(archive.get(DeploymentArchiveProcessorUtils.WEBXML_PATH).getAsset().openStream());

            LOG.debug("Setting BASIC as auth-method in WEB.XML for " + archive.getName());
            modifyDocElementValue(webXmlDoc, "auth-method", "KEYCLOAK-SAML", "BASIC");
            modifyDocElementValue(webXmlDoc, "auth-method", "KEYCLOAK", "BASIC");

            archive.add(new StringAsset((documentToString(webXmlDoc))), DeploymentArchiveProcessorUtils.WEBXML_PATH);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Error when processing " + archive.getName(), ex);
        }
    }

    public static void removeServletConfigurationInWebXML(Archive<?> archive) {
        if (!archive.contains(DeploymentArchiveProcessorUtils.WEBXML_PATH)) return;

        try {
            Document webXmlDoc = loadXML(archive.get(DeploymentArchiveProcessorUtils.WEBXML_PATH).getAsset().openStream());

            LOG.debug("Removing web.xml servlet configuration for " + archive.getName());
            removeElementFromDoc(webXmlDoc, "web-app/servlet");
            removeElementFromDoc(webXmlDoc, "web-app/servlet-mapping");

            archive.add(new StringAsset((documentToString(webXmlDoc))), DeploymentArchiveProcessorUtils.WEBXML_PATH);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Error when processing " + archive.getName(), ex);
        }
    }

    public static void replaceOIDCValveWithSAMLValve(Archive<?> archive) {
        try {
            String contextXmlContent = IOUtils.toString(archive.get(CONTEXT_PATH).getAsset().openStream(), "UTF-8")
                    .replace(OIDC_VALVE_CLASS, SAML_VALVE_CLASS);
            archive.add(new StringAsset(contextXmlContent), CONTEXT_PATH);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean isJaxRSApp(Archive<?> archive) {
        WebArchive webArchive = (WebArchive) archive;
        Set<Class<?>> classes = webArchive.getContent(archivePath ->
                archivePath.get().startsWith("/WEB-INF/classes/") &&
                        archivePath.get().endsWith(".class")
        ).values().stream()
                .filter(node -> node.getAsset() instanceof ClassAsset)
                .map(node -> ((ClassAsset)node.getAsset()).getSource())
                .filter(clazz -> clazz.isAnnotationPresent(javax.ws.rs.Path.class))
                .collect(Collectors.toSet());

        return !classes.isEmpty();
    }

    public static Set<Class<?>> getApplicationConfigClasses(Archive<?> archive) {
        WebArchive webArchive = (WebArchive) archive;
        return webArchive.getContent(archivePath ->
                archivePath.get().startsWith("/WEB-INF/classes/") &&
                        archivePath.get().endsWith(".class")
        ).values().stream()
                .filter(node -> node.getAsset() instanceof ClassAsset)
                .map(node -> ((ClassAsset)node.getAsset()).getSource())
                .filter(clazz -> clazz.isAnnotationPresent(ApplicationPath.class))
                .collect(Collectors.toSet());
    }

    public static boolean containsApplicationConfigClass(Archive<?> archive) {
        return !getApplicationConfigClasses(archive).isEmpty();
    }
}
