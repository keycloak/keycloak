package org.keycloak.test.migration;

public class PageObjectRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int pageLine = findLine("import org\\.jboss\\.arquillian\\.graphene\\.page\\.Page;");
        int secondBrowserLine = findLine("import org\\.keycloak\\.testsuite\\.util\\.SecondBrowser;");

        if (pageLine >= 0) {
            String current = content.get(pageLine);
            String migrateTo = "import org.keycloak.testframework.ui.annotations.InjectPage;";
            replaceLine(pageLine, migrateTo);
            info(pageLine, "Import rewritten: '" + current + "' --> '" + migrateTo + "'");

            for (int i = 0; i < content.size(); i++) {
                String n = content.get(i);
                if (n.trim().equals("@Page")) {
                    content.remove(i);
                    if (!content.get(i).trim().equals("@SecondBrowser")) {
                        content.add(i, n.replace("@Page", "@InjectPage"));
                    } else {
                        content.add(i, n.replace("@Page", "@InjectPage(ref = \"secondDriver\", webDriverRef = \"secondDriver\")"));
                        content.remove(i + 1);
                    }
                    info(i, "@Page rewritten to @InjectPage");
                }
                if (n.trim().contains("org.keycloak.testsuite.pages")) {
                    content.remove(i);
                    content.add(i, n.replaceAll("testsuite\\.pages", "testframework.ui.page"));
                    info(i, "Page imports rewritten to org.keycloak.testframework.ui.page");
                }
                if (n.trim().contains("org.keycloak.testsuite.webauthn.pages")) {
                    content.remove(i);
                    content.add(i, n.replaceAll("testsuite\\.webauthn\\.pages", "tests.webauthn.page"));
                    info(i, "Page imports rewritten to org.keycloak.testframework.ui.page");
                }
            }
        }
    }

}
