package org.keycloak.requestfilter;

import io.netty.handler.ipfilter.IpFilterRuleType;
import junit.framework.TestCase;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AccessRulesTest extends TestCase {

    private void testCombinations(AccessRules ruleUnderTest,
                                  boolean matchIsExpected,
                                  List<String> ips,
                                  List<String> paths) {
        for (String ipStr : ips) {
            InetSocketAddress ip = new InetSocketAddress(ipStr, 80);
            for (String path : paths) {
                if (matchIsExpected) {
                    assertTrue("Expected " + ipStr + ", " + path + " matching " + ruleUnderTest,
                            ruleUnderTest.matches(ip, path));
                } else {
                    assertFalse("Expected " + ipStr + ", " + path + " NOT TO MATCH " + ruleUnderTest,
                            ruleUnderTest.matches(ip, path));

                }
            }
        }

    }
    private void testRules(AccessRules ruleUnderTest,
                           List<String> matchingIps,
                           List<String> matchingPaths,
                           List<String> nonMatchingIps,
                           List<String> nonMatchingPaths) {

        testCombinations(ruleUnderTest, true, matchingIps, matchingPaths);
        testCombinations(ruleUnderTest, false, matchingIps, nonMatchingPaths);
        testCombinations(ruleUnderTest, false, nonMatchingIps, matchingPaths);
        testCombinations(ruleUnderTest, false, nonMatchingIps, nonMatchingPaths);
    }

    AccessRules acceptFilterFor(String rulesString, String contextPath) {
        return  AccessRules.parse(rulesString,
                                                contextPath,
                                                IpFilterRuleType.ACCEPT);
    }

    public void testSingleRuleOnePathOneIp() {
            testRules(acceptFilterFor("/realms/master/account:11.1.1.1/32",""),
                    Collections.singletonList("11.1.1.1"),
                    Arrays.asList("/realms/master/account", "/realms/master/account/foo"),
                    Collections.singletonList("11.1.1.2"),
                    Arrays.asList("/realms/bar/account", "/realms/bar/account/foo")
                    );
    }

    public void testSingleRuleOnePathIpRange() {
        testRules(acceptFilterFor("/realms/master/account:11.1.1.1/24", ""),
                Arrays.asList("11.1.1.1", "11.1.1.2", "11.1.1.255"),
                Arrays.asList("/realms/master/account", "/realms/master/account/foo"),
                Arrays.asList("11.0.255.255", "11.1.2.1"),
                Arrays.asList("/realms/bar/account", "/realms/bar/account/foo")
        );
    }

    public void testSingleRuleOnePathMultipleIpRanges() {
        testRules(acceptFilterFor("/realms/master/account:11.1.1.1/24|192.168.1.1/16", ""),
                Arrays.asList("11.1.1.1", "11.1.1.2", "11.1.1.255", "192.168.1.1", "192.168.255.255"),
                Arrays.asList("/realms/master/account", "/realms/master/account/foo"),
                Arrays.asList("11.0.255.255", "11.1.2.1", "192.167.255.255", "192.169.1.1"),
                Arrays.asList("/realms/bar/account", "/realms/bar/account/foo")
        );
    }

    public void testMultipleRulesMultipleIpRanges() {
        testRules(acceptFilterFor("/realms/master/account:11.1.1.1/24|192.168.1.1/16,/realms/thud/account:11.1.1.1/24|192.168.1.1/16", ""),
                Arrays.asList("11.1.1.1", "11.1.1.2", "11.1.1.255", "192.168.1.1", "192.168.255.255"),
                Arrays.asList("/realms/master/account", "/realms/master/account/foo", "/realms/thud/account", "/realms/thud/account/foo"),
                Arrays.asList("11.0.255.255", "11.1.2.1", "192.167.255.255", "192.169.1.1"),
                Arrays.asList("/realms/bar/account", "/realms/bar/account/foo")
        );
    }

    public void testMultipleRulesMultipleIpRangesAndContextPath() {
        /*
         * Path /realms/master/account is absolute
         *      => context is not relevant, effective path = /realms/master/account
         *
         * Path realms/thud/account is relative
         * => context is added, effective path = /auth/realms/thud/account
         */
        testRules(acceptFilterFor("/realms/master/account:11.1.1.1/24|192.168.1.1/16,realms/thud/account:11.1.1.1/24|192.168.1.1/16", "/auth"),
                Arrays.asList("11.1.1.1", "11.1.1.2", "11.1.1.255", "192.168.1.1", "192.168.255.255"),
                Arrays.asList("/realms/master/account", "/realms/master/account/foo",
                                "/auth/realms/thud/account", "/auth/realms/thud/account/foo"),
                Arrays.asList("11.0.255.255", "11.1.2.1", "192.167.255.255", "192.169.1.1"),
                Arrays.asList("/auth/realms/bar/account", "/auth/realms/bar/account/foo",
                                "/auth/realms/master/account", "/auth/realms/master/account/foo",
                                "/realms/thud/account", "/realms/thud/account/foo")
        );
    }


    public void testEmptyRule() {
        assertNull(acceptFilterFor("", ""));
    }

    public void testInvalidConfigurationThrowsException() {
        expectExceptionForConfiguration("noColonInConfiguration");
        expectExceptionForConfiguration("/secondRuleInvalid:127.0.0.1/32,162.168.1.1/32|noColonInConfiguration");
        expectExceptionForConfiguration("/inValidCidr:127.0.0.1");
        expectExceptionForConfiguration("/inValidCidr:127.0.0.1_32,162.168.1.1/32");
    }

    private void expectExceptionForConfiguration(String configuration) {
        try {
            acceptFilterFor(configuration, "");
        } catch (RuntimeException e) {
            return;
        }
        fail("Exception expected for filter configuration '" + configuration + "'");
    }
}