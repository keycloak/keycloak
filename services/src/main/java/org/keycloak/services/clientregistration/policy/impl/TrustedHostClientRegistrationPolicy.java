/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientregistration.policy.impl;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TrustedHostClientRegistrationPolicy implements ClientRegistrationPolicy {

    private static final Logger logger = Logger.getLogger(TrustedHostClientRegistrationPolicy.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public TrustedHostClientRegistrationPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        verifyHost();
        verifyClientUrls(context);
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel) {
    }


    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        verifyHost();
        verifyClientUrls(context);
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {

    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
        verifyHost();
    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
        verifyHost();
    }

    // IMPL

    protected void verifyHost() throws ClientRegistrationPolicyException {
        boolean hostMustMatch = isHostMustMatch();
        if (!hostMustMatch) {
            return;
        }

        String hostAddress = session.getContext().getConnection().getRemoteAddr();

        logger.debugf("Verifying remote host : %s", session.getContext().getConnection().getRemoteHost());

        if (!verifyHost(hostAddress)) {
            ServicesLogger.LOGGER.failedToVerifyRemoteHost(session.getContext().getConnection().getRemoteHost());
            throw new ClientRegistrationPolicyException("Host not trusted.");
        }
    }

    protected boolean verifyHost(String hostAddress) {
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
        List<String> trustedHostsConfig = componentModel.getConfig().getList(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS);
        return trustedHostsConfig.stream().filter((String hostname) -> {

            return !hostname.startsWith("*.");

        }).collect(Collectors.toList());
    }


    protected List<String> getTrustedDomains() {
        return componentModel.getConfig().getList(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS);
    }

    protected String verifyHostInTrustedHosts(String hostAddress, List<String> trustedHosts) {
        for (String confHostName : trustedHosts) {
            try {
                String hostIPAddress = InetAddress.getByName(confHostName).getHostAddress();

                logger.tracef("Trying host '%s' of address '%s'", confHostName, hostIPAddress);
                if (hostIPAddress.equals(hostAddress)) {
                    logger.debugf("Successfully verified host : %s", confHostName);
                    return confHostName;
                }
            } catch (UnknownHostException uhe) {
                logger.debugf(uhe, "Unknown host from realm configuration: %s", confHostName);
            }
        }

        return null;
    }

    private boolean checkTrustedDomain(String hostname, String trustedDomain) {
        if (trustedDomain.startsWith("*.")) {
            String domain = trustedDomain.substring(2);
            return hostname.equals(domain) || hostname.endsWith("." + domain);
        }
        return hostname.equals(trustedDomain);
    }

    protected String verifyHostInTrustedDomains(String hostAddress, List<String> trustedDomains) {
        try {
            InetAddress address = InetAddress.getByName(hostAddress);
            String hostname = address.getHostName();

            logger.debugf("Trying verify request from address '%s' of host '%s' by domains", hostAddress, hostname);

            // On Windows, reverse lookup for loopback may return the IP (e.g., 127.0.0.1) instead of 'localhost'.
            // Normalize to 'localhost' for consistent domain checks.
            if (address.isLoopbackAddress()) {
                hostname = "localhost";
            } else if (hostname.equals(address.getHostAddress())) {
                logger.debugf("The hostAddress '%s' was not resolved to a hostname", hostAddress);
                return null;
            }

            // For non-loopback addresses, perform a forward-confirmation check: the hostname must resolve back to the same address.
            if (!address.isLoopbackAddress() && Arrays.stream(InetAddress.getAllByName(hostname)).noneMatch(a -> address.equals(a))) {
                logger.debugf("The hostAddress '%s' is not among the direct lookups returned resolving '%s'", hostAddress, hostname);
                return null;
            }

            for (String confDomain : trustedDomains) {
                if (checkTrustedDomain(hostname, confDomain)) {
                    logger.debugf("Successfully verified host '%s' by trusted domain '%s'", hostname, confDomain);
                    return hostname;
                }
            }
        } catch (UnknownHostException uhe) {
            logger.debugf(uhe, "Request of address '%s' came from unknown host. Skip verification by domains unless it's within localhost domain", hostAddress);

            String lower = hostAddress == null ? null : hostAddress.toLowerCase();
            if (lower != null && ("localhost".equals(lower) || lower.endsWith(".localhost"))) {
                for (String confDomain : trustedDomains) {
                    if (checkTrustedDomain(lower, confDomain)) {
                        logger.debugf("Treating host '%s' as loopback due to localhost domain and returning success by trusted domain '%s'", lower, confDomain);
                        return lower;
                    }
                }
            }
        }

        return null;
    }


    protected void verifyClientUrls(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        boolean redirectUriMustMatch = isClientUrisMustMatch();
        if (!redirectUriMustMatch) {
            return;
        }

        List<String> trustedHosts = getTrustedHosts();
        List<String> trustedDomains = getTrustedDomains();

        ClientRepresentation client = context.getClient();
        String rootUrl = client.getRootUrl();
        String baseUrl = client.getBaseUrl();
        String adminUrl = client.getAdminUrl();
        List<String> redirectUris = client.getRedirectUris();

        baseUrl = relativeToAbsoluteURI(rootUrl, baseUrl);
        adminUrl = relativeToAbsoluteURI(rootUrl, adminUrl);
        Set<String> resolvedRedirects = PairwiseSubMapperUtils.resolveValidRedirectUris(rootUrl, redirectUris);

        if (rootUrl != null) {
            checkURLTrusted(rootUrl, trustedHosts, trustedDomains);
        }

        if (baseUrl != null) {
            checkURLTrusted(baseUrl, trustedHosts, trustedDomains);
        }
        if (adminUrl != null) {
            checkURLTrusted(adminUrl, trustedHosts, trustedDomains);
        }
        for (String redirect : resolvedRedirects) {
            checkURITrusted(redirect, trustedHosts, trustedDomains);
        }

    }

    protected void checkURLTrusted(String url, List<String> trustedHosts, List<String> trustedDomains) throws ClientRegistrationPolicyException {
        try {
            String host = new URL(url).getHost();

            if (checkHostTrusted(host, trustedHosts, trustedDomains)) {
                return;
            }
        } catch (MalformedURLException mfe) {
            logger.debugf(mfe, "URL '%s' is malformed", url);
            throw new ClientRegistrationPolicyException("URL is malformed");
        }

        ServicesLogger.LOGGER.urlDoesntMatch(url);
        throw new ClientRegistrationPolicyException("URL doesn't match any trusted host or trusted domain");
    }

    protected void checkURITrusted(String uri, List<String> trustedHosts, List<String> trustedDomains) throws ClientRegistrationPolicyException {
        try {
            String host = new URI(uri).getHost();

            if (checkHostTrusted(host, trustedHosts, trustedDomains)) {
                return;
            }
        } catch (URISyntaxException use) {
            logger.debugf(use, "URI '%s' is malformed", uri);
            throw new ClientRegistrationPolicyException("URI is malformed");
        }

        ServicesLogger.LOGGER.uriDoesntMatch(uri);
        throw new ClientRegistrationPolicyException("URI doesn't match any trusted host or trusted domain");
    }

    private boolean checkHostTrusted(String host, List<String> trustedHosts, List<String> trustedDomains) {
        for (String trustedHost : trustedHosts) {
            if (host.equals(trustedHost)) {
                return true;
            }
        }

        for (String trustedDomain : trustedDomains) {
            if (checkTrustedDomain(host, trustedDomain)) {
                return true;
            }
        }

        return false;
    }


    private static String relativeToAbsoluteURI(String rootUrl, String relative) {
        if (relative == null) {
            return null;
        }

        if (!relative.startsWith("/")) {
            return relative;
        } else if (rootUrl == null || rootUrl.isEmpty()) {
            return null;
        }

        return rootUrl + relative;
    }

    boolean isHostMustMatch() {
        return parseBoolean(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH);
    }

    boolean isClientUrisMustMatch() {
        return parseBoolean(TrustedHostClientRegistrationPolicyFactory.CLIENT_URIS_MUST_MATCH);
    }

    // True by default
    private boolean parseBoolean(String propertyKey) {
        String val = componentModel.getConfig().getFirst(propertyKey);
        return val == null || Boolean.parseBoolean(val);
    }
}
