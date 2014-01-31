package org.keycloak.models.mongo.test;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.QueryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.impl.MongoStoreImpl;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBModelTest {

    private static final Class<? extends MongoEntity>[] MANAGED_DATA_TYPES = (Class<? extends MongoEntity>[])new Class<?>[] {
            Person.class,
            Address.class,
    };

    private MongoClient mongoClient;
    private MongoStore mongoDB;

    @Before
    public void before() throws Exception {
        try {
            // TODO: authentication support
            mongoClient = new MongoClient("localhost", 27017);

            DB db = mongoClient.getDB("keycloakTest");
            mongoDB = new MongoStoreImpl(db, true, MANAGED_DATA_TYPES);

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void after() throws Exception {
        mongoClient.close();
    }

    @Test
    public void mongoModelTest() throws Exception {
        // Add some user
        Person john = new Person();
        john.setFirstName("john");
        john.setAge(25);
        john.setGender(Person.Gender.MALE);

        mongoDB.insertObject(john);

        // Add another user
        Person mary = new Person();
        mary.setFirstName("mary");
        mary.setKids(Arrays.asList("Peter", "Paul", "Wendy"));

        Address addr1 = new Address();
        addr1.setStreet("Elm");
        addr1.setNumber(5);
        addr1.setFlatNumbers(Arrays.asList("flat1", "flat2"));
        Address addr2 = new Address();
        List<Address> addresses = new ArrayList<Address>();
        addresses.add(addr1);
        addresses.add(addr2);

        mary.setAddresses(addresses);
        mary.setMainAddress(addr1);
        mary.setGender(Person.Gender.FEMALE);
        mary.setGenders(Arrays.asList(Person.Gender.FEMALE));

        mongoDB.insertObject(mary);

        Assert.assertEquals(2, mongoDB.loadObjects(Person.class, new QueryBuilder().get()).size());

        DBObject query = new QueryBuilder().and("addresses.flatNumbers").is("flat1").get();
        List<Person> persons = mongoDB.loadObjects(Person.class, query);
        Assert.assertEquals(1, persons.size());
        mary = persons.get(0);
        Assert.assertEquals(mary.getFirstName(), "mary");
        Assert.assertTrue(mary.getKids().contains("Paul"));
        Assert.assertEquals(2, mary.getAddresses().size());
        Assert.assertEquals(Address.class, mary.getAddresses().get(0).getClass());

        // Test push/pull
        mongoDB.pushItemToList(mary, "kids", "Pauline", true);
        mongoDB.pullItemFromList(mary, "kids", "Paul");

        Address addr3 = new Address();
        addr3.setNumber(6);
        addr3.setStreet("Broadway");
        mongoDB.pushItemToList(mary, "addresses", addr3, true);

        mary = mongoDB.loadObject(Person.class, mary.getId());
        Assert.assertEquals(3, mary.getKids().size());
        Assert.assertTrue(mary.getKids().contains("Pauline"));
        Assert.assertFalse(mary.getKids().contains("Paul"));
        Assert.assertEquals(3, mary.getAddresses().size());
        Address mainAddress = mary.getMainAddress();
        Assert.assertEquals("Elm", mainAddress.getStreet());
        Assert.assertEquals(5, mainAddress.getNumber());
        Assert.assertEquals(Person.Gender.FEMALE, mary.getGender());
        Assert.assertTrue(mary.getGenders().contains(Person.Gender.FEMALE));


        // Some test of Map (attributes)
        mary.addAttribute("attr1", "value1");
        mary.addAttribute("attr2", "value2");
        mary.addAttribute("attr.some3", "value3");
        mongoDB.updateObject(mary);

        mary = mongoDB.loadObject(Person.class, mary.getId());
        Assert.assertEquals(3, mary.getAttributes().size());

        mary.removeAttribute("attr2");
        mary.removeAttribute("nonExisting");
        mongoDB.updateObject(mary);

        mary = mongoDB.loadObject(Person.class, mary.getId());
        Assert.assertEquals(2, mary.getAttributes().size());
        Assert.assertEquals("value1", mary.getAttributes().get("attr1"));
        Assert.assertEquals("value3", mary.getAttributes().get("attr.some3"));
    }
}
