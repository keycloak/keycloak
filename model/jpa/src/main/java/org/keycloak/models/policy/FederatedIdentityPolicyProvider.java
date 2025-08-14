/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.policy;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.FederatedIdentityEntity;
import org.keycloak.models.jpa.entities.UserEntity;

public class FederatedIdentityPolicyProvider extends UserLastSessionRefreshTimeResourcePolicyProvider {

    public FederatedIdentityPolicyProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
    }

    @Override
    public Predicate timePredicate(long time, CriteriaBuilder cb, CriteriaQuery<String> query, Root<UserEntity> userRoot) {
        Predicate lastSessionRefreshTimePredicate = super.timePredicate(time, cb, query, userRoot);
        Predicate federatedIdentityByBrokerPredicate = createFederatedIdentityByBrokerPredicate(cb, query, userRoot);

        return cb.and(lastSessionRefreshTimePredicate, federatedIdentityByBrokerPredicate);
    }

    private Predicate createFederatedIdentityByBrokerPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<UserEntity> userRoot) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<?> from = subquery.from(FederatedIdentityEntity.class);

        subquery.select(cb.literal(1));

        List<Predicate> finalPredicates = new ArrayList<>();

        finalPredicates.add(cb.equal(from.get("user").get("id"), userRoot.get("id")));
        finalPredicates.add(from.get("identityProvider").in(getBrokerAliases()));

        subquery.where(finalPredicates.toArray(Predicate[]::new));

        return cb.exists(subquery);
    }

    private List<String> getBrokerAliases() {
        return getModel().getConfig().getOrDefault("broker-aliases", List.of());
    }
}
