package org.keycloak.test.nosql;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;
import org.keycloak.services.models.nosql.impl.MongoDBImpl;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBModelTest {

    private static final Class<? extends NoSQLObject>[] MANAGED_DATA_TYPES = (Class<? extends NoSQLObject>[])new Class<?>[] {
            Person.class,
            Address.class,
    };

    private MongoClient mongoClient;
    private NoSQL mongoDB;

    @Before
    public void before() throws Exception {
        try {
            // TODO: authentication support
            mongoClient = new MongoClient("localhost", 27017);

            DB db = mongoClient.getDB("keycloakTest");
            mongoDB = new MongoDBImpl(db, true, MANAGED_DATA_TYPES);

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void after() throws Exception {
        mongoClient.close();
    }

    // @Test
    public void mongoModelTest() throws Exception {
        // Add some user
        Person john = new Person();
        john.setFirstName("john");
        john.setAge(25);

        mongoDB.saveObject(john);

        // Add another user
        Person mary = new Person();
        mary.setFirstName("mary");
        mary.setKids(Arrays.asList(new String[] {"Peter", "Paul", "Wendy"}));

        Address addr1 = new Address();
        addr1.setStreet("Elm");
        addr1.setNumber(5);
        addr1.setFlatNumbers(Arrays.asList(new String[] {"flat1", "flat2"}));
        Address addr2 = new Address();
        List<Address> addresses = new ArrayList<Address>();
        addresses.add(addr1);
        addresses.add(addr2);

        mary.setAddresses(addresses);
        mongoDB.saveObject(mary);

        Assert.assertEquals(2, mongoDB.loadObjects(Person.class, mongoDB.createQueryBuilder().build()).size());

        NoSQLQuery query = mongoDB.createQueryBuilder().andCondition("addresses.flatNumbers", "flat1").build();
        List<Person> persons = mongoDB.loadObjects(Person.class, query);
        Assert.assertEquals(1, persons.size());
        mary = persons.get(0);
        Assert.assertEquals(mary.getFirstName(), "mary");
        Assert.assertTrue(mary.getKids().contains("Paul"));
        Assert.assertEquals(2, mary.getAddresses().size());
        Assert.assertEquals(Address.class, mary.getAddresses().get(0).getClass());

        // Test push/pull
        mongoDB.pushItemToList(mary, "kids", "Pauline");
        mongoDB.pullItemFromList(mary, "kids", "Paul");

        Address addr3 = new Address();
        addr3.setNumber(6);
        addr3.setStreet("Broadway");
        mongoDB.pushItemToList(mary, "addresses", addr3);

        mary = mongoDB.loadObject(Person.class, mary.getId());
        Assert.assertEquals(3, mary.getKids().size());
        Assert.assertTrue(mary.getKids().contains("Pauline"));
        Assert.assertFalse(mary.getKids().contains("Paul"));
        Assert.assertEquals(3, mary.getAddresses().size());
    }
}
