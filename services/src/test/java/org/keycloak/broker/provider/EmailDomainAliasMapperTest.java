/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.broker.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test for {@link EmailDomainAliasMapper}.
 */
public class EmailDomainAliasMapperTest {

    private static final List<String> ALIASES = List.of("example.org");

    @Test
    public void testAliasDomainIsRewrittenToCanonical() {
        assertEquals("jane.doe@example.com", EmailDomainAliasMapper.canonicalize("jane.doe@example.org", "example.com", ALIASES));
        assertEquals("Jane.Doe@example.com", EmailDomainAliasMapper.canonicalize("Jane.Doe@Example.ORG", "example.com", ALIASES));
    }

    @Test
    public void testOtherAddressesAreUnchanged() {
        assertEquals("jane.doe@example.com", EmailDomainAliasMapper.canonicalize("jane.doe@example.com", "example.com", ALIASES));
        assertEquals("jane@personal.example", EmailDomainAliasMapper.canonicalize("jane@personal.example", "example.com", ALIASES));
        assertEquals("a@notexample.org", EmailDomainAliasMapper.canonicalize("a@notexample.org", "example.com", ALIASES));
        assertEquals("a@sub.example.org", EmailDomainAliasMapper.canonicalize("a@sub.example.org", "example.com", ALIASES));
        assertEquals("not-an-email", EmailDomainAliasMapper.canonicalize("not-an-email", "example.com", ALIASES));
        assertNull(EmailDomainAliasMapper.canonicalize(null, "example.com", ALIASES));
    }

    @Test
    public void testNoCanonicalDomainMeansNoRewrite() {
        assertEquals("jane.doe@example.org", EmailDomainAliasMapper.canonicalize("jane.doe@example.org", " ", ALIASES));
        assertEquals("jane.doe@example.org", EmailDomainAliasMapper.canonicalize("jane.doe@example.org", null, ALIASES));
    }

    @Test
    public void testPreprocessRewritesContextEmail() {
        Map<String, String> config = new HashMap<>();
        config.put(EmailDomainAliasMapper.CANONICAL_DOMAIN, "example.com");
        config.put(EmailDomainAliasMapper.ALIAS_DOMAINS, "example.org, other.example");
        IdentityProviderMapperModel mapperModel = new IdentityProviderMapperModel();
        mapperModel.setConfig(config);

        IdentityProviderModel idp = new IdentityProviderModel();
        idp.setEnabled(true);
        BrokeredIdentityContext context = new BrokeredIdentityContext("id", idp);
        context.setEmail("jane.doe@example.org");
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertEquals("jane.doe@example.com", context.getEmail());

        context.setEmail("x@other.example");
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertEquals("x@example.com", context.getEmail());

        context.setEmail(null);
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertNull(context.getEmail());
    }
}
