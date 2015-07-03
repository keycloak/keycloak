package org.keycloak.testsuite.arquillian;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.keycloak.testsuite.TestRealmsManager;
import static org.keycloak.testsuite.arquillian.ContainersTestEnricher.getNearestSuperclassWithAnnotation;
import org.keycloak.testsuite.arquillian.annotation.RequiresRealms;

/**
 *
 * @author tkyjovsk
 */
public class RealmsTestEnricher {

    public static final int PRECEDENCE = Integer.MAX_VALUE;

    public void importRequiredRealmsBeforeTest(@Observes(precedence = PRECEDENCE) Before event) {
        if (isTestRealmsManager(event)) {
            TestRealmsManager test = (TestRealmsManager) event.getTestInstance();
            for (String requiredRealm : getRequiredRealms(event)) {
                test.importTestRealm(requiredRealm);
            }
        }
    }

    public void removeRequiredRealmsAfterTest(@Observes(precedence = -PRECEDENCE) After event) {
        if (RealmsTestEnricher.this.isTestRealmsManager(event)) {
            TestRealmsManager realmManager = (TestRealmsManager) event.getTestInstance();
            for (String requiredRealm : getRequiredRealms(event)) {
                realmManager.removeTestRealm(requiredRealm);
            }
        }
    }

    public boolean isTestRealmsManager(Before event) {
        return isTestRealmsManager(event.getTestClass().getJavaClass());
    }

    public boolean isTestRealmsManager(After event) {
        return isTestRealmsManager(event.getTestClass().getJavaClass());
    }

    public boolean isTestRealmsManager(Class testClass) {
        return TestRealmsManager.class.isAssignableFrom(testClass);
    }

    public Set<String> getRequiredRealms(Before event) {
        return getRequiredRealms(event.getTestClass(), event.getTestMethod());
    }

    public Set<String> getRequiredRealms(After event) {
        return getRequiredRealms(event.getTestClass(), event.getTestMethod());
    }

    public Set<String> getRequiredRealms(TestClass testClass, Method testMethod) {
        Set<String> requiredRealms = new HashSet<>();
        Class c = getNearestSuperclassWithAnnotation(testClass.getJavaClass(), RequiresRealms.class);
        RequiresRealms crr = (RequiresRealms) (c == null ? null : c.getAnnotation(RequiresRealms.class));
        RequiresRealms mrr = testMethod.getAnnotation(RequiresRealms.class);
        if (crr != null) {
            requiredRealms.addAll(Arrays.asList(crr.value()));
        }
        if (mrr != null) {
            requiredRealms.addAll(Arrays.asList(mrr.value()));
        }
        System.out.print("Required realms: ");
        for(String r: requiredRealms) {
            System.out.print(r+" ");
        }
        System.out.println("");
        return requiredRealms;
    }

}
