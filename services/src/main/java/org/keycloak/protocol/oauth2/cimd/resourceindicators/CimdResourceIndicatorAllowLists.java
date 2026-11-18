package org.keycloak.protocol.oauth2.cimd.resourceindicators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedHashMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.json.RawJsonValue;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeCondition;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeConditionFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor.Configuration;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutorFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyMode;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.condition.AcrConditionFactory;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAttributesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProviderFactory;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceHostsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.IdentityProviderConditionFactory;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * The resource indicator allow lists of the CIMD executors in the enabled client policies of a realm. They tell whether
 * a resource is currently allowed for a client outside an authorization request, e.g. when a token is issued (see
 * {@link CimdResourceIndicatorAudienceMapper}).
 *
 * <p>The CIMD executors are the executors of any provider whose factory is an
 * {@link AbstractClientIdMetadataDocumentExecutorFactory}, so none of them works while the CIMD feature is disabled. No
 * resource is allowed either while the Client Policies feature or the Resource Indicators feature is disabled.
 *
 * <p>For a client, whether a client policy applies is evaluated with its client ID in the same way as on the
 * {@code PRE_AUTHORIZATION_REQUEST} event, the only event on which the CIMD executors work. The conditions that depend
 * only on the client ({@code client-id-uri}, {@code any-client}, {@code client-attributes}, {@code client-roles}), and
 * the built-in conditions evaluated only on other events (e.g. {@code grant-type}), are evaluated definitely. A policy
 * whose applicability depends on any other condition might apply depending on the values of an actual request, even if
 * the condition abstains without them. A policy allows the intersection of the allow lists of the CIMD executors in its
 * profiles, because all of these executors work whenever the policy applies, and each of them rejects an authorization
 * request whose resource is not in its own allow list. The allow list in effect is the intersection of the ones of the
 * enabled client policies, including the global ones, that apply to the client. If no policy definitely applies, a
 * resource is regarded as not allowed only if none of the policies that might apply allows it, so that an uncertain
 * policy alone does not deny a resource.
 */
final class CimdResourceIndicatorAllowLists {

    private static final Logger logger = Logger.getLogger(CimdResourceIndicatorAllowLists.class);

    // a session attribute (prefix) holding the allow lists of a realm, so that they are loaded once per request.
    private static final String SESSION_ATTRIBUTE = "cimd.resource-indicator-allow-lists.";

    // a substring of the client IDs of the clients managed by CIMD (URLs).
    private static final String CIMD_CLIENT_ID_SUBSTRING = "://";

    // the conditions whose vote on the PRE_AUTHORIZATION_REQUEST event depends only on the client, so that their
    // vote with the client ID alone is definite.
    private static final Set<String> CLIENT_CONDITIONS = Set.of(ClientIdUriSchemeConditionFactory.PROVIDER_ID, AnyClientConditionFactory.PROVIDER_ID,
            ClientAttributesConditionFactory.PROVIDER_ID, ClientRolesConditionFactory.PROVIDER_ID);

    // the conditions that abstain on the PRE_AUTHORIZATION_REQUEST event, regardless of the request, because they
    // are evaluated only on other events.
    private static final Set<String> NON_AUTHORIZATION_REQUEST_CONDITIONS = Set.of(AcrConditionFactory.PROVIDER_ID,
            ClientAccessTypeConditionFactory.PROVIDER_ID, ClientProtocolConditionFactory.PROVIDER_ID, ClientScopesConditionFactory.PROVIDER_ID,
            ClientUpdaterContextConditionFactory.PROVIDER_ID, ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID,
            ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID, ClientUpdaterSourceRolesConditionFactory.PROVIDER_ID,
            GrantTypeConditionFactory.PROVIDER_ID, IdentityProviderConditionFactory.PROVIDER_ID);

    private final List<EnabledPolicy> enabledPolicies;
    // profile name -> the allow list of each CIMD executor in the profile, only for the profiles with CIMD executors
    private final Map<String, List<List<String>>> allowListsByProfile;
    // client ID -> the allow list in effect for the client
    private final Map<String, List<String>> allowListsByClientId = new HashMap<>();

