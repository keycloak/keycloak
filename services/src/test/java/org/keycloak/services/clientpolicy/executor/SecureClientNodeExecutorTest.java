/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.executor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientNodeRegistrationContext;
import org.keycloak.services.clientpolicy.executor.SecureClientNodeExecutor.Configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SecureClientNodeExecutorTest {

    private static final String PATTERN_INTERNAL = "^app-node-\\d+\\.internal\\.example\\.com$";
    private static final String PATTERN_LEGACY   = "^legacy\\.prod\\.example\\.com$";

    private SecureClientNodeExecutor executor;

    @BeforeEach
    public void setUp() {
        executor = new SecureClientNodeExecutor(null);
    }

    @Test
    public void registerNode_validHostname_singlePattern_passes() throws ClientPolicyException {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterPasses("app-node-1.internal.example.com");
    }

    @Test
    public void registerNode_validHostname_matchesSecondOfTwoPatterns_passes() throws ClientPolicyException {
        configure(List.of(PATTERN_INTERNAL, PATTERN_LEGACY));
        assertRegisterPasses("legacy.prod.example.com");
    }

    @Test
    public void registerNode_hostnameNotMatchingAnyPattern_throws() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("evil.attacker.com", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not permitted by policy.");
    }

    @Test
    public void registerNode_internalIpAddress_throws() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("169.254.169.254", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not permitted by policy.");
    }

    @Test
    public void registerNode_emptyHostname_throws() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is empty.");
    }

    @Test
    public void registerNode_nullHostname_throws() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows(null, OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is empty.");
    }

    @Test
    public void registerNode_noPatternsConfigured_failsClosed() {
        configure(Collections.emptyList());
        assertRegisterThrows("app-node-1.internal.example.com", OAuthErrorException.INVALID_REQUEST,
                "No valid hostname patterns configured in the executor. Node registration is blocked by policy.");
    }

    @Test
    public void registerNode_allPatternsInvalid_failsClosed() {
        configure(List.of("[invalid", "(unclosed"));
        assertRegisterThrows("app-node-1.internal.example.com", OAuthErrorException.INVALID_REQUEST,
                "No valid hostname patterns configured in the executor. Node registration is blocked by policy.");
    }

    @Test
    public void registerNode_oneValidOneMalformedPattern_validPatternStillEvaluated() throws ClientPolicyException {
        configure(List.of("[bad", PATTERN_INTERNAL));
        assertRegisterPasses("app-node-1.internal.example.com");
    }

    @Test
    public void registerNode_subdomainPrefixBypassAttempt_rejected() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("evil.app-node-1.internal.example.com", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not permitted by policy.");
    }

    @Test
    void registerNode_portSuffixedHostname_rejectedWhenPatternExcludesPort() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("app-node-1.internal.example.com:8443", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname must not include a port number. "
                        + "Supply a bare hostname or IP address only "
                        + "(e.g. 'app.example.com', '192.0.2.1', or '2001:db8::1').");
    }

    @Test
    public void registerNode_overLengthPattern_treatedAsInvalid_failsClosed() {
        String tooLong = "a".repeat(SecureClientNodeExecutorFactory.HOSTNAME_REGEX_MAX_LENGTH + 1);
        configure(List.of(tooLong));
        assertRegisterThrows("aaa", OAuthErrorException.INVALID_REQUEST,
                "No valid hostname patterns configured in the executor. Node registration is blocked by policy.");
    }

    @Test
    public void registerNode_mixedCaseHostname_notFoldedToLowercase_rejectedByLowercasePattern() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("App-Node-1.Internal.Example.Com", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not permitted by policy.");
    }

    @Test
    public void registerNode_mixedCaseHostname_matchesMixedCasePattern() throws ClientPolicyException {
        configure(List.of("^App-Node-\\d+\\.Internal\\.Example\\.Com$"));
        assertRegisterPasses("App-Node-1.Internal.Example.Com");
    }

    @Test
    public void registerNode_whitespaceOnlyHostname_treatedAsBlank_throws() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("   ", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is empty.");
    }

    @Test
    public void registerNode_portSuffixedHostname_rejectedEvenWhenPatternIncludesPort() {
        configure(List.of("^app-node-\\d+\\.internal\\.example\\.com:\\d+$"));
        assertRegisterThrows("app-node-1.internal.example.com:8443", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname must not include a port number. "
                        + "Supply a bare hostname or IP address only "
                        + "(e.g. 'app.example.com', '192.0.2.1', or '2001:db8::1').");
    }

    @Test
    public void registerNode_bracketedIpv6_matchesUnbracketedPattern_passes() throws ClientPolicyException {
        configure(List.of("^::1$"));
        assertRegisterPasses("[::1]");
    }

    @Test
    public void registerNode_bareIpv6Address_rejected() {
        configure(List.of("^::1$"));
        assertRegisterThrows("::1", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not a valid DNS name or IP address.");
    }

    @Test
    public void registerNode_bracketedIpv6_patternWithBrackets_rejectedByPolicy() {
        configure(List.of("^\\[::1\\]$"));
        assertRegisterThrows("[::1]", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not permitted by policy.");
    }

    @Test
    public void registerNode_bracketedIpv6WithPort_rejectedAsContainsPort() {
        configure(List.of("^::1$"));
        assertRegisterThrows("[::1]:8443", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname must not include a port number. "
                        + "Supply a bare hostname or IP address only "
                        + "(e.g. 'app.example.com', '192.0.2.1', or '2001:db8::1').");
    }

    @Test
    public void registerNode_malformedBracketedHostname_rejectedAsInvalid() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("[malformed", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not a valid DNS name or IP address.");
    }

    @Test
    public void registerNode_uriFragmentBypass_rejected() {
        // SSRF bypass attempt: "169.254.169.254#node.example.com" the URI parser
        // extracts "169.254.169.254" as the host (fragment is stripped), so parsedHost
        // does not equal nodeHost and the round-trip check rejects it.
        configure(List.of("^.*\\.example\\.com$"));
        assertRegisterThrows("169.254.169.254#node.example.com", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname contains URI structural characters and is not permitted.");
    }

    @Test
    public void registerNode_uriPathBypass_rejected() {
        // "evil.attacker/node.example.com" the URI parser extracts "evil.attacker"
        // as the host (path component is stripped), so the round-trip check rejects it.
        configure(List.of("^.*\\.example\\.com$"));
        assertRegisterThrows("evil.attacker/node.example.com", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname contains URI structural characters and is not permitted.");
    }

    @Test
    public void registerNode_userinfoBypass_rejected() {
        configure(List.of("^.*\\.example\\.com$"));
        assertRegisterThrows("169.254.169.254@node.example.com", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname contains URI structural characters and is not permitted.");
    }

    @Test
    public void registerNode_leadingWhitespace_rejected() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows(" app-node-1.internal.example.com", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname must not contain leading or trailing whitespace.");
    }

    @Test
    public void registerNode_trailingWhitespace_rejected() {
        configure(List.of(PATTERN_INTERNAL));
        assertRegisterThrows("app-node-1.internal.example.com ", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname must not contain leading or trailing whitespace.");
    }

    @Test
    public void registerNode_ipv4Address_matchesPattern_passes() throws ClientPolicyException {
        configure(List.of("^192\\.168\\.1\\.\\d+$"));
        assertRegisterPasses("192.168.1.10");
    }

    @Test
    public void registerNode_ipv4Address_notMatchingPattern_rejected() {
        configure(List.of("^192\\.168\\.1\\.\\d+$"));
        assertRegisterThrows("10.0.0.1", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not permitted by policy.");
    }

    @Test
    public void registerNode_bracketedIpv6_fullAddress_matchesPattern_passes() throws ClientPolicyException {
        configure(List.of("^2001:db8::1$"));
        assertRegisterPasses("[2001:db8::1]");
    }

    @Test
    public void registerNode_bracketedIpv6_fullAddress_notMatchingPattern_rejected() {
        configure(List.of("^2001:db8::1$"));
        assertRegisterThrows("[2001:db8::2]", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname is not permitted by policy.");
    }

    @Test
    public void registerNode_uriQueryBypass_rejected() {
        // "host?param=value" URI parser extracts "node.example.com" as the host
        // and "evil=1" as the query. parsedHost != nodeHost so the round-trip check
        // rejects it as containing URI structural characters.
        configure(List.of("^.*\\.example\\.com$"));
        assertRegisterThrows("node.example.com?evil=1", OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname contains URI structural characters and is not permitted.");
    }

    @Test
    public void registerNode_exactMaxLengthHostname_passes() throws ClientPolicyException {
        int max = SecureClientNodeExecutorFactory.MAX_NODE_HOST_LENGTH;
        String label = "a".repeat(max - 2);
        String hostname = label + ".b";  // e.g. "aaa...a.b" = max chars total
        configure(List.of(".*"));
        assertRegisterPasses(hostname);
    }

    @Test
    public void executeOnEvent_nonRegisterNodeEvent_ignored() throws ClientPolicyException {
        configure(Collections.emptyList());
        executor.executeOnEvent(new ClientNodeRegistrationContext(null, "evil.host.com",
                ClientPolicyEvent.TOKEN_REQUEST));
        // No exception = pass.
    }

    @Test
    public void registerNode_nullPatternList_failsClosed() {
        Configuration config = new Configuration();
        executor.setupConfiguration(config);
        assertRegisterThrows("app-node-1.internal.example.com", OAuthErrorException.INVALID_REQUEST,
                "No valid hostname patterns configured in the executor. Node registration is blocked by policy.");
    }

    @Test
    public void registerNode_nullEntryInPatternList_nullSkipped_validPatternStillEvaluated() throws ClientPolicyException {
        configure(Arrays.asList(null, PATTERN_INTERNAL));
        assertRegisterPasses("app-node-1.internal.example.com");
    }

    @Test
    public void registerNode_allNullPatternEntries_failsClosed() {
        configure(Arrays.asList(null, null));
        assertRegisterThrows("app-node-1.internal.example.com", OAuthErrorException.INVALID_REQUEST,
                "No valid hostname patterns configured in the executor. Node registration is blocked by policy.");
    }

    @Test
    public void registerNode_overLengthHostname_throws() {
        configure(List.of(".*"));
        String tooLong = "a".repeat(SecureClientNodeExecutorFactory.MAX_NODE_HOST_LENGTH + 1);
        assertRegisterThrows(tooLong, OAuthErrorException.INVALID_REQUEST,
                "Client cluster node hostname exceeds maximum allowed length.");
    }

    private void configure(List<String> patterns) {
        Configuration config = new Configuration();
        config.setHostnameAllowedPatterns(patterns);
        executor.setupConfiguration(config);
    }

    private void assertRegisterPasses(String nodeHost) throws ClientPolicyException {
        executor.executeOnEvent(new ClientNodeRegistrationContext(null, nodeHost, ClientPolicyEvent.REGISTER_NODE));
    }

    private void assertRegisterThrows(String nodeHost, String expectedError, String expectedDetail) {
        try {
            executor.executeOnEvent(new ClientNodeRegistrationContext(null, nodeHost, ClientPolicyEvent.REGISTER_NODE));
            Assertions.fail("Expected ClientPolicyException for hostname: " + nodeHost);
        } catch (ClientPolicyException cpe) {
            Assertions.assertEquals(expectedError, cpe.getError());
            Assertions.assertEquals(expectedDetail, cpe.getErrorDetail());
        }
    }
}
