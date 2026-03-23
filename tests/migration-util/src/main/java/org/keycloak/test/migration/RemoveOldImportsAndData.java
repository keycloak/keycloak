package org.keycloak.test.migration;

public class RemoveOldImportsAndData extends TestRewrite {

    @Override
    public void rewrite() {
        removeLine("import org.junit.ClassRule;");
        removeAll("@ClassRule");
        removeLine("import org.junit.Rule;");
        removeAll("@Rule");
        removeLine("import org.jboss.arquillian.test.api.ArquillianResource;");
        removeAll("    @ArquillianResource");
        removeLine("import org.keycloak.testsuite.AssertAdminEvents;");
        removeLine("    public AssertAdminEvents assertAdminEvents \\= new AssertAdminEvents\\(this\\)");
        removeLine("import org.keycloak.testsuite.AssertEvents;");
        removeLine("    public AssertEvents events \\= new AssertEvents\\(this\\);");
        removeLine("import org.keycloak.testsuite.util.GreenMailRule;");
        removeLine("    public GreenMailRule greenMail[Rule]* \\= new GreenMailRule\\(\\);");
    }

}
