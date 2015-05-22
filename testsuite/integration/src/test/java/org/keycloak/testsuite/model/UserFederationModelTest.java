package org.keycloak.testsuite.model;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationModelTest extends AbstractModelTest {


    @Test
    public void federationMapperCrudTest() {
        RealmModel realm = realmManager.createRealm("test-realm");
        UserFederationProviderModel fedProvider = realm.addUserFederationProvider("dummy", new TreeMap<String, String>(), 1, "my-cool-provider", -1, -1, 0);
        UserFederationProviderModel fedProvider2 = realm.addUserFederationProvider("dummy", new TreeMap<String, String>(), 1, "my-cool-provider2", -1, -1, 0);

        UserFederationMapperModel mapperModel1 = createMapper("name1", fedProvider.getId(), "key1", "value1");
        UserFederationMapperModel mapperModel2 = createMapper("name2", fedProvider.getId(), "key2", "value2");
        UserFederationMapperModel mapperModel3 = createMapper("name1", fedProvider2.getId(), "key3", "value3");

        mapperModel1 = realm.addUserFederationMapper(mapperModel1);
        mapperModel2 = realm.addUserFederationMapper(mapperModel2);
        mapperModel3 = realm.addUserFederationMapper(mapperModel3);

        commit();

        try {
            UserFederationMapperModel conflictMapper = createMapper("name1", fedProvider.getId(), "key4", "value4");
            realmManager.getRealmByName("test-realm").addUserFederationMapper(conflictMapper);
            commit();
            Assert.fail("Don't expect to end here");
        } catch (ModelDuplicateException expected) {
        }

        realm = realmManager.getRealmByName("test-realm");
        Set<UserFederationMapperModel> mappers = realm.getUserFederationMappers();
        Assert.assertEquals(3, mappers.size());
        Assert.assertTrue(mappers.contains(mapperModel1));
        Assert.assertTrue(mappers.contains(mapperModel2));
        Assert.assertTrue(mappers.contains(mapperModel3));

        mappers = realm.getUserFederationMappersByFederationProvider(fedProvider.getId());
        Assert.assertEquals(2, mappers.size());
        Assert.assertTrue(mappers.contains(mapperModel1));
        Assert.assertTrue(mappers.contains(mapperModel2));

        mapperModel3.getConfig().put("otherKey", "otherValue");
        realm.updateUserFederationMapper(mapperModel3);

        commit();

        realm = realmManager.getRealmByName("test-realm");
        mapperModel3 = realm.getUserFederationMapperById(mapperModel3.getId());
        Assert.assertEquals(2, mapperModel3.getConfig().size());
        Assert.assertEquals("value3", mapperModel3.getConfig().get("key3"));
        Assert.assertEquals("otherValue", mapperModel3.getConfig().get("otherKey"));
    }


    @Test
    public void federationProviderRemovalTest() {
        RealmModel realm = realmManager.createRealm("test-realm");
        UserFederationProviderModel fedProvider = realm.addUserFederationProvider("dummy", new TreeMap<String, String>(), 1, "my-cool-provider", -1, -1, 0);
        UserFederationProviderModel fedProvider2 = realm.addUserFederationProvider("dummy", new TreeMap<String, String>(), 1, "my-cool-provider2", -1, -1, 0);

        UserFederationMapperModel mapperModel1 = createMapper("name1", fedProvider.getId(), "key1", "value1");
        UserFederationMapperModel mapperModel2 = createMapper("name2", fedProvider.getId(), "key2", "value2");
        UserFederationMapperModel mapperModel3 = createMapper("name1", fedProvider2.getId(), "key3", "value3");

        mapperModel1 = realm.addUserFederationMapper(mapperModel1);
        mapperModel2 = realm.addUserFederationMapper(mapperModel2);
        mapperModel3 = realm.addUserFederationMapper(mapperModel3);

        commit();

        realmManager.getRealmByName("test-realm").removeUserFederationProvider(fedProvider);

        commit();

        realm = realmManager.getRealmByName("test-realm");
        Set<UserFederationMapperModel> mappers = realm.getUserFederationMappers();
        Assert.assertEquals(1, mappers.size());
        Assert.assertEquals(mapperModel3, mappers.iterator().next());

        realm = realmManager.getRealmByName("test-realm");
        realmManager.removeRealm(realm);

        commit();
    }

    private UserFederationMapperModel createMapper(String name, String fedProviderId, String... config) {
        UserFederationMapperModel mapperModel = new UserFederationMapperModel();
        mapperModel.setName(name);
        mapperModel.setFederationMapperType("someType");
        mapperModel.setFederationProviderId(fedProviderId);
        Map<String, String> configMap = new TreeMap<String, String>();
        String key = null;
        for (String configEntry : config) {
            if (key == null) {
                key = configEntry;
            } else {
                configMap.put(key, configEntry);
                key = null;
            }
        }
        mapperModel.setConfig(configMap);
        return mapperModel;
    }
}