    // a condition of an enabled client policy, with its configuration if it is client-id-uri, otherwise its
    // provider. both are null if the condition cannot be set up.
    private record PolicyCondition(String providerId, ClientIdUriSchemeCondition.Configuration clientIdUriConfiguration,
                                   ClientPolicyConditionProvider<?> provider) {
    }

    // an enabled client policy with conditions, using CIMD executors.
    private record EnabledPolicy(List<String> profiles, boolean strict, List<PolicyCondition> conditions) {
    }

    // the resources that a policy allows for a client, to which it applies or might apply.
    private record PolicyAllowList(Applicability applicability, List<String> allowList) {
    }

    private enum Applicability {
        APPLIES,
        // depending on the values of an actual request.
        MAY_APPLY,
        DOES_NOT_APPLY
    }

    private CimdResourceIndicatorAllowLists(List<EnabledPolicy> enabledPolicies, Map<String, List<List<String>>> allowListsByProfile) {
        this.enabledPolicies = enabledPolicies;
        this.allowListsByProfile = allowListsByProfile;
    }

    /**
     * Tells whether a resource is currently allowed for a client. The client profiles and policies are read once per
     * session, i.e. once per request. Nothing is allowed if it cannot be told, e.g. the client profiles cannot be read.
     *
     * @param session the session, whose context has the realm of the client
     * @param client the client
     * @param resource the resource
     * @return {@code true} if the resource is allowed for the client
     */
    static boolean isAllowed(KeycloakSession session, ClientModel client, String resource) {
        if (client == null || resource == null) {
            return false;
        }
        RealmModel realm = session.getContext().getRealm();
        if (realm == null || !Objects.equals(realm.getId(), client.getRealm().getId())) {
            // the conditions look up the client in the realm of the context.
            return false;
        }
        try {
            CimdResourceIndicatorAllowLists allowLists = get(session, realm);
            return allowLists != null && allowLists.getAllowList(client.getClientId()).contains(resource);
        } catch (RuntimeException e) {
            logger.warnv(e, "resource indicator allow list not evaluated, so the resource is regarded as not allowed: realm = {0}, client = {1}, resource = {2}",
                    realm.getName(), client.getClientId(), resource);
            return false;
        }
    }

