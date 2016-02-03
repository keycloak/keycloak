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

package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.keycloak.connections.mongo.updater.impl.DefaultMongoUpdaterProvider;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Update1_0_0_Final extends Update {

    @Override
    public String getId() {
        return "1.0.0.Final";
    }

    @Override
    public void update(KeycloakSession session) throws ClassNotFoundException {
        DBCollection realmsCollection = db.getCollection("realms");
        realmsCollection.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));

        DefaultMongoUpdaterProvider.log.debugv("Created collection {0}", "realms");

        createCollection("users");
        ensureIndex("users", new String[] { "realmId", "username"}, true, false);
        ensureIndex("users", "emailIndex", true, true);

        createCollection("roles");
        ensureIndex("roles", "nameIndex", true, false);

        createCollection("applications");
        ensureIndex("applications", new String[]{"realmId", "name"}, true, false);

        createCollection("oauthClients");
        ensureIndex("oauthClients", new String[] { "realmId", "name"}, true, false);

        createCollection("userFailures");

        createCollection("sessions");

        createCollection("clientSessions");
    }

}
