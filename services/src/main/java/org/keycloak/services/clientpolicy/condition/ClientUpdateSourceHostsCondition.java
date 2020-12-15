/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

public class ClientUpdateSourceHostsCondition implements ClientPolicyConditionProvider {

    private static final Logger logger = Logger.getLogger(ClientUpdateSourceHostsCondition.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public ClientUpdateSourceHostsCondition(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
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

        ClientPolicyLogger.logv(logger, "Verifying remote host {0}", hostAddress);

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
        List<String> trustedHostsConfig = componentModel.getConfig().getList(ClientUpdateSourceHostsConditionFactory.TRUSTED_HOSTS);
        return trustedHostsConfig.stream().filter((String hostname) -> {

            return !hostname.startsWith("*.");

        }).collect(Collectors.toList());
    }

    protected List<String> getTrustedDomains() {
        List<String> trustedHostsConfig = componentModel.getConfig().getList(ClientUpdateSourceHostsConditionFactory.TRUSTED_HOSTS);
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

                ClientPolicyLogger.logv(logger, "Trying host {0} of address {1}", confHostName, hostIPAddress);
                if (hostIPAddress.equals(hostAddress)) {
                    ClientPolicyLogger.logv(logger, "Successfully verified host : {0}", confHostName);
                    return confHostName;
                }
            } catch (UnknownHostException uhe) {
                ClientPolicyLogger.logv(logger, "Unknown host from realm configuration: {0}", confHostName);
            }
        }

        return null;
    }

    protected String verifyHostInTrustedDomains(String hostAddress, List<String> trustedDomains) {
        if (!trustedDomains.isEmpty()) {
            try {
                String hostname = InetAddress.getByName(hostAddress).getHostName();

                ClientPolicyLogger.logv(logger, "Trying verify request from address {0} of host {1} by domains", hostAddress, hostname);

                for (String confDomain : trustedDomains) {
                    if (hostname.endsWith(confDomain)) {
                        ClientPolicyLogger.logv(logger, "Successfully verified host {0} by trusted domain {1}", hostname, confDomain);
                        return hostname;
                    }
                }
            } catch (UnknownHostException uhe) {
                ClientPolicyLogger.logv(logger, "Request of address {0} came from unknown host. Skip verification by domains", hostAddress);
            }
        }

        return null;
    }

    boolean isHostMustMatch() {
        return parseBoolean(ClientUpdateSourceHostsConditionFactory.HOST_SENDING_REQUEST_MUST_MATCH);
    }

    // True by default
    private boolean parseBoolean(String propertyKey) {
        String val = componentModel.getConfig().getFirst(propertyKey);
        return val==null || Boolean.parseBoolean(val);
    }

}
