package org.keycloak.testsuite;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

public class Deployments {
    public static WebArchive appDeployment() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml")
                .resolve("org.keycloak:keycloak-core", "org.keycloak:keycloak-as7-adapter").withoutTransitivity().asFile();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "app.war").addClasses(TestApplication.class)
                .addAsLibraries(libs).addAsWebInfResource("jboss-deployment-structure.xml")
                .addAsWebInfResource("app-web.xml", "web.xml").addAsWebInfResource("app-jboss-web.xml", "jboss-web.xml")
                .addAsWebInfResource("app-resteasy-oauth.json", "resteasy-oauth.json").addAsWebResource("user.jsp");
        return archive;
    }

    public static WebArchive deployment() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity()
                .asFile();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "auth-server.war").addClasses(TestApplication.class)
                .addAsLibraries(libs).addAsWebInfResource("jboss-deployment-structure.xml").addAsWebInfResource("web.xml")
                .addAsResource("persistence.xml", "META-INF/persistence.xml")
                .addAsResource("testrealm.json", "META-INF/testrealm.json");

        return archive;
    }
}
