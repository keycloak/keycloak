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
package org.keycloak.storage.mongo.entity;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.AbstractIdentifiableEntity;
import org.keycloak.models.mongo.keycloak.entities.CredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.FederatedIdentityEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserConsentEntity;
import org.keycloak.models.mongo.keycloak.entities.UserConsentEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@MongoCollection(collectionName = "federatedusers")
public class FederatedUser extends AbstractIdentifiableEntity implements MongoIdentifiableEntity {
    protected String realmId;
    protected String storageId;

    private Map<String, List<String>> attributes;
    private List<String> roleIds;
    private List<String> groupIds;
    private List<String> requiredActions;
    private List<CredentialEntity> credentials;
    private List<FederatedIdentityEntity> federatedIdentities;

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }

    public List<String> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<String> groupIds) {
        this.groupIds = groupIds;
    }

    public List<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public List<CredentialEntity> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialEntity> credentials) {
        this.credentials = credentials;
    }

    public List<FederatedIdentityEntity> getFederatedIdentities() {
        return federatedIdentities;
    }

    public void setFederatedIdentities(List<FederatedIdentityEntity> federatedIdentities) {
        this.federatedIdentities = federatedIdentities;
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
        // Remove all consents of this user
        DBObject query = new QueryBuilder()
                .and("userId").is(getId())
                .get();

        context.getMongoStore().removeEntities(MongoUserConsentEntity.class, query, true, context);
    }

}
