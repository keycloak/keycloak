/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import static org.keycloak.models.map.processor.FieldAccessorType.GETTER;
import static org.keycloak.models.map.processor.Util.getGenericsDeclaration;
import static org.keycloak.models.map.processor.Util.isMapType;
import static org.keycloak.models.map.processor.Util.singularToPlural;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class AbstractGenerateEntityImplementationsProcessor extends AbstractProcessor {

    protected static final String FQN_DEEP_CLONER = "org.keycloak.models.map.common.DeepCloner";
    protected static final String FQN_ENTITY_FIELD = "org.keycloak.models.map.common.EntityField";
    protected static final String FQN_HAS_ENTITY_FIELD_DELEGATE = "org.keycloak.models.map.common.delegate.HasEntityFieldDelegate";
    protected static final String FQN_ENTITY_FIELD_DELEGATE = "org.keycloak.models.map.common.delegate.EntityFieldDelegate";

    protected Elements elements;
    protected Types types;

    protected static interface Generator {
        void generate(TypeElement e) throws IOException;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();

        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            annotatedElements.stream()
                    .map(TypeElement.class::cast)
                    .filter(this::testAnnotationElement)
                    .forEach(this::processTypeElement);
        }

        if (!annotations.isEmpty()) {
            afterAnnotationProcessing();
        }

        return true;
    }

    protected boolean testAnnotationElement(TypeElement kind) { return true; }
    protected void afterAnnotationProcessing() {}
    protected abstract Generator[] getGenerators();

    private void processTypeElement(TypeElement e) {
        for (GenerateEntityImplementationsProcessor.Generator generator : getGenerators()) {
            try {
                generator.generate(e);
            } catch (Exception ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not generate implementation for class: " + ex, e);
            }
        }

//        methodsPerAttribute.entrySet().stream()
//          .sorted(Comparator.comparing(Map.Entry::getKey))
//          .forEach(me -> processingEnv.getMessager().printMessage(
//              Diagnostic.Kind.NOTE,
//              "** " + me.getKey() + ": " + me.getValue().stream().map(ExecutableElement::getSimpleName).sorted(Comparator.comparing(Object::toString)).collect(Collectors.joining(", ")))
//          );
    }

    protected Stream<ExecutableElement> getAllAbstractMethods(TypeElement e) {
        return elements.getAllMembers(e).stream()
          .filter(el -> el.getKind() == ElementKind.METHOD)
          .filter(el -> el.getModifiers().contains(Modifier.ABSTRACT))
          .filter(ExecutableElement.class::isInstance)
          .map(ExecutableElement.class::cast);
    }

    protected Map<String, HashSet<ExecutableElement>> methodsPerAttributeMapping(TypeElement e) {
        Map<String, HashSet<ExecutableElement>> methodsPerAttribute = getAllAbstractMethods(e)
          .filter(Util::isNotIgnored)
          .filter(ee -> !(ee.getReceiverType() instanceof NoType && ee.getReceiverType().getKind() != TypeKind.NONE))
          .collect(Collectors.toMap(this::determineAttributeFromMethodName, v -> new HashSet<>(Arrays.asList(v)), (a,b) -> { a.addAll(b); return a; }));

        // Merge plurals with singulars
        methodsPerAttribute.keySet().stream()
                .filter(key -> methodsPerAttribute.containsKey(singularToPlural(key)))
                .collect(Collectors.toSet())
                .forEach(key -> {
                    HashSet<ExecutableElement> removed = methodsPerAttribute.remove(key);
                    methodsPerAttribute.get(singularToPlural(key)).addAll(removed);
                });

        return methodsPerAttribute;
    }

    private static final Pattern BEAN_NAME = Pattern.compile("(get|set|is|delete|remove|add|update)([A-Z]\\S+)");
    private static final Map<String, String> FORBIDDEN_PREFIXES = new HashMap<>();
    static {
        FORBIDDEN_PREFIXES.put("delete", "remove");
    }

    protected String determineAttributeFromMethodName(ExecutableElement e) {
        Name name = e.getSimpleName();
        Matcher m = BEAN_NAME.matcher(name.toString());
        if (m.matches()) {
            String prefix = m.group(1);
            if (FORBIDDEN_PREFIXES.containsKey(prefix)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Forbidden prefix " + prefix + "... detected, use " + FORBIDDEN_PREFIXES.get(prefix) + "... instead", e
                );
            }
            return m.group(2);
        }
        return null;
    }

    protected Stream<ExecutableElement> fieldGetters(Map<String, HashSet<ExecutableElement>> methodsPerAttribute) {
        return methodsPerAttribute.entrySet().stream()
                .map(me -> FieldAccessorType.getMethod(GETTER, me.getValue(), me.getKey(), types, determineFieldType(me.getKey(), me.getValue())))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    protected boolean isImmutableFinalType(TypeMirror fieldType) {
        return isPrimitiveType(fieldType)
                || isBoxedPrimitiveType(fieldType)
                || isEnumType(fieldType)
                || Objects.equals("java.lang.String", fieldType.toString());
    }

    protected boolean isKnownCollectionOfImmutableFinalTypes(TypeMirror fieldType) {
        List<TypeMirror> res = getGenericsDeclaration(fieldType);
        return isCollection(fieldType) && res.stream().allMatch(this::isImmutableFinalType);
    }

    protected boolean isCollection(TypeMirror fieldType) {
        TypeElement typeElement = elements.getTypeElement(types.erasure(fieldType).toString());
        switch (typeElement.getQualifiedName().toString()) {
            case "java.util.List":
            case "java.util.Map":
            case "java.util.Set":
            case "java.util.Collection":
            case "org.keycloak.common.util.MultivaluedHashMap":
                return true;
            default:
                return false;
        }
    }

    protected String deepClone(TypeMirror fieldType, String parameterName) {
        TypeElement typeElement = elements.getTypeElement(types.erasure(fieldType).toString());
        if (isKnownCollectionOfImmutableFinalTypes(fieldType)) {
            return parameterName + " == null ? null : " + interfaceToImplementation(typeElement, parameterName);
        } else if (isMapType(typeElement)) {
            List<TypeMirror> mapTypes = getGenericsDeclaration(fieldType);
            boolean isKeyImmutable = isImmutableFinalType(mapTypes.get(0));
            boolean isValueImmutable = isImmutableFinalType(mapTypes.get(1));

            return parameterName + " == null ? null : " + parameterName + ".entrySet().stream().collect(" +
                    "java.util.stream.Collectors.toMap(" +
                            (isKeyImmutable ? "java.util.Map.Entry::getKey" : "entry -> " + deepClone(mapTypes.get(0), "entry.getKey()")) +
                            ", " +
                            (isValueImmutable ? "java.util.Map.Entry::getValue" : "entry -> " + deepClone(mapTypes.get(1), "entry.getValue()")) +
                            ", (o1, o2) -> o1" +
                            ", java.util.HashMap::new" +
                    "))";
        }
        return "deepClone(" + parameterName + ")";
    }

    protected boolean isEnumType(TypeMirror fieldType) {
        return types.asElement(fieldType).getKind() == ElementKind.ENUM;
    }

    protected boolean isPrimitiveType(TypeMirror fieldType) {
        try {
            types.getPrimitiveType(fieldType.getKind());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    protected boolean isBoxedPrimitiveType(TypeMirror fieldType) {
        try {
            types.unboxedType(fieldType);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    protected String interfaceToImplementation(TypeElement typeElement, String parameter) {
        Name parameterTypeQN = typeElement.getQualifiedName();
        switch (parameterTypeQN.toString()) {
            case "java.util.List":
            case "java.util.Collection":
                return "new java.util.LinkedList<>(" + parameter + ")";
            case "java.util.Map":
                return "new java.util.HashMap<>(" + parameter + ")";
            case "java.util.Set":
                return "new java.util.HashSet<>(" + parameter + ")";
            default:
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not determine implementation for type " + typeElement, typeElement);
                return "TODO()";
        }
    }

    protected TypeMirror determineFieldType(String fieldName, HashSet<ExecutableElement> methods) {
        Pattern getter = Pattern.compile("(get|is)" + Pattern.quote(fieldName));
        TypeMirror res = null;
        for (ExecutableElement method : methods) {
            if (getter.matcher(method.getSimpleName()).matches() && method.getParameters().isEmpty()) {
                return method.getReturnType();
            }
        }
        if (res == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not determine return type for the field " + fieldName, methods.iterator().next());
        }
        return res;
    }

    protected static class NameFirstComparator implements Comparator<String> {
        protected static final Comparator<String> ID_INSTANCE = new NameFirstComparator("id").thenComparing(Comparator.naturalOrder());
        protected static final Comparator<String> GET_ID_INSTANCE = new NameFirstComparator("getId").thenComparing(Comparator.naturalOrder());
        private final String name;
        public NameFirstComparator(String name) {
            this.name = name;
        }
        @Override
        public int compare(String o1, String o2) {
            return Objects.equals(o1, o2)
                    ? 0
                    : name.equalsIgnoreCase(o1)
                    ? -1
                    : name.equalsIgnoreCase(o2)
                    ? 1
                    : 0;
        }

    }
}
