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

package org.keycloak.services.clientpolicy.executor;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.UriType;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

public class SecureRedirectUrisEnforcerExecutor implements ClientPolicyExecutorProvider<SecureRedirectUrisEnforcerExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureRedirectUrisEnforcerExecutor.class);

    private final KeycloakSession session;
    private Configuration configuration;

    public static final String ERR_GENERAL = "Invalid Redirect Uri: invalid uri";

    public static final String ERR_LOOPBACK = "Invalid Redirect Uri: invalid loopback address";
    public static final String ERR_PRIVATESCHEME = "Invalid Redirect Uri: invalid private use scheme";
    public static final String ERR_NORMALURI = "Invalid Redirect Uri: invalid uri";

    public SecureRedirectUrisEnforcerExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty(SecureRedirectUrisEnforcerExecutorFactory.ALLOW_PRIVATE_USE_URI_SCHEME)
        protected boolean allowPrivateUseUriScheme;
        @JsonProperty(SecureRedirectUrisEnforcerExecutorFactory.ALLOW_IPV4_LOOPBACK_ADDRESS)
        protected boolean allowIPv4LoopbackAddress;
        @JsonProperty(SecureRedirectUrisEnforcerExecutorFactory.ALLOW_IPV6_LOOPBACK_ADDRESS)
        protected boolean allowIPv6LoopbackAddress;
        @JsonProperty(SecureRedirectUrisEnforcerExecutorFactory.ALLOW_HTTP_SCHEME)
        protected boolean allowHttpScheme;
        @JsonProperty(SecureRedirectUrisEnforcerExecutorFactory.ALLOW_WILDCARD_CONTEXT_PATH)
        protected boolean allowWildcardContextPath;
        @JsonProperty(SecureRedirectUrisEnforcerExecutorFactory.ALLOW_PERMITTED_DOMAINS)
        protected List<String> allowPermittedDomains = Collections.emptyList();
        @JsonProperty(SecureRedirectUrisEnforcerExecutorFactory.OAUTH_2_1_COMPLIANT)
        protected boolean oauth2_1complient;
        @JsonProperty(SecureRedirectUrisEnforcerExecutorFactory.ALLOW_OPEN_REDIRECT)
        protected boolean allowOpenRedirect;

        public boolean isAllowPrivateUseUriScheme() {
            return allowPrivateUseUriScheme;
        }

        public void setAllowPrivateUseUriScheme(boolean allowPrivateUseUriScheme) {
            this.allowPrivateUseUriScheme = allowPrivateUseUriScheme;
        }

        public boolean isAllowIPv4LoopbackAddress() {
            return allowIPv4LoopbackAddress;
        }

        public void setAllowIPv4LoopbackAddress(boolean allowIPv4LoopbackAddress) {
            this.allowIPv4LoopbackAddress = allowIPv4LoopbackAddress;
        }

        public boolean isAllowIPv6LoopbackAddress() {
            return allowIPv6LoopbackAddress;
        }

        public void setAllowIPv6LoopbackAddress(boolean allowIPv6LoopbackAddress) {
            this.allowIPv6LoopbackAddress = allowIPv6LoopbackAddress;
        }

        public boolean isAllowHttpScheme() {
            return allowHttpScheme;
        }

        public void setAllowHttpScheme(boolean allowHttpScheme) {
            this.allowHttpScheme = allowHttpScheme;
        }

        public boolean isAllowWildcardContextPath() {
            return allowWildcardContextPath;
        }

        public void setAllowWildcardContextPath(boolean allowWildcardContextPath) {
            this.allowWildcardContextPath = allowWildcardContextPath;
        }

        public List<String> getAllowPermittedDomains() {
            return allowPermittedDomains;
        }

        public void setAllowPermittedDomains(List<String> permittedDomains) {
            this.allowPermittedDomains = permittedDomains;
        }

        public boolean isOAuth2_1Compliant() {
            return oauth2_1complient;
        }

        public void setOAuth2_1Compliant(boolean oauth21complient) {
            this.oauth2_1complient = oauth21complient;
        }

        public boolean isAllowOpenRedirect() {
            return allowOpenRedirect;
        }

        public void setAllowOpenRedirect(boolean allowOpenRedirect) {
            this.allowOpenRedirect = allowOpenRedirect;
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
                if (context instanceof AdminClientRegisterContext || context instanceof DynamicClientRegisterContext) {
                    verifyRedirectUris((ClientCRUDContext) context);
                } else {
                    throw invalidRedirectUri(ERR_GENERAL);
                }
                return;
            case UPDATE:
                if (context instanceof AdminClientUpdateContext || context instanceof DynamicClientUpdateContext) {
                    verifyRedirectUris((ClientCRUDContext) context);
                } else {
                    throw invalidRedirectUri(ERR_GENERAL);
                }
                return;
            case PRE_AUTHORIZATION_REQUEST:
                String redirectUriParam = ((PreAuthorizationRequestContext)context).getRequestParameters()
                        .getFirst(OAuth2Constants.REDIRECT_URI);
                String clientId = ((PreAuthorizationRequestContext)context).getClientId();
                ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
                if (client == null) {
                    throw invalidRedirectUri("Invalid parameter: clientId");
                }
                if (isAuthFlowWithRedirectEnabled(client)) {
                    verifyRedirectUri(redirectUriParam, true);
                }
                return;
            default:
        }
    }

    private void verifyRedirectUris(ClientCRUDContext context) throws ClientPolicyException {
        ClientRepresentation client = context.getProposedClientRepresentation();
        if (isAuthFlowWithRedirectEnabled(client)) {
            List<String> redirectUris = client.getRedirectUris();
            if (redirectUris == null || redirectUris.isEmpty()) {
                throw invalidRedirectUri(ERR_GENERAL);
            }
            verifyRedirectUris(client.getRootUrl(), redirectUris);
            verifyPostLogoutRedirectUriUpdate(client);
        }
    }

    private static boolean isAuthFlowWithRedirectEnabled(ClientModel client) {
        return client.isStandardFlowEnabled() || client.isImplicitFlowEnabled();
    }

    private static boolean isAuthFlowWithRedirectEnabled(ClientRepresentation client) {
        return (client.isStandardFlowEnabled() == null || client.isStandardFlowEnabled() == Boolean.TRUE) || client.isImplicitFlowEnabled() == Boolean.TRUE;
    }

    private void verifyPostLogoutRedirectUriUpdate(ClientRepresentation client) throws ClientPolicyException {
        List<String> postLogoutRedirectUris = OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPostLogoutRedirectUris();
        if (postLogoutRedirectUris == null || postLogoutRedirectUris.isEmpty()) {
            return;
        }
        logger.tracef("Verifying post-logout redirect uris. Target client: %s, Effective post-logout uris: %s", client.getClientId(), postLogoutRedirectUris);
        verifyRedirectUris(client.getRootUrl(), postLogoutRedirectUris);
    }

    void verifyRedirectUris(String rootUri, List<String> redirectUris) throws ClientPolicyException {
        // open redirect allows any value for redirect uri
        if (configuration.isAllowOpenRedirect()) {
            return;
        }

        for (String uri : RedirectUtils.resolveValidRedirects(session, rootUri, new HashSet<>(redirectUris))) {
            verifyRedirectUri(uri, false);
        }
    }

    void verifyRedirectUri(String redirectUri, boolean isRedirectUriParam) throws ClientPolicyException {
        UriValidation validation;

        try {
            validation = new UriValidation(redirectUri, isRedirectUriParam, configuration);
        } catch (URISyntaxException e) {
            logger.debugv("URISyntaxException - input = {0}, errMessage = {1], errReason = {2}, redirectUri = {3}", e.getInput(), e.getMessage(), e.getReason(), redirectUri);
            throw invalidRedirectUri(ERR_GENERAL);
        }

        validation.validate();
    }

    public static class UriValidation {
        public final URI uri;
        public final boolean isRedirectUriParameter;
        public final Configuration config;

        public UriValidation(String uriString, boolean isRedirectUriParameter, Configuration config) throws URISyntaxException {
            this.uri = new URI(uriString);
            this.isRedirectUriParameter = isRedirectUriParameter;
            this.config = config;
        }

        public void validate() throws ClientPolicyException {
            switch (identifyUriType()) {
                case IPV4_LOOPBACK_ADDRESS:
                    if(!config.isAllowIPv4LoopbackAddress() || !isValidIPv4LoopbackAddress()) {
                        throw invalidRedirectUri(ERR_LOOPBACK);
                    }
                    break;
                case IPV6_LOOPBACK_ADDRESS:
                    if(!config.isAllowIPv6LoopbackAddress() || !isValidIPv6LoopbackAddress()) {
                        throw invalidRedirectUri(ERR_LOOPBACK);
                    }
                    break;
                case PRIVATE_USE_URI_SCHEME:
                    if(!config.isAllowPrivateUseUriScheme() || !isValidPrivateUseUriScheme()) {
                        throw invalidRedirectUri(ERR_PRIVATESCHEME);
                    }
                    break;
                case NORMAL_URI:
                    if(!isValidNormalUri()) {
                        throw invalidRedirectUri(ERR_NORMALURI);
                    }
                    break;
                default :
                    logger.debugv("Invalid URI Type - input = {0}", uri.toString());
                    throw invalidRedirectUri(ERR_GENERAL);
            }
        }

        UriType identifyUriType() {
            // NOTE: the order of evaluation methods is important.
            if (isIPv4LoopbackAddress()) {
                return UriType.IPV4_LOOPBACK_ADDRESS;
            } else if (isIPv6LoopbackAddress()) {
                return UriType.IPV6_LOOPBACK_ADDRESS;
            } else if (isPrivateUseScheme()) {
                return UriType.PRIVATE_USE_URI_SCHEME;
            } else if (isNormalUri()) {
                return UriType.NORMAL_URI;
            } else {
                return UriType.INVALID_URI;
            }
        }

        boolean isIPv4LoopbackAddress() {
            return isLoopbackAddressRedirectUri(i->i instanceof Inet4Address);
        }

        boolean isIPv6LoopbackAddress() {
            return isLoopbackAddressRedirectUri(i->i instanceof Inet6Address);
        }

        boolean isLoopbackAddressRedirectUri(Predicate<InetAddress> p) {
            if (uri.getHost() == null) {
                return false;
            }

            InetAddress addr;
            try {
                addr = InetAddress.getByName(uri.getHost());
            } catch (UnknownHostException e) {
                return false;
            }

            if (!addr.isLoopbackAddress()) {
                return false;
            }

            if (!p.test(addr)) return false;

            return true;
        }

        boolean isPrivateUseScheme() {
            // NOTE: this method assumes that the uri is not loopback address
            return uri.isAbsolute() && !isHttp() && !isHttps();
        }

        boolean isNormalUri() {
            // NOTE: this method assumes that the uri is not loopback address
            return isHttp() || isHttps();
        }

        boolean isValidIPv4LoopbackAddress() {
            return isValidLoopbackAddress(i->true);
        }

        boolean isValidIPv6LoopbackAddress() {
            return isValidLoopbackAddress(i->{
                if (!"[::1]".equals(i.getHost())) { // [::1] is only allowed.
                    logger.debugv("Invalid IPv6LoopbackAddress: unacceptable form - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                } else {
                    return true;
                }
            });
        }

        boolean isValidLoopbackAddress(Predicate<URI> p)  {
            // valid addresses depend on configurations

            if (!config.isAllowHttpScheme() && isHttp()) {
                logger.debugv("Invalid LoopbackAddress: HTTP not allowed - input = {0}", uri.toString());
                return false;
            }

            if (config.isAllowWildcardContextPath()) {
                if (isIncludeInvalidWildcard()) {
                    logger.debugv("Invalid LoopbackAddress: invalid Wildcard - input = {0}", uri.toString());
                    return false;
                }
            } else {
                if (isIncludeWildcard()) {
                    logger.debugv("Invalid LoopbackAddress: Wildcard not allowed - input = {0}", uri.toString());
                    return false;
                }
            }

            if (config.isOAuth2_1Compliant()) {
                if (isIncludeUriFragment()) { // URL fragment is not allowed
                    logger.debugv("Invalid LoopbackAddress: URI fragment not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }

                if (isIncludeWildcard()) { // wildcard is not allowed
                    logger.debugv("Invalid LoopbackAddress: Wildcard not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }

                if ("localhost".equalsIgnoreCase(uri.getHost())) { // "localhost" is not allowed.
                    logger.debugv("Invalid LoopbackAddress: localhost not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }

                if (isRedirectUriParameter) {
                    if (uri.getPort() < 0 || uri.getPort() > 65535) { // only 0 to 65535 are allowed. no port number is not allowed.
                        logger.debugv("Invalid LoopbackAddress: invalid port number - OAuth 2.1 compliant - redirect_uri parameter - input = {0}", uri.toString());
                        return false;
                    }
                } else {
                    if (uri.getPort() > -1) { // any port number is not allowed
                        logger.debugv("Invalid LoopbackAddress: port number not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                        return false;
                    }
                }

                if (!p.test(uri)) return false; // additional tests for OAuth 2.1 compliant
            }
            return true;
        }

        boolean isValidPrivateUseUriScheme() {
            // valid addresses depend on configurations

            if (config.isAllowWildcardContextPath()) {
                if (isIncludeInvalidWildcard()) {
                    logger.debugv("Invalid PrivateUseUriScheme: invalid Wildcard - input = {0}", uri.toString());
                    return false;
                }
            } else {
                if (isIncludeWildcard()) {
                    logger.debugv("Invalid PrivateUseUriScheme: Wildcard not allowed - input = {0}", uri.toString());
                    return false;
                }
            }

            if (config.isOAuth2_1Compliant()) {
                if (isIncludeUriFragment()) { // URL fragment is not allowed
                    logger.debugv("Invalid PrivateUseUriScheme: URI fragment not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }

                if (isIncludeWildcard()) { // wildcard is not allowed
                    logger.debugv("Invalid PrivateUseUriScheme: Wildcard not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }

                if (uri.getScheme() == null || !uri.getScheme().contains(".")) { // a single word scheme name is not allowed.
                    logger.debugv("Invalid PrivateUseUriScheme: a single word scheme name is not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }
            }

            return true;
        }

        boolean isValidNormalUri() {
            // valid addresses depend on configurations

            if (!config.isAllowHttpScheme() && isHttp()) {
                logger.debugv("Invalid NormalUri: HTTP not allowed - input = {0}", uri.toString());
                return false;
            }

            if (config.isAllowWildcardContextPath()) {
                if (isIncludeInvalidWildcard()) {
                    logger.debugv("Invalid NormalUri: invalid Wildcard - input = {0}", uri.toString());
                    return false;
                }
            } else {
                if (isIncludeWildcard()) {
                    logger.debugv("Invalid NormalUri: Wildcard not allowed - input = {0}", uri.toString());
                    return false;
                }
            }

            if (config.getAllowPermittedDomains() != null && !config.getAllowPermittedDomains().isEmpty()) {
                if (!matchDomains(config.getAllowPermittedDomains())) {
                    logger.debugv("Invalid NormalUri: no permitted domain matched - input = {0}", uri.toString());
                    return false;
                }
            }

            if (config.isOAuth2_1Compliant()) {
                if (!isHttps()) { // only https scheme is allowed.
                    logger.debugv("Invalid NormalUri: HTTP not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }

                if (isIncludeUriFragment()) { // URL fragment is not allowed.
                    logger.debugv("Invalid NormalUri: URI fragment not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }

                if (isIncludeWildcard()) { // wildcard is not allowed.
                    logger.debugv("Invalid NormalUri: Wildcard not allowed - OAuth 2.1 compliant - input = {0}", uri.toString());
                    return false;
                }
            }

            return true;
        }

        boolean matchDomain(String domainPattern) {
            return uri.getHost() != null && uri.getHost().matches(domainPattern);
        }

        boolean matchDomains(List<String> permittedDomains) {
            return permittedDomains.stream().anyMatch(this::matchDomain);
        }

        boolean isHttp() {
            return "http".equals(uri.getScheme());
        }

        boolean isHttps() {
            return "https".equals(uri.getScheme());
        }

        boolean isWildcardContextPath() {
            return uri.getPath() != null && (uri.getPath().startsWith("/*") || uri.getPath().startsWith("*"));
        }

        boolean isIncludeUriFragment() {
            return uri.toString().contains("#");
        }

        boolean isIncludeWildcard() {
            return uri.toString().contains("*");
        }

        boolean isIncludeInvalidWildcard() {
            // NOTE: this method assumes that the uri includes at least one wildcard.
            if (!isWildcardContextPath()) {
                return false;
            }
            return uri.toString().length() - uri.toString().replace("*", "").length() != 1;
        }

    }

    private static ClientPolicyException invalidRedirectUri(String errorDetail) {
        return new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, errorDetail);
    }
}
