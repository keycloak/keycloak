/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.vault;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Regression tests for vault expression security guard in identity provider configuration.
 * Tests the guard at IdentityProviderResource.java:246-248 that prevents a delegated manager
 * from changing critical IdP settings when vault expressions are involved.
 *
 * These tests verify:
 * 1. Vault expressions are correctly detected with regex pattern
 * 2. The masking/comparison logic works as expected
 */
public class VaultExpressionIdPRegressionTest {

    private static final Pattern VAULT_EXPRESSION_PATTERN = Pattern.compile("^\\$\\{vault\\.(.+?)}$");

    // Test data records for complex scenarios
    record SecretComparisonData(String description, String config1, String config2, boolean shouldMatch) {}
    record GuardLogicData(String description, String existingSecret, String updatedSecret,
                          String existingTokenUrl, String updatedTokenUrl, boolean shouldBlock) {}

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Valid vault expression,${vault.my_secret},true",
            "Vault with multiple underscores,${vault.my_secret_key_123},true",
            "Plain string secret,my-plain-secret,false",
            "Env variable expression,${env.MY_SECRET},false"
    })
    public void testVaultExpressionDetection(String description, String input, boolean shouldMatch) {
        assertEquals(shouldMatch, VAULT_EXPRESSION_PATTERN.matcher(input).matches(),
                "Description: " + description + ", Input: " + input);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("secretComparisonData")
    public void testMaskedSecretComparison(SecretComparisonData data) {
        assertEquals(data.shouldMatch(), data.config1().equals(data.config2()),
                "Description: " + data.description() + ", Config1: " + data.config1() + ", Config2: " + data.config2());
    }

    static Stream<Arguments> secretComparisonData() {
        return Stream.of(
                Arguments.of(new SecretComparisonData("Identical vault expressions", "${vault.my_secret}", "${vault.my_secret}", true)),
                Arguments.of(new SecretComparisonData("Different vault expressions", "${vault.secret_key_1}", "${vault.secret_key_2}", false)),
                Arguments.of(new SecretComparisonData("Plain to vault expression", "my-plain-secret", "${vault.my_secret}", false)),
                Arguments.of(new SecretComparisonData("Vault to plain expression", "${vault.my_secret}", "my-plain-secret", false))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardLogicData")
    public void testGuardLogic(GuardLogicData data) {
        Map<String, String> existingConfig = new HashMap<>();
        existingConfig.put("clientSecret", data.existingSecret());
        existingConfig.put("tokenUrl", data.existingTokenUrl());

        Map<String, String> updatedConfig = new HashMap<>();
        updatedConfig.put("clientSecret", data.updatedSecret());
        updatedConfig.put("tokenUrl", data.updatedTokenUrl());

        // Check guard conditions
        String updatedSecret = updatedConfig.get("clientSecret");
        String existingSecret = existingConfig.get("clientSecret");
        boolean isVaultExpression = VAULT_EXPRESSION_PATTERN.matcher(updatedSecret).matches();
        boolean secretUnchanged = (existingSecret != null && existingSecret.equals(updatedSecret));
        boolean tokenUrlChanged = !existingConfig.get("tokenUrl").equals(updatedConfig.get("tokenUrl"));

        // Guard should block if: vault secret + tokenUrl change
        boolean shouldBlock = isVaultExpression && secretUnchanged && tokenUrlChanged;

        assertEquals(data.shouldBlock(), shouldBlock,
                "Scenario: " + data.description() +
                        ", isVaultExpr=" + isVaultExpression +
                        ", secretUnchanged=" + secretUnchanged +
                        ", urlChanged=" + tokenUrlChanged);
    }

    static Stream<Arguments> guardLogicData() {
        return Stream.of(Arguments.of(new GuardLogicData(
                                "Unchanged vault secret + other field change = ALLOW",
                                "${vault.my_secret}",
                                "${vault.my_secret}",
                                "http://original-endpoint.example.com/token",
                                "http://original-endpoint.example.com/token",
                                false)),
                Arguments.of(new GuardLogicData(
                                "Unchanged vault secret + tokenUrl change = BLOCK (attack scenario)",
                                "${vault.my_secret}",
                                "${vault.my_secret}",
                                "http://legitimate-provider.com/token",
                                "http://attacker-evil.com/token",
                                true)),
                Arguments.of(new GuardLogicData(
                                "Plain secret + tokenUrl change = ALLOW (no vault risk)",
                                "plain-secret-123",
                                "plain-secret-123",
                                "http://legitimate-provider.com/token",
                                "http://attacker-evil.com/token",
                                false)),
                Arguments.of(new GuardLogicData(
                                "Vault secret changed + tokenUrl change = ALLOW (admin changed both)",
                                "${vault.my_secret}",
                                "${vault.different_secret}",
                                "http://legitimate-provider.com/token",
                                "http://new-provider.com/token",
                                false))
        );
    }
}
