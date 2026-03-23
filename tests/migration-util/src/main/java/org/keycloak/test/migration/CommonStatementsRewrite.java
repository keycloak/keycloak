package org.keycloak.test.migration;

import java.util.Map;

public class CommonStatementsRewrite extends TestRewrite {

    private static final Map<String, String> STATEMENTS = Map.of(
           "testRealmResource()", "managedRealm.admin()",
           "testRealm()", "managedRealm.admin()",
           "adminClient.realm", "managedRealm.admin()",
           "adminClient.realms()", "managedRealm.admin()",
           "realmsResouce().realm", "managedRealm.admin()",
           "greenMail", "mailServer",
           "greenMailRule", "mailServer",
           "driver", "managedDriver.driver()",
           "driver2", "managedDriver2.driver()"
    );

    private static final Map<String, String> UTILS = Map.of(
            "ApiUtil", "AdminApiUtil"
    );

    private static final String REGEX_REALM = "adminClient\\.realm\\([a-zA-Z\"_-]*\\)";
    private static final String REGEX_REALMS = "adminClient\\.realms\\(\\)\\.realm\\([a-zA-Z\"_-]*\\)";
    private static final String REGEX_RESOURCE = "realmsResouce\\(\\)\\.realm\\([a-zA-Z\"_-]*\\)"; //realmsResouce() is a typo in the AbstractKeycloakTest class

    @Override
    public void rewrite() {
        int startingLine = findClassDeclaration();
        contentLoop(startingLine, STATEMENTS);
        contentLoop(startingLine, UTILS);
    }

    private void contentLoop(int startingLine, Map<String, String> dataMap) {
        for (int i = startingLine; i < content.size(); i++) {
            String l = content.get(i);
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                if (l.contains(entry.getKey())) {
                    if (!entry.getKey().contains(".realm")) {
                        replaceLine(i, l.replace(entry.getKey(), entry.getValue()));
                    } else if (entry.getKey().equals("realmsResouce().realm")) {
                        replaceLine(i, l.replaceAll(REGEX_RESOURCE, entry.getValue()));
                    } else {
                        replaceLine(i, l.replaceAll(entry.getKey().equals("adminClient.realm") ? REGEX_REALM : REGEX_REALMS, entry.getValue()));
                    }
                    info(i + 1, "Data rewritten: '" + entry.getKey() + "' --> '" + entry.getValue() + "'");
                }
            }
        }
    }
}
