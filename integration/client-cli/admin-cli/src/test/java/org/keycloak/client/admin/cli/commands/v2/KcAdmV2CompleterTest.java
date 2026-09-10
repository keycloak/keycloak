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
    public void testEmptyInputShowsResourceGroupsAndConnectionOptions() {
        List<String> candidates = complete("");
        assertTrue("Should suggest 'client', but found: " + candidates, candidates.contains("client"));
        assertTrue("Should suggest '-r' (connection option), but found: " + candidates, candidates.contains("-r"));
    }

    @Test
    public void testRootDashSuggestsConnectionOptions() {
        List<String> candidates = complete("--");
        assertTrue("Should suggest '--config', but found: " + candidates, candidates.contains("--config"));
        assertTrue("Should suggest '--server', but found: " + candidates, candidates.contains("--server"));
        assertTrue("Should suggest '--realm', but found: " + candidates, candidates.contains("--realm"));
        assertTrue("Should suggest '--token', but found: " + candidates, candidates.contains("--token"));
        assertTrue("Should suggest '--user', but found: " + candidates, candidates.contains("--user"));
        assertTrue("Should suggest '--password', but found: " + candidates, candidates.contains("--password"));
        assertTrue("Should suggest '--secret', but found: " + candidates, candidates.contains("--secret"));
        assertTrue("Should suggest '--no-config', but found: " + candidates, candidates.contains("--no-config"));
        assertTrue("Should suggest '--truststore', but found: " + candidates, candidates.contains("--truststore"));
        assertTrue("Should suggest '--trustpass', but found: " + candidates, candidates.contains("--trustpass"));
        assertTrue("Should suggest '--insecure', but found: " + candidates, candidates.contains("--insecure"));
        assertTrue("Should suggest '--keystore', but found: " + candidates, candidates.contains("--keystore"));
        assertTrue("Should suggest '--storepass', but found: " + candidates, candidates.contains("--storepass"));
        assertTrue("Should suggest '--keypass', but found: " + candidates, candidates.contains("--keypass"));
        assertTrue("Should suggest '--alias', but found: " + candidates, candidates.contains("--alias"));
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
        assertTrue("Should suggest 'apply'", candidates.contains("apply"));
        assertTrue("Should suggest 'delete'", candidates.contains("delete"));
        assertTrue("Should suggest 'edit'", candidates.contains("edit"));
    }

    @Test
    public void testPartialCommand() {
        List<String> candidates = complete("client", "l");
        assertTrue("Should match 'list'", candidates.contains("list"));
        assertSubcommandsDoNotContain(candidates);
    }

    @Test
    public void testLeafDashSuggestsOnlyLeafOptions() {
        List<String> candidates = complete("client", "list", "--");
        assertTrue("Should suggest '--compressed', but found: " + candidates, candidates.contains("--compressed"));
        assertFalse("Should not suggest '--config' (connection option), but found: " + candidates, candidates.contains("--config"));
        assertFalse("Should not suggest '--truststore' (connection option), but found: " + candidates, candidates.contains("--truststore"));
        assertFalse("Should not suggest '--trustpass' (connection option), but found: " + candidates, candidates.contains("--trustpass"));
        assertFalse("Should not suggest '--insecure' (connection option), but found: " + candidates, candidates.contains("--insecure"));
        assertFalse("Should not suggest '--password' (connection option), but found: " + candidates, candidates.contains("--password"));
        assertFalse("Should not suggest '--secret' (connection option), but found: " + candidates, candidates.contains("--secret"));
        assertFalse("Should not suggest '--no-config' (connection option), but found: " + candidates, candidates.contains("--no-config"));
        assertFalse("Should not suggest '--keystore' (connection option), but found: " + candidates, candidates.contains("--keystore"));
        assertFalse("Should not suggest '--storepass' (connection option), but found: " + candidates, candidates.contains("--storepass"));
        assertFalse("Should not suggest '--keypass' (connection option), but found: " + candidates, candidates.contains("--keypass"));
        assertFalse("Should not suggest '--alias' (connection option), but found: " + candidates, candidates.contains("--alias"));
    }

    @Test
    public void testDoubleDashSuggestsLongOptionsForVariant() {
        List<String> candidates = complete("client", "create", "oidc", "--");
        assertFalse("Should not suggest '--client-id' (use positional)", candidates.contains("--client-id"));
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
        assertFalse("Should not suggest '--uuid' on create: " + candidates, candidates.contains("--uuid"));
    }

    @Test
    public void testSamlOptionsInAutocompleteForCreateSaml() {
        List<String> candidates = complete("client", "create", "saml", "--");
        assertTrue("Should suggest '--sign-documents'", candidates.contains("--sign-documents"));
        assertFalse("Should not suggest '--login-flows'", candidates.contains("--login-flows"));
    }

    @Test
    public void testFileOptionInAutocompleteForCreateParent() {
        List<String> candidates = complete("client", "create", "-");
        assertTrue("Should suggest '-f' at parent level", candidates.contains("-f"));
    }

    @Test
    public void testFieldOptionsNotInAutocompleteForCreateParent() {
        List<String> candidates = complete("client", "create", "--");
        assertFalse("Should not suggest '--client-id' at parent level", candidates.contains("--client-id"));
        assertFalse("Should not suggest '--login-flows' at parent level", candidates.contains("--login-flows"));
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
    public void testApplyVariantsInAutocomplete() {
        List<String> candidates = complete("client", "apply", "");
        assertTrue("Should suggest 'oidc'", candidates.contains("oidc"));
        assertTrue("Should suggest 'saml'", candidates.contains("saml"));
    }

    @Test
    public void testApplyOidcOptionsInAutocomplete() {
        List<String> candidates = complete("client", "apply", "oidc", "--");
        assertFalse("Should not suggest '--client-id' (use positional)", candidates.contains("--client-id"));
        assertTrue("Should suggest '--login-flows'", candidates.contains("--login-flows"));
        assertTrue("Should suggest '--compressed'", candidates.contains("--compressed"));
        assertFalse("Should not suggest '--sign-documents'", candidates.contains("--sign-documents"));
        assertFalse("Should not suggest '--uuid' on apply: " + candidates, candidates.contains("--uuid"));
    }

    @Test
    public void testEditOptionsInAutocomplete() {
        List<String> candidates = complete("client", "edit", "--");
        assertTrue("Should suggest '--compressed', but found: " + candidates, candidates.contains("--compressed"));
        assertFalse("Should not suggest '--config' (connection option), but found: " + candidates, candidates.contains("--config"));
        assertFalse("Should not suggest '--file', but found: " + candidates, candidates.contains("--file"));
        assertFalse("Should not suggest '--client-id', but found: " + candidates, candidates.contains("--client-id"));
    }

    @Test
    public void testConfigShowsSubcommands() {
        List<String> candidates = complete("config", "");
        assertTrue("Should suggest 'credentials'", candidates.contains("credentials"));
        assertTrue("Should suggest 'openapi'", candidates.contains("openapi"));
        assertTrue("Should suggest 'editor'", candidates.contains("editor"));
    }

    @Test
    public void testConfigEditorOptionsInAutocomplete() {
        List<String> candidates = complete("config", "editor", "--");
        assertTrue("Should suggest '--config': " + candidates, candidates.contains("--config"));
        assertTrue("Should suggest '--help': " + candidates, candidates.contains("--help"));
    }

    @Test
    public void testConfigOpenApiOptionsInAutocomplete() {
        List<String> candidates = complete("config", "openapi", "--");
        assertTrue("Should suggest '--config': " + candidates, candidates.contains("--config"));
        assertTrue("Should suggest '--help': " + candidates, candidates.contains("--help"));
    }

    @Test
    public void testConfigCredentialsShowsOpenApiUrlOption() {
        List<String> candidates = complete("config", "credentials", "--");
        assertTrue("Should suggest '--openapi-url': " + candidates, candidates.contains("--openapi-url"));
        assertTrue("Should suggest '--server': " + candidates, candidates.contains("--server"));
        assertTrue("Should suggest '--realm': " + candidates, candidates.contains("--realm"));
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
        for (String name : new String[]{"create", "get", "patch", "apply", "delete"}) {
            if (candidates.contains(name)) {
                throw new AssertionError("Should not contain '" + name + "' but did");
            }
        }
    }
}
