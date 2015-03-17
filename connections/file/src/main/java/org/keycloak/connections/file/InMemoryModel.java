/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.connections.file;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * This class provides an in-memory copy of the entire model for each
 * Keycloak session.  At the start of the session, the model is read
 * from JSON.  When the session's transaction ends, the model is written back
 * out.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class InMemoryModel {
    private final Map<String, RealmModel> allRealms = new HashMap<String, RealmModel>();

    //                realmId,    userId, userModel
    private final Map<String, Map<String,UserModel>> allUsers = new HashMap<String, Map<String,UserModel>>();

    public InMemoryModel() {
    }

    public void putRealm(String id, RealmModel realm) {
        allRealms.put(id, realm);
        allUsers.put(id, new HashMap<String, UserModel>());
    }

    public RealmModel getRealm(String id) {
        return allRealms.get(id);
    }

    public Collection<RealmModel> getRealms() {
       return allRealms.values();
    }

    public RealmModel getRealmByName(String name) {
        for (RealmModel realm : getRealms()) {
            if (realm.getName().equals(name)) return realm;
        }

        return null;
    }

    public boolean removeRealm(String id) {
        allUsers.remove(id);
        return (allRealms.remove(id) != null);
    }

    protected Map<String, UserModel> realmUsers(String realmId) {
        Map<String, UserModel> realmUsers = allUsers.get(realmId);
        if (realmUsers == null) throw new NullPointerException("Realm users not found for id=" + realmId);
        return realmUsers;
    }

    public void putUser(String realmId, String userId, UserModel user) {
        realmUsers(realmId).put(userId, user);
    }

    public UserModel getUser(String realmId, String userId) {
        return realmUsers(realmId).get(userId);
    }

    public boolean hasUserWithUsername(String realmId, String username) {
        for (UserModel user : getUsers(realmId)) {
            if (user.getUsername().equals(username)) return true;
        }

        return false;
    }

    public Collection<UserModel> getUsers(String realmId) {
       return realmUsers(realmId).values();
    }

    public boolean removeUser(String realmId, String userId) {
        return (realmUsers(realmId).remove(userId) != null);
    }

}
