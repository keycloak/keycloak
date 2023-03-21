package org.keycloak.quarkus.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Driver;
import java.util.*;

public class KeycloakClassLoaderTest {
    private static final Map<String, List<URL>> NAME_TO_RESOURCES;
    static {
        try {
            Map<String, List<URL>> enumerationMap = new HashMap<>();

            List<URL> urls = new ArrayList<>();
            urls.add(new URL("file://Test.class"));
            urls.add(new URL("file://foo.Test.class"));
            enumerationMap.put("Test", urls);

            urls = new ArrayList<>();
            urls.add(new URL("file://Driver.class"));
            urls.add(new URL("file://Other.class"));
            urls.add(new URL("file://TestDriver.class"));
            urls.add(new URL("file://TestWrapperDriver.class"));
            enumerationMap.put(Driver.class.getName(), urls);

            NAME_TO_RESOURCES = Collections.unmodifiableMap(enumerationMap);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetResourcesWithLazyLoadingDrivers() throws Exception {
        ClassLoader kcLoader = new KeycloakClassLoader(new DummyClassLoader(), true);

        Enumeration<URL> kcLoaderResources = kcLoader.getResources("Test");
        assertEquals(NAME_TO_RESOURCES.get("Test"), kcLoaderResources);

        kcLoaderResources = kcLoader.getResources(Driver.class.getName());
        Assert.assertFalse(kcLoaderResources.hasMoreElements());
    }

    @Test
    public void testGetResourcesWithoutLazyLoadingDrivers() throws Exception {
        ClassLoader kcLoader = new KeycloakClassLoader(new DummyClassLoader(), false);

        Enumeration<URL> kcLoaderResources = kcLoader.getResources("Test");
        assertEquals(NAME_TO_RESOURCES.get("Test"), kcLoaderResources);

        kcLoaderResources = kcLoader.getResources(Driver.class.getName());
        assertEquals(NAME_TO_RESOURCES.get(Driver.class.getName()), kcLoaderResources);
    }

    @Test
    public void testDefaultBehaviorIsLazyLoad() {
        Assert.assertTrue(KeycloakClassLoader.DEFAULT_LAZY_LOAD_DATABASE_DRIVERS);
    }

    private void assertEquals(List<?> expectedList, Enumeration<?> actual) {

        List<Object> actualList = new ArrayList<>();
        actual.asIterator().forEachRemaining(e -> actualList.add(e));

        Assert.assertEquals(expectedList, actualList);
    }

    private static class DummyClassLoader extends ClassLoader {
        public Enumeration<URL> getResources(String name) {
            return Collections.enumeration(NAME_TO_RESOURCES.get(name));
        }
    }
}
