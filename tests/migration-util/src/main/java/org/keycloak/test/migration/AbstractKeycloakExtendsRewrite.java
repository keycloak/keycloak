package org.keycloak.test.migration;

public class AbstractKeycloakExtendsRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int abstractImport = findLine("import[ ]org\\.keycloak\\.testsuite\\.AbstractKeycloakTest;");
        int abstractTestRealmImport = findLine("import[ ]org\\.keycloak\\.testsuite\\.AbstractTestRealmKeycloakTest;");

        if (abstractImport >= 0 || abstractTestRealmImport >= 0) {
            for (int i = 0; i < content.size(); i++) {
                String l = content.get(i);
                if (l.contains(" extends AbstractKeycloakTest")) {
                    replaceLine(i, l.replace(" extends AbstractKeycloakTest", ""));
                    content.remove(abstractImport);
                    info(i, "Statement removed: 'AbstractKeycloakTest'");
                    break;
                }

                if (l.contains(" extends AbstractTestRealmKeycloakTest")) {
                    replaceLine(i, l.replace(" extends AbstractTestRealmKeycloakTest", ""));
                    content.remove(abstractTestRealmImport);
                    info(i, "Statement removed: 'AbstractTestRealmKeycloakTest'");
                    break;
                }
            }
        }
    }

}
