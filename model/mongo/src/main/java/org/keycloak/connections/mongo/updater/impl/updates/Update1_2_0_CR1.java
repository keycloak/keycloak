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
public class Update1_2_0_CR1 extends Update {

    @Override
    public String getId() {
        return "1.2.0.CR1";
    }

    @Override
    public void update(KeycloakSession session) {
        deleteEntries("clientSessions");
        deleteEntries("sessions");

        convertApplicationsToClients();
        convertOAuthClientsToClients();

        db.getCollection("realms").update(new BasicDBObject(), new BasicDBObject("$rename", new BasicDBObject("adminAppId", "masterAdminClient")), false, true);

        ensureIndex("userConsents", new String[]{"clientId", "userId"}, true, false);
    }

    private void convertApplicationsToClients() {
        DBCollection applications = db.getCollection("applications");
        applications.dropIndex("realmId_1_name_1");

        applications.update(new BasicDBObject(), new BasicDBObject("$set", new BasicDBObject("consentRequired", false)), false, true);
        applications.update(new BasicDBObject(), new BasicDBObject("$rename", new BasicDBObject("name", "clientId")), false, true);
        renameCollection("applications", "clients");
        log.debugv("Converted applications to clients");

        DBCollection roles = db.getCollection("roles");
        roles.update(new BasicDBObject(), new BasicDBObject("$rename", new BasicDBObject("applicationId", "clientId")), false, true);
        log.debugv("Renamed roles.applicationId to roles.clientId");

        ensureIndex("clients", new String[]{"realmId", "clientId"}, true, false);
    }

    private void convertOAuthClientsToClients() {
        DBCollection clients = db.getCollection("clients");
        DBCollection oauthClients = db.getCollection("oauthClients");
        oauthClients.dropIndex("realmId_1_name_1");

        oauthClients.update(new BasicDBObject(), new BasicDBObject("$rename", new BasicDBObject("name", "clientId")), false, true);
        oauthClients.update(new BasicDBObject(), new BasicDBObject("$set", new BasicDBObject("consentRequired", true)), false, true);

        DBCursor curs = oauthClients.find();
        while (curs.hasNext()) {
            clients.insert(curs.next());
        }

        oauthClients.drop();
        log.debugv("Converted oauthClients to clients");
    }

}
