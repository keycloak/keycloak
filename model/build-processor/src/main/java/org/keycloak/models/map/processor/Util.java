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

import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;

/**
 *
 * @author hmlnarik
 */
public class Util {

    private static final HashSet<String> SET_TYPES = new HashSet<>(Arrays.asList(Set.class.getCanonicalName(), TreeSet.class.getCanonicalName(), HashSet.class.getCanonicalName(), LinkedHashSet.class.getCanonicalName()));
    private static final HashSet<String> MAP_TYPES = new HashSet<>(Arrays.asList(Map.class.getCanonicalName(), HashMap.class.getCanonicalName()));

    public static List<TypeMirror> getGenericsDeclaration(TypeMirror fieldType) {
        List<TypeMirror> res = new LinkedList<>();

        fieldType.accept(new SimpleTypeVisitor8<Void, List<TypeMirror>>() {
            @Override
            public Void visitDeclared(DeclaredType t, List<TypeMirror> p) {
                List<? extends TypeMirror> typeArguments = t.getTypeArguments();
                res.addAll(typeArguments);
                return null;
            }
        }, res);

        return res;
    }

    public static String methodParameters(List<? extends VariableElement> parameters) {
        return parameters.stream()
          .map(p -> p.asType() + " " + p.getSimpleName())
          .collect(Collectors.joining(", "));
    }

    public static boolean isSetType(TypeElement typeElement) {
        Name name = typeElement.getQualifiedName();
        return SET_TYPES.contains(name.toString());
    }

    public static boolean isMapType(TypeElement typeElement) {
        Name name = typeElement.getQualifiedName();
        return MAP_TYPES.contains(name.toString());
    }

    public static boolean isNotIgnored(Element el) {
        do {
            IgnoreForEntityImplementationGenerator a = el.getAnnotation(IgnoreForEntityImplementationGenerator.class);
            if (a != null) {
                return false;
            }
            el = el.getEnclosingElement();
        } while (el != null);
        return true;
    }

    protected static Optional<ExecutableElement> findParentMethodImplementation(List<? extends Element> allParentMembers, ExecutableElement method) {
        return allParentMembers.stream()
          .filter(ExecutableElement.class::isInstance)
          .map(ExecutableElement.class::cast)
          .filter(ee -> Objects.equals(ee.toString(), method.toString()))
          .filter((ExecutableElement ee) ->  ! ee.getModifiers().contains(Modifier.ABSTRACT))
          .findAny();
    }

    public static String singularToPlural(String word) {
        if (word.endsWith("y")) {
            return word.substring(0, word.length() -1) + "ies";
        } else if (word.endsWith("s")) {
            return word + "es";
        } else {
            return word + "s";
        }
    }

    public static String pluralToSingular(String word) {
        if (word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        } else if (word.endsWith("ses")) {
            return word.substring(0, word.length() - 2);
        } else {
            return word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
        }
    }
}
