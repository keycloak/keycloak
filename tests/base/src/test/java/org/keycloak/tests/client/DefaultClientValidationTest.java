package org.keycloak.tests.client;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.validation.DefaultClientValidationProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class DefaultClientValidationTest {

    @Test
    public void that_checkCurlyBracketsBalanced_worksCorrectly() {
        String urlWithCurlyBrackets1="http://{test}/prova123";
        String urlWithCurlyBrackets2="http://{test}/{prova123}";
        String urlWithCurlyBrackets3="http://{{test}/{prova123}}";
        assertTrue(DefaultClientValidationProvider.checkCurlyBracketsBalanced(urlWithCurlyBrackets1));
        assertTrue(DefaultClientValidationProvider.checkCurlyBracketsBalanced(urlWithCurlyBrackets2));
        assertTrue(DefaultClientValidationProvider.checkCurlyBracketsBalanced(urlWithCurlyBrackets3));
    }
    @Test
    public void that_checkCurlyBracketsBalanced_notWorksCorrectly() {
        String urlWithImproperlyCurlyBrackets="http://}test}/prova123";
        String urlWithImproperlyCurlyBrackets1="http://{test}/prova123}";
        assertFalse(DefaultClientValidationProvider.checkCurlyBracketsBalanced(urlWithImproperlyCurlyBrackets));
        assertFalse(DefaultClientValidationProvider.checkCurlyBracketsBalanced(urlWithImproperlyCurlyBrackets1));
    }
}
