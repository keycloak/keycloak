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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.mongo.keycloak.entities.ComponentEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Collections;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update2_3_0 extends Update {

    @Override
    public String getId() {
        return "2.3.0";
    }

    @Override
    public void update(KeycloakSession session) {

        DBCollection realms = db.getCollection("realms");
        DBCursor cursor = realms.find();
        while (cursor.hasNext()) {
            BasicDBObject realm = (BasicDBObject) cursor.next();

            String realmId = realm.getString("_id");

            String privateKeyPem = realm.getString("privateKeyPem");
            String certificatePem = realm.getString("certificatePem");

            BasicDBList entities = (BasicDBList) realm.get("componentEntities");

            BasicDBObject component = new BasicDBObject();
            component.put("id", KeycloakModelUtils.generateId());
            component.put("name", "rsa");
            component.put("providerType", KeyProvider.class.getName());
            component.put("providerId", "rsa");
            component.put("parentId", realmId);

            BasicDBObject config = new BasicDBObject();
            config.put("priority", Collections.singletonList("100"));
            config.put("privateKey", Collections.singletonList(privateKeyPem));
            config.put("certificate", Collections.singletonList(certificatePem));

            component.put("config", config);

            entities.add(component);

            realm.remove("privateKeyPem");
            realm.remove("certificatePem");
            realm.remove("publicKeyPem");
            realm.remove("codeSecret");

            realms.update(new BasicDBObject().append("_id", realmId), realm);
        }
    }
}
