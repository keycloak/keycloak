/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.saml.SamlProtocolFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pascal Knüppel
 */
public class ProtocolFactoryTest {

    /**
     * this test makes sure that the LoginProtocolFactories represent the expected order in the UI for selection. The
     * order-value is used to sort the protocol-selections in the drop-down boxes in the UI and openid-connect should be
     * always set as default
     */
    @Test
    public void testOrderOfLoginProtocolFactories() {
        Iterator<LoginProtocolFactory> iterator = ServiceLoader.load(LoginProtocolFactory.class).iterator();

        Map<Integer, LoginProtocolFactory> factories = new LinkedHashMap<>();
        int numberOfImplementations = 0;
        LoginProtocolFactory factory;
        while (iterator.hasNext()) {
            factory = iterator.next();
            factories.put(factory.order(), factory);
            numberOfImplementations++;
        }
        Assert.assertEquals("No two LoginProtocolFactories must have the same order number",
                            numberOfImplementations, factories.size());
        Assert.assertEquals("The OIDCLoginProtocolFactory should always come first!",
                            OIDCLoginProtocolFactory.class,
                            factories.get(OIDCLoginProtocolFactory.UI_ORDER).getClass());
        Assert.assertEquals(SamlProtocolFactory.class,
                            factories.get(OIDCLoginProtocolFactory.UI_ORDER - 10).getClass());
        Assert.assertEquals(OID4VCLoginProtocolFactory.class,
                            factories.get(OIDCLoginProtocolFactory.UI_ORDER - 20).getClass());
    }
}
