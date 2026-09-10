package org.keycloak.testframework;

import java.lang.reflect.Method;
import java.util.List;

import org.keycloak.testframework.injection.Registry;

public interface TestFrameworkExecutor {

    List<Class<?>> getMethodValueTypes(Method method);

    boolean supportsParameter(Method method, Class<?> parameterType);

    boolean shouldExecute(Method testMethod);

    void execute(Registry registry, Class<?> testClass, Method testMethod);

}
