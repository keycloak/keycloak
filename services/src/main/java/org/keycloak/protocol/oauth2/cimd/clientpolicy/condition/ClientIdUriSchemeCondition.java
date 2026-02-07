package org.keycloak.protocol.oauth2.cimd.clientpolicy.condition;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.condition.AbstractClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * The class is a condition of client policies. On {@code PRE_AUTHORIZATION_REQUEST} event,
 * it checks if the value of {@code client_id} parameter is URI and
 * the scheme part of the URI is the one defined in its configuration.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdUriSchemeCondition extends AbstractClientPolicyConditionProvider<ClientIdUriSchemeCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientIdUriSchemeCondition.class);

    public ClientIdUriSchemeCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<ClientIdUriSchemeCondition.Configuration> getConditionConfigurationClass() {
        return ClientIdUriSchemeCondition.Configuration.class;
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {
        @JsonProperty(ClientIdUriSchemeConditionFactory.CLIENT_ID_URI_SCHEME)
        protected List<String> clientIdUriSchemes = Collections.emptyList();

        @JsonProperty(ClientIdUriSchemeConditionFactory.TRUSTED_DOMAINS)
        protected List<String> trustedDomains = null;

        public List<String> getClientIdUriSchemes() {
            return clientIdUriSchemes;
        }

        public void setClientIdUriSchemes(List<String> clientIdUriSchemes) {
            this.clientIdUriSchemes = clientIdUriSchemes;
        }

        public List<String> getTrustedDomains() {
            return trustedDomains;
        }

        public void setTrustedDomains(List<String> permittedDomains) {
            this.trustedDomains = permittedDomains;
        }
    }

    @Override
    public String getProviderId() {
        return ClientIdUriSchemeConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case PRE_AUTHORIZATION_REQUEST:
                PreAuthorizationRequestContext paContext = (PreAuthorizationRequestContext) context;
                String clientId = ((PreAuthorizationRequestContext) context).getRequestParameters().getFirst(OAuth2Constants.CLIENT_ID);
                if (isUriSchemeMatched(clientId) && isTrustedDomainMatched(clientId)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isUriSchemeMatched(String clientId) {
        if (clientId == null || configuration.getClientIdUriSchemes() == null || configuration.getClientIdUriSchemes().isEmpty()) {
            return false;
        }

        final URI uri;
        try {
            uri = new URI(clientId);
        } catch (URISyntaxException e) {
            logger.debugv("not URL: clientId = {0}", clientId);
            return false;
        }

        return configuration.getClientIdUriSchemes().stream().anyMatch(i->i.equals(uri.getScheme()));
    }

    private boolean isTrustedDomainMatched(String clientId) {
        List<String> trustedDomains = convertContentFilledList(configuration.getTrustedDomains());
        if (trustedDomains == null || trustedDomains.isEmpty()) {
            logger.debug("trusted domain list is vacant.");
            return false;
        }

        final URI uri;
        try {
            uri = new URI(clientId);
        } catch (URISyntaxException e) {
            logger.warnv("Malformed URL: {0}", clientId);
            return false;
        }

        if (uri.getHost() == null) {
            logger.warnv("not trusted domain: host = {0}", uri.getHost());
            return false;
        }

        if (trustedDomains.stream().noneMatch(i->checkTrustedDomain(uri.getHost(), i))) {
            logger.warnv("not trusted domain: host = {0}", uri.getHost());
            return false;
        }

        return true;
    }

    private List<String> convertContentFilledList(List<String> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().filter(Objects::nonNull).filter(i->!i.isBlank()).distinct().toList();
    }

    // apply the same logic in TrustedHostClientRegistrationPolicy.
    private boolean checkTrustedDomain(String hostname, String trustedDomain) {
        if (trustedDomain.startsWith("*.")) {
            String domain = trustedDomain.substring(2);
            return hostname.equals(domain) || hostname.endsWith("." + domain);
        }
        return hostname.equals(trustedDomain);
    }
}
