package org.keycloak.test.migration;

import java.util.Map;

public class MailServerRewrite extends TestRewrite {

    private static final Map<String, String> STATEMENTS = Map.of(
            "greenMail.", "mailServer.",
            "greenMailRule.", "mailServer."
    );

    @Override
    public void rewrite() {
        boolean mailServer = findLine(".*greenMail.*") != -1;

        if (mailServer) {
            addImport("org.keycloak.testframework.mail.annotations.InjectMailServer");
            addImport("org.keycloak.testframework.mail.MailServer");

            int managedRealm = findLine("    ManagedRealm managedRealm;");

            content.add(managedRealm + 1, "");
            content.add(managedRealm + 2, "    @InjectMailServer");
            content.add(managedRealm + 3, "    MailServer mailServer;");

            info(managedRealm + 2, "Injecting: MailServer");

            int startingLine = findClassDeclaration();
            for (int i = startingLine; i < content.size(); i++) {
                String l = content.get(i);
                for (Map.Entry<String, String> entry : STATEMENTS.entrySet()) {
                    if (l.contains(entry.getKey())) {
                        replaceLine(i, l.replace(entry.getKey(), entry.getValue()));
                        info(i + 1, "Data rewritten: '" + entry.getKey() + "' --> '" + entry.getValue() + "'");
                    }
                }
            }
        }
    }

}
