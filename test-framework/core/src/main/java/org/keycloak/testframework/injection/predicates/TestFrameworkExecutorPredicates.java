package org.keycloak.testframework.injection.predicates;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.keycloak.testframework.TestFrameworkExecutor;

public interface TestFrameworkExecutorPredicates {

    static Predicate<TestFrameworkExecutor> shouldExecute(Method method) {
        return r -> r.shouldExecute(method);
    }

}
