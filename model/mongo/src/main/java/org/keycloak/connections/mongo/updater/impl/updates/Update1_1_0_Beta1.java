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

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Update1_1_0_Beta1 extends Update {

    @Override
    public String getId() {
        return "1.1.0.Beta1";
    }

    @Override
    public void update(KeycloakSession session) {
        deleteEntries("clientSessions");
        deleteEntries("sessions");

        addRealmCodeSecret();
    }

    private void addRealmCodeSecret() {
        DBCollection realms = db.getCollection("realms");

        DBObject query = new QueryBuilder()
                .and("codeSecret").is(null).get();

        DBCursor objects = realms.find(query);
        while (objects.hasNext()) {
            DBObject object = objects.next();
            object.put("codeSecret", KeycloakModelUtils.generateCodeSecret());
            realms.save(object);

            log.debugv("Added realm.codeSecret, id={0}", object.get("id"));
        }
    }

}
