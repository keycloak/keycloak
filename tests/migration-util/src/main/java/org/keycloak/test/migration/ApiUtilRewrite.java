package org.keycloak.test.migration;

public class ApiUtilRewrite extends TestRewrite {

    private static final String TARGET = "ApiUtil";
    private static final String REPLACEMENT = "AdminApiUtil";

    @Override
    public void rewrite() {
        int startingLine = findClassDeclaration();
        boolean hasAdminApiUtil = false;

        for (int i = startingLine; i < content.size(); i++) {
            String l = content.get(i);
            if (l.contains(TARGET) && !l.contains("ApiUtil.getCreateId")) {
                replaceLine(i, l.replace(TARGET, REPLACEMENT));
                hasAdminApiUtil = true;
                info(i + 1, "Data rewritten: '" + TARGET + "' --> '" + REPLACEMENT + "'");
            }
        }
        if (hasAdminApiUtil) {
            addImport("org.keycloak.tests.utils.AdminApiUtil");
        }
    }
}
