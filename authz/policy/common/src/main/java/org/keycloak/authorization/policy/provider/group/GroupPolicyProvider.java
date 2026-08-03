/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.attribute.Attributes.Entry;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceType;

import org.jboss.logging.Logger;

import static org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider.Outcome.DENY;
import static org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider.Outcome.GRANT;
import static org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider.Outcome.SKIP;
import static org.keycloak.models.utils.ModelToRepresentation.buildGroupPath;
import static org.keycloak.models.utils.RoleUtils.isDirectMember;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupPolicyProvider implements PolicyProvider, PartialEvaluationPolicyProvider {

    private static final Logger logger = Logger.getLogger(GroupPolicyProvider.class);
    private final BiFunction<Policy, AuthorizationProvider, GroupPolicyRepresentation> representationFunction;

    public GroupPolicyProvider(BiFunction<Policy, AuthorizationProvider, GroupPolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        GroupPolicyRepresentation policy = representationFunction.apply(evaluation.getPolicy(), authorizationProvider);
        RealmModel realm = authorizationProvider.getRealm();
        Attributes.Entry groupsClaim = evaluation.getContext().getIdentity().getAttributes().getValue(policy.getGroupsClaim());

        if (groupsClaim == null || groupsClaim.isEmpty()) {
            List<String> userGroups = evaluation.getRealm().getUserGroups(evaluation.getContext().getIdentity().getId());
            groupsClaim = new Entry(policy.getGroupsClaim(), userGroups);
        }

        Outcome outcome = isGranted(realm, null, policy, groupsClaim);

        if (GRANT.equals(outcome)) {
            evaluation.grant();
        }

        logger.debugf("Groups policy %s evaluated to %s with identity groups %s", policy.getName(), evaluation.getEffect(), groupsClaim);
    }

    private Outcome isGranted(RealmModel realm, UserModel subject, GroupPolicyRepresentation policy, Entry groupsClaim) {
        boolean skipped = false;

        for (GroupPolicyRepresentation.GroupDefinition definition : policy.getGroups()) {
            GroupModel allowedGroup = realm.getGroupById(definition.getId());

            if (allowedGroup == null) {
                continue;
            }

            if (subject != null && !definition.isExtendChildren() && !isDirectMember(subject.getGroupsStream(), allowedGroup)) {
                skipped = true;
                continue;
            }

            String allowedGroupPath = buildGroupPath(allowedGroup);

            for (int i = 0; i < groupsClaim.size(); i++) {
                String group = groupsClaim.asString(i);

                if (group.indexOf('/') != -1) {
                    if (group.equals(allowedGroupPath)) {
                        return GRANT;
                    }

                    if (definition.isExtendChildren() && group.startsWith(allowedGroupPath + "/")) {
                        return GRANT;
                    }
                }

                // in case the group from the claim does not represent a path, we just check an exact name match
                if (group.equals(allowedGroup.getName())) {
                    return GRANT;
                }
            }
        }

        if (skipped) {
            return SKIP;
        }

        return DENY;
    }

    @Override
    public Stream<Policy> getPermissions(KeycloakSession session, ResourceType resourceType, ResourceType groupResourceType, UserModel user) {
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        RealmModel realm = session.getContext().getRealm();
        ClientModel adminPermissionsClient = realm.getAdminPermissionsClient();
        StoreFactory storeFactory = provider.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(adminPermissionsClient);
        PolicyStore policyStore = storeFactory.getPolicyStore();
        Set<String> visited = new HashSet<>();
        List<String> groupIds = user.getGroupsStream()
                .flatMap(group -> {
                    List<String> ids = new ArrayList<>();
                    GroupModel current = group;
                    while (current != null && visited.add(current.getId())) {
                        ids.add(current.getId());
                        current = current.getParent();
                    }
                    return ids.stream();
                })
                .toList();

        return policyStore.findDependentPolicies(resourceServer, resourceType.getType(), groupResourceType == null ? null : groupResourceType.getType(), GroupPolicyProviderFactory.ID, "groups", groupIds);
    }

    @Override
    public boolean evaluate(KeycloakSession session, Policy policy, UserModel subject) {
        return Outcome.GRANT.equals(evaluateOutcome(session, policy, subject));
    }

    @Override
    public Outcome evaluateOutcome(KeycloakSession session, Policy policy, UserModel subject) {
        RealmModel realm = session.getContext().getRealm();
        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        GroupPolicyRepresentation groupPolicy = representationFunction.apply(policy, authorizationProvider);
        List<String> userGroups = subject.getGroupsStream().map(ModelToRepresentation::buildGroupPath)
                .collect(Collectors.toList());
        return isGranted(realm, subject, groupPolicy, new Entry(groupPolicy.getGroupsClaim(), userGroups));
    }

    @Override
    public boolean supports(Policy policy) {
        return GroupPolicyProviderFactory.ID.equals(policy.getType());
    }

    @Override
    public void close() {

    }
}
