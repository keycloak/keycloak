package org.keycloak.forms.login.freemarker.model;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AbstractUserProfileBeanTest {

    @Test
    public void sanitizeAnnotationsFiltersInvalidKeys() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("kcValid", "value1");
        input.put("kc-also-valid", "value2");
        input.put("kc_underscore.dot", "value3");
        input.put("kc key with spaces", "value4");
        input.put("kcKey\" onclick=\"alert(1)", "malicious");
        input.put("kcNormal", "safe");

        Map<String, Object> result = invokeSanitizeAnnotations(input);

        Assert.assertTrue(result.containsKey("kcValid"));
        Assert.assertTrue(result.containsKey("kc-also-valid"));
        Assert.assertTrue(result.containsKey("kc_underscore.dot"));
        Assert.assertFalse(result.containsKey("kc key with spaces"));
        Assert.assertFalse(result.containsKey("kcKey\" onclick=\"alert(1)"));
        Assert.assertTrue(result.containsKey("kcNormal"));
    }

    @Test
    public void sanitizeAnnotationsEscapesValues() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("kcTest", "value\" onfocus=\"alert(1)");
        input.put("kcAngle", "<script>alert(1)</script>");
        input.put("kcAmpersand", "a&b");
        input.put("kcSafe", "normalvalue");

        Map<String, Object> result = invokeSanitizeAnnotations(input);

        Assert.assertEquals("value&quot; onfocus=&quot;alert(1)", result.get("kcTest"));
        Assert.assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", result.get("kcAngle"));
        Assert.assertEquals("a&amp;b", result.get("kcAmpersand"));
        Assert.assertEquals("normalvalue", result.get("kcSafe"));
    }

    @Test
    public void sanitizeAnnotationsPreservesNonStringValues() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("kcNumber", 42);
        input.put("kcBoolean", true);

        Map<String, Object> result = invokeSanitizeAnnotations(input);

        Assert.assertEquals(42, result.get("kcNumber"));
        Assert.assertEquals(true, result.get("kcBoolean"));
    }

    @Test
    public void sanitizeAnnotationsHandlesEmptyMap() throws Exception {
        Map<String, Object> input = new HashMap<>();

        Map<String, Object> result = invokeSanitizeAnnotations(input);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void sanitizeAnnotationsFiltersNullKey() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put(null, "value");
        input.put("kcValid", "ok");

        Map<String, Object> result = invokeSanitizeAnnotations(input);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("ok", result.get("kcValid"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeSanitizeAnnotations(Map<String, Object> input) throws Exception {
        Method method = AbstractUserProfileBean.class.getDeclaredMethod("sanitizeAnnotations", Map.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(null, input);
    }
}
