package org.keycloak.test.migration;

public class OAuthClientRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int oAuthImportLine = findLine("import org\\.keycloak\\.testsuite\\.util\\.oauth\\.OAuthClient;");
        int oAuthFieldRef = findLine(".*[ ]oauth\\..*");

        if (oAuthImportLine >= 0 || oAuthFieldRef >= 0) {
            int oAuthResourceLine = findLine(".*OAuthClient[ ]oauth;.*");

            if (oAuthResourceLine >= 0) {
                String current = content.get(oAuthImportLine);
                String migrateTo = "import org.keycloak.testframework.oauth.OAuthClient;";
                replaceLine(oAuthImportLine, migrateTo);
                info(oAuthImportLine, "Import rewritten: '" + current + "' --> '" + migrateTo + "'");

                replaceLine(oAuthResourceLine - 1, "    @InjectOAuthClient");
                info(oAuthResourceLine - 1, "@ArquillianResource rewritten to @InjectOAuthClient");
            } else if (oAuthFieldRef >= 0) {
                addImport("org.keycloak.testframework.oauth.OAuthClient");

                int managedRealm = findLine("    ManagedRealm managedRealm;");

                content.add(managedRealm + 1, "");
                content.add(managedRealm + 2, "    @InjectOAuthClient");
                content.add(managedRealm + 3, "    OAuthClient oauth;");

                info(managedRealm + 2, "Injecting: OAuthClient");
            }
            addImport("org.keycloak.testframework.oauth.annotations.InjectOAuthClient");
        }
    }

}
