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

    private static final List<String> ALIASES = List.of("wunder.io");

    @Test
    public void testAliasDomainIsRewrittenToCanonical() {
        assertEquals("olli.erinko@mearra.com", EmailDomainAliasMapper.canonicalize("olli.erinko@wunder.io", "mearra.com", ALIASES));
        assertEquals("Olli.Erinko@mearra.com", EmailDomainAliasMapper.canonicalize("Olli.Erinko@Wunder.IO", "mearra.com", ALIASES));
    }

    @Test
    public void testOtherAddressesAreUnchanged() {
        assertEquals("olli.erinko@mearra.com", EmailDomainAliasMapper.canonicalize("olli.erinko@mearra.com", "mearra.com", ALIASES));
        assertEquals("olli@example.com", EmailDomainAliasMapper.canonicalize("olli@example.com", "mearra.com", ALIASES));
        assertEquals("a@notwunder.io", EmailDomainAliasMapper.canonicalize("a@notwunder.io", "mearra.com", ALIASES));
        assertEquals("a@sub.wunder.io", EmailDomainAliasMapper.canonicalize("a@sub.wunder.io", "mearra.com", ALIASES));
        assertEquals("not-an-email", EmailDomainAliasMapper.canonicalize("not-an-email", "mearra.com", ALIASES));
        assertNull(EmailDomainAliasMapper.canonicalize(null, "mearra.com", ALIASES));
    }

    @Test
    public void testNoCanonicalDomainMeansNoRewrite() {
        assertEquals("olli.erinko@wunder.io", EmailDomainAliasMapper.canonicalize("olli.erinko@wunder.io", " ", ALIASES));
        assertEquals("olli.erinko@wunder.io", EmailDomainAliasMapper.canonicalize("olli.erinko@wunder.io", null, ALIASES));
    }

    @Test
    public void testPreprocessRewritesContextEmail() {
        Map<String, String> config = new HashMap<>();
        config.put(EmailDomainAliasMapper.CANONICAL_DOMAIN, "mearra.com");
        config.put(EmailDomainAliasMapper.ALIAS_DOMAINS, "wunder.io, other.example");
        IdentityProviderMapperModel mapperModel = new IdentityProviderMapperModel();
        mapperModel.setConfig(config);

        IdentityProviderModel idp = new IdentityProviderModel();
        idp.setEnabled(true);
        BrokeredIdentityContext context = new BrokeredIdentityContext("id", idp);
        context.setEmail("olli.erinko@wunder.io");
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertEquals("olli.erinko@mearra.com", context.getEmail());

        context.setEmail("x@other.example");
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertEquals("x@mearra.com", context.getEmail());

        context.setEmail(null);
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertNull(context.getEmail());
    }
}
