package org.keycloak.client.registration.cli.util;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.client.registration.cli.common.AttributeKey;
import org.keycloak.client.registration.cli.common.AttributeKey.Component;
import org.keycloak.client.registration.cli.common.AttributeOperation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.keycloak.client.registration.cli.common.AttributeOperation.Type.DELETE;
import static org.keycloak.client.registration.cli.common.AttributeOperation.Type.SET;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ReflectionUtilTest {

    @Ignore
    @Test
    public void testListAttributes() {
        LinkedHashMap<String, String> items = null;
/*
        items = getAttributeListWithJSonTypes(Data.class, new AttributeKey(""));

        for (Map.Entry<String, String> item: items.entrySet()) {
            System.out.printf("%-40s %s\n", item.getKey(), item.getValue());
        }
*/
/*
        System.out.println("\n-- nested ------------------------\n");

        items = getAttributeListWithJSonTypes(Data.class, new AttributeKey("nested"));
        for (Map.Entry<String, String> item: items.entrySet()) {
            System.out.printf("%-40s %s\n", item.getKey(), item.getValue());
        }
*/

        System.out.println("\n-- dataList ----------------------\n");

        items = ReflectionUtil.getAttributeListWithJSonTypes(Data.class, new AttributeKey("dataList"));
        for (Map.Entry<String, String> item: items.entrySet()) {
            System.out.printf("%-40s %s\n", item.getKey(), item.getValue());
        }

        if (items.size() == 0) {
            Field f = ReflectionUtil.resolveField(Data.class, new AttributeKey("dataList"));
            String ts = ReflectionUtil.getTypeString(null, f);
            Type t = f.getGenericType();
            if ((List.class.isAssignableFrom(f.getType()) || f.getType().isArray()) && t instanceof ParameterizedType) {
                System.out.printf("%s, where object is:\n", ts);
            }
            t = ((ParameterizedType) t).getActualTypeArguments()[0];
            if (t instanceof Class) {
                items = ReflectionUtil.getAttributeListWithJSonTypes((Class) t, null);
                for (Map.Entry<String, String> item: items.entrySet()) {
                    System.out.printf("   %-37s %s\n", item.getKey(), item.getValue());
                }
            }
        }
    }

    @Test
    public void testSettingAttibutes() {
        Data data = new Data();

        LinkedList<AttributeOperation> attrs = new LinkedList<>();

        attrs.add(new AttributeOperation(SET, "longAttr", "42"));
        attrs.add(new AttributeOperation(SET, "strAttr", "not null"));
        attrs.add(new AttributeOperation(SET, "strList+", "two"));
        attrs.add(new AttributeOperation(SET, "strList+", "three"));
        attrs.add(new AttributeOperation(SET, "strList[0]+", "one"));
        attrs.add(new AttributeOperation(SET, "config", "{\"key1\": \"value1\"}"));
        attrs.add(new AttributeOperation(SET, "config.key2", "value2"));
        attrs.add(new AttributeOperation(SET, "nestedConfig", "{\"key1\": {\"sub key1\": \"sub value1\"}}"));
        attrs.add(new AttributeOperation(SET, "nestedConfig.key1.\"sub key2\"", "sub value2"));
        attrs.add(new AttributeOperation(SET, "nested.strList", "[1,2,3,4]"));
        attrs.add(new AttributeOperation(SET, "nested.dataList+", "{\"baseAttr\": \"item1\", \"strList\": [\"confidential\", \"public\"]}"));
        attrs.add(new AttributeOperation(SET, "nested.dataList+", "{\"baseAttr\": \"item2\", \"strList\": [\"external\"]}"));
        attrs.add(new AttributeOperation(SET, "nested.dataList[1].baseAttr", "changed item2"));
        attrs.add(new AttributeOperation(SET, "nested.nested.strList", "[\"first\",\"second\"]"));
        attrs.add(new AttributeOperation(DELETE, "nested.strList[1]"));
        attrs.add(new AttributeOperation(SET, "nested.nested.nested", "{\"baseAttr\": \"NEW VALUE\", \"strList\": [true, false]}"));
        attrs.add(new AttributeOperation(SET, "nested.strAttr", "NOT NULL"));
        attrs.add(new AttributeOperation(DELETE, "nested.strAttr"));

        ReflectionUtil.setAttributes(data, attrs);

        Assert.assertEquals("longAttr", Long.valueOf(42), data.getLongAttr());
        Assert.assertEquals("strAttr", "not null", data.getStrAttr());
        Assert.assertEquals("strList", Arrays.asList("one", "two", "three"), data.getStrList());

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("key1", "value1");
        expectedMap.put("key2", "value2");
        Assert.assertEquals("config", expectedMap, data.getConfig());


        expectedMap = new HashMap<>();
        expectedMap.put("sub key1", "sub value1");
        expectedMap.put("sub key2", "sub value2");

        Assert.assertNotNull("nestedConfig", data.getNestedConfig());
        Assert.assertEquals("nestedConfig has one element", 1, data.getNestedConfig().size());
        Assert.assertEquals("nestedConfig.key1", expectedMap, data.getNestedConfig().get("key1"));


        Data nested = data.getNested();
        Assert.assertEquals("nested.strAttr", null, nested.getStrAttr());
        Assert.assertEquals("nested.strList", Arrays.asList("1", "3", "4"), nested.getStrList());
        Assert.assertEquals("nested.dataList[0].baseAttr", "item1", nested.getDataList().get(0).getBaseAttr());
        Assert.assertEquals("nested.dataList[0].strList", Arrays.asList("confidential", "public"), nested.getDataList().get(0).getStrList());
        Assert.assertEquals("nested.dataList[1].baseAttr", "changed item2", nested.getDataList().get(1).getBaseAttr());
        Assert.assertEquals("nested.dataList[1].strList", Arrays.asList("external"), nested.getDataList().get(1).getStrList());

        nested = nested.getNested();
        Assert.assertEquals("nested.nested.strList", Arrays.asList("first", "second"), nested.getStrList());

        nested = nested.getNested();
        Assert.assertEquals("nested.nested.nested.baseAttr", "NEW VALUE", nested.getBaseAttr());
        Assert.assertEquals("nested.nested.nested.strList", Arrays.asList("true", "false"), nested.getStrList());
    }

    @Test
    public void testKeyParsing() {

        assertAttributeKey(new AttributeKey("am.bam.pet"), "am", -1, "bam", -1, "pet", -1);

        assertAttributeKey(new AttributeKey("a"), "a", -1);

        assertAttributeKey(new AttributeKey("a.b"), "a", -1, "b", -1);

        assertAttributeKey(new AttributeKey("a.b[1]"), "a", -1, "b", 1);

        assertAttributeKey(new AttributeKey("a[12].b"), "a", 12, "b", -1);

        assertAttributeKey(new AttributeKey("a[10].b[20]"), "a", 10, "b", 20);

        assertAttributeKey(new AttributeKey("\"am\".\"bam\".\"pet\""), "am", -1, "bam", -1, "pet", -1);

        assertAttributeKey(new AttributeKey("\"am\".bam.\"pet\""), "am", -1, "bam", -1, "pet", -1);

        assertAttributeKey(new AttributeKey("\"am.bam\".\"pet\""), "am.bam", -1, "pet", -1);

        assertAttributeKey(new AttributeKey("\"am.bam[2]\".\"pet[6]\""), "am.bam", 2, "pet", 6);

        try {
            new AttributeKey("a.");

            Assert.fail("Should have failed");
        } catch (RuntimeException expected) {
        }

        try {
            new AttributeKey("a[]");

            Assert.fail("Should have failed");
        } catch (RuntimeException expected) {
        }

        try {
            new AttributeKey("a[lala]");

            Assert.fail("Should have failed");
        } catch (RuntimeException expected) {
        }

        try {
            new AttributeKey("a[\"lala\"]");

            Assert.fail("Should have failed");
        } catch (RuntimeException expected) {
        }

        try {
            new AttributeKey(".a");

            Assert.fail("Should have failed");
        } catch (RuntimeException expected) {
        }

        try {
            new AttributeKey("\"am\"..\"bam\".\"pet\"");

            Assert.fail("Should have failed");
        } catch (RuntimeException expected) {
        }

        try {
            new AttributeKey("\"am\"ups.\"bam\".\"pet\"");

            Assert.fail("Should have failed");
        } catch (RuntimeException expected) {
        }

        try {
            new AttributeKey("ups\"am\"ups.\"bam\".\"pet\"");

            Assert.fail("Should have failed");
        } catch (RuntimeException expected) {
        }
    }

    private void assertAttributeKey(AttributeKey key, Object ... args) {
        Iterator<Component> it = key.getComponents().iterator();

        for (int i = 0; i < args.length; i++) {
            String name = String.valueOf(args[i++]);
            int idx = Integer.valueOf(String.valueOf(args[i]));

            Component component = it.next();
            Assert.assertEquals(name, component.getName());
            Assert.assertEquals(idx, component.getIndex());
        }
    }


    public static class BaseData {

        String baseAttr;

        public String getBaseAttr() {
            return baseAttr;
        }

        public void setBaseAttr(String baseAttr) {
            this.baseAttr = baseAttr;
        }
    }

    public static class Data extends BaseData {

        String strAttr;

        Integer intAttr;

        Long longAttr;

        Boolean boolAttr;

        List<String> strList;

        List<Integer> intList;

        List<Data> dataList;

        List<List<String>> deepList;

        Data nested;

        Map<String, String> config;

        Map<String, Map<String, Data>> nestedConfig;


        public String getStrAttr() {
            return strAttr;
        }

        public void setStrAttr(String strAttr) {
            this.strAttr = strAttr;
        }

        public Integer getIntAttr() {
            return intAttr;
        }

        public void setIntAttr(Integer intAttr) {
            this.intAttr = intAttr;
        }

        public Long getLongAttr() {
            return longAttr;
        }

        public void setLongAttr(Long longAttr) {
            this.longAttr = longAttr;
        }

        public Boolean getBoolAttr() {
            return boolAttr;
        }

        public void setBoolAttr(Boolean boolAttr) {
            this.boolAttr = boolAttr;
        }

        public List<String> getStrList() {
            return strList;
        }

        public void setStrList(List<String> strList) {
            this.strList = strList;
        }

        public List<Integer> getIntList() {
            return intList;
        }

        public void setIntList(List<Integer> intList) {
            this.intList = intList;
        }

        public List<Data> getDataList() {
            return dataList;
        }

        public void setDataList(List<Data> dataList) {
            this.dataList = dataList;
        }

        public Data getNested() {
            return nested;
        }

        public void setNested(Data nested) {
            this.nested = nested;
        }

        public List<List<String>> getDeepList() {
            return deepList;
        }

        public void setDeepList(List<List<String>> deepList) {
            this.deepList = deepList;
        }

        public void setConfig(Map<String, String> config) {
            this.config = config;
        }

        public Map<String, String> getConfig() {
            return config;
        }

        public void setNestedConfig(Map<String, Map<String, Data>> nestedConfig) {
            this.nestedConfig = nestedConfig;
        }

        public Map<String, Map<String, Data>> getNestedConfig() {
            return nestedConfig;
        }
    }
}