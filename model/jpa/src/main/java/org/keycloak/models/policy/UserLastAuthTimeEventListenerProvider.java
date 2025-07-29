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

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.jpa.entities.UserEntity;

public class UserLastAuthTimeEventListenerProvider implements EventListenerProvider {

    private final EntityManager em;

    public UserLastAuthTimeEventListenerProvider(EntityManager em) {
        this.em = em;
    }

    @Override
    public void onEvent(Event event) {
        switch (event.getType()) {
            case LOGIN -> {
                updateLastAutenticationTime(event);
                resetPolicyState(event.getUserId());
            }
            case REGISTER -> updateLastAutenticationTime(event);
            //TODO handle all other necessary events
            default -> {
                //no-op
            }
        }
    }

    private void updateLastAutenticationTime(Event event) {
        String userId = event.getUserId();
        UserEntity user = em.find(UserEntity.class, userId);
        if (user != null) user.setLastAuthenticationTime(event.getTime());
    }

    private void resetPolicyState(String userId) {
        if (userId == null) return;

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<ResourcePolicyStateEntity> delete = cb.createCriteriaDelete(ResourcePolicyStateEntity.class);
        Root<ResourcePolicyStateEntity> stateRoot = delete.from(ResourcePolicyStateEntity.class);

        Predicate userIdPredicate = cb.equal(stateRoot.get("resourceId"), userId);
        Predicate providerIdPredicate = cb.equal(stateRoot.get("policyProviderId"), UserLastAuthTimeResourcePolicyProviderFactory.ID);

        delete.where(cb.and(userIdPredicate, providerIdPredicate));

        em.createQuery(delete).executeUpdate();
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // No-op for admin events
    }

    @Override
    public void close() {
    }
}
