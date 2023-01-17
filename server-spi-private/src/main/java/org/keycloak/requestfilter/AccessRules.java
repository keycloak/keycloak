package org.keycloak.requestfilter;

import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import org.jboss.logging.Logger;
import org.keycloak.utils.StringUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class AccessRules {

    private final List<AccessRule> rules;

    private AccessRules(List<AccessRule> rules) {
        this.rules = rules;
    }

    public boolean matches(InetSocketAddress address, String requestPath) {

        for (AccessRule accessRule : rules) {
            if (accessRule.matches(address, requestPath)) {
                return true;
            }
        }

        return false;
    }

    static AccessRules parse(String rulesString, String contextPath, IpFilterRuleType ruleType) {
        return (new AccessRulesParser(rulesString, contextPath, ruleType)).parse();
    }

    static class IpRuleConfigurationException extends RuntimeException {
        IpRuleConfigurationException(String msg) {
            super(msg);
        }
    }

    private static class AccessRulesParser {

        private static final Logger log = Logger.getLogger(AccessRulesParser.class);
        private final String rulesString;
        private final String contextPath;
        private final IpFilterRuleType ruleType;

        private final List<String> cidrs = new ArrayList<>();

        private AccessRulesParser(String rulesString, String contextPath, IpFilterRuleType ruleType) {
            this.rulesString = rulesString;
            this.contextPath = contextPath;
            this.ruleType = ruleType;
        }

        private AccessRules parse() {
            if ((this.rulesString == null) || StringUtil.isBlank(this.rulesString)) {
                log.info("No access rules");
                return null;
            }

            // Split "$PATH1:cidr1/8|cidr2/8,$PATH2:cidr3/8" into [ "$PATH1:cidr1/8|cidr2/8", "$PATH2:cidr3/8" ]
            String[] ruleEntries = rulesString.trim().split(",");
            return new AccessRules(parseAccessRules(ruleEntries));
        }

        private List<AccessRule> parseAccessRules(String[] ruleEntries) {
            List<AccessRule> accessRules = new ArrayList<>();

            for (String ruleEntry : ruleEntries) {

                // Split "$PATH1:cidr1/8|cidr2/8" into [ "$PATH1", "cidr1/8|cidr2/8" ]
                String[] pathAndCidrs = ruleEntry.trim().split(":");
                if (pathAndCidrs.length != 2) {
                    log.errorf("Invalid access rule configuration: '%s'", ruleEntry);
                    throw new IpRuleConfigurationException("Invalid access rule configuration: '" + ruleEntry + "'");
                }

                String path = asAbsolutePath(pathAndCidrs[0]);
                Set<IpSubnetFilterRule> rules = parseIpSubnetFilterRules(pathAndCidrs[1]);

                String ruleDescription = ruleType + " " + path + " from " + String.join(",", cidrs);
                accessRules.add(new AccessRule(ruleDescription, path, rules));
            }
            log.infof("Created Security Filter rules for %s", accessRules);
            return accessRules;
        }

        private String asAbsolutePath(String path) {
            if (!path.startsWith("/")) {
                path = makeContextPath(contextPath, path);
            }
            return path;
        }

        private String makeContextPath(String contextPath, String subPath) {
            if (contextPath.endsWith("/")) {
                return contextPath + subPath;
            }
            return contextPath + "/" + subPath;
        }

        private Set<IpSubnetFilterRule> parseIpSubnetFilterRules(String cidrEntries) {

            Set<IpSubnetFilterRule> rules = new LinkedHashSet<>();

            // Split "cidr1/8|cidr2/8" into [ "cidr1/8", "cidr2/8" ]
            for (String cidrEntry : cidrEntries.trim().split("\\|")) {

                // Split cidr1/8 into [ "cidr1", "8" ]
                String[] ipAndCidrPrefix = cidrEntry.trim().split("/");
                if (ipAndCidrPrefix.length != 2) {
                    log.errorf("Invalid access rule cidr entry configuration: '%s'", cidrEntry);
                    throw new IpRuleConfigurationException("Invalid access rule cidr entry configuration: '" + cidrEntry + "'");
                }

                this.cidrs.add(cidrEntry);
                String ip = ipAndCidrPrefix[0];
                int cidrPrefix = Integer.parseInt(ipAndCidrPrefix[1]);
                rules.add(new IpSubnetFilterRule(ip, cidrPrefix, this.ruleType));
            }
            return rules;
        }
    }

    private static class AccessRule {

        public AccessRule(String ruleDescription, String pathPrefix, Set<IpSubnetFilterRule> ipFilterRules) {
            this.ruleDescription = ruleDescription;
            this.pathPrefix = pathPrefix;
            this.ipFilterRules = ipFilterRules;
        }

        private final String ruleDescription;

        private final String pathPrefix;

        private final Set<IpSubnetFilterRule> ipFilterRules;

        private boolean matches(InetSocketAddress address, String requestPath) {

            if (!requestPath.startsWith(pathPrefix)) {
                return false;
            }

            for (IpSubnetFilterRule filterRule : ipFilterRules) {
                if (filterRule.matches(address)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String toString() {
            return "AccessRule{" +
                    "ruleDescription='" + ruleDescription + '\'' +
                    ", pathPrefix='" + pathPrefix + '\'' +
                    ", ipFilterRules=" + ipFilterRules +
                    '}';
        }
    }
}
