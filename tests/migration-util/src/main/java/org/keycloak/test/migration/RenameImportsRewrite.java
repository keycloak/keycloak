package org.keycloak.test.migration;

import java.util.Map;

public class RenameImportsRewrite extends TestRewrite {

    Map<String, String> IMPORTS = Map.of(
            "org.junit.Assert", "org.junit.jupiter.api.Assertions",
            "org.junit.Test", "org.junit.jupiter.api.Test",
            "org.keycloak.testsuite.util.AdminEventPaths", "org.keycloak.tests.utils.admin.AdminEventPaths",
            "org.keycloak.testsuite.admin.ApiUtil", "org.keycloak.testframework.util.ApiUtil"
    );

    @Override
    public void rewrite() {
        for (int i = 0; i < findClassDeclaration(); i++) {
            String l = content.get(i);
            if (l.startsWith("import ")) {
                String current = l.substring("import ".length(), l.length() - 1);
                String migrateTo = IMPORTS.get(current);
                if (migrateTo != null) {
                    replaceLine(i, "import " + migrateTo + ";");

                    info(i, "Import rewritten: '" + current + "' --> '" + migrateTo + "'");
                }
            }
        }
    }

}
