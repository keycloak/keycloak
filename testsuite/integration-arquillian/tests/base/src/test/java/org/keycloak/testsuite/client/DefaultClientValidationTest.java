package org.keycloak.testsuite.client;

import org.keycloak.validation.DefaultClientValidationProvider;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
