/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util.reflections;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Types
 */
public class Types {
    private Types() {

    }

    /**
     * Gets the boxed type of a class
     *
     * @param type The type
     *
     * @return The boxed type
     */
    public static Type boxedType(Type type) {
        if (type instanceof Class<?>) {
            return boxedClass((Class<?>) type);
        } else {
            return type;
        }
    }

    public static Class<?> boxedClass(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        } else if (type.equals(Boolean.TYPE)) {
            return Boolean.class;
        } else if (type.equals(Character.TYPE)) {
            return Character.class;
        } else if (type.equals(Byte.TYPE)) {
            return Byte.class;
        } else if (type.equals(Short.TYPE)) {
            return Short.class;
        } else if (type.equals(Integer.TYPE)) {
            return Integer.class;
        } else if (type.equals(Long.TYPE)) {
            return Long.class;
        } else if (type.equals(Float.TYPE)) {
            return Float.class;
        } else if (type.equals(Double.TYPE)) {
            return Double.class;
        } else if (type.equals(Void.TYPE)) {
            return Void.class;
        } else {
            // Vagaries of if/else statement, can't be reached ;-)
            return type;
        }
    }

    /**
     * Is the genericType of a certain class?
     */
    public static boolean isA(Class clazz, ParameterizedType pType)
    {
        return clazz.isAssignableFrom((Class) pType.getRawType());
    }

    /**
     * Gets the index-th type argument.
     */
    public static Class getArgumentType(ParameterizedType pType, int index)
    {
        return (Class) pType.getActualTypeArguments()[index];
    }

    public static Class getTemplateParameterOfInterface(Class base, Class desiredInterface)
    {
        Object rtn = searchForInterfaceTemplateParameter(base, desiredInterface);
        if (rtn != null && rtn instanceof Class) return (Class) rtn;
        return null;
    }


    private static Object searchForInterfaceTemplateParameter(Class base, Class desiredInterface)
    {
        for (int i = 0; i < base.getInterfaces().length; i++)
        {
            Class intf = base.getInterfaces()[i];
            if (intf.equals(desiredInterface))
            {
                Type generic = base.getGenericInterfaces()[i];
                if (generic instanceof ParameterizedType)
                {
                    ParameterizedType p = (ParameterizedType) generic;
                    Type type = p.getActualTypeArguments()[0];
                    Class rtn = getRawTypeNoException(type);
                    if (rtn != null) return rtn;
                    return type;
                }
                else
                {
                    return null;
                }
            }
        }
        if (base.getSuperclass() == null || base.getSuperclass().equals(Object.class)) return null;
        Object rtn = searchForInterfaceTemplateParameter(base.getSuperclass(), desiredInterface);
        if (rtn == null || rtn instanceof Class) return rtn;
        if (!(rtn instanceof TypeVariable)) return null;

        String name = ((TypeVariable) rtn).getName();
        int index = -1;
        TypeVariable[] variables = base.getSuperclass().getTypeParameters();
        if (variables == null || variables.length < 1) return null;

        for (int i = 0; i < variables.length; i++)
        {
            if (variables[i].getName().equals(name)) index = i;
        }
        if (index == -1) return null;


        Type genericSuperclass = base.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType)) return null;

        ParameterizedType pt = (ParameterizedType) genericSuperclass;
        Type type = pt.getActualTypeArguments()[index];

        Class clazz = getRawTypeNoException(type);
        if (clazz != null) return clazz;
        return type;
    }


    /**
     * See if the two methods are compatible, that is they have the same relative signature
     *
     * @param method
     * @param intfMethod
     * @return
     */
    public static boolean isCompatible(Method method, Method intfMethod)
    {
        if (method == intfMethod) return true;

        if (!method.getName().equals(intfMethod.getName())) return false;
        if (method.getParameterTypes().length != intfMethod.getParameterTypes().length) return false;

        for (int i = 0; i < method.getParameterTypes().length; i++)
        {
            Class rootParam = method.getParameterTypes()[i];
            Class intfParam = intfMethod.getParameterTypes()[i];
            if (!intfParam.isAssignableFrom(rootParam)) return false;
        }
        return true;
    }

    /**
     * Given a method and a root class, find the actual method declared in the root that implements the method.
     *
     * @param clazz
     * @param intfMethod
     * @return
     */
    public static Method getImplementingMethod(Class clazz, Method intfMethod)
    {
        Class<?> declaringClass = intfMethod.getDeclaringClass();
        if (declaringClass.equals(clazz)) return intfMethod;

        Class[] paramTypes = intfMethod.getParameterTypes();

        if (declaringClass.getTypeParameters().length > 0 && paramTypes.length > 0)
        {
            Type[] intfTypes = findParameterizedTypes(clazz, declaringClass);
            Map<String, Type> typeVarMap = new HashMap<String, Type>();
            TypeVariable<? extends Class<?>>[] vars = declaringClass.getTypeParameters();
            for (int i = 0; i < vars.length; i++)
            {
                if (intfTypes != null && i < intfTypes.length)
                {
                    typeVarMap.put(vars[i].getName(), intfTypes[i]);
                }
                else
                {
                    // Interface type parameters may not have been filled out
                    typeVarMap.put(vars[i].getName(), vars[i].getGenericDeclaration());
                }
            }
            Type[] paramGenericTypes = intfMethod.getGenericParameterTypes();
            paramTypes = new Class[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++)
            {
                if (paramGenericTypes[i] instanceof TypeVariable)
                {
                    TypeVariable tv = (TypeVariable)paramGenericTypes[i];
                    Type t = typeVarMap.get(tv.getName());
                    if (t == null)
                    {
                        throw new RuntimeException("Unable to resolve type variable");
                    }
                    paramTypes[i] = getRawType(t);
                }
                else
                {
                    paramTypes[i] = getRawType(paramGenericTypes[i]);
                }
            }

        }

        try
        {
            return clazz.getMethod(intfMethod.getName(), paramTypes);
        }
        catch (NoSuchMethodException e)
        {
        }

        try
        {
            Method tmp = clazz.getMethod(intfMethod.getName(), intfMethod.getParameterTypes());
            return tmp;
        }
        catch (NoSuchMethodException e)
        {

        }
        return intfMethod;
    }


    public static Class<?> getRawType(Type type)
    {
        if (type instanceof Class<?>)
        {
            // type is a normal class.
            return (Class<?>) type;

        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            return (Class<?>) rawType;
        }
        else if (type instanceof GenericArrayType)
        {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            final Class<?> componentRawType = getRawType(genericArrayType.getGenericComponentType());
            return Array.newInstance(componentRawType, 0).getClass();
        }
        else if (type instanceof TypeVariable)
        {
            final TypeVariable typeVar = (TypeVariable) type;
            if (typeVar.getBounds() != null && typeVar.getBounds().length > 0)
            {
                return getRawType(typeVar.getBounds()[0]);
            }
        }
        throw new RuntimeException("unable to determine base class");
    }


    public static Class<?> getRawTypeNoException(Type type)
    {
        if (type instanceof Class<?>)
        {
            // type is a normal class.
            return (Class<?>) type;

        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            return (Class<?>) rawType;
        }
        else if (type instanceof GenericArrayType)
        {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            final Class<?> componentRawType = getRawType(genericArrayType.getGenericComponentType());
            return Array.newInstance(componentRawType, 0).getClass();
        }
        return null;
    }

    /**
     * Returns the type argument from a parameterized type
     *
     * @param genericType
     * @return null if there is no type parameter
     */
    public static Class<?> getTypeArgument(Type genericType)
    {
        if (!(genericType instanceof ParameterizedType)) return null;
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Class<?> typeArg = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        return typeArg;
    }


    public static Class getCollectionBaseType(Class type, Type genericType)
    {
        if (genericType instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type componentGenericType = parameterizedType.getActualTypeArguments()[0];
            return getRawType(componentGenericType);
        }
        else if (genericType instanceof GenericArrayType)
        {
            final GenericArrayType genericArrayType = (GenericArrayType) genericType;
            Type componentGenericType = genericArrayType.getGenericComponentType();
            return getRawType(componentGenericType);
        }
        else if (type.isArray())
        {
            return type.getComponentType();
        }
        return null;
    }


    public static Class getMapKeyType(Type genericType)
    {
        if (genericType instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type componentGenericType = parameterizedType.getActualTypeArguments()[0];
            return getRawType(componentGenericType);
        }
        return null;
    }

    public static Class getMapValueType(Type genericType)
    {
        if (genericType instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type componentGenericType = parameterizedType.getActualTypeArguments()[1];
            return getRawType(componentGenericType);
        }
        return null;
    }

    public static Type resolveTypeVariables(Class<?> root, Type type)
    {
        if (type instanceof TypeVariable)
        {
            Type newType = resolveTypeVariable(root, (TypeVariable)type);
            return (newType == null) ? type : newType;
        }
        else if (type instanceof ParameterizedType)
        {
            final ParameterizedType param = (ParameterizedType)type;
            final Type[] actuals = new Type[param.getActualTypeArguments().length];
            for (int i = 0; i < actuals.length; i++)
            {
                Type newType = resolveTypeVariables(root, param.getActualTypeArguments()[i]);
                actuals[i] = newType == null ? param.getActualTypeArguments()[i] : newType;
            }
            return new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments()
                {
                    return actuals;
                }

                @Override
                public Type getRawType()
                {
                    return param.getRawType();
                }

                @Override
                public Type getOwnerType()
                {
                    return param.getOwnerType();
                }
            };
        }
        else if (type instanceof GenericArrayType)
        {
            GenericArrayType arrayType = (GenericArrayType)type;
            final Type componentType = resolveTypeVariables(root, arrayType.getGenericComponentType());
            if (componentType == null) return type;
            return new GenericArrayType()
            {
                @Override
                public Type getGenericComponentType()
                {
                    return componentType;
                }
            };
        }
        else
        {
            return type;
        }
    }


    /**
     * Finds an actual value of a type variable. The method looks in a class hierarchy for a class defining the variable
     * and returns the value if present.
     *
     * @param root
     * @param typeVariable
     * @return actual type of the type variable
     */
    public static Type resolveTypeVariable(Class<?> root, TypeVariable<?> typeVariable)
    {
        if (typeVariable.getGenericDeclaration() instanceof Class<?>)
        {
            Class<?> classDeclaringTypeVariable = (Class<?>) typeVariable.getGenericDeclaration();
            Type[] types = findParameterizedTypes(root, classDeclaringTypeVariable);
            if (types == null) return  null;
            for (int i = 0; i < types.length; i++)
            {
                TypeVariable<?> tv = classDeclaringTypeVariable.getTypeParameters()[i];
                if (tv.equals(typeVariable))
                {
                    return types[i];
                }
            }
        }
        return null;
    }


    /**
     * Given a class and an interfaces, go through the class hierarchy to find the interface and return its type arguments.
     *
     * @param classToSearch
     * @param interfaceToFind
     * @return type arguments of the interface
     */
    public static Type[] getActualTypeArgumentsOfAnInterface(Class<?> classToSearch, Class<?> interfaceToFind)
    {
        Type[] types = findParameterizedTypes(classToSearch, interfaceToFind);
        if (types == null) throw new RuntimeException("Unable to find type arguments");
        return types;
    }

    private static final Type[] EMPTY_TYPE_ARRAY = {};

    /**
     * Search for the given interface or class within the root's class/interface hierarchy.
     * If the searched for class/interface is a generic return an array of real types that fill it out.
     *
     * @param root
     * @param searchedFor
     * @return
     */
    public static Type[] findParameterizedTypes(Class<?> root, Class<?> searchedFor)
    {
        if (searchedFor.isInterface())
        {
            return findInterfaceParameterizedTypes(root, null, searchedFor);
        }
        return findClassParameterizedTypes(root, null, searchedFor);
    }

    public static Type[] findClassParameterizedTypes(Class<?> root, ParameterizedType rootType, Class<?> searchedForClass)
    {
        if (Object.class.equals(root)) return null;

        Map<String, Type> typeVarMap = populateParameterizedMap(root, rootType);

        Class<?> superclass = root.getSuperclass();
        Type genericSuper = root.getGenericSuperclass();

        if (superclass.equals(searchedForClass))
        {
            return extractTypes(typeVarMap, genericSuper);
        }


        if (genericSuper instanceof ParameterizedType)
        {
            ParameterizedType intfParam = (ParameterizedType) genericSuper;
            Type[] types = findClassParameterizedTypes(superclass, intfParam, searchedForClass);
            if (types != null)
            {
                return extractTypeVariables(typeVarMap, types);
            }
        }
        else
        {
            Type[] types = findClassParameterizedTypes(superclass, null, searchedForClass);
            if (types != null)
            {
                return types;
            }
        }
        return null;
    }

    private static Map<String, Type> populateParameterizedMap(Class<?> root, ParameterizedType rootType)
    {
        Map<String, Type> typeVarMap = new HashMap<String, Type>();
        if (rootType != null)
        {
            TypeVariable<? extends Class<?>>[] vars = root.getTypeParameters();
            for (int i = 0; i < vars.length; i++)
            {
                typeVarMap.put(vars[i].getName(), rootType.getActualTypeArguments()[i]);
            }
        }
        return typeVarMap;
    }


    public static Type[] findInterfaceParameterizedTypes(Class<?> root, ParameterizedType rootType, Class<?> searchedForInterface)
    {
        Map<String, Type> typeVarMap = populateParameterizedMap(root, rootType);

        for (int i = 0; i < root.getInterfaces().length; i++)
        {
            Class<?> sub = root.getInterfaces()[i];
            Type genericSub = root.getGenericInterfaces()[i];
            if (sub.equals(searchedForInterface))
            {
                return extractTypes(typeVarMap, genericSub);
            }
        }

        for (int i = 0; i < root.getInterfaces().length; i++)
        {
            Type genericSub = root.getGenericInterfaces()[i];
            Class<?> sub = root.getInterfaces()[i];

            Type[] types = recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSub, sub);
            if (types != null) return types;
        }
        if (root.isInterface()) return null;

        Class<?> superclass = root.getSuperclass();
        Type genericSuper = root.getGenericSuperclass();


        return recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSuper, superclass);
    }

    private static Type[] recurseSuperclassForInterface(Class<?> searchedForInterface, Map<String, Type> typeVarMap, Type genericSub, Class<?> sub)
    {
        if (genericSub instanceof ParameterizedType)
        {
            ParameterizedType intfParam = (ParameterizedType) genericSub;
            Type[] types = findInterfaceParameterizedTypes(sub, intfParam, searchedForInterface);
            if (types != null)
            {
                return extractTypeVariables(typeVarMap, types);
            }
        }
        else
        {
            Type[] types = findInterfaceParameterizedTypes(sub, null, searchedForInterface);
            if (types != null)
            {
                return types;
            }
        }
        return null;
    }

    private static Type[] extractTypeVariables(Map<String, Type> typeVarMap, Type[] types)
    {
        for (int j = 0; j < types.length; j++)
        {
            if (types[j] instanceof TypeVariable)
            {
                TypeVariable tv = (TypeVariable) types[j];
                types[j] = typeVarMap.get(tv.getName());
            }
            else
            {
                types[j] = types[j];
            }
        }
        return types;
    }

    private static Type[] extractTypes(Map<String, Type> typeVarMap, Type genericSub)
    {
        if (genericSub instanceof ParameterizedType)
        {
            ParameterizedType param = (ParameterizedType) genericSub;
            Type[] types = param.getActualTypeArguments();
            Type[] returnTypes = new Type[types.length];
            System.arraycopy(types, 0, returnTypes, 0, types.length);
            extractTypeVariables(typeVarMap, returnTypes);
            return returnTypes;
        }
        else
        {
            return EMPTY_TYPE_ARRAY;
        }
    }

    /**
     * Grabs the parameterized type of fromInterface
     * that object implements and sees if it is assignable from type.
     *
     * @param type
     * @param object
     * @param fromInterface
     * @param <T>
     * @return
     */
    public static <T> boolean supports(Class<T> type, Object object, Class<?> fromInterface) {
        Type providerType = getActualTypeArgumentsOfAnInterface(object.getClass(), fromInterface)[0];
        Class providerClass = getRawType(providerType);
        return type.isAssignableFrom(providerClass);
    }


}