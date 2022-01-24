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

import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.exceptions.CannotMigrateTypeException;

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
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.keycloak.models.map.processor.Util.getGenericsDeclaration;
import static org.keycloak.models.map.processor.Util.isMapType;
import static org.keycloak.models.map.processor.Util.isSetType;
import static org.keycloak.models.map.processor.Util.methodParameters;

@SupportedAnnotationTypes("org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GenerateHotRodEntityImplementationsProcessor extends AbstractGenerateEntityImplementationsProcessor {


    @Override
    protected Generator[] getGenerators() {
        return new Generator[] { new HotRodGettersAndSettersDelegateGenerator() };
    }


    private class HotRodGettersAndSettersDelegateGenerator implements Generator {

        private static final String ENTITY_VARIABLE = "hotRodEntity";
        private String hotRodSimpleClassName;
        private TypeElement generalHotRodDelegate;
        private TypeElement abstractEntity;
        private TypeElement abstractHotRodEntity;
        private TypeElement hotRodUtils;

        @Override
        public void generate(TypeElement e) throws IOException {
            GenerateHotRodEntityImplementation hotRodAnnotation = e.getAnnotation(GenerateHotRodEntityImplementation.class);
            String interfaceClass = hotRodAnnotation.implementInterface();
            if (interfaceClass == null || interfaceClass.isEmpty()) return;
            TypeElement parentClassElement = elements.getTypeElement(hotRodAnnotation.inherits());
            if (parentClassElement == null) return;
            boolean parentClassHasGeneric = !getGenericsDeclaration(parentClassElement.asType()).isEmpty();


            TypeElement parentInterfaceElement = elements.getTypeElement(interfaceClass);
            if (parentInterfaceElement == null) return;
            Map<String, HashSet<ExecutableElement>> methodsPerAttribute = methodsPerAttributeMapping(parentInterfaceElement);


            final List<? extends Element> allMembers = elements.getAllMembers(parentClassElement);
            String className = e.getQualifiedName().toString();

            String packageName = null;
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = className.substring(0, lastDot);
            }

            String simpleClassName = className.substring(lastDot + 1);
            String hotRodImplClassName = className + "Delegate";
            hotRodSimpleClassName = simpleClassName + "Delegate";
            generalHotRodDelegate = elements.getTypeElement("org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate");
            abstractEntity = elements.getTypeElement("org.keycloak.models.map.common.AbstractEntity");
            abstractHotRodEntity = elements.getTypeElement("org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity");
            hotRodUtils = elements.getTypeElement("org.keycloak.models.map.storage.hotRod.common.HotRodTypesUtils");

            boolean hasDeepClone = allMembers.stream().filter(el -> el.getKind() == ElementKind.METHOD).anyMatch(el -> "deepClone".equals(el.getSimpleName().toString()));
            boolean needsDeepClone = fieldGetters(methodsPerAttribute)
                    .map(ExecutableElement::getReturnType)
                    .anyMatch(fieldType -> ! isKnownCollectionOfImmutableFinalTypes(fieldType) && ! isImmutableFinalType(fieldType));
            boolean usingGeneratedCloner = ! hasDeepClone && needsDeepClone;
            boolean hasId = methodsPerAttribute.containsKey("Id") || allMembers.stream().anyMatch(el -> "getId".equals(el.getSimpleName().toString()));

            JavaFileObject file = processingEnv.getFiler().createSourceFile(hotRodImplClassName);
            try (PrintWriter pw = new PrintWriterNoJavaLang(file.openWriter())) {
                if (packageName != null) {
                    pw.println("package " + packageName + ";");
                }

                pw.println("import java.util.Objects;");
                pw.println("import " + FQN_DEEP_CLONER + ";");
                pw.println("import java.util.Optional;");
                pw.println("import java.util.stream.Collectors;");
                pw.println();
                pw.println("// DO NOT CHANGE THIS CLASS, IT IS GENERATED AUTOMATICALLY BY " + GenerateHotRodEntityImplementationsProcessor.class.getSimpleName());
                pw.println("public class " + hotRodSimpleClassName
                        + " extends "
                        + parentClassElement.getQualifiedName().toString() + (parentClassHasGeneric ? "<" + e.getQualifiedName().toString() + ">" : "")
                        + " implements "
                        + parentInterfaceElement.getQualifiedName().toString()
                        + " {");
                pw.println();
                pw.println("    private final " + className + " " + ENTITY_VARIABLE + ";");
                pw.println();

                // Constructors
                allMembers.stream()
                        .filter(ExecutableElement.class::isInstance)
                        .map(ExecutableElement.class::cast)
                        .filter((ExecutableElement ee) -> ee.getKind() == ElementKind.CONSTRUCTOR)
                        .forEach((ExecutableElement ee) -> {
                            // Create constructor and initialize cloner to DUMB_CLONER if necessary
                            if (usingGeneratedCloner) {
                                pw.println("    /**");
                                pw.println("     * @deprecated This constructor uses a {@link DeepCloner#DUMB_CLONER} that does not clone anything. Use {@link #" + hotRodSimpleClassName + "(DeepCloner)} variant instead");
                                pw.println("     */");
                            }
                            pw.println("    "
                                    + ee.getModifiers().stream().map(Object::toString).collect(Collectors.joining(" "))
                                    + " " + hotRodSimpleClassName + "(" + methodParameters(ee.getParameters()) + ") {"
                            );
                            pw.println("        super(" + ee.getParameters() + ");");
                            if (usingGeneratedCloner) pw.println("        this.cloner = DeepCloner.DUMB_CLONER;");
                            pw.println("        this." + ENTITY_VARIABLE + " = new " + className + "();");
                            pw.println("    }");
                        });

                // Add constructor for setting HotRodEntity
                if (usingGeneratedCloner) {
                    pw.println("    /**");
                    pw.println("     * @deprecated This constructor uses a {@link DeepCloner#DUMB_CLONER} that does not clone anything. Use {@link #" + hotRodSimpleClassName + "(DeepCloner)} variant instead");
                    pw.println("     */");
                }
                pw.println("    " +
                        "public " + hotRodSimpleClassName + "(" + className + " " + ENTITY_VARIABLE + ") {"
                );
                pw.println("        this." + ENTITY_VARIABLE + " = " + ENTITY_VARIABLE + ";");
                if (usingGeneratedCloner) {
                    pw.println("        this.cloner = DeepCloner.DUMB_CLONER;");
                }
                pw.println("    }");

                pw.println("    public " + hotRodSimpleClassName + "(DeepCloner cloner) {");
                pw.println("        super();");
                pw.println("        this." + ENTITY_VARIABLE + " = new " + className + "();");
                if (usingGeneratedCloner) pw.println("        this.cloner = cloner;");
                pw.println("    }");

                // equals, hashCode, toString
                pw.println("    @Override public boolean equals(Object o) {");
                pw.println("        if (o == this) return true; ");
                pw.println("        if (! (o instanceof " + hotRodSimpleClassName + ")) return false; ");
                pw.println("        " + hotRodSimpleClassName + " other = (" + hotRodSimpleClassName + ") o; ");
                pw.println("        return "
                        + fieldGetters(methodsPerAttribute)
                        .map(ExecutableElement::getSimpleName)
                        .map(Name::toString)
                        .sorted(NameFirstComparator.GET_ID_INSTANCE)
                        .map(v -> "Objects.equals(" + v + "(), other." + v + "())")
                        .collect(Collectors.joining("\n          && "))
                        + ";");
                pw.println("    }");
                pw.println("    @Override public int hashCode() {");
                pw.println("        return "
                        + (hasId
                        ? "(getId() == null ? super.hashCode() : getId().hashCode())"
                        : "Objects.hash("
                        + fieldGetters(methodsPerAttribute)
                        .filter(ee -> isImmutableFinalType(ee.getReturnType()))
                        .map(ExecutableElement::getSimpleName)
                        .map(Name::toString)
                        .sorted(GenerateEntityImplementationsProcessor.NameFirstComparator.GET_ID_INSTANCE)
                        .map(v -> v + "()")
                        .collect(Collectors.joining(",\n          "))
                        + ")")
                        + ";");
                pw.println("    }");
                pw.println("    @Override public String toString() {");
                pw.println("        return String.format(\"%s@%08x\", " + (hasId ? "getId()" : "\"" + hotRodSimpleClassName + "\"" ) + ", System.identityHashCode(this));");
                pw.println("    }");

                pw.println("    public static boolean entityEquals(Object o1, Object o2) {");
                pw.println("        if (!(o1 instanceof " + className + ")) return false;");
                pw.println("        if (!(o2 instanceof " + className + ")) return false;");

                pw.println("        if (o1 == o2) return true;");

                pw.println("        " + className + " e1 = (" + className + ") o1;");
                pw.println("        " + className + " e2 = (" + className + ") o2;");

                pw.print("        return ");
                pw.println(elements.getAllMembers(e).stream()
                        .filter(VariableElement.class::isInstance)
                        .map(VariableElement.class::cast)
                        .map(var -> "Objects.equals(e1." + var.getSimpleName().toString() + ", e2." + var.getSimpleName().toString() + ")")
                        .collect(Collectors.joining("\n            && ")));
                pw.println("            ;");
                pw.println("    }");

                pw.println("    public static int entityHashCode(" + className + " e) {");
                pw.println("        return "
                        + (hasId
                        ? "(e.id == null ? Objects.hash(e) : e.id.hashCode())"
                        : "Objects.hash("
                        + elements.getAllMembers(e).stream()
                        .filter(VariableElement.class::isInstance)
                        .map(VariableElement.class::cast)
                        .map(var -> var.getSimpleName().toString())
                        .collect(Collectors.joining(",\n          "))
                        + ")")
                        + ";"
                );
                pw.println("    }");

                // deepClone
                if (! hasDeepClone && needsDeepClone) {
                    pw.println("    private final DeepCloner cloner;");
                    pw.println("    public <V> V deepClone(V obj) {");
                    pw.println("        return cloner.from(obj);");
                    pw.println("    }");
                }

                // getters, setters
                methodsPerAttribute.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey, GenerateEntityImplementationsProcessor.NameFirstComparator.ID_INSTANCE)).forEach(me -> {
                    HashSet<ExecutableElement> methods = me.getValue();
                    TypeMirror fieldType = determineFieldType(me.getKey(), methods);
                    if (fieldType == null) {
                        return;
                    }

                    // Determine HotRod entity field name by changing case of first letter
                    char[] c = me.getKey().toCharArray();
                    c[0] = Character.toLowerCase(c[0]);
                    String hotRodEntityFieldName = new String(c);

                    // Find corresponding variable in HotRod*Entity
                    Optional<VariableElement> hotRodVariable = elements.getAllMembers(e).stream()
                            .filter(VariableElement.class::isInstance)
                            .map(VariableElement.class::cast)
                            .filter(variableElement -> variableElement.getSimpleName().toString().equals(hotRodEntityFieldName))
                            .findFirst();

                    if (!hotRodVariable.isPresent()) {
                        // throw an error when no variable found
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot find " + e.getSimpleName().toString() + " field for methods: [" + me.getValue().stream().map(ee -> ee.getSimpleName().toString()).collect(Collectors.joining(", ")) + "]", parentInterfaceElement);
                        return;
                    }

                    // Implement each method
                    for (ExecutableElement method : methods) {
                        FieldAccessorType fat = FieldAccessorType.determineType(method, me.getKey(), types, fieldType);

                        // Check if the parent class implements the method already
                        Optional<ExecutableElement> parentMethod = allMembers.stream()
                                .filter(ExecutableElement.class::isInstance)
                                .map(ExecutableElement.class::cast)
                                .filter(ee -> Objects.equals(ee.toString(), method.toString()))
                                .filter((ExecutableElement ee) ->  ! ee.getModifiers().contains(Modifier.ABSTRACT))
                                .findAny();

                        try {
                            if (parentMethod.isPresent()) {
                                // Do not implement the method if it is already implemented by the parent class
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.OTHER, "Method " + method + " is declared in a parent class.", method);
                            } else if (fat != FieldAccessorType.UNKNOWN && !printMethodBody(pw, fat, method, hotRodEntityFieldName, fieldType, hotRodVariable.get().asType())) {
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Could not determine desired semantics of method from its signature", method);
                            }
                        } catch (CannotMigrateTypeException ex) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getFormattedMessage(), method);
                        }
                    }
                });

                // Implement HotRodDelegate interface
                pw.println("    public " + className + " getHotRodEntity() {");
                pw.println("        return this." + ENTITY_VARIABLE  + ";");
                pw.println("    }");
                pw.println("}");
            }
        }

        private String hotRodEntityField(String fieldName) {
            return "this." + ENTITY_VARIABLE + "." + fieldName;
        }

        private boolean printMethodBody(PrintWriter pw, FieldAccessorType accessorType, ExecutableElement method, String fieldName, TypeMirror fieldType, TypeMirror hotRodFieldType) {
            TypeMirror firstParameterType = method.getParameters().isEmpty()
                    ? types.getNullType()
                    : method.getParameters().get(0).asType();
            TypeElement typeElement = elements.getTypeElement(types.erasure(fieldType).toString());

            switch (accessorType) {
                case GETTER:
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method + " {");
                    pw.println("        return " + migrateToType(method.getReturnType(), hotRodFieldType, hotRodEntityField(fieldName)) + ";");
                    pw.println("    }");
                    return true;
                case SETTER:
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
                    if (! isImmutableFinalType(firstParameterType)) {
                        pw.println("        p0 = " + deepClone(fieldType, "p0") + ";");
                    }
                    pw.println("        " + hotRodFieldType.toString() + " migrated = " + migrateToType(hotRodFieldType, firstParameterType, "p0") + ";");
                    pw.println("        " + hotRodEntityField("updated") + " |= ! Objects.equals(" + hotRodEntityField(fieldName) + ", migrated);");
                    pw.println("        " + hotRodEntityField(fieldName) + " = migrated;");
                    pw.println("    }");
                    return true;
                case COLLECTION_ADD:
                    TypeMirror collectionItemType = getGenericsDeclaration(hotRodFieldType).get(0);
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
                    pw.println("        if (" + hotRodEntityField(fieldName) + " == null) { " + hotRodEntityField(fieldName) + " = " + interfaceToImplementation(typeElement, "") + "; }");
                    if (! isImmutableFinalType(firstParameterType)) {
                        pw.println("        p0 = " + deepClone(fieldType, "p0") + ";");
                    }
                    pw.println("        " + collectionItemType.toString() + " migrated = " + migrateToType(collectionItemType, firstParameterType, "p0") + ";");
                    if (isSetType(typeElement)) {
                        pw.println("        " + hotRodEntityField("updated") + " |= " + hotRodEntityField(fieldName) + ".add(migrated);");
                    } else {
                        pw.println("        " + hotRodEntityField(fieldName) + ".add(migrated);");
                        pw.println("        " + hotRodEntityField("updated") + " = true;");
                    }
                    pw.println("    }");
                    return true;
                case COLLECTION_DELETE:
                    collectionItemType = getGenericsDeclaration(hotRodFieldType).get(0);
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
                    if (isMapType(typeElement)) {
                        // Maps are stored as sets
                        pw.println("        " + hotRodEntityField("updated") + " |= " + hotRodUtils.getQualifiedName().toString() + ".removeFromSetByMapKey("
                                + hotRodEntityField(fieldName) + ", "
                                + "p0, "
                                + keyGetterReference(collectionItemType) + ");"
                        );
                    } else {
                        pw.println("        if (" + hotRodEntityField(fieldName) + " == null) { return; }");
                        pw.println("        boolean removed = " + hotRodEntityField(fieldName) + ".remove(p0);");
                        pw.println("        " + hotRodEntityField("updated") + " |= removed;");
                    }
                    pw.println("    }");
                    return true;
                case MAP_ADD:
                    collectionItemType = getGenericsDeclaration(hotRodFieldType).get(0);
                    TypeMirror secondParameterType = method.getParameters().get(1).asType();
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0, " + secondParameterType + " p1) {");
                    pw.println("        if (" + hotRodEntityField(fieldName) + " == null) { " + hotRodEntityField(fieldName) + " = " + interfaceToImplementation((TypeElement) types.asElement(types.erasure(hotRodFieldType)), "") + "; }");
                    pw.println("        boolean valueUndefined = p1 == null" + (isCollection(secondParameterType) ? " || p1.isEmpty()" : "") + ";");
                    if (! isImmutableFinalType(secondParameterType)) {
                        pw.println("        p1 = " + deepClone(secondParameterType, "p1") + ";");
                    }
                    pw.println("        " + hotRodEntityField("updated") + " |= " + hotRodUtils.getQualifiedName().toString() + ".removeFromSetByMapKey("
                            + hotRodEntityField(fieldName) + ", "
                            + "p0, "
                            + keyGetterReference(collectionItemType) + ");"
                    );
                    pw.println("        " + hotRodEntityField("updated") + " |= !valueUndefined && " + hotRodEntityField(fieldName)
                            + ".add(" + migrateToType(collectionItemType, new TypeMirror[]{firstParameterType, secondParameterType}, new String[]{"p0", "p1"}) + ");");
                    pw.println("    }");
                    return true;
                case MAP_GET:
                    pw.println("    @SuppressWarnings(\"unchecked\") @Override public " + method.getReturnType() + " " + method.getSimpleName() + "(" + firstParameterType + " p0) {");
                    collectionItemType = getGenericsDeclaration(hotRodFieldType).get(0);
                    pw.println("        return " + hotRodUtils.getQualifiedName().toString() + ".getMapValueFromSet("
                            + hotRodEntityField(fieldName) + ", "
                            + "p0, "
                            + keyGetterReference(collectionItemType) + ", "
                            + valueGetterReference(collectionItemType) + ");"
                    );
                    pw.println("    }");
                    return true;
            }

            return false;
        }

        private String migrateToType(TypeMirror toType, TypeMirror fromType, String fieldName) {
            return migrateToType(toType, new TypeMirror[] {fromType}, new String[]{fieldName});
        }

        private String toSimpleName(TypeMirror typeMirror) {
            TypeElement e = elements.getTypeElement(types.erasure(typeMirror).toString());
            return e.getSimpleName().toString();
        }

        private String keyGetterReference(TypeMirror type) {
            if (types.isAssignable(type, abstractHotRodEntity.asType())) {
                return "e -> e.id";
            }
            return hotRodUtils.getQualifiedName().toString() + "::getKey";
        }

        private String valueGetterReference(TypeMirror type) {
            if (types.isAssignable(type, abstractHotRodEntity.asType())) {
                return toSimpleName(type) + "Delegate::new";
            }
            return hotRodUtils.getQualifiedName().toString() + "::getValue";
        }

        private String migrateToType(TypeMirror toType, TypeMirror[] fromType, String[] fieldNames) {
            // No migration needed, fromType is assignable to toType directly
            if (fromType.length == 1 && types.isAssignable(types.erasure(fromType[0]), types.erasure(toType))) {
                return fieldNames[0];
            }

            // HotRod entities are not allowed to use Maps, therefore we often need to migrate from Map to Set and the other way around
            if (fromType.length == 1) {
                if (isSetType((TypeElement) types.asElement(types.erasure(toType)))
                        && isMapType((TypeElement) types.asElement(types.erasure(fromType[0])))) {
                    TypeMirror setType = getGenericsDeclaration(toType).get(0);

                    return hotRodUtils.getQualifiedName().toString() + ".migrateMapToSet("
                            + fieldNames[0] + ", "
                            + hotRodUtils.getQualifiedName().toString() + "::create" + toSimpleName(setType) + "FromMapEntry)";
                } else if (isMapType((TypeElement) types.asElement(types.erasure(toType)))
                        && isSetType((TypeElement) types.asElement(types.erasure(fromType[0])))) {
                    TypeMirror setType = getGenericsDeclaration(fromType[0]).get(0);

                    return hotRodUtils.getQualifiedName().toString() + ".migrateSetToMap("
                            + fieldNames[0] + ", "
                            + keyGetterReference(setType) + ", "
                            + valueGetterReference(setType)
                            + ")";
                }

            }

            // Try to find constructor that can do the migration
            if (findSuitableConstructor(toType, fromType).isPresent()) {
                return "new " + toType.toString() + "(" + String.join(", ", fieldNames) + ")";
            }

            // Check if any of parameters is another Map*Entity
            OptionalInt anotherMapEntityIndex = IntStream.range(0, fromType.length)
                    .filter(i -> types.isAssignable(fromType[i], abstractEntity.asType()))
                    .findFirst();

            if (anotherMapEntityIndex.isPresent()) {
                // If yes, we can be sure that it implements HotRodEntityDelegate (this is achieved by HotRod cloner settings) so we can just call getHotRodEntity method
                return "((" + generalHotRodDelegate.getQualifiedName().toString() + "<" + toType.toString() + ">) " + fieldNames[anotherMapEntityIndex.getAsInt()] + ").getHotRodEntity()";
            }

            // Check if any of parameters is another HotRod*Entity
            OptionalInt anotherHotRodEntityIndex = IntStream.range(0, fromType.length)
                    .filter(i -> types.isAssignable(fromType[i], abstractHotRodEntity.asType()))
                    .findFirst();

            if (anotherHotRodEntityIndex.isPresent()) {
                // If yes, we can be sure that it implements HotRodEntityDelegate (this is achieved by HotRod cloner settings) so we can just call getHotRodEntity method
                return "new " + fromType[anotherHotRodEntityIndex.getAsInt()] + "Delegate(" + String.join(", ", fieldNames) + ")";
            }

            throw new CannotMigrateTypeException(toType, fromType);
        }

        private Optional<ExecutableElement> findSuitableConstructor(TypeMirror desiredType, TypeMirror[] parameters) {
            // Try to find constructor that can do the migration
            TypeElement type = (TypeElement) types.asElement(desiredType);
            return elements.getAllMembers(type)
                    .stream()
                    .filter(ExecutableElement.class::isInstance)
                    .map(ExecutableElement.class::cast)
                    .filter(ee -> ee.getKind() == ElementKind.CONSTRUCTOR)
                    .filter(ee -> ee.getParameters().size() == parameters.length)
                    .filter(method -> IntStream.range(0, parameters.length).allMatch(i -> deepCompareTypes(parameters[i], method.getParameters().get(i).asType())))
                    .findFirst();
        }


        private boolean deepCompareTypes(TypeMirror fromType, TypeMirror toType) {
            return types.isAssignable(types.erasure(fromType), types.erasure(toType))
                    && deepCompareTypes(getGenericsDeclaration(fromType), getGenericsDeclaration(toType));
        }

        private boolean deepCompareTypes(List<TypeMirror> fromTypes, List<TypeMirror> toTypes) {
            if (fromTypes.size() == 0 && toTypes.size() == 0) return true;
            if (fromTypes.size() != toTypes.size()) return false;

            for (int i = 0; i < fromTypes.size(); i++) {
                if (!deepCompareTypes(fromTypes.get(i), toTypes.get(i))) return false;
            }
            return true;
        }
    }
}
