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

import java.time.Duration;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.UserEntity;

import static org.keycloak.models.policy.ResourceOperationType.CREATE;
import static org.keycloak.models.policy.ResourceOperationType.LOGIN;

public class UserSessionRefreshTimeResourcePolicyProvider extends AbstractUserResourcePolicyProvider {

    public UserSessionRefreshTimeResourcePolicyProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
    }

    @Override
    public Predicate timePredicate(long time, CriteriaBuilder cb, CriteriaQuery<String> query, Root<UserEntity> userRoot) {
        long currentTimeSeconds = Time.currentTime();
        Path<Long> lastSessionRefreshTime = userRoot.get("lastSessionRefreshTime");
        Expression<Long> lastSessionRefreshTimeExpiration = cb.sum(lastSessionRefreshTime, cb.literal(Duration.ofMillis(time).toSeconds()));
        return cb.and(cb.isNotNull(lastSessionRefreshTime), cb.lessThan(lastSessionRefreshTimeExpiration, cb.literal(currentTimeSeconds)));
    }

    @Override
    protected List<ResourceOperationType> getSupportedOperationsForScheduling() {
        return List.of(CREATE, LOGIN);
    }

    @Override
    protected List<ResourceOperationType> getSupportedOperationsForResetting() {
        return List.of(LOGIN);
    }
}
