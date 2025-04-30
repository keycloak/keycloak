package org.keycloak.test.migration;

public class AddKeycloakIntegrationTestRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        addImport("org.keycloak.testframework.annotations.KeycloakIntegrationTest");

        int classDeclaration = findClassDeclaration();
        content.add(classDeclaration, "@KeycloakIntegrationTest");

        info(classDeclaration,"Added @KeycloakIntegrationTest");
    }

}
