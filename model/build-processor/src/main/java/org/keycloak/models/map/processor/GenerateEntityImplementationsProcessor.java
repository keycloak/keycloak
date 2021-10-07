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

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import static org.keycloak.models.map.processor.FieldAccessorType.*;
import static org.keycloak.models.map.processor.Util.getGenericsDeclaration;
import static org.keycloak.models.map.processor.Util.isSetType;
import static org.keycloak.models.map.processor.Util.methodParameters;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Optional;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 *
 * @author hmlnarik
 */
@SupportedAnnotationTypes("org.keycloak.models.map.annotations.GenerateEntityImplementations")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GenerateEntityImplementationsProcessor extends AbstractProcessor {

    private static interface Generator {
        void generate(TypeElement e, Map<String, HashSet<ExecutableElement>> methodsPerAttribute) throws IOException;
    }

    private Elements elements;
    private Types types;

    private final Generator[] generators = new Generator[] {
        new DelegateGenerator(),
        new FieldsGenerator(),
        new ImplGenerator()
    };

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();

        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            annotatedElements.stream()
              .map(TypeElement.class::cast)
              .forEach(this::processTypeElement);
        }

        return true;
    }

    private void processTypeElement(TypeElement e) {
        if (e.getKind() != ElementKind.INTERFACE) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Annotation @GenerateEntityImplementations is only applicable to an interface", e);
            return;
        }

        // Find all properties
        Map<String, HashSet<ExecutableElement>> methodsPerAttribute = e.getEnclosedElements().stream()
          .filter(ExecutableElement.class::isInstance)
          .map(ExecutableElement.class::cast)
          .filter(ee -> ! (ee.getReceiverType() instanceof NoType))
          .collect(Collectors.toMap(this::determineAttributeFromMethodName, v -> new HashSet(Arrays.asList(v)), (a,b) -> { a.addAll(b); return a; }));

        // Merge plurals with singulars
        methodsPerAttribute.keySet().stream()
          .filter(key -> methodsPerAttribute.containsKey(key + "s"))
          .collect(Collectors.toSet())
          .forEach(key -> {
              HashSet<ExecutableElement> removed = methodsPerAttribute.remove(key);
              methodsPerAttribute.get(key + "s").addAll(removed);
          });

        for (Generator generator : this.generators) {
            try {
                generator.generate(e, methodsPerAttribute);
            } catch (Exception ex) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Could not generate implementation for class: " + ex, e);
            }
        }

