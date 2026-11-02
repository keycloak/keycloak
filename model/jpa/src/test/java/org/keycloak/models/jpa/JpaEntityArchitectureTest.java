/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.jpa;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.keycloak", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
public class JpaEntityArchitectureTest {
    @ArchTest
    public static final ArchRule ENTITY_CONSTRUCTORS = classes().that().areAnnotatedWith(Entity.class)
            .should(new ArchCondition<>("declare a public or protected no-argument constructor") {
                @Override
                public void check(JavaClass entity, ConditionEvents events) {
                    boolean valid = entity.getConstructors().stream()
                            .anyMatch(constructor -> constructor.getRawParameterTypes().isEmpty()
                                    && (constructor.getModifiers().contains(JavaModifier.PUBLIC)
                                    || constructor.getModifiers().contains(JavaModifier.PROTECTED)));
                    events.add(new SimpleConditionEvent(entity, valid,
                            entity.getName() + " must declare a public or protected no-argument constructor"));
                }
            }).because("JPA must be able to instantiate entities");

    @ArchTest
    public static final ArchRule COMPOSITE_KEY_CONTRACTS = classes().that().areAnnotatedWith(IdClass.class)
            .should(new ArchCondition<>("use an ID class declaring equals(Object) and hashCode()") {
                @Override
                public void check(JavaClass entity, ConditionEvents events) {
                    JavaClass key = (JavaClass) entity.getAnnotationOfType(IdClass.class.getName()).get("value").orElseThrow();
                    boolean equals = key.getMethods().stream().anyMatch(method -> method.getName().equals("equals")
                            && method.getModifiers().contains(JavaModifier.PUBLIC)
                            && method.getRawReturnType().isEquivalentTo(boolean.class)
                            && method.getRawParameterTypes().size() == 1
                            && method.getRawParameterTypes().get(0).isEquivalentTo(Object.class));
                    boolean hashCode = key.getMethods().stream().anyMatch(method -> method.getName().equals("hashCode")
                            && method.getModifiers().contains(JavaModifier.PUBLIC)
                            && method.getRawReturnType().isEquivalentTo(int.class)
                            && method.getRawParameterTypes().isEmpty());
                    events.add(new SimpleConditionEvent(key, equals && hashCode,
                            key.getName() + " used by " + entity.getName() + " must declare public equals(Object) and hashCode()"));
                }
            }).because("composite keys need value equality");

    @ArchTest
    public static final ArchRule ENTITY_EQUALITY_USES_ID_FIELDS_ONLY = classes().that().areAnnotatedWith(Entity.class)
            .should(useExactlyFields("the ID fields", JpaEntityArchitectureTest::idFields))
            .because("entities must be equal based on their identity, and not based on other attributes");

    @ArchTest
    public static final ArchRule COMPOSITE_KEY_EQUALITY_USES_ALL_FIELDS = classes().that().areAnnotatedWith(IdClass.class)
            .should(new ArchCondition<>("use an ID class whose equals(Object) and hashCode() use all its fields") {
                @Override
                public void check(JavaClass entity, ConditionEvents events) {
                    JavaClass key = (JavaClass) entity.getAnnotationOfType(IdClass.class.getName()).get("value").orElseThrow();
                    checkEqualityMethods(key, JpaEntityArchitectureTest::allInstanceFields, "all its fields", events);
                }
            }).because("composite keys must be compared by all their fields");

    @ArchTest
    public static final ArchRule COLLECTION_ELEMENTS_DECLARE_EQUALITY = fields()
            .that().areAnnotatedWith(OneToMany.class).or().areAnnotatedWith(ManyToMany.class)
            .should(new ArchCondition<>("have an element type declaring equals(Object) and hashCode()") {
                @Override
                public void check(JavaField field, ConditionEvents events) {
                    if (!(field.getType() instanceof JavaParameterizedType type) || type.getActualTypeArguments().isEmpty()) {
                        events.add(SimpleConditionEvent.violated(field, field.getFullName() + " must declare its element type"));
                        return;
                    }
                    // the last type argument is the element for collections and the value for maps
                    JavaClass element = type.getActualTypeArguments().get(type.getActualTypeArguments().size() - 1).toErasure();
                    boolean valid = element.tryGetMethod("equals", Object.class).isPresent()
                            && element.tryGetMethod("hashCode").isPresent();
                    events.add(new SimpleConditionEvent(field, valid,
                            "%s holds %s, which must declare equals and hashCode".formatted(field.getFullName(), element.getName())));
                }
            }).because("Hibernate collections rely on value equality of their elements to avoid duplicates and failed removals");

    private static ArchCondition<JavaClass> useExactlyFields(String description, Function<JavaClass, Set<String>> expectedFields) {
        return new ArchCondition<>("use exactly " + description + " in equals(Object) and hashCode(), if declared") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                checkEqualityMethods(clazz, expectedFields, description, events);
            }
        };
    }

    // Declaring the methods is enforced elsewhere; this only validates the ones that exist
    private static void checkEqualityMethods(JavaClass clazz, Function<JavaClass, Set<String>> expectedFields, String description, ConditionEvents events) {
        Set<String> expected = expectedFields.apply(clazz);
        if (clazz.isRecord()) {
            // equals and hashCode are generated from all components
            return;
        }
        for (var method : new String[]{"equals", "hashCode"}) {
            var declared = "equals".equals(method) ? clazz.tryGetMethod(method, Object.class) : clazz.tryGetMethod(method);
            if (declared.isEmpty()) {
                continue;
            }
            Set<String> used = fieldsUsedBy(declared.get(), clazz);
            events.add(new SimpleConditionEvent(clazz, used.equals(expected),
                    "%s.%s uses fields %s, but should use %s: %s".formatted(clazz.getName(), method, used, description, expected)));
        }
    }

    private static Set<String> idFields(JavaClass entity) {
        return entity.getAllFields().stream()
                .filter(field -> field.isAnnotatedWith(Id.class) || field.isAnnotatedWith(EmbeddedId.class))
                .map(JavaField::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> allInstanceFields(JavaClass clazz) {
        return clazz.getAllFields().stream()
                .filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
                .map(JavaField::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    // Fields read directly, or via a getter like getId() or isEnabled() on the same class
    private static Set<String> fieldsUsedBy(JavaMethod method, JavaClass clazz) {
        Set<String> fields = new TreeSet<>();
        method.getFieldAccesses().stream()
                .filter(access -> clazz.isAssignableTo(access.getTargetOwner().getName()))
                .forEach(access -> fields.add(access.getName()));
        method.getMethodCallsFromSelf().stream()
                .filter(call -> clazz.isAssignableTo(call.getTargetOwner().getName()))
                .forEach(call -> {
                    String name = call.getName();
                    String property = name.startsWith("get") ? name.substring(3) : name.startsWith("is") ? name.substring(2) : "";
                    if (!property.isEmpty()) {
                        fields.add(property.substring(0, 1).toLowerCase(Locale.ROOT) + property.substring(1));
                    }
                });
        // getter calls of unrelated names are not fields of this class
        Set<String> known = allInstanceFields(clazz);
        fields.retainAll(known);
        return fields;
    }
}
