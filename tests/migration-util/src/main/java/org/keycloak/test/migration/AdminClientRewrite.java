package org.keycloak.test.migration;

public class AdminClientRewrite extends TestRewrite {

    private static final String REPLACEMENT = "managedRealm.admin()";
    private static final String REGEX_REALM = "adminClient\\.realm\\(\"?[a-zA-Z_-]*\"?\\)";
    private static final String REGEX_REALMS = "adminClient\\.realms\\(\\)\\.realm\\(\"?[a-zA-Z_-]*\"?\\)";

    @Override
    public void rewrite() {
        int startingLine = findClassDeclaration();

        for (int i = startingLine; i < content.size(); i++) {
            String l = content.get(i);
            if (l.contains("adminClient.realm(")) {
                replaceLine(i, l.replaceAll(REGEX_REALM, REPLACEMENT));
                info(i + 1, "Data rewritten: '" + REGEX_REALM + "' --> '" + REPLACEMENT + "'");
            }
            if (l.contains("adminClient.realms()")) {
                replaceLine(i, l.replaceAll(REGEX_REALMS, REPLACEMENT));
                info(i + 1, "Data rewritten: '" + REGEX_REALMS + "' --> '" + REPLACEMENT + "'");
            }
        }
    }
}
