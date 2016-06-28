package org.keycloak.services.util;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

public class LocaleHelperTest {

    @Test
    public void shouldNotExceptionOnNullLocaleAttributeItem() throws Exception {
        final Method method = LocaleHelper.class.getDeclaredMethod("findLocale", Set.class, String[].class);
        method.setAccessible(true);
        Locale foundLocale = (Locale) method.invoke(null, Stream.of("en", "es", "fr").collect(Collectors.toSet()), new String[]{null});
        assertThat(foundLocale, nullValue());
    }
}
