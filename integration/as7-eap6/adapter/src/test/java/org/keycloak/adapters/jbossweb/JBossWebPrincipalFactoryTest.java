package org.keycloak.adapters.jbossweb;

import org.apache.catalina.Realm;
import org.junit.Assert;
import org.junit.Test;

import javax.security.auth.Subject;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JBossWebPrincipalFactoryTest {

    @Test
    public void test() {
        Constructor constructor = JBossWebPrincipalFactory.findJBossGenericPrincipalConstructor();
        Assert.assertNotNull(constructor);
        Assert.assertEquals(Realm.class, constructor.getParameterTypes()[0]);
        Assert.assertEquals(String.class, constructor.getParameterTypes()[1]);
        Assert.assertEquals(List.class, constructor.getParameterTypes()[3]);
        Assert.assertEquals(Principal.class, constructor.getParameterTypes()[4]);
        Assert.assertEquals(Object.class, constructor.getParameterTypes()[6]);
        Assert.assertEquals(Subject.class, constructor.getParameterTypes()[8]);
    }

}
