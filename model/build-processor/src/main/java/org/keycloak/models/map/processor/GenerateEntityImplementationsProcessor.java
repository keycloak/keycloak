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
import org.keycloak.models.map.annotations.GenerateEnumMapFieldType;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 *
 * @author hmlnarik
 */
@SupportedAnnotationTypes("org.keycloak.models.map.annotations.GenerateEntityImplementations")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GenerateEntityImplementationsProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
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
            processingEnv.getMessager().printMessage(Kind.ERROR, "Annotation @GenerateEntityImplementations is only applicable to interface", e);
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

        try {
            generateImpl(e, methodsPerAttribute);
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Could not generate implementation for class", e);
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

    private void generateImpl(TypeElement e, Map<String, HashSet<ExecutableElement>> methodsPerAttribute) throws IOException {
        GenerateEntityImplementations an = e.getAnnotation(GenerateEntityImplementations.class);
        Elements elements = processingEnv.getElementUtils();
        TypeElement parentTypeElement = elements.getTypeElement(an.inherits().isEmpty() ? "void" : an.inherits());
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

        JavaFileObject enumFile = processingEnv.getFiler().createSourceFile(mapImplClassName);
        try (PrintWriter pw = new PrintWriter(enumFile.openWriter()) {
            @Override
            public void println(String x) {
                super.println(x == null ? x : x.replaceAll("java.lang.", ""));
            }
        }) {
            if (packageName != null) {
                pw.println("package " + packageName + ";");
            }

            pw.println("import java.util.EnumMap;");
            pw.println("import java.util.Objects;");
            pw.println("// DO NOT CHANGE THIS CLASS, IT IS GENERATED AUTOMATICALLY BY " + GenerateEntityImplementationsProcessor.class.getSimpleName());
            pw.println("public class " + mapSimpleClassName + (an.inherits().isEmpty() ? "" : " extends " + an.inherits()) + " implements " + className + " {");
                pw.println("    public enum Field {");
                methodsPerAttribute.keySet().stream()
                  .sorted()
                  .map(GenerateEntityImplementationsProcessor::toEnumConstant)
                  .forEach(key -> pw.println("        " + key + ","));
                pw.println("    }");
            pw.println("    private final EnumMap<Field, Object> values = new EnumMap<>(Field.class);");
            pw.println("    protected Object get(Field field) { return values.get(field); }");
            pw.println("    protected Object set(Field field, Object p0) { return values.put(field, p0); }");
            
            // Constructors
            allMembers.stream()
              .filter(ExecutableElement.class::isInstance)
              .map(ExecutableElement.class::cast)
              .filter((ExecutableElement ee) -> ee.getKind() == ElementKind.CONSTRUCTOR)
              .forEach((ExecutableElement ee) -> pw.println("    public " + mapSimpleClassName + "(" + methodParameters(ee.getParameters()) + ") { super(" + ee.getParameters() + "); }"));

            for (Entry<String, HashSet<ExecutableElement>> me : methodsPerAttribute.entrySet()) {
                String enumConstant = toEnumConstant(me.getKey());
                HashSet<ExecutableElement> methods = me.getValue();
                TypeMirror fieldType = determineFieldType(me.getKey(), methods);
                if (fieldType == null) {
                    continue;
                }

                for (ExecutableElement method : methods) {
                    if (! printMethodBody(pw, method, me.getKey(), enumConstant, fieldType)) {
                        List<ExecutableElement> parentMethods = allMembers.stream()
                          .filter(ExecutableElement.class::isInstance)
                          .map(ExecutableElement.class::cast)
                          .filter(ee -> Objects.equals(ee.toString(), method.toString()))
                          .filter((ExecutableElement ee) -> ! ee.getModifiers().contains(Modifier.ABSTRACT))
                          .collect(Collectors.toList());
                        if (! parentMethods.isEmpty()) {
                            processingEnv.getMessager().printMessage(Kind.OTHER, "Method " + method + " is declared in a parent class.");
                        } else {
                            processingEnv.getMessager().printMessage(Kind.WARNING, "Could not determine desired semantics of method from its signature", method);
                        }
                    }
                }
            }
            pw.println("}");

        }
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
            processingEnv.getMessager().printMessage(Kind.ERROR, "Could not determine return type for field " + fieldName, methods.iterator().next());
        }
        return res;
    }

    private boolean printMethodBody(PrintWriter pw, ExecutableElement method, String fieldName, String enumConstant, TypeMirror fieldType) {
        Pattern getter = Pattern.compile("(get|is)" + Pattern.quote(fieldName));
        Types types = processingEnv.getTypeUtils();
        final String methodName = method.getSimpleName().toString();
        String setter = "set" + fieldName;
        TypeMirror firstParameterType = method.getParameters().isEmpty()
          ? types.getNullType()
          : method.getParameters().get(0).asType();
        String fieldNameSingular = fieldName.endsWith("s") ? fieldName.substring(0, fieldName.length() - 1) : fieldName;
        String getFromMap = "get" + fieldNameSingular;
        String addToCollection = "add" + fieldNameSingular;
        String updateMap = "set" + fieldNameSingular;
        String removeFromCollection = "remove" + fieldNameSingular;
        Elements elements = processingEnv.getElementUtils();
        TypeElement typeElement = elements.getTypeElement(types.erasure(fieldType).toString());

        if (getter.matcher(methodName).matches() && method.getParameters().isEmpty() && types.isSameType(fieldType, method.getReturnType())) {
            pw.println("    @Override public " + method.getReturnType() + " " + method + " {");
            pw.println("        return (" + fieldType + ") get(Field." + enumConstant + ");");
            pw.println("    }");
            return true;
        } else if (setter.equals(methodName) && types.isSameType(firstParameterType, fieldType)) {
            pw.println("    @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
            pw.println("        Object o = set(Field." + enumConstant + ", p0);");
            pw.println("        updated |= ! Objects.equals(o, p0);");
            pw.println("    }");
            return true;
        } else if (addToCollection.equals(methodName) && method.getParameters().size() == 1) {
            pw.println("    @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
            pw.println("        " + fieldType + " o = (" + fieldType + ") get(Field." + enumConstant + ");");
            pw.println("        if (o == null) { o = " + interfaceToImplementation(typeElement) + "; set(Field." + enumConstant + ", o); }");
            if (isSetType(typeElement)) {
                pw.println("        updated |= o.add(p0);");
            } else {
                pw.println("        o.add(p0);");
                pw.println("        updated = true;");
            }
            pw.println("    }");
            return true;
        } else if (removeFromCollection.equals(methodName) && method.getParameters().size() == 1) {
            pw.println("    @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
            pw.println("        " + fieldType + " o = (" + fieldType + ") get(Field." + enumConstant + ");");
            pw.println("        if (o == null) { return; }");
            pw.println("        boolean removed = o.remove(p0)" + ("java.util.Map".equals(typeElement.getQualifiedName().toString()) ? " != null" : "") + ";");
            pw.println("        updated |= removed;");
            pw.println("    }");
            return true;
        } else if (updateMap.equals(methodName) && method.getParameters().size() == 2) {
            pw.println("    @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0, " + method.getParameters().get(1).asType() + " p1) {");
            pw.println("        " + fieldType + " o = (" + fieldType + ") get(Field." + enumConstant + ");");
            pw.println("        if (o == null) { o = " + interfaceToImplementation(typeElement) + "; set(Field." + enumConstant + ", o); }");
            pw.println("        Object v = o.put(p0, p1);");
            pw.println("        updated |= ! Objects.equals(v, p1);");
            pw.println("    }");
            return true;
        } else if (getFromMap.equals(methodName) && method.getParameters().size() == 1) {
            pw.println("    @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
            pw.println("        " + fieldType + " o = (" + fieldType + ") get(Field." + enumConstant + ");");
            pw.println("        return o == null ? null : o.get(p0);");
            pw.println("    }");
            return true;
        }

        return false;
    }

    private String interfaceToImplementation(TypeElement typeElement) {
        GenerateEnumMapFieldType an = typeElement.getAnnotation(GenerateEnumMapFieldType.class);
        if (an != null) {
            return "new " + an.value().getCanonicalName() + "<>()";
        }

        Name parameterTypeQN = typeElement.getQualifiedName();
        switch (parameterTypeQN.toString()) {
            case "java.util.List":
                return "new java.util.LinkedList<>()";
            case "java.util.Map":
                return "new java.util.HashMap<>()";
            case "java.util.Set":
                return "new java.util.HashSet<>()";
            case "java.util.Collection":
                return "new java.util.LinkedList<>()";
            default:
                processingEnv.getMessager().printMessage(Kind.ERROR, "Could not determine implementation for type " + typeElement, typeElement);
                return "TODO()";
        }
    }

    private String methodParameters(List<? extends VariableElement> parameters) {
        return parameters.stream()
          .map(p -> p.asType() + " " + p.getSimpleName())
          .collect(Collectors.joining(", "));
    }

    private static final HashSet<String> SET_TYPES = new HashSet<>(Arrays.asList(Set.class.getCanonicalName(), TreeSet.class.getCanonicalName(), HashSet.class.getCanonicalName(), LinkedHashSet.class.getCanonicalName()));

    private boolean isSetType(TypeElement typeElement) {
        Name name = typeElement.getQualifiedName();
        return SET_TYPES.contains(name.toString());
    }
}
