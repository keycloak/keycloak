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
import com.mongodb.WriteResult;
import org.keycloak.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.HmacOTP;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_9_2 extends Update {

    @Override
    public String getId() {
        return "1.9.2";
    }

    @Override
    public void update(KeycloakSession session) {
        BasicDBList orArgs = new BasicDBList();
        orArgs.add(new BasicDBObject("type", UserCredentialModel.PASSWORD));
        orArgs.add(new BasicDBObject("type", UserCredentialModel.PASSWORD_HISTORY));

        BasicDBObject elemMatch = new BasicDBObject("$or", orArgs);
        elemMatch.put("algorithm", HmacOTP.HMAC_SHA1);

        BasicDBObject query = new BasicDBObject("credentials", new BasicDBObject("$elemMatch", elemMatch));

        BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("credentials.$.algorithm", Pbkdf2PasswordHashProvider.ID));

        DBCollection users = db.getCollection("users");

        // Not sure how to do in single query
        int countModified = 1;
        while (countModified > 0) {
            WriteResult wr = users.update(query, update, false, true);
            countModified = wr.getN();
            log.debugf("%d credentials modified in current iteration during upgrade to 1.8", countModified);
        }
    }
}
