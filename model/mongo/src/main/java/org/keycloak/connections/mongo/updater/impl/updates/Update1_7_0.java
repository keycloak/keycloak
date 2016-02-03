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
import com.mongodb.DBCursor;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_7_0 extends Update {

    @Override
    public String getId() {
        return "1.7.0";
    }

    @Override
    public void update(KeycloakSession session) throws ClassNotFoundException {
        DBCollection clients = db.getCollection("clients");
        DBCursor clientsCursor = clients.find();

        try {
            while (clientsCursor.hasNext()) {
                BasicDBObject client = (BasicDBObject) clientsCursor.next();

                boolean directGrantsOnly = client.getBoolean("directGrantsOnly", false);
                client.append("standardFlowEnabled", !directGrantsOnly);
                client.append("implicitFlowEnabled", false);
                client.append("directAccessGrantsEnabled", directGrantsOnly);
                client.removeField("directGrantsOnly");

                clients.save(client);
            }
        } finally {
            clientsCursor.close();
        }
    }
}
