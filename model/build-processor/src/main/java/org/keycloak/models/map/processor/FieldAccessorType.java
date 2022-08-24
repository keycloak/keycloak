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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import static org.keycloak.models.map.processor.Util.getGenericsDeclaration;
import static org.keycloak.models.map.processor.Util.pluralToSingular;

/**
 *
 * @author hmlnarik
 */
enum FieldAccessorType {
    GETTER {
        @Override
        public boolean is(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType) {
            Pattern getter = Pattern.compile("(get|is)" + Pattern.quote(fieldName));
            Name methodName = method.getSimpleName();
            return getter.matcher(methodName).matches() && method.getParameters().isEmpty() && types.isSameType(fieldType, method.getReturnType());
        }
    },
    SETTER {
        @Override
        public boolean is(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType) {
            String methodName = "set" + fieldName;
            return Objects.equals(methodName, method.getSimpleName().toString())
              && method.getParameters().size() == 1
              && types.isSameType(fieldType, method.getParameters().get(0).asType());
        }
    },
    COLLECTION_ADD {
        @Override
        public boolean is(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType) {
            String fieldNameSingular = pluralToSingular(fieldName);
            String methodName = "add" + fieldNameSingular;
            List<TypeMirror> res = getGenericsDeclaration(fieldType);
            return Objects.equals(methodName, method.getSimpleName().toString())
              && res.size() == 1
              && types.isSameType(res.get(0), method.getParameters().get(0).asType());
        }
    },
    COLLECTION_DELETE {
        @Override
        public boolean is(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType) {
            String fieldNameSingular = pluralToSingular(fieldName);
            String removeFromCollection = "remove" + fieldNameSingular;
            List<TypeMirror> res = getGenericsDeclaration(fieldType);
            return Objects.equals(removeFromCollection, method.getSimpleName().toString())
              && method.getParameters().size() == 1
              && types.isSameType(res.get(0), method.getParameters().get(0).asType());
        }
    },
    MAP_ADD {
        @Override
        public boolean is(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType) {
            String fieldNameSingular = pluralToSingular(fieldName);
            String methodName = "set" + fieldNameSingular;
            List<TypeMirror> res = getGenericsDeclaration(fieldType);
            return Objects.equals(methodName, method.getSimpleName().toString())
              && res.size() == 2
              && types.isSameType(res.get(0), method.getParameters().get(0).asType())
              && types.isSameType(res.get(1), method.getParameters().get(1).asType());
        }
    },
    MAP_GET {
        @Override
        public boolean is(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType) {
            String fieldNameSingular = pluralToSingular(fieldName);
            String methodName = "get" + fieldNameSingular;
            List<TypeMirror> res = getGenericsDeclaration(fieldType);
            return Objects.equals(methodName, method.getSimpleName().toString())
              && res.size() == 2
              && types.isSameType(res.get(0), method.getParameters().get(0).asType());
        }
    },
    UNKNOWN /* Must be the last */ {
        @Override
        public boolean is(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType) {
            return true;
        }

    }
    ;

    public abstract boolean is(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType);

    public static Optional<ExecutableElement> getMethod(FieldAccessorType type, 
      HashSet<ExecutableElement> methods, String fieldName, Types types, TypeMirror fieldType) {
        return methods.stream().filter(ee -> type.is(ee, fieldName, types, fieldType)).findAny();
    }

    public static FieldAccessorType determineType(ExecutableElement method, String fieldName, Types types, TypeMirror fieldType) {
        for (FieldAccessorType fat : values()) {
            if (fat.is(method, fieldName, types, fieldType)) {
                return fat;
            }
        }
        return UNKNOWN;
    }
}
