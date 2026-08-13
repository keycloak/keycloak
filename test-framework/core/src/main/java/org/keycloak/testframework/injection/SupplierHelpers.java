package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.keycloak.testframework.FatalTestClassException;
import org.keycloak.testframework.annotations.InjectDependency;
import org.keycloak.testframework.injection.predicates.DependencyPredicates;
import org.keycloak.testframework.injection.predicates.InstanceContextPredicates;

public class SupplierHelpers {

    public static <T> T getInstanceWithInjectedFields(Class<T> clazz, InstanceContext<?, ?> instanceContext) {
        T configInstance = getInstance(clazz);

        List<Field> fields = ReflectionUtils.listFields(configInstance.getClass()).stream().filter(f -> f.getAnnotation(InjectDependency.class) != null).toList();
        if (!fields.isEmpty()) {
            List<InstanceContext<?, ?>> deployedInstances = instanceContext.getRegistry().getDeployedInstances();
            List<Dependency> dependencies = findAllDependencies(new LinkedList<>(), instanceContext.getDeclaredDependencies(), deployedInstances);

            fields.forEach(f -> {
                Dependency dependency = dependencies.stream().filter(DependencyPredicates.assignableTo(f.getType())).findFirst().orElseThrow(injectedDependencyNotFound(f, instanceContext.getSupplier()));
                InstanceContext<?, ?> instance = deployedInstances.stream()
                        .filter(InstanceContextPredicates.matches(f.getType(), dependency.ref()))
                        .findFirst()
                        .orElseThrow(dependencyNotFound(dependency));
                ReflectionUtils.setField(f, configInstance, instance.getValue());
            });
        }
        return configInstance;
    }

    public static <T> T getInstance(Class<T> clazz) {
        try {
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(String clazzName) {
        try {
            Class<T> clazz = (Class<T>) SupplierHelpers.class.getClassLoader().loadClass(clazzName);
            return getInstance(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getAnnotationField(Annotation annotation, String name, T defaultValue) {
        T value = getAnnotationField(annotation, name);
        return value != null ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationField(Annotation annotation, String name) {
        if (annotation != null) {
            for (Method m : annotation.annotationType().getMethods()) {
                if (m.getName().equals(name)) {
                    try {
                        return (T) m.invoke(annotation);
                    } catch (Exception e){
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return null;
    }

    public static String createName(InstanceContext<?, ?> instanceContext) {
        return instanceContext.getRef() != null ? instanceContext.getRef() : "default";
    }

    private static List<Dependency> findAllDependencies(List<Dependency> allDependencies, List<Dependency> dependencies, List<InstanceContext<?, ?>> deployedInstances) {
        for (Dependency dependency : dependencies) {
            if (allDependencies.stream().noneMatch(DependencyPredicates.matches(dependency.valueType(), dependency.ref()))) {
                allDependencies.add(dependency);
                InstanceContext<?, ?> instance = deployedInstances.stream().filter(InstanceContextPredicates.matches(dependency.valueType(), dependency.ref())).findFirst().orElseThrow(dependencyNotFound(dependency));
                findAllDependencies(allDependencies, instance.getDeclaredDependencies(), deployedInstances);
            }
        }
        return allDependencies;
    }

    private static Supplier<FatalTestClassException> dependencyNotFound(Dependency dependency) {
        return () -> new FatalTestClassException("Unexpected error in registry; requested dependency " + dependency.valueType().getName() + " not found in deployed instances");
    }

    private static Supplier<FatalTestClassException> injectedDependencyNotFound(Field field, org.keycloak.testframework.injection.Supplier<?, ?> supplier) {
        return () -> new FatalTestClassException(field.getDeclaringClass().getName() + " requested injection of " + field.getType().getSimpleName() + " not found in dependency tree for supplier " + supplier.getClass().getName());
    }

}
