package org.keycloak.test.migration;

public class BeforeRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int beforeLine = findLine("import org\\.junit\\.Before;");
        if (beforeLine >= 0) {
            String current = content.get(beforeLine);
            String migrateTo = "import org.junit.jupiter.api.BeforeEach;";
            replaceLine(beforeLine, migrateTo);
            info(beforeLine, "Import rewritten: '" + current + "' --> '" + migrateTo + "'");

            for (int i = 0; i < content.size(); i++) {
                String n = content.get(i);
                if (n.trim().equals("@Before")) {
                    content.remove(i);
                    content.add(i, n.replace("@Before", "@BeforeEach"));
                    info(i, "@Before rewritten to @BeforeEach");
                }
            }
        }
    }

}
