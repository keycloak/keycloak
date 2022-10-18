/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.attributes;

import org.junit.Before;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.attributes.AttributeStoreProvider;
import org.keycloak.storage.attributes.RESTAttributeStoreProviderConfig;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Abstract class for tests for {@link org.keycloak.storage.attributes.RESTAttributeStoreProvider} functionality
 */
public class AbstractRESTAttributeStoreTest extends AbstractAttributeStoreTest {
    protected static final String ATTR_STORE_ID = KeycloakModelUtils.generateId();

    @Before
    public void initialize() {
        super.initialize();
        initAttrLookupRealm();
    }

    /**
     * Helper function to initialize the attribute lookup realm with an instance of the {@link org.keycloak.storage.attributes.RESTAttributeStoreProvider}
     */
    private void initAttrLookupRealm() {
        realmsResouce().realm(ATTR_LOOKUP_REALM).components().add(new ComponentRepresentation() {{
            setConfig(new MultivaluedHashMap<>() {{
                putSingle(RESTAttributeStoreProviderConfig.ProviderConfig.URL, String.format("%s/realms/%s/.well-known/openid-configuration", OAuthClient.AUTH_SERVER_ROOT, ATTR_LOOKUP_REALM));
                putSingle(RESTAttributeStoreProviderConfig.ProviderConfig.METHOD, RESTAttributeStoreProviderConfig.HTTPMethodTypes.GET.name());
                putSingle(RESTAttributeStoreProviderConfig.ProviderConfig.HEADERS, parseConfigMap(Collections.emptyMap()));
                putSingle(RESTAttributeStoreProviderConfig.ProviderConfig.CLIENT_CERTIFICATE, null);
            }});
            setId(ATTR_STORE_ID);
            setName("test-attribute-store");
            setProviderType(AttributeStoreProvider.class.getName());
            setProviderId("rest-attribute-store");
        }});
    }

    /**
     * Helper function to serialize a map as a MAP_TYPE from ({@link org.keycloak.provider.ProviderConfigProperty}
     * @param config the config
     * @return the serialized config 
     */
    protected String parseConfigMap(Map<String, String> config) {
        List<Map<String, String>> transformed = new ArrayList<>();
        config.forEach((k, v) -> transformed.add(new HashMap<>(){{
            put("key", k);
            put("value", v);
        }}));

        try {
            return JsonSerialization.writeValueAsString(transformed);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}