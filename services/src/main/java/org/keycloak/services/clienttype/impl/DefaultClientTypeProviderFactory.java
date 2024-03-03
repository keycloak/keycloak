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
 *
 */

package org.keycloak.services.clienttype.impl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.client.clienttype.ClientTypeProvider;
import org.keycloak.client.clienttype.ClientTypeProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientTypeProviderFactory implements ClientTypeProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "default";

    private Map<String, PropertyDescriptor> clientRepresentationProperties;

    @Override
    public ClientTypeProvider create(KeycloakSession session) {
        return new DefaultClientTypeProvider(session, clientRepresentationProperties);
    }

    @Override
    public void init(Config.Scope config) {
        Set<String> filtered = Arrays.stream(new String[] {"attributes", "type"}).collect(Collectors.toSet());

        try {
            BeanInfo bi = Introspector.getBeanInfo(ClientRepresentation.class);
            PropertyDescriptor[] pd = bi.getPropertyDescriptors();
            clientRepresentationProperties = Arrays.stream(pd)
                    .filter(desc -> !filtered.contains(desc.getName()))
                    .filter(desc -> desc.getWriteMethod() != null)
                    .map(desc -> {
                        // Take "is" methods into consideration
                        if (desc.getReadMethod() == null && Boolean.class.equals(desc.getPropertyType())) {
                            String methodName = "is" + desc.getName().substring(0, 1).toUpperCase() + desc.getName().substring(1);
                            try {
                                Method getter = ClientRepresentation.class.getDeclaredMethod(methodName);
                                desc.setReadMethod(getter);
                            } catch (Exception e) {
                                throw new IllegalStateException("Getter method for property " + desc.getName() + " cannot be found");
                            }
                        }
                        return desc;
                    })
                    .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity()));
        } catch (IntrospectionException ie) {
            throw new IllegalStateException("Introspection of Client representation failed", ie);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_TYPES);
    }
}