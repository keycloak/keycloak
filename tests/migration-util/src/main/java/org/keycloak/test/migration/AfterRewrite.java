package org.keycloak.test.migration;

public class AfterRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int afterLine = findLine("import org\\.junit\\.After;");
        if (afterLine >= 0) {
            String current = content.get(afterLine);
            String migrateTo = "import org.junit.jupiter.api.AfterEach;";
            replaceLine(afterLine, migrateTo);
            info(afterLine, "Import rewritten: '" + current + "' --> '" + migrateTo + "'");

            for (int i = 0; i < content.size(); i++) {
                String n = content.get(i);
                if (n.trim().equals("@After")) {
                    content.remove(i);
                    content.add(i, n.replace("@After", "@AfterEach"));
                    info(i, "@After rewritten to @AfterEach");
                }
            }
        }
    }

}
