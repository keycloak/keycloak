/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.model;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.storage.SearchableModelField;


/**
 * Represents a resource server, whose resources are managed and protected. A resource server is basically an existing
 * client application in Keycloak that will also act as a resource server.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ResourceServer {

    public static class SearchableFields {
        public static final SearchableModelField<ResourceServer> ID =           new SearchableModelField<>("id", String.class);
        /** ID of the client (not the clientId) associated with resource server*/
        public static final SearchableModelField<ResourceServer> CLIENT_ID =    new SearchableModelField<>("clientId", String.class);
        public static final SearchableModelField<ResourceServer> REALM_ID =     new SearchableModelField<>("realmId", String.class);
    }

    /**
     * Returns the unique identifier for this instance.
     *
     * @return the unique identifier for this instance
     */
    String getId();

    /**
     * Indicates if the resource server is allowed to manage its own resources remotely using the Protection API.
     *
     * @return {@code true} if the resource server is allowed to managed them remotely
     */
    boolean isAllowRemoteResourceManagement();

    /**
     * Indicates if the resource server is allowed to manage its own resources remotely using the Protection API.
     *
     * @param allowRemoteResourceManagement {@code true} if the resource server is allowed to managed them remotely
     */
    void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement);

    /**
     * Returns the {@code PolicyEnforcementMode} configured for this instance.
     *
     * @return the {@code PolicyEnforcementMode} configured for this instance.
     */
    PolicyEnforcementMode getPolicyEnforcementMode();

    /**
     * Defines a {@code PolicyEnforcementMode} for this instance.
     *
     * @param enforcementMode one of the available options in {@code PolicyEnforcementMode}
     */
    void setPolicyEnforcementMode(PolicyEnforcementMode enforcementMode);

    /**
     * Defines a {@link DecisionStrategy} for this instance, indicating how permissions should be granted depending on the given
     * {@code decisionStrategy}.
     * 
     * @param decisionStrategy the decision strategy
     */
    void setDecisionStrategy(DecisionStrategy decisionStrategy);

    /**
     * Returns the {@link DecisionStrategy} configured for this instance.
     * 
     * @return the decision strategy
     */
    DecisionStrategy getDecisionStrategy();

    /**
     * Returns id of a client that this {@link ResourceServer} is associated with
     * @return id of client
     */
    String getClientId();

    /**
     * Returns reference of a realm that this {@link ResourceServer} belongs to.
     *
     * @return reference of a realm
     */
    RealmModel getRealm();
}