    /**
     * Tells whether a CIMD executor that has rejected a resource on an authorization request of a client would still
     * reject it, i.e. whether any client policy that applies or might apply to the client does not allow the resource
     * according to the client profiles and policies as the session reads them. It is used to confirm a rejection
     * against the committed configuration, which might have changed since the authorization request has read it. The
     * rejection is regarded as still valid if it cannot be told.
     *
     * @param session the session, whose context has the realm of the client
     * @param client the client
     * @param resource the resource rejected
     * @return {@code false} if no CIMD executor that applies or might apply to the client rejects the resource
     */
    static boolean isRejected(KeycloakSession session, ClientModel client, String resource) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null || !Objects.equals(realm.getId(), client.getRealm().getId())) {
            return true;
        }
        try {
            CimdResourceIndicatorAllowLists allowLists = get(session, realm);
            return allowLists == null || allowLists.getPolicyAllowLists(client.getClientId()).stream()
                    .anyMatch(policy -> !policy.allowList().contains(resource));
        } catch (RuntimeException e) {
            logger.warnv(e, "resource indicator allow list not evaluated, so the resource is regarded as still rejected: realm = {0}, client = {1}, resource = {2}",
                    realm.getName(), client.getClientId(), resource);
            return true;
        }
    }

    private static CimdResourceIndicatorAllowLists get(KeycloakSession session, RealmModel realm) {
        String attribute = SESSION_ATTRIBUTE + realm.getId();
        CimdResourceIndicatorAllowLists allowLists = (CimdResourceIndicatorAllowLists) session.getAttribute(attribute);
        if (allowLists == null) {
            allowLists = load(session, realm);
            if (allowLists != null) {
                session.setAttribute(attribute, allowLists);
            }
        }
        return allowLists;
    }

    /**
     * @return the allow lists, or {@code null} if the client profiles or the client policies cannot be loaded.
     */
    private static CimdResourceIndicatorAllowLists load(KeycloakSession session, RealmModel realm) {
        if (!Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES)) {
            // no client policy is applied, so no CIMD executor works.
            return new CimdResourceIndicatorAllowLists(List.of(), Map.of());
        }
        ClientProfilesRepresentation profiles;
        ClientPoliciesRepresentation policies;
        try {
            profiles = session.clientPolicy().getClientProfiles(realm, true);
            policies = session.clientPolicy().getClientPolicies(realm, true);
        } catch (ClientPolicyException e) {
            logger.warnv("client profiles or policies not loaded, so no resource is regarded as allowed: realm = {0}, error = {1}",
                    realm.getName(), e.getErrorDetail());
            return null;
        }

        // executor provider ID -> the configuration class of its executors, or null if they are not CIMD executors.
        Map<String, Class<? extends Configuration>> configurationClasses = new HashMap<>();
        Map<String, List<List<String>>> allowListsByProfile = new HashMap<>();
        Set<String> seenProfiles = new java.util.HashSet<>();
        Stream.concat(nullSafe(profiles.getProfiles()), nullSafe(profiles.getGlobalProfiles())).forEach(profile -> {
            if (!seenProfiles.add(profile.getName())) {
                return;
            }
            List<List<String>> allowLists = new ArrayList<>();
            nullSafe(profile.getExecutors()).forEach(executor -> {
                String providerId = executor.getExecutorProviderId();
                if (providerId == null) {
                    return;
                }
                if (!configurationClasses.containsKey(providerId)) {
                    configurationClasses.put(providerId, getConfigurationClass(session, providerId));
                }
                Class<? extends Configuration> configurationClass = configurationClasses.get(providerId);
                if (configurationClass != null) {
                    allowLists.add(readAllowList(realm, profile.getName(), executor, configurationClass));
                }
            });
            if (!allowLists.isEmpty()) {
                allowListsByProfile.merge(profile.getName(), allowLists, (a, b) -> Stream.concat(a.stream(), b.stream()).toList());
            }
        });

        // the global policies (from the security profile) are applied as well as the realm ones. a client policy
        // without conditions is never applied, and the ones without CIMD executors are irrelevant.
        List<EnabledPolicy> enabledPolicies = Stream.concat(nullSafe(policies.getGlobalPolicies()), nullSafe(policies.getPolicies()))
                .filter(p -> Boolean.TRUE.equals(p.isEnabled()))
                .filter(p -> p.getConditions() != null && !p.getConditions().isEmpty())
                .filter(p -> nullSafe(p.getProfiles()).anyMatch(allowListsByProfile::containsKey))
                .map(p -> readEnabledPolicy(session, realm, p))
                .toList();
        return new CimdResourceIndicatorAllowLists(enabledPolicies, allowListsByProfile);
    }

    // the configuration class of the executors of a provider, or null if they are not CIMD executors, which is also
    // the case while the CIMD feature is disabled.
    private static Class<? extends Configuration> getConfigurationClass(KeycloakSession session, String executorProviderId) {
        ProviderFactory<ClientPolicyExecutorProvider> factory = session.getKeycloakSessionFactory()
                .getProviderFactory(ClientPolicyExecutorProvider.class, executorProviderId);
        if (!(factory instanceof AbstractClientIdMetadataDocumentExecutorFactory)) {
            return null;
        }
        ClientPolicyExecutorProvider<?> executor = factory.create(session);
        try {
            Class<?> configurationClass = executor.getExecutorConfigurationClass();
            return configurationClass != null && Configuration.class.isAssignableFrom(configurationClass)
                    ? configurationClass.asSubclass(Configuration.class) : null;
        } finally {
            executor.close();
        }
    }

    private static List<String> readAllowList(RealmModel realm, String profileName, ClientPolicyExecutorRepresentation executor,
                                              Class<? extends Configuration> configurationClass) {
        Configuration configuration;
        try {
            configuration = JsonSerialization.mapper.convertValue(RawJsonValue.unwrap(JsonNode.class, executor.getConfiguration()), configurationClass);
        } catch (RuntimeException e) {
            logger.warnv("executor configuration not readable, so the executor is regarded as allowing no resource: realm = {0}, profile = {1}, executor = {2}, error = {3}",
                    realm.getName(), profileName, executor.getExecutorProviderId(), e.getMessage());
            return Collections.emptyList();
        }
        // an executor without configuration allows nothing.
        return configuration == null ? Collections.emptyList() : nullSafe(configuration.getResourceIndicatorAllowList())
                .filter(Objects::nonNull).filter(i -> !i.isBlank()).distinct().toList();
    }

    private static EnabledPolicy readEnabledPolicy(KeycloakSession session, RealmModel realm, ClientPolicyRepresentation policy) {
        List<PolicyCondition> conditions = policy.getConditions().stream()
                .map(condition -> readPolicyCondition(session, realm, policy, condition))
                .toList();
        boolean strict = ClientPolicyMode.STRICT.name().equalsIgnoreCase(policy.getMode());
        return new EnabledPolicy(nullSafe(policy.getProfiles()).toList(), strict, conditions);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PolicyCondition readPolicyCondition(KeycloakSession session, RealmModel realm, ClientPolicyRepresentation policy,
                                                       ClientPolicyConditionRepresentation condition) {
        String providerId = condition.getConditionProviderId();
        try {
            JsonNode configuration = RawJsonValue.unwrap(JsonNode.class, condition.getConfiguration());
            if (ClientIdUriSchemeConditionFactory.PROVIDER_ID.equals(providerId)) {
                // evaluated without the provider, which logs a mismatch. without configuration, it has the default
                // one, as the provider does.
                ClientIdUriSchemeCondition.Configuration clientIdUriConfiguration = configuration == null || configuration.isNull()
                        ? new ClientIdUriSchemeCondition.Configuration()
                        : JsonSerialization.mapper.convertValue(configuration, ClientIdUriSchemeCondition.Configuration.class);
                return new PolicyCondition(providerId, clientIdUriConfiguration, null);
            }
            if (!CLIENT_CONDITIONS.contains(providerId)) {
                // never evaluated (see getVote), so no provider is needed.
                return new PolicyCondition(providerId, null, null);
            }
            ClientPolicyConditionProviderFactory factory = (ClientPolicyConditionProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(ClientPolicyConditionProvider.class, providerId);
            if (factory == null) {
                throw new IllegalStateException("condition provider not found");
            }
            ClientPolicyConditionProvider provider = factory.create(session);
            // created outside the providers managed by the session, so it is closed with the session explicitly,
            // even if it is not set up.
            session.enlistForClose(provider);
            provider.setupConfiguration((ClientPolicyConditionConfigurationRepresentation) JsonSerialization.mapper.convertValue(
                    configuration, provider.getConditionConfigurationClass()));
            return new PolicyCondition(providerId, null, provider);
        } catch (RuntimeException e) {
            logger.warnv("condition not set up, so it is regarded as possibly satisfied: realm = {0}, policy = {1}, condition = {2}, error = {3}",
                    realm.getName(), policy.getName(), providerId, e.getMessage());
            return new PolicyCondition(providerId, null, null);
        }
    }

    /**
     * @return the allow list in effect for a client. It is empty if the Resource Indicators feature is disabled, the
     * client ID is not a URL, or no CIMD executor applies.
     */
    private List<String> getAllowList(String clientId) {
        if (!Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATORS)) {
            return Collections.emptyList();
        }
        if (clientId == null || !clientId.contains(CIMD_CLIENT_ID_SUBSTRING)) {
            // CIMD manages only clients whose client ID is a URL.
            return Collections.emptyList();
        }
        List<String> allowList = allowListsByClientId.get(clientId);
        if (allowList == null) {
            allowList = evaluateAllowList(clientId);
            allowListsByClientId.put(clientId, allowList);
        }
        return allowList;
    }

    private List<String> evaluateAllowList(String clientId) {
        List<PolicyAllowList> policyAllowLists = getPolicyAllowLists(clientId);
        List<List<String>> applyingAllowLists = policyAllowLists.stream()
                .filter(policy -> policy.applicability() == Applicability.APPLIES).map(PolicyAllowList::allowList).toList();
        if (!applyingAllowLists.isEmpty()) {
            // a resource is effectively allowed only if all the policies that apply allow it.
            return intersect(applyingAllowLists);
        }
        // no CIMD policy definitely applies: a resource is regarded as not allowed only if none of the ones that might
        // apply allows it, so that an uncertain policy alone does not deny a resource.
        return policyAllowLists.stream().map(PolicyAllowList::allowList).flatMap(List::stream).distinct().toList();
    }

    // the resources allowed by each policy that applies, and by each policy that might apply, to the client.
    private List<PolicyAllowList> getPolicyAllowLists(String clientId) {
        List<PolicyAllowList> policyAllowLists = new ArrayList<>();
        for (EnabledPolicy policy : enabledPolicies) {
            Applicability applicability = getApplicability(policy, clientId);
            if (applicability == Applicability.DOES_NOT_APPLY) {
                continue;
            }
            List<List<String>> executorAllowLists = policy.profiles().stream()
                    .map(allowListsByProfile::get)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .toList();
            if (executorAllowLists.isEmpty()) {
                continue;
            }
            // all the CIMD executors of a policy work whenever the policy applies, and each of them rejects a
            // resource not in its own allow list, so the policy allows a resource only if all of them allow it.
            policyAllowLists.add(new PolicyAllowList(applicability, intersect(executorAllowLists)));
        }
        return policyAllowLists;
    }

    // the resources included in all the allow lists, which are not empty.
    private static List<String> intersect(List<List<String>> allowLists) {
        List<String> allowList = new ArrayList<>(allowLists.get(0));
        for (List<String> other : allowLists.subList(1, allowLists.size())) {
            allowList.retainAll(other);
        }
        return allowList;
    }

    // in the same way as DefaultClientPolicyManager evaluates the conditions: a condition voting NO, or abstaining in
    // the strict mode, makes the policy unsatisfied. otherwise, an abstaining condition is ignored, and at least one
    // condition has to be satisfied.
    private static Applicability getApplicability(EnabledPolicy policy, String clientId) {
        boolean satisfied = false;
        boolean uncertain = false;
        for (PolicyCondition condition : policy.conditions()) {
            ClientPolicyVote vote = getVote(condition, clientId);
            if (vote == null) {
                uncertain = true;
                satisfied = true;
            } else if (vote == ClientPolicyVote.ABSTAIN) {
                if (policy.strict()) {
                    return Applicability.DOES_NOT_APPLY;
                }
            } else if (vote == ClientPolicyVote.NO) {
                return Applicability.DOES_NOT_APPLY;
            } else {
                satisfied = true;
            }
        }
        if (!satisfied) {
            return Applicability.DOES_NOT_APPLY;
        }
        return uncertain ? Applicability.MAY_APPLY : Applicability.APPLIES;
    }

    // the vote of a condition on the PRE_AUTHORIZATION_REQUEST event for a client, with its negative logic applied,
    // or null if it is uncertain: any condition other than the known ones might vote differently with the values of
    // an actual request, even if it abstains without them, and so might a condition that cannot be evaluated.
    private static ClientPolicyVote getVote(PolicyCondition condition, String clientId) {
        if (NON_AUTHORIZATION_REQUEST_CONDITIONS.contains(condition.providerId())) {
            return ClientPolicyVote.ABSTAIN;
        }
        ClientPolicyVote vote;
        boolean negativeLogic;
        if (condition.clientIdUriConfiguration() != null) {
            vote = ClientIdUriSchemeCondition.isClientIdMatched(condition.clientIdUriConfiguration(), clientId) ? ClientPolicyVote.YES : ClientPolicyVote.NO;
            negativeLogic = Boolean.TRUE.equals(condition.clientIdUriConfiguration().isNegativeLogic());
        } else if (condition.provider() != null && CLIENT_CONDITIONS.contains(condition.providerId())) {
            try {
                MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
                parameters.putSingle(OAuth2Constants.CLIENT_ID, clientId);
                vote = condition.provider().applyPolicy(new PreAuthorizationRequestContext(clientId, parameters));
                negativeLogic = condition.provider().isNegativeLogic();
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
        if (negativeLogic && vote == ClientPolicyVote.YES) {
            return ClientPolicyVote.NO;
        }
        if (negativeLogic && vote == ClientPolicyVote.NO) {
            return ClientPolicyVote.YES;
        }
        return vote;
    }

    private static <T> Stream<T> nullSafe(List<T> list) {
        return list == null ? Stream.empty() : list.stream();
    }
}
