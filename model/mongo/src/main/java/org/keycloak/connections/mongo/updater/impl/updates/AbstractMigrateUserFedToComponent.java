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
import org.jboss.logging.Logger;
import org.keycloak.storage.UserStorageProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractMigrateUserFedToComponent extends Update {
    private final Logger logger = Logger.getLogger(getClass());

    public void portUserFedToComponent(String providerId) {
        DBCollection realms = db.getCollection("realms");
        DBCursor cursor = realms.find();
        while (cursor.hasNext()) {
            BasicDBObject realm = (BasicDBObject) cursor.next();

            String realmId = realm.getString("_id");
            Set<String> removedProviders = new HashSet<>();

            BasicDBList componentEntities = (BasicDBList) realm.get("componentEntities");
            BasicDBList federationProviders = (BasicDBList) realm.get("userFederationProviders");
            for (Object obj : federationProviders) {
                BasicDBObject fedProvider = (BasicDBObject)obj;
                if (fedProvider.getString("providerName").equals(providerId)) {
                    String id = fedProvider.getString("id");
                    removedProviders.add(id);
                    int priority = fedProvider.getInt("priority");
                    String displayName = fedProvider.getString("displayName");
                    int fullSyncPeriod = fedProvider.getInt("fullSyncPeriod");
                    int changedSyncPeriod = fedProvider.getInt("changedSyncPeriod");
                    int lastSync = fedProvider.getInt("lastSync");
                    BasicDBObject component = new BasicDBObject();
                    component.put("id", id);
                    component.put("name", displayName);
                    component.put("providerType", UserStorageProvider.class.getName());
                    component.put("providerId", providerId);
                    component.put("parentId", realmId);

                    BasicDBObject config = new BasicDBObject();
                    config.put("priority", Collections.singletonList(Integer.toString(priority)));
                    config.put("fullSyncPeriod", Collections.singletonList(Integer.toString(fullSyncPeriod)));
                    config.put("changedSyncPeriod", Collections.singletonList(Integer.toString(changedSyncPeriod)));
                    config.put("lastSync", Collections.singletonList(Integer.toString(lastSync)));

                    BasicDBObject fedConfig = (BasicDBObject)fedProvider.get("config");
                    if (fedConfig != null) {
                        for (Map.Entry<String, Object> attr : new HashSet<>(fedConfig.entrySet())) {
                            String attrName = attr.getKey();
                            String attrValue = attr.getValue().toString();
                            config.put(attrName, Collections.singletonList(attrValue));

                        }
                    }


                    component.put("config", config);

                    componentEntities.add(component);

                }
            }
            Iterator<Object> it = federationProviders.iterator();
            while (it.hasNext()) {
                BasicDBObject fedProvider = (BasicDBObject)it.next();
                String id = fedProvider.getString("id");
                if (removedProviders.contains(id)) {
                    it.remove();
                }

            }
            realms.update(new BasicDBObject().append("_id", realmId), realm);
        }
    }

    public void portUserFedMappersToComponent(String providerId, String mapperType) {
        //logger.info("*** port mappers");
        DBCollection realms = db.getCollection("realms");
        DBCursor cursor = realms.find();
        while (cursor.hasNext()) {
            BasicDBObject realm = (BasicDBObject) cursor.next();

            String realmId = realm.getString("_id");
            Set<String> removedProviders = new HashSet<>();

            BasicDBList componentEntities = (BasicDBList) realm.get("componentEntities");
            BasicDBList federationProviders = (BasicDBList) realm.get("userFederationProviders");
            BasicDBList fedMappers = (BasicDBList) realm.get("userFederationMappers");
            for (Object obj : federationProviders) {
                BasicDBObject fedProvider = (BasicDBObject)obj;
                String providerName = fedProvider.getString("providerName");
                //logger.info("looking for mappers of fed provider: " + providerName);
                if (providerName.equals(providerId)) {
                    String id = fedProvider.getString("id");
                    //logger.info("found fed provider: " + id + ", looking at mappers");
                    for (Object obj2 : fedMappers) {
                        BasicDBObject fedMapper = (BasicDBObject)obj2;
                        String federationProviderId = fedMapper.getString("federationProviderId");
                        //logger.info("looking at mapper with federationProviderId: " + federationProviderId);
                        if (federationProviderId.equals(id)) {
                            String name = fedMapper.getString("name");
                            String mapperId = fedMapper.getString("id");
                            removedProviders.add(mapperId);
                            String mapperProviderId = fedMapper.getString("federationMapperType");
                            BasicDBObject component = new BasicDBObject();
                            component.put("id", mapperId);
                            component.put("name", name);
                            component.put("providerType", mapperType);
                            component.put("providerId", mapperProviderId);
                            component.put("parentId", id);

                            BasicDBObject fedConfig = (BasicDBObject)fedMapper.get("config");
                            BasicDBObject config = new BasicDBObject();
                            if (fedConfig != null) {
                                for (Map.Entry<String, Object> attr : new HashSet<>(fedConfig.entrySet())) {
                                    String attrName = attr.getKey();
                                    String attrValue = attr.getValue().toString();
                                    config.put(attrName, Collections.singletonList(attrValue));

                                }
                            }
                            component.put("config", config);
                            componentEntities.add(component);
                        }
                    }
                }
            }
            Iterator<Object> it = fedMappers.iterator();
            while (it.hasNext()) {
                BasicDBObject fedMapper = (BasicDBObject)it.next();
                String id = fedMapper.getString("id");
                if (removedProviders.contains(id)) {
                    it.remove();
                }

            }
            realms.update(new BasicDBObject().append("_id", realmId), realm);
        }
    }
}
