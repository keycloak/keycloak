package org.keycloak.test.migration;

import java.util.LinkedList;
import java.util.List;

public class TestRealmRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int start = findLine(".*void configureTestRealm\\(RealmRepresentation testRealm\\).*");
        if (start == -1) {
            return;
        }

        String className = content.get(findClassDeclaration()).split(" ")[2];

        addImport("org.keycloak.tests.utils.LegacyRealmConfig");

        String realmConfigClassName = className.replace("Test", "RealmConfig");

        int end = findLine("[ ]*}[ ]*", start);

        List<String> configureTestMethod = new LinkedList<>();
        for (int i = start; i <= end; i++) {
            configureTestMethod.add(content.get(i));
        }

        for (int i = start - 1; i <= end; i++) {
            content.remove(start);
        }

        int lastLine = content.size() - 1;

        List<String> realmConfig = new LinkedList<>();
        realmConfig.add("");
        realmConfig.add("    private static class " + realmConfigClassName + " extends LegacyRealmConfig {");
        realmConfig.add("");
        configureTestMethod.forEach(l -> realmConfig.add("    " + l));
        realmConfig.add("    }");

        insertContent(lastLine, realmConfig);

        int injectRealmLine = findLine(".*@InjectRealm");
        replaceLine(injectRealmLine, content.get(injectRealmLine) + "(config = " + realmConfigClassName + ".class)");
    }

}
