package org.keycloak.test.migration;

import java.util.Map;

public class CommonStatementsRewrite extends TestRewrite {

    private static final Map<String, String> STATEMENTS = Map.of(
        "testRealm()", "managedRealm.admin()",
        "realmsResouce().realm", "managedRealm.admin()"
    );

    private static final String REGEX_RESOURCE = "realmsResouce\\(\\)\\.realm\\(\"?[a-zA-Z_-]*\"?\\)"; //realmsResouce() is a typo in the AbstractKeycloakTest class

    @Override
    public void rewrite() {
        int startingLine = findClassDeclaration();

        for (int i = startingLine; i < content.size(); i++) {
            String l = content.get(i);
            for (Map.Entry<String, String> entry : STATEMENTS.entrySet()) {
                if (l.contains(entry.getKey())) {
                    if (!entry.getKey().equals("realmsResouce().realm")) {
                        replaceLine(i, l.replace(entry.getKey(), entry.getValue()));
                    } else {
                        replaceLine(i, l.replaceAll(REGEX_RESOURCE, entry.getValue()));
                    }
                    info(i + 1, "Data rewritten: '" + entry.getKey() + "' --> '" + entry.getValue() + "'");
                }
            }
        }
    }
}
