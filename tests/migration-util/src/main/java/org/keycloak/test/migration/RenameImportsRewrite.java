package org.keycloak.test.migration;

import java.util.Map;

public class RenameImportsRewrite extends TestRewrite {

    Map<String, String> IMPORTS = Map.of(
            "org.junit.Assert", "org.junit.jupiter.api.Assertions",
            "org.keycloak.testsuite.Assert", "org.keycloak.tests.utils.Assert",
            "org.junit.Test", "org.junit.jupiter.api.Test",
            "org.keycloak.testsuite.util.AdminEventPaths", "org.keycloak.tests.utils.admin.AdminEventPaths",
            "org.keycloak.testsuite.admin.ApiUtil", "org.keycloak.testframework.util.ApiUtil"
    );

    Map<String, String> STATIC_IMPORTS = Map.of(
            "org.junit.Assert", "org.junit.jupiter.api.Assertions"
    );

    @Override
    public void rewrite() {
        for (int i = 0; i < findClassDeclaration(); i++) {
            String l = content.get(i);

            if (l.startsWith("import static ")) {
                String current = l.substring("import static ".length(), l.lastIndexOf('.'));
                String method = l.substring(l.lastIndexOf('.'), l.length() - 1);
                String migrateTo = STATIC_IMPORTS.get(current);

                if (migrateTo != null) {
                    replaceLine(i, l.replace(current, migrateTo));

                    info(i, "Static import rewritten: '" + current + method + "' --> '" + migrateTo + method + "'");
                }
            } else if (l.startsWith("import ")) {
                String current = l.substring("import ".length(), l.length() - 1);
                String migrateTo = IMPORTS.get(current);

                if (migrateTo != null) {
                    replaceLine(i, l.replace(current, migrateTo));

                    info(i, "Import rewritten: '" + current + "' --> '" + migrateTo + "'");
                }
            }
        }
    }

}
