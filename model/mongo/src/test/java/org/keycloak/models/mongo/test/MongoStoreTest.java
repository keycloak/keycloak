package org.keycloak.models.mongo.test;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.impl.MongoStoreImpl;
import org.keycloak.models.mongo.impl.context.TransactionMongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.config.MongoClientProvider;
import org.keycloak.models.mongo.keycloak.config.MongoClientProviderHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoStoreTest {

    private static final Class<?>[] MANAGED_DATA_TYPES = (Class<?>[])new Class<?>[] {
            Person.class,
            Address.class,
            AddressWithFlats.class
    };

    private MongoClientProvider mongoClientProvider;
    private MongoStore mongoStore;

    @Before
    public void before() throws Exception {
        mongoClientProvider = MongoClientProviderHolder.getInstance();
        mongoStore = new MongoStoreImpl(mongoClientProvider.getDB(), true, MANAGED_DATA_TYPES);
    }

    @After
    public void after() throws Exception {
        mongoClientProvider.close();
    }

    @Test
    public void mongoModelTest() throws Exception {
        MongoStoreInvocationContext context = new TransactionMongoStoreInvocationContext(mongoStore);

        // Add some user
        Person john = new Person();
        john.setFirstName("john");
        john.setAge(25);
        john.setGender(Person.Gender.MALE);

        mongoStore.insertEntity(john, context);

        // Add another user
        Person mary = new Person();
        mary.setFirstName("mary");
        mary.setKids(asList("Peter", "Paul", "Wendy"));

        AddressWithFlats addr1 = new AddressWithFlats();
        addr1.setStreet("Elm");
        addr1.setNumber(5);
        addr1.setFlatNumbers(asList("flat1", "flat2"));
        AddressWithFlats addr2 = new AddressWithFlats();
        List<AddressWithFlats> addresses = new ArrayList<AddressWithFlats>();
        addresses.add(addr1);
        addresses.add(addr2);

        mary.setAddresses(addresses);
        mary.setMainAddress(addr1);
        mary.setGender(Person.Gender.FEMALE);
        mary.setGenders(asList(Person.Gender.FEMALE));

        mongoStore.insertEntity(mary, context);

        Assert.assertEquals(2, mongoStore.loadEntities(Person.class, new QueryBuilder().get(), context).size());

        // Commit this context
        context.commit();

        Assert.assertEquals(2, mongoStore.loadEntities(Person.class, new QueryBuilder().get(), context).size());

        DBObject query = new QueryBuilder().and("addresses.flatNumbers").is("flat1").get();
        List<Person> persons = mongoStore.loadEntities(Person.class, query, context);
        Assert.assertEquals(1, persons.size());
        mary = persons.get(0);
        Assert.assertEquals(mary.getFirstName(), "mary");
        Assert.assertTrue(mary.getKids().contains("Paul"));
        Assert.assertEquals(2, mary.getAddresses().size());
        Assert.assertEquals(AddressWithFlats.class, mary.getAddresses().get(0).getClass());

        // Test push/pull
        mongoStore.pushItemToList(mary, "kids", "Pauline", true, context);
        mongoStore.pullItemFromList(mary, "kids", "Paul", context);

        Address addr3 = new Address();
        addr3.setNumber(6);
        addr3.setStreet("Broadway");
        mongoStore.pushItemToList(mary, "addresses", addr3, true, context);

        mary = mongoStore.loadEntity(Person.class, mary.getId(), context);
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
        mongoStore.updateEntity(mary, context);

        mary = mongoStore.loadEntity(Person.class, mary.getId(), context);
        Assert.assertEquals(3, mary.getAttributes().size());

        mary.removeAttribute("attr2");
        mary.removeAttribute("nonExisting");
        mongoStore.updateEntity(mary, context);

        mary = mongoStore.loadEntity(Person.class, mary.getId(), context);
        Assert.assertEquals(2, mary.getAttributes().size());
        Assert.assertEquals("value1", mary.getAttributes().get("attr1"));
        Assert.assertEquals("value3", mary.getAttributes().get("attr.some3"));

        context.commit();
    }

    private <T> List<T> asList(T... objects) {
        List<T> list = new ArrayList<T>();
        list.addAll(Arrays.asList(objects));
        return list;
    }
}
