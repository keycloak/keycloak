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
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientUpdaterSourceHostsCondition extends AbstractClientPolicyConditionProvider<ClientUpdaterSourceHostsCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientUpdaterSourceHostsCondition.class);

    public ClientUpdaterSourceHostsCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }


    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        @JsonProperty("trusted-hosts")
        protected List<String> trustedHosts;

        public List<String> getTrustedHosts() {
            return trustedHosts;
        }

        public void setTrustedHosts(List<String> trustedHosts) {
            this.trustedHosts = trustedHosts;
        }
    }

    @Override
    public String getProviderId() {
        return ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
        case UPDATE:
        case REGISTERED:
        case UPDATED:
            if (isHostMatched()) return ClientPolicyVote.YES;
            return ClientPolicyVote.NO;
        default:
            return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isHostMatched() {
        String hostAddress = session.getContext().getConnection().getRemoteAddr();

        logger.tracev("Verifying remote host = {0}", session.getContext().getConnection().getRemoteHost());

        if (hostAddress == null) {
            return false;
        }

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
                logger.tracev("Trying host {0} of address {1}", confHostName, hostIPAddress);
                if (hostIPAddress.equals(hostAddress)) {
                    logger.tracev("Successfully verified host = {0}", confHostName);
                    return confHostName;
                }
            } catch (UnknownHostException uhe) {
                logger.tracev("Unknown host from realm configuration = {0}", confHostName);
            }
        }

        return null;
    }

    protected String verifyHostInTrustedDomains(String hostAddress, List<String> trustedDomains) {
        if (!trustedDomains.isEmpty()) {
            try {
                String hostname = InetAddress.getByName(hostAddress).getHostName();
                logger.tracev("Trying verify request from address {0} of host {1} by domains", hostAddress, hostname);
                for (String confDomain : trustedDomains) {
                    if (hostname.endsWith(confDomain)) {
                        logger.tracev("Successfully verified host {0} by trusted domain {1}", hostname, confDomain);
                        return hostname;
                    }
                }
            } catch (UnknownHostException uhe) {
                logger.tracev("Request of address {0} came from unknown host. Skip verification by domains", hostAddress);
            }
        }

        return null;
    }
}
