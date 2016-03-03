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
package org.keycloak.subsystem.as7;


import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class RealmDefinitionTestCase {

    private ModelNode model;

    @Before
    public void setUp() {
        model = new ModelNode();
        model.get("realm").set("demo");
        model.get("resource").set("customer-portal");
        model.get("realm-public-key").set("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB");
        model.get("auth-url").set("http://localhost:8080/auth-server/realms/demo/protocol/openid-connect/login");
        model.get("code-url").set("http://localhost:8080/auth-server/realms/demo/protocol/openid-connect/access/codes");
        model.get("expose-token").set(true);
        ModelNode credential = new ModelNode();
        credential.get("password").set("password");
        model.get("credentials").set(credential);
    }

    @Test
    public void testIsTruststoreSetIfRequired() throws Exception {
        model.get("ssl-required").set("none");
        model.get("disable-trust-manager").set(true);
        Assert.assertTrue(SharedAttributeDefinitons.validateTruststoreSetIfRequired(model));

        model.get("ssl-required").set("none");
        model.get("disable-trust-manager").set(false);
        Assert.assertTrue(SharedAttributeDefinitons.validateTruststoreSetIfRequired(model));

        model.get("ssl-required").set("all");
        model.get("disable-trust-manager").set(true);
        Assert.assertTrue(SharedAttributeDefinitons.validateTruststoreSetIfRequired(model));

        model.get("ssl-required").set("all");
        model.get("disable-trust-manager").set(false);
        Assert.assertFalse(SharedAttributeDefinitons.validateTruststoreSetIfRequired(model));

        model.get("ssl-required").set("external");
        model.get("disable-trust-manager").set(false);
        Assert.assertFalse(SharedAttributeDefinitons.validateTruststoreSetIfRequired(model));

        model.get("ssl-required").set("all");
        model.get("disable-trust-manager").set(false);
        model.get("truststore").set("foo");
        Assert.assertFalse(SharedAttributeDefinitons.validateTruststoreSetIfRequired(model));

        model.get("ssl-required").set("all");
        model.get("disable-trust-manager").set(false);
        model.get("truststore").set("foo");
        model.get("truststore-password").set("password");
        Assert.assertTrue(SharedAttributeDefinitons.validateTruststoreSetIfRequired(model));

        model.get("ssl-required").set("external");
        model.get("disable-trust-manager").set(false);
        model.get("truststore").set("foo");
        model.get("truststore-password").set("password");
        Assert.assertTrue(SharedAttributeDefinitons.validateTruststoreSetIfRequired(model));
    }

}