//        methodsPerAttribute.entrySet().stream()
//          .sorted(Comparator.comparing(Map.Entry::getKey))
//          .forEach(me -> processingEnv.getMessager().printMessage(
//              Diagnostic.Kind.NOTE,
//              "** " + me.getKey() + ": " + me.getValue().stream().map(ExecutableElement::getSimpleName).sorted(Comparator.comparing(Object::toString)).collect(Collectors.joining(", ")))
//          );
    }

    private static final Pattern BEAN_NAME = Pattern.compile("(get|set|is|delete|remove|add|update)([A-Z]\\S+)");
    private static final Map<String, String> FORBIDDEN_PREFIXES = new HashMap<>();
    static {
        FORBIDDEN_PREFIXES.put("delete", "remove");
    }

    private String determineAttributeFromMethodName(ExecutableElement e) {
        Name name = e.getSimpleName();
        Matcher m = BEAN_NAME.matcher(name.toString());
        if (m.matches()) {
            String prefix = m.group(1);
            if (FORBIDDEN_PREFIXES.containsKey(prefix)) {
                processingEnv.getMessager().printMessage(
                  Kind.ERROR,
                  "Forbidden prefix " + prefix + "... detected, use " + FORBIDDEN_PREFIXES.get(prefix) + "... instead", e
                );
            }
            return m.group(2);
        }
        return null;
    }

    protected static String toEnumConstant(String key) {
        return key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    private TypeMirror determineFieldType(String fieldName, HashSet<ExecutableElement> methods) {
        Pattern getter = Pattern.compile("(get|is)" + Pattern.quote(fieldName));
        TypeMirror res = null;
        for (ExecutableElement method : methods) {
            if (getter.matcher(method.getSimpleName()).matches() && method.getParameters().isEmpty()) {
                return method.getReturnType();
            }
        }
        if (res == null) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Could not determine return type for the field " + fieldName, methods.iterator().next());
        }
        return res;
    }

    private boolean isImmutableFinalType(TypeMirror fieldType) {
        return isPrimitiveType(fieldType) || isBoxedPrimitiveType(fieldType) || Objects.equals("java.lang.String", fieldType.toString());
    }

    private boolean isKnownCollectionOfImmutableFinalTypes(TypeMirror fieldType) {
        TypeElement typeElement = elements.getTypeElement(types.erasure(fieldType).toString());
        switch (typeElement.getQualifiedName().toString()) {
            case "java.util.List":
            case "java.util.Map":
            case "java.util.Set":
            case "java.util.Collection":
            case "org.keycloak.common.util.MultivaluedHashMap":
                List<TypeMirror> res = getGenericsDeclaration(fieldType);
                return res.stream().allMatch(tm -> isImmutableFinalType(tm) || isKnownCollectionOfImmutableFinalTypes(tm));
            default:
                return false;
        }
    }

    private boolean isPrimitiveType(TypeMirror fieldType) {
        try {
            types.getPrimitiveType(fieldType.getKind());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
            }
        }

    private boolean isBoxedPrimitiveType(TypeMirror fieldType) {
        try {
            types.unboxedType(fieldType);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String interfaceToImplementation(TypeElement typeElement, String parameter) {
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
                processingEnv.getMessager().printMessage(Kind.ERROR, "Could not determine implementation for type " + typeElement, typeElement);
                return "TODO()";
        }
    }

    private class FieldsGenerator implements Generator {
        @Override
        public void generate(TypeElement e, Map<String, HashSet<ExecutableElement>> methodsPerAttribute) throws IOException {
            String className = e.getQualifiedName().toString();
            String packageName = null;
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = className.substring(0, lastDot);
            }

            String simpleClassName = className.substring(lastDot + 1);
            String mapFieldsClassName = className + "Fields";
            String mapSimpleFieldsClassName = simpleClassName + "Fields";

            JavaFileObject file = processingEnv.getFiler().createSourceFile(mapFieldsClassName);
            try (PrintWriter pw = new PrintWriterNoJavaLang(file.openWriter())) {
                if (packageName != null) {
                    pw.println("package " + packageName + ";");
                }

                pw.println("public enum " + mapSimpleFieldsClassName + " {");
                methodsPerAttribute.keySet().stream()
                  .sorted()
                  .map(GenerateEntityImplementationsProcessor::toEnumConstant)
                  .forEach(key -> pw.println("    " + key + ","));
                pw.println("}");
            }
        }
    }

    private class ImplGenerator implements Generator {

        @Override
        public void generate(TypeElement e, Map<String, HashSet<ExecutableElement>> methodsPerAttribute) throws IOException {
            GenerateEntityImplementations an = e.getAnnotation(GenerateEntityImplementations.class);
            TypeElement parentTypeElement = elements.getTypeElement((an.inherits() == null || an.inherits().isEmpty()) ? "void" : an.inherits());
            if (parentTypeElement == null) {
                return;
            }
            final List<? extends Element> allMembers = elements.getAllMembers(parentTypeElement);
            String className = e.getQualifiedName().toString();
            String packageName = null;
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = className.substring(0, lastDot);
            }

            String simpleClassName = className.substring(lastDot + 1);
            String mapImplClassName = className + "Impl";
            String mapSimpleClassName = simpleClassName + "Impl";
            boolean hasId = methodsPerAttribute.containsKey("Id") || allMembers.stream().anyMatch(el -> "getId".equals(el.getSimpleName().toString()));

            JavaFileObject file = processingEnv.getFiler().createSourceFile(mapImplClassName);
            try (PrintWriter pw = new PrintWriterNoJavaLang(file.openWriter())) {
                if (packageName != null) {
                    pw.println("package " + packageName + ";");
                }

                pw.println("import java.util.Objects;");
                pw.println("// DO NOT CHANGE THIS CLASS, IT IS GENERATED AUTOMATICALLY BY " + GenerateEntityImplementationsProcessor.class.getSimpleName());
                pw.println("public class " + mapSimpleClassName + (an.inherits().isEmpty() ? "" : " extends " + an.inherits()) + " implements " + className + " {");
//                pw.println("    private final EnumMap<Field, Object> values = new EnumMap<>(Field.class);");
//                pw.println("    protected Object get(Field field) { return values.get(field); }");
//                pw.println("    protected Object set(Field field, Object p0) { return values.put(field, p0); }");
                pw.println("    @Override public boolean equals(Object o) {");
                pw.println("        if (o == this) return true; ");
                pw.println("        if (! (o instanceof " + mapSimpleClassName + ")) return false; ");
                pw.println("        " + mapSimpleClassName + " other = (" + mapSimpleClassName + ") o; ");
                pw.println("        return "
                  + methodsPerAttribute.entrySet().stream()
                    .map(me -> FieldAccessorType.getMethod(GETTER, me.getValue(), me.getKey(), types, determineFieldType(me.getKey(), me.getValue())))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(ExecutableElement::getSimpleName)
                    .map(Name::toString)
                    .sorted()
                    .map(v -> "Objects.equals(" + v + "(), other." + v + "())")
                    .collect(Collectors.joining("\n          && "))
                  + ";");
                pw.println("    }");
                pw.println("    @Override public int hashCode() {");
                pw.println("        return " 
                  + (hasId
                    ? "(getId() == null ? super.hashCode() : getId().hashCode())"
                    : "Objects.hash("
                      + methodsPerAttribute.entrySet().stream() // generate hashcode from simple-typed properties (no collections etc.)
                        .map(me -> FieldAccessorType.getMethod(GETTER, me.getValue(), me.getKey(), types, determineFieldType(me.getKey(), me.getValue())))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(ee -> isImmutableFinalType(ee.getReturnType()))
                        .map(ExecutableElement::getSimpleName)
                        .map(Name::toString)
                        .sorted()
                        .map(v -> v + "()")
                        .collect(Collectors.joining(",\n          "))
                      + ")")
                  + ";");
                pw.println("    }");
                pw.println("    @Override public String toString() {");
                pw.println("        return String.format(\"%s@%08x\", " + (hasId ? "getId()" : "\"" + mapSimpleClassName + "\"" ) + ", System.identityHashCode(this));");
                pw.println("    }");

                // Constructors
                allMembers.stream()
                  .filter(ExecutableElement.class::isInstance)
                  .map(ExecutableElement.class::cast)
                  .filter((ExecutableElement ee) -> ee.getKind() == ElementKind.CONSTRUCTOR)
                  .forEach((ExecutableElement ee) -> pw.println("    "
                  + ee.getModifiers().stream().map(Object::toString).collect(Collectors.joining(" "))
                  + " " + mapSimpleClassName + "(" + methodParameters(ee.getParameters()) + ") { super(" + ee.getParameters() + "); }"));

                methodsPerAttribute.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(me -> {
                    HashSet<ExecutableElement> methods = me.getValue();
                    TypeMirror fieldType = determineFieldType(me.getKey(), methods);
                    if (fieldType == null) {
                        return;
                    }

                    pw.println("");
                    pw.println("    private " + fieldType + " f" + me.getKey() + ";");

                    for (ExecutableElement method : methods) {
                        FieldAccessorType fat = FieldAccessorType.determineType(method, me.getKey(), types, fieldType);
                        Optional<ExecutableElement> parentMethod = allMembers.stream()
                          .filter(ExecutableElement.class::isInstance)
                          .map(ExecutableElement.class::cast)
                          .filter(ee -> Objects.equals(ee.toString(), method.toString()))
                          .filter((ExecutableElement ee) ->  ! ee.getModifiers().contains(Modifier.ABSTRACT))
                          .findAny();

                        if (parentMethod.isPresent()) {
                            processingEnv.getMessager().printMessage(Kind.OTHER, "Method " + method + " is declared in a parent class.", method);
                        } else if (fat != FieldAccessorType.UNKNOWN && ! printMethodBody(pw, fat, method, "f" + me.getKey(), fieldType)) {
                            processingEnv.getMessager().printMessage(Kind.WARNING, "Could not determine desired semantics of method from its signature", method);
                        }
                    }
                });
                pw.println("}");
            }
        }

        private boolean printMethodBody(PrintWriter pw, FieldAccessorType accessorType, ExecutableElement method, String fieldName, TypeMirror fieldType) {
            TypeMirror firstParameterType = method.getParameters().isEmpty()
              ? types.getNullType()
              : method.getParameters().get(0).asType();
            TypeElement typeElement = elements.getTypeElement(types.erasure(fieldType).toString());

            switch (accessorType) {
                case GETTER:
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method + " {");
                    pw.println("        return " + fieldName + ";");
                    pw.println("    }");
                    return true;
                case SETTER:
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
                    if (! isImmutableFinalType(fieldType)) {
                        pw.println("        p0 = " + deepClone(fieldType, "p0") + ";");
                    }
                    pw.println("        updated |= ! Objects.equals(" + fieldName + ", p0);");
                    pw.println("        " + fieldName + " = p0;");
                    pw.println("    }");
                    return true;
                case COLLECTION_ADD:
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
                    pw.println("        if (" + fieldName + " == null) { " + fieldName + " = " + interfaceToImplementation(typeElement, "") + "; }");
                    if (! isImmutableFinalType(firstParameterType)) {
                        pw.println("        p0 = " + deepClone(fieldType, "p0") + ";");
                    }
                    if (isSetType(typeElement)) {
                        pw.println("        updated |= " + fieldName + ".add(p0);");
                    } else {
                        pw.println("        " + fieldName + ".add(p0);");
                        pw.println("        updated = true;");
                    }
                    pw.println("    }");
                    return true;
                case COLLECTION_DELETE:
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
                    pw.println("        if (" + fieldName + " == null) { return; }");
                    pw.println("        boolean removed = " + fieldName + ".remove(p0)" + ("java.util.Map".equals(typeElement.getQualifiedName().toString()) ? " != null" : "") + ";");
                    pw.println("        updated |= removed;");
                    pw.println("    }");
                    return true;
                case MAP_ADD:
                    TypeMirror secondParameterType = method.getParameters().get(1).asType();
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0, " + secondParameterType + " p1) {");
                    pw.println("        if (" + fieldName + " == null) { " + fieldName + " = " + interfaceToImplementation(typeElement, "") + "; }");
                    if (! isImmutableFinalType(secondParameterType)) {
                        pw.println("        p1 = " + deepClone(secondParameterType, "p1") + ";");
                    }
                    pw.println("        Object v = " + fieldName + ".put(p0, p1);");
                    pw.println("        updated |= ! Objects.equals(v, p1);");
                    pw.println("    }");
                    return true;
                case MAP_GET:
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
                    pw.println("        return " + fieldName + " == null ? null : " + fieldName + ".get(p0);");
                    pw.println("    }");
                    return true;
            }

            return false;
        }

        private String deepClone(TypeMirror fieldType, String parameterName) {
            if (isKnownCollectionOfImmutableFinalTypes(fieldType)) {
                TypeElement typeElement = elements.getTypeElement(types.erasure(fieldType).toString());
                return parameterName + " == null ? null : " + interfaceToImplementation(typeElement, parameterName);
            } else {
                return "deepClone(" + parameterName + ")";
            }
        }
    }

    private class DelegateGenerator implements Generator {
        @Override
        public void generate(TypeElement e, Map<String, HashSet<ExecutableElement>> methodsPerAttribute) throws IOException {
            String className = e.getQualifiedName().toString();
            String packageName = null;
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = className.substring(0, lastDot);
            }

            String simpleClassName = className.substring(lastDot + 1);
            String mapClassName = className + "Delegate";
            String mapSimpleClassName = simpleClassName + "Delegate";
            String fieldsClassName = className + "Fields";

            GenerateEntityImplementations an = e.getAnnotation(GenerateEntityImplementations.class);
            TypeElement parentTypeElement = elements.getTypeElement((an.inherits() == null || an.inherits().isEmpty()) ? "void" : an.inherits());
            if (parentTypeElement == null) {
                return;
            }
            final List<? extends Element> allMembers = elements.getAllMembers(e);

            JavaFileObject file = processingEnv.getFiler().createSourceFile(mapClassName);
            IdentityHashMap<ExecutableElement, String> m2field = new IdentityHashMap<>();
            methodsPerAttribute.forEach((f, s) -> s.forEach(m -> m2field.put(m, f)));   // Create reverse map
            try (PrintWriter pw = new PrintWriterNoJavaLang(file.openWriter())) {
                if (packageName != null) {
                    pw.println("package " + packageName + ";");
                }

                pw.println("public class " + mapSimpleClassName + " implements " + className + " {");
                pw.println("    private final org.keycloak.models.map.common.delegate.DelegateProvider<" + mapSimpleClassName + "> delegateProvider;");
                pw.println("    public " + mapSimpleClassName + "(org.keycloak.models.map.common.delegate.DelegateProvider<" + mapSimpleClassName + "> delegateProvider) {");
                pw.println("        this.delegateProvider = delegateProvider;");
                pw.println("    }");

                allMembers.stream()
                  .filter(m -> m.getKind() == ElementKind.METHOD)
                  .filter(ExecutableElement.class::isInstance)
                  .map(ExecutableElement.class::cast)
                  .filter(ee -> ee.getModifiers().contains(Modifier.ABSTRACT))
                  .forEach(ee -> {
                      pw.println("    @Override "
                        + ee.getModifiers().stream().filter(m -> m != Modifier.ABSTRACT).map(Object::toString).collect(Collectors.joining(" "))
                        + " " + ee.getReturnType()
                        + " " + ee.getSimpleName()
                        + "(" + methodParameters(ee.getParameters()) + ") {");
                      String field = m2field.get(ee);
                      field = field == null ? "null" : fieldsClassName + "." + toEnumConstant(field);
                      if (ee.getReturnType().getKind() == TypeKind.BOOLEAN && "isUpdated".equals(ee.getSimpleName().toString())) {
                          pw.println("        return delegateProvider.isUpdated();");
                      } else if (ee.getReturnType().getKind() == TypeKind.VOID) {  // write operation
                          pw.println("        delegateProvider.getDelegate(false, " + field + ")." + ee.getSimpleName() + "("
                            + ee.getParameters().stream().map(VariableElement::getSimpleName).collect(Collectors.joining(", "))
                            + ");");
                      } else {
                          pw.println("        return delegateProvider.getDelegate(true, " + field + ")." + ee.getSimpleName() + "("
                            + ee.getParameters().stream().map(VariableElement::getSimpleName).collect(Collectors.joining(", "))
                            + ");");
                      }
                      pw.println("    }");
                  });

                pw.println("}");
            }
        }
    }
}
