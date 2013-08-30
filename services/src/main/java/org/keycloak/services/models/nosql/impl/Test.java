package org.keycloak.services.models.nosql.impl;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.keycloak.services.models.nosql.adapters.NoSQLRealm;

/**
 * TODO: delete
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Test {

    public static void main(String[] args) throws UnknownHostException {
        MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
        DB javaDB = mongoClient.getDB("java");

        MongoDBImpl test = new MongoDBImpl(javaDB);
        NoSQLRealm realm = new NoSQLRealm();
        realm.setOid("522085fc31dab908ec31c0cb");
        realm.setProp1("something1");
        realm.setProp2(12);
        test.saveObject(realm);
        System.out.println(realm.getOid());

        realm = test.loadObject(NoSQLRealm.class, "522085fc31dab908ec31c0cb");
        System.out.println("Loaded realm: " + realm);

        Map<String, Object> query = new HashMap<String, Object>();
        query.put("prop1", "sm");
        List<NoSQLRealm> queryResults = test.loadObjects(NoSQLRealm.class, query);
        System.out.println("results1: " + queryResults);

        query.put("prop1", "something2");
        queryResults = test.loadObjects(NoSQLRealm.class, query);
        System.out.println("results2: " + queryResults);

        query.put("prop2", 12);
        queryResults = test.loadObjects(NoSQLRealm.class, query);
        System.out.println("results3: " + queryResults);

        query.put("prop1", "something1");
        queryResults = test.loadObjects(NoSQLRealm.class, query);
        System.out.println("results4: " + queryResults);

        mongoClient.close();
    }
}
