package org.keycloak.client.admin.cli.commands.v2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.keycloak.client.admin.cli.v2.KcAdmV2Completer;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KcAdmV2CompleterTest {

    @Test
    public void testEmptyInputShowsResourceGroups() {
        List<String> candidates = complete("");
        assertTrue("Should suggest 'client'", candidates.contains("client"));
    }

    @Test
    public void testPartialResourceName() {
        List<String> candidates = complete("cl");
        assertTrue("Should match 'client'", candidates.contains("client"));
    }

    @Test
    public void testNoMatch() {
        List<String> candidates = complete("xyz");
        assertNoSubcommands(candidates);
    }

    @Test
    public void testResourceGroupShowsCommands() {
        List<String> candidates = complete("client", "");
        assertTrue("Should suggest 'list'", candidates.contains("list"));
        assertTrue("Should suggest 'create'", candidates.contains("create"));
        assertTrue("Should suggest 'get'", candidates.contains("get"));
        assertTrue("Should suggest 'patch'", candidates.contains("patch"));
        assertTrue("Should suggest 'update'", candidates.contains("update"));
        assertTrue("Should suggest 'delete'", candidates.contains("delete"));
    }

    @Test
    public void testPartialCommand() {
        List<String> candidates = complete("client", "l");
        assertTrue("Should match 'list'", candidates.contains("list"));
        assertSubcommandsDoNotContain(candidates);
    }

    @Test
    public void testDashSuggestsOptions() {
        List<String> candidates = complete("client", "list", "--");
        assertTrue("Should suggest '--config'", candidates.contains("--config"));
        assertTrue("Should suggest '--compressed'", candidates.contains("--compressed"));
        assertTrue("Should suggest '--truststore'", candidates.contains("--truststore"));
        assertTrue("Should suggest '--trustpass'", candidates.contains("--trustpass"));
        assertTrue("Should suggest '--insecure'", candidates.contains("--insecure"));
        assertTrue("Should suggest '--password'", candidates.contains("--password"));
        assertTrue("Should suggest '--secret'", candidates.contains("--secret"));
        assertTrue("Should suggest '--no-config'", candidates.contains("--no-config"));
        assertTrue("Should suggest '--keystore'", candidates.contains("--keystore"));
        assertTrue("Should suggest '--storepass'", candidates.contains("--storepass"));
        assertTrue("Should suggest '--keypass'", candidates.contains("--keypass"));
        assertTrue("Should suggest '--alias'", candidates.contains("--alias"));
    }

    @Test
    public void testDoubleDashSuggestsLongOptionsForVariant() {
        List<String> candidates = complete("client", "create", "oidc", "--");
        assertTrue("Should suggest '--client-id'", candidates.contains("--client-id"));
        assertTrue("Should suggest '--login-flows'", candidates.contains("--login-flows"));
        assertTrue("Should suggest '--help'", candidates.contains("--help"));
        assertFalse("Should not suggest short options", candidates.contains("-f"));
    }

    @Test
    public void testProtocolVariantsInAutocompleteForCreate() {
        List<String> candidates = complete("client", "create", "");
        assertTrue("Should suggest 'oidc'", candidates.contains("oidc"));
        assertTrue("Should suggest 'saml'", candidates.contains("saml"));
    }

    @Test
    public void testFileOptionInAutocompleteForCreateOidc() {
        List<String> candidates = complete("client", "create", "oidc", "-");
        assertTrue("Should suggest '-f'", candidates.contains("-f"));
    }

    @Test
    public void testFlattenedAuthOptionsInAutocomplete() {
        List<String> candidates = complete("client", "create", "oidc", "--auth");
        assertTrue("Should suggest '--auth-method'", candidates.contains("--auth-method"));
        assertTrue("Should suggest '--auth-secret'", candidates.contains("--auth-secret"));
        assertTrue("Should suggest '--auth-certificate'", candidates.contains("--auth-certificate"));
    }

    @Test
    public void testOidcOptionsInAutocompleteForCreateOidc() {
        List<String> candidates = complete("client", "create", "oidc", "--");
        assertTrue("Should suggest '--login-flows'", candidates.contains("--login-flows"));
        assertTrue("Should suggest '--web-origins'", candidates.contains("--web-origins"));
        assertTrue("Should suggest '--auth-method'", candidates.contains("--auth-method"));
        assertFalse("Should not suggest '--sign-documents'", candidates.contains("--sign-documents"));
    }

    @Test
    public void testSamlOptionsInAutocompleteForCreateSaml() {
        List<String> candidates = complete("client", "create", "saml", "--");
        assertTrue("Should suggest '--sign-documents'", candidates.contains("--sign-documents"));
        assertFalse("Should not suggest '--login-flows'", candidates.contains("--login-flows"));
    }

    @Test
    public void testFileOptionNotInAutocompleteForList() {
        List<String> candidates = complete("client", "list", "-");
        assertFalse("list should not suggest '-f'", candidates.contains("-f"));
    }

    @Test
    public void testOutputOptionsNotInAutocompleteForDelete() {
        List<String> candidates = complete("client", "delete", "--");
        assertFalse("delete should not suggest '--compressed'", candidates.contains("--compressed"));
    }

    @Test
    public void testUpdateVariantsInAutocomplete() {
        List<String> candidates = complete("client", "update", "");
        assertTrue("Should suggest 'oidc'", candidates.contains("oidc"));
        assertTrue("Should suggest 'saml'", candidates.contains("saml"));
    }

    @Test
    public void testUpdateOidcOptionsInAutocomplete() {
        List<String> candidates = complete("client", "update", "oidc", "--");
        assertTrue("Should suggest '--client-id'", candidates.contains("--client-id"));
        assertTrue("Should suggest '--login-flows'", candidates.contains("--login-flows"));
        assertTrue("Should suggest '--compressed'", candidates.contains("--compressed"));
        assertFalse("Should not suggest '--sign-documents'", candidates.contains("--sign-documents"));
    }

    @Test
    public void testUnknownSubcommandStaysAtCurrentLevel() {
        List<String> candidates = complete("client", "nonexistent", "");
        assertTrue("Should still suggest commands under 'client'", candidates.contains("list"));
    }

    private List<String> complete(String... args) {
        StringWriter sw = new StringWriter();
        KcAdmV2Completer.complete(args, new PrintWriter(sw));
        String output = sw.toString().trim();
        if (output.isEmpty()) {
            return List.of();
        }
        return List.of(output.split(System.lineSeparator()));
    }

    private void assertNoSubcommands(List<String> candidates) {
        for (String c : candidates) {
            if (!c.startsWith("-")) {
                throw new AssertionError("Expected no subcommand candidates but found: " + c);
            }
        }
    }

    private void assertSubcommandsDoNotContain(List<String> candidates) {
        for (String name : new String[]{"create", "get", "patch", "update", "delete"}) {
            if (candidates.contains(name)) {
                throw new AssertionError("Should not contain '" + name + "' but did");
            }
        }
    }
}
