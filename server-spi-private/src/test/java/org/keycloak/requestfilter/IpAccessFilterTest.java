package org.keycloak.requestfilter;

import io.netty.handler.ipfilter.IpFilterRuleType;
import junit.framework.TestCase;

public class IpAccessFilterTest extends TestCase {
    static final String ALL_RFC1918_RULE = "/:127.0.0.1/24|192.168.80.1/16|172.16.0.1/12|10.0.0.1/8";
    static final String ALL_RULE = "/:0.0.0.0/0";
    AccessRules MATCH_INTERNAL_IPS = AccessRules.parse(
            ALL_RFC1918_RULE,
            "", IpFilterRuleType.ACCEPT);

    AccessRules MATCH_192_168 = AccessRules.parse(
            "/:192.168.0.0/16",
            "", IpFilterRuleType.ACCEPT);

    AccessRules MATCH_192_168_PATH_FOO = AccessRules.parse(
            "/foo:192.168.0.0/16",
            "", IpFilterRuleType.ACCEPT);

    AccessRules MATCH_192_168_PATH_FOO_10_0_PATH_ADMIN = AccessRules.parse(
            "/foo:192.168.0.0/16,/admin:10.0.0.0/16",
            "", IpFilterRuleType.ACCEPT);

    AccessRules MATCH_EVERYTHING = AccessRules.parse(
            ALL_RULE,
            "", IpFilterRuleType.REJECT);

    public void testAllDenied() {

        IpAccessFilter ipAccessFilter = new IpAccessFilter(null, MATCH_EVERYTHING);

        assertFalse(ipAccessFilter.accessIsAllowed("127.0.0.1", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("8.0.0.1", "/"));
    }

    public void testDeniedASubNet() {

        IpAccessFilter ipAccessFilter = new IpAccessFilter(null, MATCH_192_168_PATH_FOO);

        assertTrue(ipAccessFilter.accessIsAllowed("192.168.0.1", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("192.168.0.1", "/foo"));
        assertFalse(ipAccessFilter.accessIsAllowed("192.168.0.1", "/foo/bar"));

        assertTrue(ipAccessFilter.accessIsAllowed("8.0.0.1", "/"));
        assertTrue(ipAccessFilter.accessIsAllowed("8.0.0.1", "/foo"));
        assertTrue(ipAccessFilter.accessIsAllowed("8.0.0.1", "/foo/bar"));
    }

    public void testAllowOverridesDeny() {

        IpAccessFilter ipAccessFilter = new IpAccessFilter(MATCH_INTERNAL_IPS, MATCH_EVERYTHING);

        assertTrue(ipAccessFilter.accessIsAllowed("127.0.0.1", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("8.0.0.1", "/"));
    }

    public void testAllowedIsNullDenyMatchesEverything() {
        IpAccessFilter ipAccessFilter = new IpAccessFilter(null, MATCH_EVERYTHING);

        assertFalse(ipAccessFilter.accessIsAllowed("127.0.0.1", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("8.0.0.1", "/"));
    }

    public void testDenyIsNullAllowEverything() {
        IpAccessFilter ipAccessFilter = new IpAccessFilter(MATCH_INTERNAL_IPS, null);

        // internal ips
        assertTrue(ipAccessFilter.accessIsAllowed("127.0.0.1", "/"));
        assertTrue(ipAccessFilter.accessIsAllowed("192.168.0.1", "/"));

        // this is a non-internal ip
        assertTrue(ipAccessFilter.accessIsAllowed("8.0.0.1", "/"));
    }

    public void testAllowSubnet() {
        IpAccessFilter ipAccessFilter = new IpAccessFilter(MATCH_192_168, MATCH_EVERYTHING);

        assertTrue(ipAccessFilter.accessIsAllowed("192.168.0.1", "/"));
        assertTrue(ipAccessFilter.accessIsAllowed("192.168.255.255", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("192.167.255.255", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("192.169.0.1", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("127.0.0.1", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("8.0.0.1", "/"));
    }

    public void testAllowSubnetWithPath() {
        IpAccessFilter ipAccessFilter = new IpAccessFilter(MATCH_192_168_PATH_FOO, MATCH_EVERYTHING);

        assertFalse(ipAccessFilter.accessIsAllowed("192.168.0.1", "/"));
        assertTrue(ipAccessFilter.accessIsAllowed("192.168.0.1", "/foo"));
    }

    public void testAllowDifferentSubnetsWithDifferentPaths() {
        IpAccessFilter ipAccessFilter = new IpAccessFilter(MATCH_192_168_PATH_FOO_10_0_PATH_ADMIN, MATCH_EVERYTHING);

        assertFalse(ipAccessFilter.accessIsAllowed("192.168.0.1", "/"));
        assertTrue(ipAccessFilter.accessIsAllowed("192.168.0.1", "/foo"));
        assertTrue(ipAccessFilter.accessIsAllowed("192.168.0.1", "/foo/sub"));
        assertFalse(ipAccessFilter.accessIsAllowed("192.168.0.1", "/admin"));

        assertFalse(ipAccessFilter.accessIsAllowed("10.0.0.1", "/"));
        assertTrue(ipAccessFilter.accessIsAllowed("10.0.0.1", "/admin"));
        assertTrue(ipAccessFilter.accessIsAllowed("10.0.0.1", "/admin/sub"));
        assertFalse(ipAccessFilter.accessIsAllowed("10.0.0.1", "/foo"));

        assertFalse(ipAccessFilter.accessIsAllowed("11.0.0.1", "/"));
        assertFalse(ipAccessFilter.accessIsAllowed("11.0.0.1", "/admin"));
        assertFalse(ipAccessFilter.accessIsAllowed("11.0.0.1", "/admin/sub"));
        assertFalse(ipAccessFilter.accessIsAllowed("11.0.0.1", "/foo"));
    }
}