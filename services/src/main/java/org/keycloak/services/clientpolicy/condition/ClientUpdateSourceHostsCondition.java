/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.condition;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientUpdateSourceHostsCondition extends AbstractClientCondition {

    private static final Logger logger = Logger.getLogger(ClientUpdateSourceHostsCondition.class);

    // to avoid null configuration, use vacant new instance to indicate that there is no configuration set up.
    private Configuration configuration = new Configuration();

    public ClientUpdateSourceHostsCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    protected <T extends AbstractClientCondition.Configuration> T getConfiguration(Class<T> clazz) {
        return (T) configuration;
    }
 
    @Override
    public void setupConfiguration(Object config) {
        // to avoid null configuration, use vacant new instance to indicate that there is no configuration set up.
        configuration = Optional.ofNullable(getConvertedConfiguration(config, Configuration.class)).orElse(new Configuration());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration extends AbstractClientCondition.Configuration {
        @JsonProperty("trusted-hosts")
        protected List<String> trustedHosts;
        @JsonProperty("host-sending-request-must-match")
        protected List<Boolean> hostSendingRequestMustMatch;

        public List<String> getTrustedHosts() {
            return trustedHosts;
        }

        public void setTrustedHosts(List<String> trustedHosts) {
            this.trustedHosts = trustedHosts;
        }

        public List<Boolean> getHostSendingRequestMustMatch() {
            return hostSendingRequestMustMatch;
        }

        public void setHostSendingRequestMustMatch(List<Boolean> hostSendingRequestMustMatch) {
            this.hostSendingRequestMustMatch = hostSendingRequestMustMatch;
        }
    }

    @Override
    public String getProviderId() {
        return ClientUpdateSourceHostsConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
        case UPDATE:
            if (!isHostMustMatch()) return ClientPolicyVote.ABSTAIN;
            if (isHostMatched()) return ClientPolicyVote.YES;
            return ClientPolicyVote.NO;
        default:
            return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isHostMatched() {
        String hostAddress = session.getContext().getConnection().getRemoteAddr();

        ClientPolicyLogger.logv(logger, "{0} :: Verifying remote host = {1}", logMsgPrefix(), hostAddress);

        List<String> trustedHosts = getTrustedHosts();
        List<String> trustedDomains = getTrustedDomains();

        // Verify trustedHosts by their IP addresses
        String verifiedHost = verifyHostInTrustedHosts(hostAddress, trustedHosts);
        if (verifiedHost != null) {
            return true;
        }

        // Verify domains if hostAddress hostname belongs to the domain. This assumes proper DNS setup
        verifiedHost = verifyHostInTrustedDomains(hostAddress, trustedDomains);
        if (verifiedHost != null) {
            return true;
        }

        return false;
    }

    protected List<String> getTrustedHosts() {
        List<String> trustedHostsConfig = configuration.getTrustedHosts();
        return trustedHostsConfig.stream().filter((String hostname) -> {
            return !hostname.startsWith("*.");
        }).collect(Collectors.toList());
    }

    protected List<String> getTrustedDomains() {
        List<String> trustedHostsConfig = configuration.getTrustedHosts();
        List<String> domains = new LinkedList<>();

        for (String hostname : trustedHostsConfig) {
            if (hostname.startsWith("*.")) {
                hostname = hostname.substring(2);
                domains.add(hostname);
            }
        }

        return domains;
    }

    protected String verifyHostInTrustedHosts(String hostAddress, List<String> trustedHosts) {
        for (String confHostName : trustedHosts) {
            try {
                String hostIPAddress = InetAddress.getByName(confHostName).getHostAddress();

                ClientPolicyLogger.logv(logger, "{0} :: Trying host {1} of address {2}", logMsgPrefix(), confHostName, hostIPAddress);
                if (hostIPAddress.equals(hostAddress)) {
                    ClientPolicyLogger.logv(logger, "{0} :: Successfully verified host = {1}", logMsgPrefix(), confHostName);
                    return confHostName;
                }
            } catch (UnknownHostException uhe) {
                ClientPolicyLogger.logv(logger, "{0} :: Unknown host from realm configuration = {1}", logMsgPrefix(), confHostName);
            }
        }

        return null;
    }

    protected String verifyHostInTrustedDomains(String hostAddress, List<String> trustedDomains) {
        if (!trustedDomains.isEmpty()) {
            try {
                String hostname = InetAddress.getByName(hostAddress).getHostName();

                ClientPolicyLogger.logv(logger, "{0} :: Trying verify request from address {1} of host {2} by domains", logMsgPrefix(), hostAddress, hostname);

                for (String confDomain : trustedDomains) {
                    if (hostname.endsWith(confDomain)) {
                        ClientPolicyLogger.logv(logger, "{0} :: Successfully verified host {1} by trusted domain {2}", logMsgPrefix(), hostname, confDomain);
                        return hostname;
                    }
                }
            } catch (UnknownHostException uhe) {
                ClientPolicyLogger.logv(logger, "{0} :: Request of address {1} came from unknown host. Skip verification by domains", logMsgPrefix(), hostAddress);
            }
        }

        return null;
    }

    boolean isHostMustMatch() {
        List<Boolean> l = configuration.getHostSendingRequestMustMatch();
        if (l != null && !l.isEmpty()) return l.get(0).booleanValue();
        return true;
    }

    // True by default
    //private boolean parseBoolean(String propertyKey) {
    //    String val = componentModel.getConfig().getFirst(propertyKey);
    //    return val==null || Boolean.parseBoolean(val);
    //}

}
