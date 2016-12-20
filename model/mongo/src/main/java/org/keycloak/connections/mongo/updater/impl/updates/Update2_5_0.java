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
import org.keycloak.provider.ProviderFactory;
import org.keycloak.storage.UserStorageProvider;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update2_5_0 extends AbstractMigrateUserFedToComponent {

    @Override
    public String getId() {
        return "2.5.0";
    }

    @Override
    public void update(KeycloakSession session) {
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(UserStorageProvider.class);
        for (ProviderFactory factory : factories) {
            portUserFedToComponent(factory.getId());
        }
        
        DBCollection realms = db.getCollection("realms");
        try (DBCursor realmsCursor = realms.find()) {
            while (realmsCursor.hasNext()) {
                BasicDBObject realm = (BasicDBObject) realmsCursor.next();
                realm.append("loginWithEmailAllowed", true);
                realm.append("duplicateEmailsAllowed", false);
                realms.save(realm);
            }
        }
    }

}
