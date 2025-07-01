package org.keycloak.test.migration;

import java.util.Map;

public class CommonStatementsRewrite extends TestRewrite {

   private final Map<String, String> STATEMENTS = Map.of(
           "testRealmResource()", "managedRealm.admin()"
   );

    @Override
    public void rewrite() {
        for (int i = 0; i < content.size(); i++) {
            String l = content.get(i);
            for (Map.Entry<String, String> entry : STATEMENTS.entrySet()) {
                if (l.contains(entry.getKey())) {
                    replaceLine(i, l.replace(entry.getKey(), entry.getValue()));
                    info(i, "Statement rewritten: '" + entry.getKey() + "' --> '" + entry.getValue() + "'");
                }
            }
        }
    }
}
