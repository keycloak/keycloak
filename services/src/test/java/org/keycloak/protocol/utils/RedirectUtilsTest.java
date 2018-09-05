package org.keycloak.procotol.oidc.utils;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.protocol.oidc.utils.RedirectUtils;

/**
 * 
 *
 */
public class RedirectUtilsTest {
    
    private Method method;
    
    @Before
    public void setUp() throws Exception {
        method = RedirectUtils.class.getDeclaredMethod("matchesRedirects", Set.class, String.class);
        method.setAccessible(true); 
    }

    @Test
    public void testWhenWildCardIsAfterProtocol() throws Exception {       
        boolean matches = (boolean) method.invoke(null, Stream.of("https://*.example.com/*").collect(Collectors.toSet()), "https://sub.example.com/path/");
        Assert.assertEquals("https://sub.example.com/path/ matches with https://*.example.com/*", true, matches);
    }
    
    @Test
    public void testWhenWildCardIsInBeginning() throws Exception {        
        boolean matches = (boolean) method.invoke(null, Stream.of("*.example.com/*").collect(Collectors.toSet()), "https://sub.example.com/path/");
        Assert.assertEquals("https://sub.example.com/path/ matches with *.example.com/*", true, matches);
    }
    
    @Test
    public void testWhenWildCardIsInTheMiddle() throws Exception {        
        boolean matches = (boolean) method.invoke(null, Stream.of("https://sub.*.example.com/*").collect(Collectors.toSet()), "https://sub.foo.example.com/path/");
        Assert.assertEquals("https://sub.foo.example.com/path/ matches with https://sub.*.example.com/*", true, matches);
    }
    
    @Test
    public void testWhenWildCardIsInTheEnd() throws Exception {        
        boolean matches = (boolean) method.invoke(null, Stream.of("https://sub.*/*").collect(Collectors.toSet()), "https://sub.example.com/path/");
        Assert.assertEquals("https://sub.example.com/path/ matches with https://sub.*/*", true, matches);
    }
    
    @Test
    public void testWhenMultipleWildCardsPresent() throws Exception {       
        boolean matches = (boolean) method.invoke(null, Stream.of("https://*-foo.*.example.com/*").collect(Collectors.toSet()), "https://sub-foo.domain.example.com/path/");
        Assert.assertEquals("https://sub-foo.domain.example.com/path/ matches with https://*-foo.*.example.com/*", true, matches);
    }
    
    @Test
    public void testWhenRedirectDoesNotMatch() throws Exception {       
        boolean matches = (boolean) method.invoke(null, Stream.of("https://*.example.com/*").collect(Collectors.toSet()), "https://sub.example.org/path/");
        Assert.assertEquals("https://sub.example.org/path/ does not match with http://*.example.com/*", false, matches);
    }
}
